package com.example.network

import com.example.model.DealItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class EpicApiService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {

    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private val isoDateFormatFallback = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    suspend fun fetchGiveaways(): List<DealItem> = withContext(Dispatchers.IO) {
        val url = "https://store-site-backend-static.ak.epicgames.com/freeGamesPromotions?locale=uk-UA&country=UA&allowCountries=UA&_t=${System.currentTimeMillis()}"

        try {
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext emptyList()

            val json = JSONObject(body)
            val dataObj = json.optJSONObject("data") ?: return@withContext emptyList()
            val catalogObj = dataObj.optJSONObject("Catalog") ?: return@withContext emptyList()
            val searchStoreObj = catalogObj.optJSONObject("searchStore") ?: return@withContext emptyList()
            val elements = searchStoreObj.optJSONArray("elements") ?: return@withContext emptyList()

            val now = System.currentTimeMillis()
            val results = mutableListOf<DealItem>()

            for (i in 0 until elements.length()) {
                val item = elements.optJSONObject(i) ?: continue
                val title = item.optString("title", "Free Game")
                val promotions = item.optJSONObject("promotions") ?: continue

                // Check active promotions first
                var isActiveFree = false
                var isUpcoming = false
                var offerStartDate: Long? = null
                var offerEndDate: Long? = null

                val promoOffers = promotions.optJSONArray("promotionalOffers")
                if (promoOffers != null && promoOffers.length() > 0) {
                    val group = promoOffers.optJSONObject(0)
                    val offers = group?.optJSONArray("promotionalOffers")
                    if (offers != null && offers.length() > 0) {
                        val offer = offers.optJSONObject(0)
                        if (offer != null) {
                            val startStr = offer.optString("startDate", "")
                            val endStr = offer.optString("endDate", "")
                            val startTs = parseIsoDate(startStr)
                            val endTs = parseIsoDate(endStr)

                            val discountPrice = item.optJSONObject("price")
                                ?.optJSONObject("totalPrice")
                                ?.optInt("discountPrice", 0) ?: 0

                            if (startTs != null && endTs != null && now in startTs..endTs && discountPrice == 0) {
                                isActiveFree = true
                                offerStartDate = startTs
                                offerEndDate = endTs
                            }
                        }
                    }
                }

                // If not active free, check upcoming free promotions
                if (!isActiveFree) {
                    val upcomingOffers = promotions.optJSONArray("upcomingPromotionalOffers")
                    if (upcomingOffers != null && upcomingOffers.length() > 0) {
                        val group = upcomingOffers.optJSONObject(0)
                        val offers = group?.optJSONArray("promotionalOffers")
                        if (offers != null && offers.length() > 0) {
                            val offer = offers.optJSONObject(0)
                            if (offer != null) {
                                val startStr = offer.optString("startDate", "")
                                val endStr = offer.optString("endDate", "")
                                val startTs = parseIsoDate(startStr)
                                val endTs = parseIsoDate(endStr)

                                if (startTs != null && startTs > now) {
                                    isUpcoming = true
                                    offerStartDate = startTs
                                    offerEndDate = endTs
                                }
                            }
                        }
                    }
                }

                if (isActiveFree || isUpcoming) {
                    // Extract all valid images in prioritized order
                    val candidateList = mutableListOf<String>()
                    val keyImages = item.optJSONArray("keyImages")
                    if (keyImages != null) {
                        val imagesByType = mutableMapOf<String, String>()
                        for (k in 0 until keyImages.length()) {
                            val img = keyImages.optJSONObject(k) ?: continue
                            val type = img.optString("type", "")
                            val rawU = img.optString("url", "").trim()
                            if (rawU.isNotEmpty() && !rawU.startsWith("com.epicgames.video")) {
                                var u = rawU
                                if (u.startsWith("//")) u = "https:$u"
                                else if (u.startsWith("http://")) u = "https://" + u.removePrefix("http://")
                                u = u.replace(" ", "%20")
                                imagesByType[type] = u
                            }
                        }

                        val preferredTypes = listOf(
                            "OfferImageWide",
                            "DieselStoreFrontWide",
                            "featuredMedia",
                            "VaultClosed",
                            "OfferImageTall",
                            "DieselStoreFrontTall",
                            "Thumbnail",
                            "GalleryImage",
                            "OgImage"
                        )

                        for (pref in preferredTypes) {
                            val found = imagesByType[pref]
                            if (!found.isNullOrBlank() && !candidateList.contains(found)) {
                                candidateList.add(found)
                            }
                        }

                        for (u in imagesByType.values) {
                            if (!candidateList.contains(u)) {
                                candidateList.add(u)
                            }
                        }
                    }

                    val imageUrl = candidateList.joinToString("|")

                    // Extract valid public product slug, avoiding internal 32-char hex GUIDs
                    val slug = extractProductSlug(item)

                    val storeUrl = if (slug.isNotEmpty()) {
                        "https://store.epicgames.com/p/$slug"
                    } else {
                        "https://store.epicgames.com/free-games"
                    }

                    val originalPriceCents = item.optJSONObject("price")
                        ?.optJSONObject("totalPrice")
                        ?.optLong("originalPrice", 0L) ?: 0L

                    results.add(
                        DealItem(
                            id = "epic_${item.optString("id", title.hashCode().toString())}",
                            store = "epic",
                            title = title,
                            imageUrl = imageUrl,
                            storeUrl = storeUrl,
                            discountPercent = 100,
                            initialPrice = originalPriceCents,
                            finalPrice = 0L,
                            currency = "UAH",
                            formattedFinalPrice = if (isUpcoming) "Скоро безкоштовно" else "БЕЗКОШТОВНО",
                            formattedInitialPrice = if (originalPriceCents > 0) "${originalPriceCents / 100} грн" else "",
                            isFree = true,
                            isUpcoming = isUpcoming,
                            startDate = offerStartDate,
                            endDate = offerEndDate
                        )
                    )
                }
            }
            results
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Extracts the public, human-readable product slug for Epic Games Store items.
     * Rejects 32-character internal hex hash GUIDs (like 3f18c54a6e00424ba6e8f4422167cac7) which cause 404 errors.
     */
    fun extractProductSlug(item: JSONObject): String {
        // 1. Check offerMappings for pageSlug (prioritize pageType == "productHome")
        val offerMappings = item.optJSONArray("offerMappings")
        if (offerMappings != null && offerMappings.length() > 0) {
            for (m in 0 until offerMappings.length()) {
                val mapping = offerMappings.optJSONObject(m) ?: continue
                val pageSlug = mapping.optString("pageSlug", "").trim()
                val pageType = mapping.optString("pageType", "").trim()
                if (pageSlug.isNotEmpty() && pageType == "productHome" && !is32HexHash(pageSlug)) {
                    return cleanSlug(pageSlug)
                }
            }
            for (m in 0 until offerMappings.length()) {
                val mapping = offerMappings.optJSONObject(m) ?: continue
                val pageSlug = mapping.optString("pageSlug", "").trim()
                if (pageSlug.isNotEmpty() && !is32HexHash(pageSlug)) {
                    return cleanSlug(pageSlug)
                }
            }
        }

        // 2. Check catalogNs mappings
        val catalogNs = item.optJSONObject("catalogNs")
        val catMappings = catalogNs?.optJSONArray("mappings")
        if (catMappings != null && catMappings.length() > 0) {
            for (m in 0 until catMappings.length()) {
                val mapping = catMappings.optJSONObject(m) ?: continue
                val pageSlug = mapping.optString("pageSlug", "").trim()
                val pageType = mapping.optString("pageType", "").trim()
                if (pageSlug.isNotEmpty() && pageType == "productHome" && !is32HexHash(pageSlug)) {
                    return cleanSlug(pageSlug)
                }
            }
            for (m in 0 until catMappings.length()) {
                val mapping = catMappings.optJSONObject(m) ?: continue
                val pageSlug = mapping.optString("pageSlug", "").trim()
                if (pageSlug.isNotEmpty() && !is32HexHash(pageSlug)) {
                    return cleanSlug(pageSlug)
                }
            }
        }

        // 3. Check customAttributes for "com.epicgames.app.productSlug"
        val customAttributes = item.optJSONArray("customAttributes")
        if (customAttributes != null && customAttributes.length() > 0) {
            for (a in 0 until customAttributes.length()) {
                val attr = customAttributes.optJSONObject(a) ?: continue
                val key = attr.optString("key", "")
                val value = attr.optString("value", "").trim()
                if (key == "com.epicgames.app.productSlug" && value.isNotEmpty() && !is32HexHash(value)) {
                    return cleanSlug(value)
                }
            }
        }

        // 4. Check productSlug
        val productSlug = item.optString("productSlug", "").trim()
        if (productSlug.isNotEmpty() && productSlug != "[]" && !is32HexHash(productSlug)) {
            return cleanSlug(productSlug)
        }

        // 5. Check urlSlug only if not a 32-char hex hash
        val urlSlug = item.optString("urlSlug", "").trim()
        if (urlSlug.isNotEmpty() && !is32HexHash(urlSlug)) {
            return cleanSlug(urlSlug)
        }

        return ""
    }

    private fun is32HexHash(slug: String): Boolean {
        val cleaned = slug.removePrefix("/").removeSuffix("/")
        return cleaned.matches(Regex("^[a-f0-9]{32}$")) || cleaned.matches(Regex("^[A-F0-9]{32}$"))
    }

    private fun cleanSlug(slug: String): String {
        var result = slug.trim()
        if (result.endsWith("/home")) {
            result = result.substringBeforeLast("/home")
        }
        return result.removePrefix("/").removeSuffix("/")
    }

    private fun parseIsoDate(dateStr: String): Long? {
        if (dateStr.isBlank()) return null
        return try {
            isoDateFormat.parse(dateStr)?.time
        } catch (e: Exception) {
            try {
                isoDateFormatFallback.parse(dateStr)?.time
            } catch (e2: Exception) {
                null
            }
        }
    }
}
