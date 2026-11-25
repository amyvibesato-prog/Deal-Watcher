package com.example.network

import com.example.model.DealItem
import com.example.model.SteamPriceSearchResult
import com.example.model.SteamSearchSortOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.StringReader
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import javax.xml.parsers.DocumentBuilderFactory

data class SteamConnectionStatus(
    val success: Boolean,
    val steam64Id: String? = null,
    val personaName: String? = null,
    val totalWishlistGames: Int = 0,
    val discountedGamesCount: Int = 0,
    val errorCode: String? = null,
    val message: String
)

data class ResolvedSteamProfile(
    val steam64Id: String,
    val personaName: String? = null,
    val isPublic: Boolean? = null,
    val avatarUrl: String? = null
)

class SteamApiService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) {

    companion object {
        // Base constant to convert 32-bit Steam Account ID to 64-bit Steam ID
        private const val STEAM64_BASE = 76561197960265728L
    }

    /**
     * Resolves user input into a valid 64-bit numeric Steam ID.
     * Sanitizes inputs (strips spaces, hyphens, prefixes like '#', 'id:', etc.).
     * Supports:
     * - 17-digit Steam64 ID (e.g. 76561198012345678)
     * - 32-bit Friend Code / Account ID (e.g. 12345678 or 123-456-789 or 123 456 789)
     * - Profile URL (https://steamcommunity.com/profiles/76561198012345678/)
     * - Vanity URL (https://steamcommunity.com/id/username/)
     * - Vanity nickname directly (e.g. "username")
     * - SteamID standard formats (STEAM_0:0:12345, [U:1:12345678])
     */
    suspend fun resolveSteamTarget(input: String, apiKey: String = ""): ResolvedSteamProfile? = withContext(Dispatchers.IO) {
        var raw = input.trim()
        if (raw.isEmpty()) return@withContext null

        // Remove common user prefixes like "id:", "steamid:", "steam:", "#"
        raw = raw.removePrefix("id:").removePrefix("ID:").removePrefix("steamid:").removePrefix("SteamID:").removePrefix("#").trim()

        // 1. Check for full profile URL: steamcommunity.com/profiles/(\d+)
        val profileUrlMatch = Pattern.compile("steamcommunity\\.com/profiles/(\\d+)").matcher(raw)
        if (profileUrlMatch.find()) {
            val id = profileUrlMatch.group(1) ?: raw
            val profileInfo = fetchCommunityXmlProfile(id)
            return@withContext ResolvedSteamProfile(
                steam64Id = id,
                personaName = profileInfo?.personaName,
                isPublic = profileInfo?.isPublic,
                avatarUrl = profileInfo?.avatarUrl
            )
        }

        // 2. Check for custom vanity URL: steamcommunity.com/id/([^/?&#\s]+)
        val vanityUrlMatch = Pattern.compile("steamcommunity\\.com/id/([^/?&#\\s]+)").matcher(raw)
        if (vanityUrlMatch.find()) {
            val vanityName = vanityUrlMatch.group(1) ?: raw
            return@withContext resolveVanityToSteam64(vanityName, apiKey)
        }

        // 3. SteamID3 format: [U:1:12345678]
        val steamId3Match = Pattern.compile("\\[U:1:(\\d+)\\]").matcher(raw)
        if (steamId3Match.find()) {
            val accountId = steamId3Match.group(1)?.toLongOrNull()
            if (accountId != null) {
                val steam64 = (STEAM64_BASE + accountId).toString()
                val profileInfo = fetchCommunityXmlProfile(steam64)
                return@withContext ResolvedSteamProfile(
                    steam64Id = steam64,
                    personaName = profileInfo?.personaName,
                    isPublic = profileInfo?.isPublic,
                    avatarUrl = profileInfo?.avatarUrl
                )
            }
        }

        // 4. SteamID2 format: STEAM_0:0:12345 or STEAM_0:1:12345
        val steamId2Match = Pattern.compile("STEAM_[0-1]:([0-1]):(\\d+)").matcher(raw)
        if (steamId2Match.find()) {
            val authServer = steamId2Match.group(1)?.toLongOrNull() ?: 0L
            val authId = steamId2Match.group(2)?.toLongOrNull() ?: 0L
            val accountId = authId * 2 + authServer
            val steam64 = (STEAM64_BASE + accountId).toString()
            val profileInfo = fetchCommunityXmlProfile(steam64)
            return@withContext ResolvedSteamProfile(
                steam64Id = steam64,
                personaName = profileInfo?.personaName,
                isPublic = profileInfo?.isPublic,
                avatarUrl = profileInfo?.avatarUrl
            )
        }

        // Clean out whitespace and hyphens for digit-based checks (e.g. "123 456 789" or "123-456-789")
        val cleanDigits = raw.replace(" ", "").replace("-", "")

        // 5. 17-digit Steam64 numeric string (starts with 7656119...)
        if (cleanDigits.matches(Regex("^7656119\\d{10}$"))) {
            val profileInfo = fetchCommunityXmlProfile(cleanDigits)
            return@withContext ResolvedSteamProfile(
                steam64Id = cleanDigits,
                personaName = profileInfo?.personaName,
                isPublic = profileInfo?.isPublic,
                avatarUrl = profileInfo?.avatarUrl
            )
        }

        // 6. Steam Friend Code / 32-bit Account ID (5 to 10 digits)
        if (cleanDigits.matches(Regex("^\\d{5,10}$"))) {
            val accountId = cleanDigits.toLongOrNull()
            if (accountId != null && accountId < STEAM64_BASE) {
                val steam64 = (STEAM64_BASE + accountId).toString()
                val profileInfo = fetchCommunityXmlProfile(steam64)
                return@withContext ResolvedSteamProfile(
                    steam64Id = steam64,
                    personaName = profileInfo?.personaName,
                    isPublic = profileInfo?.isPublic,
                    avatarUrl = profileInfo?.avatarUrl
                )
            }
        }

        // 7. Any numeric 15-20 digits (Steam64 format)
        if (cleanDigits.matches(Regex("^\\d{15,20}$"))) {
            val profileInfo = fetchCommunityXmlProfile(cleanDigits)
            return@withContext ResolvedSteamProfile(
                steam64Id = cleanDigits,
                personaName = profileInfo?.personaName,
                isPublic = profileInfo?.isPublic,
                avatarUrl = profileInfo?.avatarUrl
            )
        }

        // 8. Otherwise treat as a vanity custom nickname
        return@withContext resolveVanityToSteam64(raw, apiKey)
    }

    private fun resolveVanityToSteam64(vanity: String, apiKey: String): ResolvedSteamProfile? {
        val cleanVanity = vanity.trim().removeSuffix("/").substringAfterLast("/")
        if (cleanVanity.isBlank()) return null

        // Method A: Steam Web API ResolveVanityURL if API key is available
        if (apiKey.isNotBlank()) {
            try {
                val url = "https://api.steampowered.com/ISteamUser/ResolveVanityURL/v1/?key=$apiKey&vanityurl=$cleanVanity"
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    val json = JSONObject(body)
                    val respObj = json.optJSONObject("response")
                    if (respObj?.optInt("success", 0) == 1) {
                        val steam64 = respObj.optString("steamid", "")
                        if (steam64.isNotBlank()) {
                            val profileInfo = fetchCommunityXmlProfile(steam64)
                            return ResolvedSteamProfile(
                                steam64Id = steam64,
                                personaName = profileInfo?.personaName,
                                isPublic = profileInfo?.isPublic,
                                avatarUrl = profileInfo?.avatarUrl
                            )
                        }
                    }
                }
            } catch (_: Exception) {
                // Fall through to Method B
            }
        }

        // Method B: Public Steam community XML endpoint
        val communityProfile = fetchCommunityXmlProfile(cleanVanity, isVanity = true)
        if (communityProfile != null) {
            return communityProfile
        }

        return null
    }

    private fun fetchCommunityXmlProfile(target: String, isVanity: Boolean = false): ResolvedSteamProfile? {
        val url = if (isVanity) {
            "https://steamcommunity.com/id/$target/?xml=1"
        } else {
            "https://steamcommunity.com/profiles/$target/?xml=1"
        }
        try {
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return null
            if (!response.isSuccessful || !body.contains("<profile>")) return null

            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(org.xml.sax.InputSource(StringReader(body)))

            val steamId64Node = doc.getElementsByTagName("steamID64").item(0)
            val steam64 = steamId64Node?.textContent?.trim() ?: if (!isVanity) target else return null

            val steamIdNode = doc.getElementsByTagName("steamID").item(0)
            val personaName = steamIdNode?.textContent?.trim()

            val privacyNode = doc.getElementsByTagName("privacyState").item(0)
            val privacy = privacyNode?.textContent?.trim()
            val isPublic = privacy.equals("public", ignoreCase = true)

            val avatarNode = doc.getElementsByTagName("avatarMedium").item(0)
            val avatarUrl = avatarNode?.textContent?.trim()

            return ResolvedSteamProfile(
                steam64Id = steam64,
                personaName = personaName,
                isPublic = isPublic,
                avatarUrl = avatarUrl
            )
        } catch (_: Exception) {
            return null
        }
    }

    /**
     * Test and diagnose the Steam Wishlist connection with clear, actionable diagnostics.
     */
    suspend fun testSteamConnection(rawSteamId: String, apiKey: String): SteamConnectionStatus = withContext(Dispatchers.IO) {
        val trimmed = rawSteamId.trim()
        if (trimmed.isEmpty()) {
            return@withContext SteamConnectionStatus(
                success = false,
                errorCode = "MISSING_ID",
                message = "Введіть ваш числовий Код друга зі Steam або посилання на профіль."
            )
        }

        // Check if user accidentally entered an email
        if (trimmed.contains("@")) {
            return@withContext SteamConnectionStatus(
                success = false,
                errorCode = "EMAIL_ENTERED",
                message = "Електронна пошта не є Steam ID.\n\n👉 Будь ласка, введіть числовий «Код друга» (8-9 цифр, наприклад: 123456789), який можна знайти в Steam у меню: Друзі → Додати друга."
            )
        }

        // 1. Resolve ID
        val profile = resolveSteamTarget(trimmed, apiKey)
        if (profile == null) {
            val looksLikeLoginName = !trimmed.all { it.isDigit() || it == '-' || it == ' ' } && !trimmed.contains("/")
            val errorMessage = if (looksLikeLoginName) {
                "Акаунт за ім'ям «$trimmed» не знайдено.\n\n⚠️ Зверніть увагу: Ваш логін (який ви вводите для входу в Steam з паролем) є конфіденційним і НЕ є Steam ID.\n\n👉 Як знайти свій код:\nУ додатку Steam відкрийте «Друзі» → «Додати друга» → скопіюйте ваш числовий «Код друга» (8-9 цифр, наприклад: 123456789) та вставте його сюди."
            } else {
                "Не вдалося знайти акаунт Steam за значенням «$trimmed».\n\n👉 Будь ласка, скопіюйте числовий «Код друга» (8-9 цифр) у Steam: меню «Друзі» → «Додати друга»."
            }
            return@withContext SteamConnectionStatus(
                success = false,
                errorCode = "INVALID_ID",
                message = errorMessage
            )
        }

        val steam64Id = profile.steam64Id
        val persona = profile.personaName ?: "Steam ID: $steam64Id"

        // 2. Check API key requirement
        if (apiKey.isBlank()) {
            return@withContext SteamConnectionStatus(
                success = false,
                steam64Id = steam64Id,
                personaName = persona,
                errorCode = "MISSING_API_KEY",
                message = "✅ Знайдено акаунт: $persona (Steam ID: $steam64Id)!\n\n🔑 Залишився 1 крок: Введіть Steam Web API Key нижче, щоб завантажити ігри зі списку бажаного.\n\n(Натисніть кнопку «Отримати ключ» нижче — це безкоштовно та займає 1 хвилину)."
            )
        }

        // 3. Test IWishlistService API call
        val wishlistUrl = "https://api.steampowered.com/IWishlistService/GetWishlist/v1/?key=${apiKey}&steamid=${steam64Id}"
        try {
            val request = Request.Builder().url(wishlistUrl).build()
            val response = client.newCall(request).execute()
            val xEresult = response.header("x-eresult")
            val body = response.body?.string() ?: ""

            if (xEresult == "15" || response.code == 403) {
                return@withContext SteamConnectionStatus(
                    success = false,
                    steam64Id = steam64Id,
                    personaName = persona,
                    errorCode = "ACCESS_DENIED",
                    message = "Акаунт: $persona (ID: $steam64Id).\n\n⚠️ Доступ заборонено (Код 15): Перевірте правильність Steam API Key або переконайтесь, що «Деталі про ігри» у Налаштуваннях приватності Steam встановлено як «Загальнодоступні» (Public)."
                )
            }

            if (!response.isSuccessful) {
                return@withContext SteamConnectionStatus(
                    success = false,
                    steam64Id = steam64Id,
                    personaName = persona,
                    errorCode = "HTTP_ERROR",
                    message = "Помилка сервера Steam: HTTP ${response.code}. Спробуйте знову через кілька секунд."
                )
            }

            val json = JSONObject(body)
            val responseObj = json.optJSONObject("response")
            val items = responseObj?.optJSONArray("items")

            if (items == null || items.length() == 0) {
                if (profile.isPublic == false) {
                    return@withContext SteamConnectionStatus(
                        success = false,
                        steam64Id = steam64Id,
                        personaName = persona,
                        errorCode = "PROFILE_PRIVATE",
                        message = "Акаунт: $persona (ID: $steam64Id).\n\n⚠️ Ваш список ігор у Steam є приватним. Увійдіть у Steam → Редагувати профіль → Налаштування приватності → встановіть «Деталі про ігри» на «Загальнодоступні»."
                    )
                }
                return@withContext SteamConnectionStatus(
                    success = true,
                    steam64Id = steam64Id,
                    personaName = persona,
                    totalWishlistGames = 0,
                    discountedGamesCount = 0,
                    message = "✅ Підключено успішно!\nАкаунт: $persona (Steam ID: $steam64Id).\nУ вашому списку бажаного наразі немає ігор."
                )
            }

            val totalCount = items.length()
            return@withContext SteamConnectionStatus(
                success = true,
                steam64Id = steam64Id,
                personaName = persona,
                totalWishlistGames = totalCount,
                message = "✅ Успішно підключено!\nАкаунт: $persona (Steam ID: $steam64Id)\nЗнайдено ігор у вішлісті: $totalCount."
            )
        } catch (e: Exception) {
            return@withContext SteamConnectionStatus(
                success = false,
                steam64Id = steam64Id,
                personaName = persona,
                errorCode = "NETWORK_ERROR",
                message = "Помилка зв'язку: ${e.localizedMessage ?: "Перевірте підключення до інтернету"}"
            )
        }
    }

    suspend fun fetchWishlistDeals(
        rawSteamId: String,
        apiKey: String,
        currencyCode: String = "UA"
    ): List<DealItem> = withContext(Dispatchers.IO) {
        val trimmed = rawSteamId.trim()
        if (trimmed.isEmpty()) return@withContext emptyList()

        val profile = resolveSteamTarget(trimmed, apiKey) ?: return@withContext emptyList()
        val steam64Id = profile.steam64Id

        // If API key is provided, use the official Steam Web API
        if (apiKey.isNotBlank()) {
            val webApiDeals = fetchViaWebApi(steam64Id, apiKey, currencyCode)
            if (webApiDeals.isNotEmpty()) {
                return@withContext webApiDeals
            }
        } else {
            // Attempt query without key
            val noKeyDeals = fetchViaWebApi(steam64Id, "", currencyCode)
            if (noKeyDeals.isNotEmpty()) {
                return@withContext noKeyDeals
            }
        }

        return@withContext emptyList()
    }

    private suspend fun fetchViaWebApi(steamId: String, apiKey: String, currencyCode: String): List<DealItem> = coroutineScope {
        val wishlistUrl = if (apiKey.isNotBlank()) {
            "https://api.steampowered.com/IWishlistService/GetWishlist/v1/?key=${apiKey}&steamid=${steamId}"
        } else {
            "https://api.steampowered.com/IWishlistService/GetWishlist/v1/?steamid=${steamId}"
        }

        val request = Request.Builder().url(wishlistUrl).build()

        try {
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@coroutineScope emptyList()
            val json = JSONObject(body)
            val responseObj = json.optJSONObject("response") ?: return@coroutineScope emptyList()
            val items = responseObj.optJSONArray("items") ?: return@coroutineScope emptyList()

            val appIds = mutableListOf<String>()
            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                val appidLong = item.optLong("appid", 0L)
                val appidStr = if (appidLong > 0) appidLong.toString() else item.optString("appid", "")
                if (appidStr.isNotBlank()) {
                    appIds.add(appidStr)
                }
            }

            if (appIds.isEmpty()) return@coroutineScope emptyList()

            val semaphore = Semaphore(3)
            val deferreds = appIds.map { appid ->
                async(Dispatchers.IO) {
                    semaphore.withPermit {
                        delay(60)
                        fetchWishlistSingleAppDetails(appid, currencyCode)
                    }
                }
            }

            deferreds.awaitAll().filterNotNull()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun fetchWishlistSingleAppDetails(appid: String, currencyCode: String): DealItem? {
        val url = "https://store.steampowered.com/api/appdetails?appids=$appid&cc=$currencyCode&l=uk&filters=price_overview,basic"
        try {
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return null
            val json = JSONObject(body)
            val appObj = json.optJSONObject(appid) ?: return null

            if (!appObj.optBoolean("success", false)) return null
            val data = appObj.optJSONObject("data") ?: return null

            val name = data.optString("name", "Steam Game")
            val isFreeGame = data.optBoolean("is_free", false)
            val headerImage = data.optString("header_image", "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/$appid/header.jpg")
            val priceOverview = data.optJSONObject("price_overview")

            var discountPercent = 0
            var finalPrice = 0L
            var initialPrice = 0L
            var currency = "UAH"
            var finalFormatted = ""
            var initialFormatted = ""

            if (priceOverview != null) {
                discountPercent = priceOverview.optInt("discount_percent", 0)
                finalPrice = priceOverview.optLong("final", 0L)
                initialPrice = priceOverview.optLong("initial", finalPrice)
                currency = priceOverview.optString("currency", "UAH")
                finalFormatted = priceOverview.optString("final_formatted", "")
                initialFormatted = priceOverview.optString("initial_formatted", "")
            }

            val is100PercentPromo = isFreeGame || discountPercent >= 100 || (finalPrice == 0L && initialPrice > 0L)

            val formattedFinal = when {
                is100PercentPromo -> "БЕЗКОШТОВНО"
                finalFormatted.isNotBlank() -> finalFormatted
                finalPrice > 0 -> formatUahPrice(finalPrice)
                isFreeGame -> "БЕЗКОШТОВНО"
                else -> "Немає в продажу"
            }

            val formattedInitial = when {
                discountPercent > 0 && initialFormatted.isNotBlank() -> initialFormatted
                discountPercent > 0 && initialPrice > 0 -> formatUahPrice(initialPrice)
                else -> ""
            }

            return DealItem(
                id = "steam_$appid",
                store = "steam",
                appId = appid,
                title = name,
                imageUrl = if (headerImage.startsWith("http")) headerImage.replace(" ", "%20") else "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/$appid/header.jpg",
                storeUrl = "https://store.steampowered.com/app/$appid/",
                discountPercent = if (is100PercentPromo && !isFreeGame) 100 else discountPercent,
                initialPrice = initialPrice,
                finalPrice = finalPrice,
                currency = currency,
                formattedFinalPrice = formattedFinal,
                formattedInitialPrice = formattedInitial,
                isFree = is100PercentPromo,
                isUpcoming = false,
                isWishlist = true
            )
        } catch (_: Exception) {
            return null
        }
    }

    suspend fun fetchSteamFreeGames(currencyCode: String = "UA"): List<DealItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<DealItem>()
        val seenAppIds = mutableSetOf<String>()

        val url = "https://store.steampowered.com/search/results/?query=&start=0&count=50&specials=1&maxprice=free&json=1&cc=$currencyCode&l=ukrainian"
        val appIdRegex = Regex("""/apps/(\d+)""")

        try {
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .addHeader("Accept", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string()
            if (body != null) {
                val json = JSONObject(body)
                val items = json.optJSONArray("items")
                if (items != null) {
                    for (i in 0 until items.length()) {
                        val item = items.optJSONObject(i) ?: continue
                        val logo = item.optString("logo", "")
                        val match = appIdRegex.find(logo)
                        val appid = match?.groupValues?.get(1) ?: continue

                        if (!seenAppIds.add(appid)) continue

                        val detailedDeal = fetchWishlistSingleAppDetails(appid, currencyCode)
                        if (detailedDeal != null) {
                            results.add(detailedDeal.copy(isWishlist = false))
                        } else {
                            val name = item.optString("name", "").trim()
                            if (name.isNotEmpty()) {
                                results.add(
                                    DealItem(
                                        id = "steam_$appid",
                                        store = "steam",
                                        appId = appid,
                                        title = name,
                                        imageUrl = "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/$appid/header.jpg",
                                        storeUrl = "https://store.steampowered.com/app/$appid/",
                                        discountPercent = 100,
                                        initialPrice = 0L,
                                        finalPrice = 0L,
                                        currency = "UAH",
                                        formattedFinalPrice = "БЕЗКОШТОВНО",
                                        formattedInitialPrice = "",
                                        isFree = true,
                                        isUpcoming = false,
                                        isWishlist = false
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        results
    }

    suspend fun fetchSteamCrazyDiscounts(currencyCode: String = "UA"): List<DealItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<DealItem>()
        val seenAppIds = mutableSetOf<String>()
        val rowRegex = Regex("""<a[^>]+data-ds-appid="(\d+)"[^>]*>([\s\S]*?)</a>""")
        val discountRegex = Regex("""data-discount="(\d+)"""")
        val priceFinalRegex = Regex("""data-price-final="(\d+)"""")
        val titleRegex = Regex("""<span class="title">([^<]+)</span>""")
        val origPriceRegex = Regex("""<div class="discount_original_price">([^<]+)</div>""")
        val finalPriceRegex = Regex("""<div class="discount_final_price">([^<]+)</div>""")

        val pages = listOf(0, 50, 100)
        for (start in pages) {
            try {
                val url = "https://store.steampowered.com/search/results/?query=&start=$start&count=50&specials=1&cc=$currencyCode&l=ukrainian"
                val request = Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: continue

                val matches = rowRegex.findAll(body)
                for (match in matches) {
                    val appid = match.groupValues[1]
                    val content = match.groupValues[2]

                    if (!seenAppIds.add(appid)) continue

                    val discount = discountRegex.find(content)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                    val finalPrice = priceFinalRegex.find(content)?.groupValues?.get(1)?.toLongOrNull() ?: 0L

                    if (discount >= 75 && discount < 100 && finalPrice > 0L) {
                        val rawTitle = titleRegex.find(content)?.groupValues?.get(1) ?: "Steam Game"
                        val title = rawTitle
                            .replace("&amp;", "&")
                            .replace("&quot;", "\"")
                            .replace("&#39;", "'")
                            .trim()
                        val origStr = origPriceRegex.find(content)?.groupValues?.get(1)?.trim() ?: ""
                        val finalStr = finalPriceRegex.find(content)?.groupValues?.get(1)?.trim() ?: formatUahPrice(finalPrice)

                        val calculatedInitial = if (discount < 100) {
                            (finalPrice * 100) / (100 - discount)
                        } else {
                            finalPrice
                        }

                        results.add(
                            DealItem(
                                id = "steam_$appid",
                                store = "steam",
                                appId = appid,
                                title = title,
                                imageUrl = "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/$appid/header.jpg",
                                storeUrl = "https://store.steampowered.com/app/$appid/",
                                discountPercent = discount,
                                initialPrice = calculatedInitial,
                                finalPrice = finalPrice,
                                currency = "UAH",
                                formattedFinalPrice = if (finalStr.isNotBlank()) finalStr else formatUahPrice(finalPrice),
                                formattedInitialPrice = if (origStr.isNotBlank()) origStr else formatUahPrice(calculatedInitial),
                                isFree = false,
                                isUpcoming = false,
                                isWishlist = false
                            )
                        )
                    }
                }
            } catch (_: Exception) {}
        }

        results
    }

    /**
     * Fetches the promotional event/sale banner currently featured live on Steam.
     * When a sale or festival goes live, Steam features it on store.steampowered.com.
     */
    suspend fun fetchActiveSeasonalBannerFromSteam(): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://store.steampowered.com/?l=ukrainian")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept-Language", "uk,en;q=0.9")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val html = response.body?.string() ?: return@withContext null
                val pattern = Pattern.compile("""(?:https:)?//(?:clan\.akamai\.steamstatic\.com/images/\d+/[a-zA-Z0-9_\.]+|cdn\.cloudflare\.steamstatic\.com/steam/clusters/[a-zA-Z0-9_\/]+/header\.jpg)""")
                val matcher = pattern.matcher(html)
                if (matcher.find()) {
                    var url = matcher.group(0)
                    if (url != null && url.startsWith("//")) {
                        url = "https:$url"
                    }
                    return@withContext url
                }
            }
        } catch (_: Exception) {}
        null
    }

    /**
     * Searches games across the entire Steam store by price parameters and sort option.
     * Supports multi-page batching (4 pages of 50 = 200 items per batch) and pagination via startOffset.
     */
    suspend fun searchSteamGamesByPrice(
        minPriceUah: Int?,
        maxPriceUah: Int?,
        exactPriceUah: Int?,
        exactPriceCents: Long? = null,
        sortOption: SteamSearchSortOption,
        searchQuery: String = "",
        currencyCode: String = "UA",
        startOffset: Int = 0
    ): SteamPriceSearchResult = withContext(Dispatchers.IO) {
        val results = mutableListOf<DealItem>()
        val seenAppIds = mutableSetOf<String>()

        val rowRegex = Regex("""<a[^>]+data-ds-appid="(\d+)"[^>]*>([\s\S]*?)</a>""")
        val discountRegex = Regex("""data-discount="(\d+)"""")
        val priceFinalRegex = Regex("""data-price-final="(\d+)"""")
        val titleRegex = Regex("""<span class="title">([^<]+)</span>""")
        val origPriceRegex = Regex("""<div class="discount_original_price">([^<]+)</div>""")
        val finalPriceRegex = Regex("""<div class="discount_final_price">([^<]+)</div>""")

        val sortParam = when (sortOption) {
            SteamSearchSortOption.POPULARITY -> "sort_by=_ASC"
            SteamSearchSortOption.DISCOUNT_DESC -> "sort_by=Price_ASC&specials=1"
            SteamSearchSortOption.PRICE_ASC -> "sort_by=Price_ASC"
            SteamSearchSortOption.PRICE_DESC -> "sort_by=Price_DESC"
            SteamSearchSortOption.REVIEWS_DESC -> "sort_by=Reviews_DESC"
            SteamSearchSortOption.RELEASED_DESC -> "sort_by=Released_DESC"
            SteamSearchSortOption.NAME_ASC -> "sort_by=Name_ASC"
        }

        // Steam predefined maxprice stops in UAH
        val steamStops = listOf(40, 80, 120, 160, 200, 240, 280, 320, 360, 400, 440, 480)
        val targetMax = if (exactPriceUah != null) exactPriceUah else maxPriceUah

        val maxPriceParam = when {
            targetMax == 0 || (minPriceUah == 0 && maxPriceUah == 0) -> "&maxprice=free"
            targetMax != null && targetMax > 0 && targetMax <= 480 -> {
                val stop = steamStops.firstOrNull { it >= targetMax } ?: 480
                "&maxprice=$stop"
            }
            else -> ""
        }

        val encodedTerm = if (searchQuery.isNotBlank()) {
            "&term=" + java.net.URLEncoder.encode(searchQuery.trim(), "UTF-8")
        } else {
            ""
        }

        // Fetch 4 pages (each 50 items = 200 items per batch) concurrently from Steam
        val pages = listOf(startOffset, startOffset + 50, startOffset + 100, startOffset + 150)
        var totalRawItemsFound = 0
        var lastPageRawItems = 0

        coroutineScope {
            val deferredPages = pages.map { start ->
                async(Dispatchers.IO) {
                    try {
                        val urlBuilder = StringBuilder(
                            "https://store.steampowered.com/search/results/?query=$encodedTerm&category1=998&start=$start&count=50&$sortParam$maxPriceParam&cc=$currencyCode&l=ukrainian"
                        )
                        if (sortOption == SteamSearchSortOption.DISCOUNT_DESC) {
                            urlBuilder.append("&specials=1")
                        }

                        val request = Request.Builder()
                            .url(urlBuilder.toString())
                            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                            .build()

                        val response = client.newCall(request).execute()
                        response.body?.string() ?: ""
                    } catch (_: Exception) {
                        ""
                    }
                }
            }

            val pageHtmls = deferredPages.awaitAll()

            pageHtmls.forEachIndexed { pageIndex, body ->
                if (body.isNotBlank()) {
                    val matches = rowRegex.findAll(body).toList()
                    totalRawItemsFound += matches.size
                    if (pageIndex == pages.lastIndex) {
                        lastPageRawItems = matches.size
                    }

                    for (match in matches) {
                        val appid = match.groupValues[1]
                        val content = match.groupValues[2]

                        if (!seenAppIds.add(appid)) continue

                        val discount = discountRegex.find(content)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                        val finalPriceCents = priceFinalRegex.find(content)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
                        val isFree = finalPriceCents == 0L

                        if (!matchesPriceCriteria(
                                finalPriceCents = finalPriceCents,
                                isFree = isFree,
                                minPriceUah = minPriceUah,
                                maxPriceUah = maxPriceUah,
                                exactPriceUah = exactPriceUah,
                                exactPriceCents = exactPriceCents
                            )
                        ) {
                            continue
                        }

                        val rawTitle = titleRegex.find(content)?.groupValues?.get(1) ?: "Steam Game"
                        val title = rawTitle
                            .replace("&amp;", "&")
                            .replace("&quot;", "\"")
                            .replace("&#39;", "'")
                            .trim()
                        val origStr = origPriceRegex.find(content)?.groupValues?.get(1)?.trim() ?: ""
                        val finalStr = finalPriceRegex.find(content)?.groupValues?.get(1)?.trim() ?: ""

                        val calculatedInitial = if (discount in 1..99) {
                            (finalPriceCents * 100) / (100 - discount)
                        } else {
                            finalPriceCents
                        }

                        val formattedFinal = when {
                            isFree -> "БЕЗКОШТОВНО"
                            finalStr.isNotBlank() -> finalStr
                            else -> formatUahPrice(finalPriceCents)
                        }

                        val formattedInitial = when {
                            discount > 0 && origStr.isNotBlank() -> origStr
                            discount > 0 && calculatedInitial > 0 -> formatUahPrice(calculatedInitial)
                            else -> ""
                        }

                        results.add(
                            DealItem(
                                id = "steam_$appid",
                                store = "steam",
                                appId = appid,
                                title = title,
                                imageUrl = "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/$appid/header.jpg",
                                storeUrl = "https://store.steampowered.com/app/$appid/",
                                discountPercent = discount,
                                initialPrice = calculatedInitial,
                                finalPrice = finalPriceCents,
                                currency = "UAH",
                                formattedFinalPrice = formattedFinal,
                                formattedInitialPrice = formattedInitial,
                                isFree = isFree,
                                isUpcoming = false,
                                isWishlist = false
                            )
                        )
                    }
                }
            }
        }

        val hasMore = when {
            totalRawItemsFound == 0 -> false
            lastPageRawItems > 0 -> true
            totalRawItemsFound >= 40 -> true
            else -> false
        }

        val sortedResults = when (sortOption) {
            SteamSearchSortOption.POPULARITY -> results
            SteamSearchSortOption.DISCOUNT_DESC -> results.sortedByDescending { it.discountPercent }
            SteamSearchSortOption.PRICE_ASC -> results.sortedWith(
                compareBy<DealItem> { it.finalPrice }.thenByDescending { it.discountPercent }
            )
            SteamSearchSortOption.PRICE_DESC -> results.sortedWith(
                compareByDescending<DealItem> { it.finalPrice }.thenByDescending { it.discountPercent }
            )
            SteamSearchSortOption.REVIEWS_DESC -> results
            SteamSearchSortOption.RELEASED_DESC -> results
            SteamSearchSortOption.NAME_ASC -> results.sortedBy { it.title.lowercase() }
        }

        SteamPriceSearchResult(
            items = sortedResults,
            hasMore = hasMore
        )
    }

    /**
     * Checks if a price matches the exact price or range with tolerance for UAH kopecks and psychological pricing.
     */
    fun matchesPriceCriteria(
        finalPriceCents: Long,
        isFree: Boolean,
        minPriceUah: Int?,
        maxPriceUah: Int?,
        exactPriceUah: Int?,
        exactPriceCents: Long? = null
    ): Boolean {
        val finalPriceUah = (finalPriceCents / 100).toInt()
        val roundedUah = Math.round(finalPriceCents / 100.0).toInt()

        if (exactPriceUah != null) {
            if (exactPriceUah == 0) {
                return isFree || finalPriceCents == 0L
            }
            if (exactPriceCents != null && exactPriceCents > 0L) {
                if (kotlin.math.abs(finalPriceCents - exactPriceCents) <= 50L) {
                    return true
                }
            }
            // User typed an exact price (e.g. 199 or 200 or 500)
            // Matches if integer part matches (199.00 / 199.50), or rounded matches (199.99 -> 200),
            // or within 1 UAH (100 cents) difference
            return finalPriceUah == exactPriceUah ||
                    roundedUah == exactPriceUah ||
                    kotlin.math.abs(finalPriceCents - exactPriceUah * 100L) <= 100L
        }

        if (minPriceUah != null) {
            if (minPriceUah == 0 && maxPriceUah == 0) {
                return isFree || finalPriceCents == 0L
            }
            if (finalPriceUah < minPriceUah && roundedUah < minPriceUah) {
                return false
            }
        }

        if (maxPriceUah != null) {
            if (finalPriceCents > (maxPriceUah * 100L + 99L)) {
                return false
            }
        }

        return true
    }

    private fun formatUahPrice(priceCents: Long): String {
        if (priceCents <= 0) return "0 грн"
        val amount = priceCents / 100.0
        return if (priceCents % 100 == 0L) {
            "${amount.toLong()} грн"
        } else {
            String.format("%.2f грн", amount)
        }
    }
}
