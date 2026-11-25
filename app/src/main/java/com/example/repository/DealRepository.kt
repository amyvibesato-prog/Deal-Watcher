package com.example.repository

import com.example.data.DealDao
import com.example.data.PreferencesManager
import com.example.model.DealItem
import com.example.model.SteamPriceSearchResult
import com.example.model.SteamSearchSortOption
import com.example.model.SeasonalEvent
import com.example.model.SeasonalEventsProvider
import com.example.network.EpicApiService
import com.example.network.SteamApiService
import com.example.network.SteamConnectionStatus
import com.example.notification.DealNotificationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

data class RefreshResult(
    val success: Boolean,
    val steamWishlistCount: Int = 0,
    val steamDealsCount: Int = 0,
    val epicCount: Int = 0,
    val newSteamDealsCount: Int = 0,
    val newEpicDealsCount: Int = 0,
    val errorMessage: String? = null
)

class DealRepository(
    private val dealDao: DealDao,
    private val preferencesManager: PreferencesManager,
    private val steamService: SteamApiService = SteamApiService(),
    private val epicService: EpicApiService = EpicApiService(),
    private val notificationManager: DealNotificationManager
) {

    val allDeals: Flow<List<DealItem>> = dealDao.getAllDeals()

    suspend fun testSteamConnection(steamId: String, apiKey: String): SteamConnectionStatus {
        return steamService.testSteamConnection(steamId, apiKey)
    }

    suspend fun searchSteamGamesByPrice(
        minPriceUah: Int?,
        maxPriceUah: Int?,
        exactPriceUah: Int?,
        exactPriceCents: Long? = null,
        sortOption: SteamSearchSortOption,
        searchQuery: String = "",
        startOffset: Int = 0
    ): SteamPriceSearchResult = withContext(Dispatchers.IO) {
        val results = mutableListOf<DealItem>()
        val seenAppIds = mutableSetOf<String>()

        val localDeals = dealDao.getAllDealsSync().filter { it.store == "steam" }
        val wishlistAppIds = localDeals.filter { it.isWishlist }.mapNotNull { it.appId }.toSet()

        // 1. Only add matching local deals on the first page (startOffset == 0)
        if (startOffset == 0) {
            val matchingLocal = localDeals.filter { deal ->
                val matchesPrice = steamService.matchesPriceCriteria(
                    finalPriceCents = deal.finalPrice,
                    isFree = deal.isFree,
                    minPriceUah = minPriceUah,
                    maxPriceUah = maxPriceUah,
                    exactPriceUah = exactPriceUah,
                    exactPriceCents = exactPriceCents
                )
                val matchesQuery = searchQuery.isBlank() || deal.title.contains(searchQuery.trim(), ignoreCase = true)
                matchesPrice && matchesQuery
            }

            // Add matching local deals first, preserving their isWishlist status
            for (deal in matchingLocal) {
                val key = deal.appId ?: deal.id
                if (seenAppIds.add(key)) {
                    results.add(deal)
                }
            }
        }

        // 2. Query the entire Steam store online for the 200-game batch starting at startOffset
        var hasMore = false
        try {
            val onlineResult = steamService.searchSteamGamesByPrice(
                minPriceUah = minPriceUah,
                maxPriceUah = maxPriceUah,
                exactPriceUah = exactPriceUah,
                exactPriceCents = exactPriceCents,
                sortOption = sortOption,
                searchQuery = searchQuery,
                startOffset = startOffset
            )
            hasMore = onlineResult.hasMore

            for (deal in onlineResult.items) {
                val key = deal.appId ?: deal.id
                if (seenAppIds.add(key)) {
                    val isWish = wishlistAppIds.contains(deal.appId)
                    results.add(if (isWish) deal.copy(isWishlist = true) else deal)
                }
            }
        } catch (_: Exception) {
            // In case of network errors, local matching deals are already present in results
        }

        // 3. Sort results if initial batch
        val finalItems = if (startOffset == 0) {
            when (sortOption) {
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
        } else {
            results
        }

        SteamPriceSearchResult(
            items = finalItems,
            hasMore = hasMore
        )
    }

    suspend fun refreshDeals(
        force: Boolean = false,
        isBackground: Boolean = false
    ): RefreshResult = withContext(Dispatchers.IO) {
        val steamId = preferencesManager.steamId
        val apiKey = preferencesManager.steamApiKey
        val notifySteam = preferencesManager.notifySteam
        val notifyEpic = preferencesManager.notifyEpic
        val minDiscount = preferencesManager.minDiscountPercent

        try {
            coroutineScope {
                val steamWishlistDeferred = async {
                    if (steamId.isNotBlank()) {
                        steamService.fetchWishlistDeals(steamId, apiKey)
                    } else {
                        emptyList()
                    }
                }

                val steamCrazyDeferred = async {
                    steamService.fetchSteamCrazyDiscounts()
                }

                val steamFreeDeferred = async {
                    steamService.fetchSteamFreeGames()
                }

                val epicDeferred = async {
                    epicService.fetchGiveaways()
                }

                val steamWishlistDeals = steamWishlistDeferred.await()
                val steamCrazyDeals = steamCrazyDeferred.await()
                val steamFreeDeals = steamFreeDeferred.await()
                val epicDeals = epicDeferred.await()

                val combinedSteamDeals = mutableListOf<DealItem>()
                val seenSteamIds = mutableSetOf<String>()

                // 1. Mark user wishlist items first (all games, both discounted and regular)
                for (deal in steamWishlistDeals) {
                    if (seenSteamIds.add(deal.id)) {
                        combinedSteamDeals.add(deal.copy(isWishlist = true))
                    }
                }

                // 2. Add Steam store-wide crazy discounts (>=75%, not free)
                for (deal in steamCrazyDeals) {
                    if (seenSteamIds.add(deal.id)) {
                        combinedSteamDeals.add(deal.copy(isWishlist = false))
                    }
                }

                // 3. Add 100% discount promo games
                for (deal in steamFreeDeals) {
                    if (seenSteamIds.add(deal.id)) {
                        combinedSteamDeals.add(deal.copy(isWishlist = false))
                    }
                }

                val previouslyNotifiedIds = preferencesManager.getNotifiedDealIds().toMutableSet()
                var newSteamCount = 0
                var newEpicCount = 0

                val isFirstRun = previouslyNotifiedIds.isEmpty()

                val newSteamDealsToNotify = mutableListOf<DealItem>()
                // Check notifications for Steam wishlist discounts
                if (notifySteam && steamWishlistDeals.isNotEmpty()) {
                    for (deal in steamWishlistDeals) {
                        if (deal.discountPercent >= minDiscount && deal.discountPercent > 0) {
                            if (!previouslyNotifiedIds.contains(deal.id)) {
                                newSteamCount++
                                newSteamDealsToNotify.add(deal)
                            }
                        }
                    }
                }

                val newEpicDealsToNotify = mutableListOf<DealItem>()
                // Check notifications for Epic giveaways
                if (notifyEpic && epicDeals.isNotEmpty()) {
                    for (deal in epicDeals) {
                        if (!previouslyNotifiedIds.contains(deal.id)) {
                            newEpicCount++
                            if (!deal.isUpcoming) {
                                newEpicDealsToNotify.add(deal)
                            }
                        }
                    }
                }

                // Only send system notifications if running in the background and not on the first launch!
                // This eliminates the storm of notifications when opening the app.
                if (isBackground && !isFirstRun) {
                    if (newSteamDealsToNotify.isNotEmpty()) {
                        notificationManager.notifySteamDeals(newSteamDealsToNotify)
                    }
                    if (newEpicDealsToNotify.isNotEmpty()) {
                        notificationManager.notifyEpicGiveaways(newEpicDealsToNotify)
                    }
                }

                val allFetched = mutableListOf<DealItem>()
                allFetched.addAll(combinedSteamDeals)
                allFetched.addAll(epicDeals)

                // Add seasonal Steam deals matching the active seasonal event dynamically
                val activeEvent = SeasonalEventsProvider.getActiveEvent()
                if (activeEvent.isActive) {
                    try {
                        val remoteBanner = steamService.fetchActiveSeasonalBannerFromSteam()
                        if (!remoteBanner.isNullOrBlank()) {
                            SeasonalEventsProvider.setDynamicBanner(activeEvent.id, remoteBanner)
                        }
                    } catch (_: Exception) {}
                }
                val seasonalDeals = getCuratedSeasonalDeals(activeEvent.id)
                for (deal in seasonalDeals) {
                    if (seenSteamIds.add(deal.id)) {
                        allFetched.add(deal)
                    }
                }

                // Clean up any remnants of old demo deals
                val demoIds = listOf(
                    "steam_4059370", "steam_1057090", "steam_1091500",
                    "steam_1145360", "steam_1245620", "steam_730",
                    "epic_demo_free", "epic_demo_upcoming"
                )
                dealDao.deleteDealsByIds(demoIds)

                // Clean and update Steam deals if we fetched any Steam items
                if (combinedSteamDeals.isNotEmpty()) {
                    dealDao.deleteDealsByStore("steam")
                } else if (steamId.isBlank()) {
                    // If user cleared Steam ID, remove wishlist items
                    dealDao.deleteDealsByStore("steam")
                }

                if (epicDeals.isNotEmpty()) {
                    dealDao.deleteDealsByStore("epic")
                }

                val evaluatedFetched = allFetched.map { evaluateHistoricalLow(it) }

                if (evaluatedFetched.isNotEmpty()) {
                    dealDao.insertDeals(evaluatedFetched)
                }

                for (deal in newSteamDealsToNotify) {
                    previouslyNotifiedIds.add(deal.id)
                }
                for (deal in newEpicDealsToNotify) {
                    previouslyNotifiedIds.add(deal.id)
                }
                if (!isBackground || isFirstRun) {
                    for (deal in allFetched) {
                        previouslyNotifiedIds.add(deal.id)
                    }
                }

                preferencesManager.saveNotifiedDealIds(previouslyNotifiedIds)
                preferencesManager.lastUpdatedTime = System.currentTimeMillis()

                RefreshResult(
                    success = true,
                    steamWishlistCount = steamWishlistDeals.size,
                    steamDealsCount = combinedSteamDeals.size,
                    epicCount = epicDeals.size,
                    newSteamDealsCount = newSteamCount,
                    newEpicDealsCount = newEpicCount
                )
            }
        } catch (e: Exception) {
            RefreshResult(
                success = false,
                errorMessage = e.localizedMessage ?: "Помилка оновлення даних"
            )
        }
    }

    suspend fun loadDemoDataIfEmpty() {
        withContext(Dispatchers.IO) {
            val demoIds = listOf(
                "steam_4059370", "steam_1057090", "steam_1091500",
                "steam_1145360", "steam_1245620", "steam_730",
                "epic_demo_free", "epic_demo_upcoming"
            )
            dealDao.deleteDealsByIds(demoIds)
            refreshDeals(force = true)
        }
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        dealDao.clearAllDeals()
        preferencesManager.saveNotifiedDealIds(emptySet())
    }

    private fun evaluateHistoricalLow(deal: DealItem): DealItem {
        val isRecord = when {
            deal.isFree && !deal.isUpcoming -> true
            deal.discountPercent >= 75 -> true
            deal.isSeasonal && deal.discountPercent >= 50 -> true
            else -> deal.isHistoricalLow
        }
        return deal.copy(isHistoricalLow = isRecord)
    }

    private fun getCuratedSteamScreamFestDeals(): List<DealItem> {
        return listOf(
            DealItem(
                id = "steam_1817070",
                store = "steam",
                appId = "1817070",
                title = "Resident Evil 4 Remake",
                imageUrl = "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/1817070/header.jpg",
                storeUrl = "https://store.steampowered.com/app/1817070/",
                discountPercent = 50,
                initialPrice = 199900L,
                finalPrice = 99950L,
                currency = "UAH",
                formattedFinalPrice = "999.50 грн",
                formattedInitialPrice = "1999.00 грн",
                isFree = false,
                isUpcoming = false,
                isWishlist = false,
                isSeasonal = true,
                isHistoricalLow = true
            ),
            DealItem(
                id = "steam_381210",
                store = "steam",
                appId = "381210",
                title = "Dead by Daylight",
                imageUrl = "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/381210/header.jpg",
                storeUrl = "https://store.steampowered.com/app/381210/",
                discountPercent = 60,
                initialPrice = 59900L,
                finalPrice = 23960L,
                currency = "UAH",
                formattedFinalPrice = "239.60 грн",
                formattedInitialPrice = "599.00 грн",
                isFree = false,
                isUpcoming = false,
                isWishlist = false,
                isSeasonal = true,
                isHistoricalLow = true
            ),
            DealItem(
                id = "steam_739630",
                store = "steam",
                appId = "739630",
                title = "Phasmophobia",
                imageUrl = "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/739630/header.jpg",
                storeUrl = "https://store.steampowered.com/app/739630/",
                discountPercent = 25,
                initialPrice = 41500L,
                finalPrice = 31125L,
                currency = "UAH",
                formattedFinalPrice = "311.25 грн",
                formattedInitialPrice = "415.00 грн",
                isFree = false,
                isUpcoming = false,
                isWishlist = false,
                isSeasonal = true,
                isHistoricalLow = false
            ),
            DealItem(
                id = "steam_534380",
                store = "steam",
                appId = "534380",
                title = "Dying Light 2 Stay Human",
                imageUrl = "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/534380/header.jpg",
                storeUrl = "https://store.steampowered.com/app/534380/",
                discountPercent = 60,
                initialPrice = 149900L,
                finalPrice = 59960L,
                currency = "UAH",
                formattedFinalPrice = "599.60 грн",
                formattedInitialPrice = "1499.00 грн",
                isFree = false,
                isUpcoming = false,
                isWishlist = false,
                isSeasonal = true,
                isHistoricalLow = true
            ),
            DealItem(
                id = "steam_1301210",
                store = "steam",
                appId = "1301210",
                title = "Cult of the Lamb",
                imageUrl = "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/1301210/header.jpg",
                storeUrl = "https://store.steampowered.com/app/1301210/",
                discountPercent = 50,
                initialPrice = 51000L,
                finalPrice = 25500L,
                currency = "UAH",
                formattedFinalPrice = "255.00 грн",
                formattedInitialPrice = "510.00 грн",
                isFree = false,
                isUpcoming = false,
                isWishlist = false,
                isSeasonal = true,
                isHistoricalLow = true
            ),
            DealItem(
                id = "steam_1304930",
                store = "steam",
                appId = "1304930",
                title = "The Outlast Trials",
                imageUrl = "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/1304930/header.jpg",
                storeUrl = "https://store.steampowered.com/app/1304930/",
                discountPercent = 33,
                initialPrice = 85000L,
                finalPrice = 56950L,
                currency = "UAH",
                formattedFinalPrice = "569.50 грн",
                formattedInitialPrice = "850.00 грн",
                isFree = false,
                isUpcoming = false,
                isWishlist = false,
                isSeasonal = true,
                isHistoricalLow = false
            ),
            DealItem(
                id = "steam_1622910",
                store = "steam",
                appId = "1622910",
                title = "Still Wakes the Deep",
                imageUrl = "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/1622910/header.jpg",
                storeUrl = "https://store.steampowered.com/app/1622910/",
                discountPercent = 30,
                initialPrice = 69900L,
                finalPrice = 48930L,
                currency = "UAH",
                formattedFinalPrice = "489.30 грн",
                formattedInitialPrice = "699.00 грн",
                isFree = false,
                isUpcoming = false,
                isWishlist = false,
                isSeasonal = true,
                isHistoricalLow = true
            )
        )
    }

    fun getCurrentSeasonalEvent(): SeasonalEvent = SeasonalEventsProvider.getActiveEvent()

    fun getCuratedSeasonalDeals(eventId: String): List<DealItem> {
        return when (eventId) {
            "steam_autumn_sale" -> getCuratedAutumnSaleDeals()
            "steam_winter_sale" -> getCuratedWinterSaleDeals()
            else -> getCuratedSteamScreamFestDeals()
        }
    }

    private fun getCuratedAutumnSaleDeals(): List<DealItem> {
        return listOf(
            DealItem(
                id = "steam_1091500",
                store = "steam",
                appId = "1091500",
                title = "Cyberpunk 2077",
                imageUrl = "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/1091500/header.jpg",
                storeUrl = "https://store.steampowered.com/app/1091500/",
                discountPercent = 50,
                initialPrice = 119900L,
                finalPrice = 59950L,
                currency = "UAH",
                formattedFinalPrice = "599.50 грн",
                formattedInitialPrice = "1199.00 грн",
                isFree = false,
                isUpcoming = false,
                isWishlist = false,
                isSeasonal = true,
                isHistoricalLow = true
            ),
            DealItem(
                id = "steam_1245620",
                store = "steam",
                appId = "1245620",
                title = "ELDEN RING",
                imageUrl = "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/1245620/header.jpg",
                storeUrl = "https://store.steampowered.com/app/1245620/",
                discountPercent = 33,
                initialPrice = 179900L,
                finalPrice = 120533L,
                currency = "UAH",
                formattedFinalPrice = "1205.33 грн",
                formattedInitialPrice = "1799.00 грн",
                isFree = false,
                isUpcoming = false,
                isWishlist = false,
                isSeasonal = true,
                isHistoricalLow = false
            ),
            DealItem(
                id = "steam_1174180",
                store = "steam",
                appId = "1174180",
                title = "Red Dead Redemption 2",
                imageUrl = "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/1174180/header.jpg",
                storeUrl = "https://store.steampowered.com/app/1174180/",
                discountPercent = 67,
                initialPrice = 149900L,
                finalPrice = 49467L,
                currency = "UAH",
                formattedFinalPrice = "494.67 грн",
                formattedInitialPrice = "1499.00 грн",
                isFree = false,
                isUpcoming = false,
                isWishlist = false,
                isSeasonal = true,
                isHistoricalLow = true
            )
        )
    }

    private fun getCuratedWinterSaleDeals(): List<DealItem> {
        return listOf(
            DealItem(
                id = "steam_1086940",
                store = "steam",
                appId = "1086940",
                title = "Baldur's Gate 3",
                imageUrl = "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/1086940/header.jpg",
                storeUrl = "https://store.steampowered.com/app/1086940/",
                discountPercent = 20,
                initialPrice = 89900L,
                finalPrice = 71920L,
                currency = "UAH",
                formattedFinalPrice = "719.20 грн",
                formattedInitialPrice = "899.00 грн",
                isFree = false,
                isUpcoming = false,
                isWishlist = false,
                isSeasonal = true,
                isHistoricalLow = true
            ),
            DealItem(
                id = "steam_990080",
                store = "steam",
                appId = "990080",
                title = "Hogwarts Legacy",
                imageUrl = "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/990080/header.jpg",
                storeUrl = "https://store.steampowered.com/app/990080/",
                discountPercent = 70,
                initialPrice = 159900L,
                finalPrice = 47970L,
                currency = "UAH",
                formattedFinalPrice = "479.70 грн",
                formattedInitialPrice = "1599.00 грн",
                isFree = false,
                isUpcoming = false,
                isWishlist = false,
                isSeasonal = true,
                isHistoricalLow = true
            )
        )
    }
}
