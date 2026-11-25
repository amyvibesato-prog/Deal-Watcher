package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.PreferencesManager
import com.example.model.DealItem
import com.example.model.FilterOption
import com.example.model.SeasonalEvent
import com.example.model.SeasonalEventsProvider
import com.example.model.SortOption
import com.example.model.SteamSearchSortOption
import com.example.network.SteamConnectionStatus
import com.example.notification.DealNotificationManager
import com.example.repository.DealRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DealWatcherUiState(
    val displayedDeals: List<DealItem> = emptyList(),
    val rawDealsList: List<DealItem> = emptyList(),
    val steamDealsCount: Int = 0,
    val steamWishlistCount: Int = 0,
    val epicDealsCount: Int = 0,
    val maxDiscountPercent: Int = 0,
    val isLoading: Boolean = false,
    val isTestingConnection: Boolean = false,
    val connectionTestStatus: SteamConnectionStatus? = null,
    val searchQuery: String = "",
    val isSearchExpanded: Boolean = false,
    val selectedFilter: FilterOption = FilterOption.ALL,
    val selectedSort: SortOption = SortOption.DISCOUNT_DESC,
    val lastUpdatedTime: Long = 0L,
    val selectedDealForDetail: DealItem? = null,
    val isSettingsOpen: Boolean = false,
    val isSalesCalendarOpen: Boolean = false,
    val userMessage: String? = null,
    val hasNotificationPermission: Boolean = true,
    val isSteamConfigured: Boolean = false,
    val currentSeasonalEvent: com.example.model.SeasonalEvent = com.example.model.SeasonalEventsProvider.getActiveEvent(),
    val isFilterMenuOpen: Boolean = false,
    val steamMinPriceInput: String = "",
    val steamMaxPriceInput: String = "",
    val steamExactPriceInput: String = "",
    val isExactPriceMode: Boolean = false,
    val selectedSteamSort: SteamSearchSortOption = SteamSearchSortOption.POPULARITY,
    val steamPriceSearchResults: List<DealItem> = emptyList(),
    val isSteamPriceSearching: Boolean = false,
    val activePriceFilterSummary: String? = null,
    val canLoadMoreSteamGames: Boolean = false,
    val isSteamLoadingMore: Boolean = false,
    val steamSearchOffset: Int = 0
)

class DealViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val preferencesManager = PreferencesManager(application)
    val notificationManager = DealNotificationManager(application)
    private val repository = DealRepository(
        dealDao = db.dealDao(),
        preferencesManager = preferencesManager,
        notificationManager = notificationManager
    )

    private val _isLoading = MutableStateFlow(false)
    private val _isTestingConnection = MutableStateFlow(false)
    private val _connectionTestStatus = MutableStateFlow<SteamConnectionStatus?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _isSearchExpanded = MutableStateFlow(false)
    private val _selectedFilter = MutableStateFlow(FilterOption.ALL)
    private val _selectedSort = MutableStateFlow(SortOption.DISCOUNT_DESC)
    private val _selectedDealForDetail = MutableStateFlow<DealItem?>(null)
    private val _isSettingsOpen = MutableStateFlow(false)
    private val _isSalesCalendarOpen = MutableStateFlow(false)
    private val _userMessage = MutableStateFlow<String?>(null)
    private val _notificationPermissionState = MutableStateFlow(notificationManager.hasNotificationPermission())
    private val _currentSeasonalEvent = MutableStateFlow<SeasonalEvent>(repository.getCurrentSeasonalEvent())

    // Steam price search and popup tabs state
    private val _isFilterMenuOpen = MutableStateFlow(false)
    private val _steamMinPriceInput = MutableStateFlow("")
    private val _steamMaxPriceInput = MutableStateFlow("")
    private val _steamExactPriceInput = MutableStateFlow("")
    private val _isExactPriceMode = MutableStateFlow(false)
    private val _selectedSteamSort = MutableStateFlow(SteamSearchSortOption.POPULARITY)
    private val _steamPriceSearchResults = MutableStateFlow<List<DealItem>>(emptyList())
    private val _isSteamPriceSearching = MutableStateFlow(false)
    private val _activePriceFilterSummary = MutableStateFlow<String?>(null)
    private val _canLoadMoreSteamGames = MutableStateFlow(false)
    private val _isSteamLoadingMore = MutableStateFlow(false)
    private val _steamSearchOffset = MutableStateFlow(0)

    val uiState: StateFlow<DealWatcherUiState> = combine(
        repository.allDeals,
        _isLoading,
        _isTestingConnection,
        _connectionTestStatus,
        _searchQuery,
        _isSearchExpanded,
        _selectedFilter,
        _selectedSort,
        _selectedDealForDetail,
        _isSettingsOpen,
        _isSalesCalendarOpen,
        _userMessage,
        _notificationPermissionState,
        _currentSeasonalEvent,
        _isFilterMenuOpen,
        _steamMinPriceInput,
        _steamMaxPriceInput,
        _steamExactPriceInput,
        _isExactPriceMode,
        _selectedSteamSort,
        _steamPriceSearchResults,
        _isSteamPriceSearching,
        _activePriceFilterSummary,
        _canLoadMoreSteamGames,
        _isSteamLoadingMore,
        _steamSearchOffset
    ) { params: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val allDeals = params[0] as List<DealItem>
        val loading = params[1] as Boolean
        val testing = params[2] as Boolean
        val testStatus = params[3] as SteamConnectionStatus?
        val query = params[4] as String
        val searchExpanded = params[5] as Boolean
        val filter = params[6] as FilterOption
        val sort = params[7] as SortOption
        val detailDeal = params[8] as DealItem?
        val settingsOpen = params[9] as Boolean
        val calendarOpen = params[10] as Boolean
        val message = params[11] as String?
        val hasPermission = params[12] as Boolean
        val seasonEvent = params[13] as com.example.model.SeasonalEvent
        val filterMenuOpen = params[14] as Boolean
        val minPriceStr = params[15] as String
        val maxPriceStr = params[16] as String
        val exactPriceStr = params[17] as String
        val exactMode = params[18] as Boolean
        val steamSort = params[19] as SteamSearchSortOption
        val steamPriceResults = params[20] as List<DealItem>
        val isPriceSearching = params[21] as Boolean
        val priceSummary = params[22] as String?
        val canLoadMore = params[23] as Boolean
        val isLoadingMore = params[24] as Boolean
        val searchOffset = params[25] as Int

        val steamWishlistCount = allDeals.count { it.store == "steam" && it.isWishlist && (it.discountPercent > 0 || it.isFree) }
        val steamTotalCount = allDeals.count { it.store == "steam" && !it.isUpcoming && (it.discountPercent > 0 || it.isFree) }
        val epicCount = allDeals.count { it.store == "epic" && !it.isUpcoming && (it.discountPercent > 0 || it.isFree) }
        val maxDiscount = allDeals.filter { !it.isUpcoming && (it.discountPercent > 0 || it.isFree) }.maxOfOrNull { it.discountPercent } ?: 0
        val isConfigured = preferencesManager.steamId.isNotBlank()

        // 1. Filter
        val sorted: List<DealItem> = if (filter == FilterOption.STEAM_PRICE_SEARCH) {
            if (query.isBlank()) {
                steamPriceResults
            } else {
                steamPriceResults.filter { it.title.contains(query, ignoreCase = true) }
            }
        } else {
            val filtered = allDeals.filter { item ->
                // Search query filter
                val matchesQuery = query.isBlank() || item.title.contains(query, ignoreCase = true)

                // Category filter
                val matchesCategory = when (filter) {
                    FilterOption.ALL -> !item.isUpcoming && (item.discountPercent > 0 || item.isFree)
                    FilterOption.SEASONAL -> !item.isUpcoming && item.store == "steam" && (item.discountPercent > 0 || item.isFree) && isSeasonalDeal(item)
                    FilterOption.CRAZY_DISCOUNTS -> !item.isUpcoming && item.discountPercent >= 75 && item.discountPercent < 100 && !item.isFree && item.finalPrice > 0
                    FilterOption.UPCOMING -> item.isUpcoming
                    FilterOption.STEAM_FREE -> !item.isUpcoming && item.store == "steam" && item.discountPercent >= 100 && (item.initialPrice > 0 || item.isFree)
                    FilterOption.STEAM -> !item.isUpcoming && item.store == "steam" && item.isWishlist && (item.discountPercent > 0 || item.isFree)
                    FilterOption.EPIC -> !item.isUpcoming && item.store == "epic" && (item.discountPercent > 0 || item.isFree)
                    FilterOption.STEAM_PRICE_SEARCH -> true
                }

                matchesQuery && matchesCategory
            }

            // 2. Sort for regular deals
            when (sort) {
                SortOption.DISCOUNT_DESC -> filtered.sortedWith(
                    compareByDescending<DealItem> { it.discountPercent }
                        .thenBy { it.finalPrice }
                )
                SortOption.DISCOUNT_ASC -> filtered.sortedWith(
                    compareBy<DealItem> { it.discountPercent }
                        .thenBy { it.finalPrice }
                )
                SortOption.PRICE_ASC -> filtered.sortedWith(
                    compareBy<DealItem> { it.finalPrice }
                        .thenByDescending { it.discountPercent }
                )
                SortOption.PRICE_DESC -> filtered.sortedWith(
                    compareByDescending<DealItem> { it.finalPrice }
                        .thenByDescending { it.discountPercent }
                )
                SortOption.TITLE_ASC -> filtered.sortedBy { it.title.lowercase() }
            }
        }

        DealWatcherUiState(
            displayedDeals = sorted,
            rawDealsList = allDeals,
            steamDealsCount = steamTotalCount,
            steamWishlistCount = steamWishlistCount,
            epicDealsCount = epicCount,
            maxDiscountPercent = maxDiscount,
            isLoading = loading || isPriceSearching,
            isTestingConnection = testing,
            connectionTestStatus = testStatus,
            searchQuery = query,
            isSearchExpanded = searchExpanded,
            selectedFilter = filter,
            selectedSort = sort,
            lastUpdatedTime = preferencesManager.lastUpdatedTime,
            selectedDealForDetail = detailDeal,
            isSettingsOpen = settingsOpen,
            isSalesCalendarOpen = calendarOpen,
            userMessage = message,
            hasNotificationPermission = hasPermission,
            isSteamConfigured = isConfigured,
            currentSeasonalEvent = seasonEvent,
            isFilterMenuOpen = filterMenuOpen,
            steamMinPriceInput = minPriceStr,
            steamMaxPriceInput = maxPriceStr,
            steamExactPriceInput = exactPriceStr,
            isExactPriceMode = exactMode,
            selectedSteamSort = steamSort,
            steamPriceSearchResults = steamPriceResults,
            isSteamPriceSearching = isPriceSearching,
            activePriceFilterSummary = priceSummary,
            canLoadMoreSteamGames = canLoadMore,
            isSteamLoadingMore = isLoadingMore,
            steamSearchOffset = searchOffset
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DealWatcherUiState(
            isLoading = false,
            lastUpdatedTime = preferencesManager.lastUpdatedTime,
            hasNotificationPermission = notificationManager.hasNotificationPermission(),
            isSteamConfigured = preferencesManager.steamId.isNotBlank(),
            currentSeasonalEvent = repository.getCurrentSeasonalEvent()
        )
    )

    init {
        com.example.worker.DealWorkScheduler.schedulePeriodicSync(application)
        viewModelScope.launch {
            repository.loadDemoDataIfEmpty()
            refresh(force = false)
        }
    }

    fun updateNotificationPermissionStatus() {
        _notificationPermissionState.value = notificationManager.hasNotificationPermission()
    }

    fun refresh(force: Boolean = true) {
        if (_isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            _currentSeasonalEvent.value = repository.getCurrentSeasonalEvent()
            val result = repository.refreshDeals(force = force, isBackground = false)
            _currentSeasonalEvent.value = repository.getCurrentSeasonalEvent()
            _isLoading.value = false

            if (result.success) {
                val msg = buildString {
                    append("Оновлено! ")
                    if (result.steamWishlistCount > 0) append("Wishlist Steam: ${result.steamWishlistCount}. ")
                    else if (result.steamDealsCount > 0) append("Steam: ${result.steamDealsCount}. ")
                    if (result.epicCount > 0) append("Epic: ${result.epicCount}. ")
                    if (result.newSteamDealsCount > 0 || result.newEpicDealsCount > 0) {
                        append("Нових знижок: ${result.newSteamDealsCount + result.newEpicDealsCount}!")
                    }
                }
                _userMessage.value = msg.trim()
            } else {
                _userMessage.value = result.errorMessage ?: "Не вдалося оновити дані"
            }
        }
    }

    fun testSteamConnection(steamId: String, apiKey: String) {
        if (_isTestingConnection.value) return
        viewModelScope.launch {
            _isTestingConnection.value = true
            _connectionTestStatus.value = null
            val status = repository.testSteamConnection(steamId, apiKey)
            _connectionTestStatus.value = status
            _isTestingConnection.value = false
        }
    }

    fun clearConnectionTest() {
        _connectionTestStatus.value = null
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleSearchExpanded(expanded: Boolean? = null) {
        val next = expanded ?: !_isSearchExpanded.value
        _isSearchExpanded.value = next
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _isSearchExpanded.value = false
    }

    fun openSalesCalendar(open: Boolean) {
        _isSalesCalendarOpen.value = open
    }

    fun showUserMessage(message: String) {
        _userMessage.value = message
    }

    fun openFilterMenu(open: Boolean) {
        _isFilterMenuOpen.value = open
    }

    fun setSteamPriceInputs(min: String, max: String, exact: String, isExact: Boolean) {
        _steamMinPriceInput.value = min
        _steamMaxPriceInput.value = max
        _steamExactPriceInput.value = exact
        _isExactPriceMode.value = isExact
    }

    fun setSteamSort(sort: SteamSearchSortOption) {
        _selectedSteamSort.value = sort
    }

    fun applySteamPriceFilter(
        minPriceStr: String? = null,
        maxPriceStr: String? = null,
        exactPriceStr: String? = null,
        isExact: Boolean? = null,
        sort: SteamSearchSortOption? = null
    ) {
        val minStr = (minPriceStr ?: _steamMinPriceInput.value).trim()
        val maxStr = (maxPriceStr ?: _steamMaxPriceInput.value).trim()
        val exactStr = (exactPriceStr ?: _steamExactPriceInput.value).trim()
        val exactMode = isExact ?: _isExactPriceMode.value
        val sortOpt = sort ?: _selectedSteamSort.value

        _steamMinPriceInput.value = minStr
        _steamMaxPriceInput.value = maxStr
        _steamExactPriceInput.value = exactStr
        _isExactPriceMode.value = exactMode
        _selectedSteamSort.value = sortOpt
        _selectedFilter.value = FilterOption.STEAM_PRICE_SEARCH
        _isFilterMenuOpen.value = false

        val minVal = minStr.replace(',', '.').toDoubleOrNull()?.toInt()
        val maxVal = maxStr.replace(',', '.').toDoubleOrNull()?.toInt()
        val exactDouble = exactStr.replace(',', '.').toDoubleOrNull()
        val exactVal = exactDouble?.toInt()
        val exactCents = exactDouble?.let { Math.round(it * 100.0) }

        val summary = if (exactMode && exactStr.isNotBlank()) {
            "Steam: $exactStr грн"
        } else if (minVal != null && maxVal != null) {
            "Steam: $minVal–$maxVal грн"
        } else if (minVal != null) {
            "Steam: від $minVal грн"
        } else if (maxVal != null) {
            "Steam: до $maxVal грн"
        } else {
            "Steam: усі ціни"
        }
        _activePriceFilterSummary.value = "$summary • ${sortOpt.label}"
        _steamSearchOffset.value = 0
        _canLoadMoreSteamGames.value = false

        viewModelScope.launch {
            _isSteamPriceSearching.value = true
            try {
                val result = repository.searchSteamGamesByPrice(
                    minPriceUah = if (!exactMode) minVal else null,
                    maxPriceUah = if (!exactMode) maxVal else null,
                    exactPriceUah = if (exactMode) exactVal else null,
                    exactPriceCents = if (exactMode) exactCents else null,
                    sortOption = sortOpt,
                    searchQuery = _searchQuery.value,
                    startOffset = 0
                )
                _steamPriceSearchResults.value = result.items
                _canLoadMoreSteamGames.value = result.hasMore
                _steamSearchOffset.value = 200

                if (result.items.isEmpty()) {
                    _userMessage.value = "За фільтром цін у Steam ігор не знайдено"
                } else {
                    val moreNote = if (result.hasMore) " (можна завантажити ще)" else ""
                    _userMessage.value = "Steam: знайдено ${result.items.size} ігор$moreNote"
                }
            } catch (e: Exception) {
                _userMessage.value = "Помилка пошуку Steam: ${e.localizedMessage ?: "перевірте інтернет"}"
            } finally {
                _isSteamPriceSearching.value = false
            }
        }
    }

    fun loadMoreSteamPriceGames() {
        if (_isSteamLoadingMore.value || !_canLoadMoreSteamGames.value || _isSteamPriceSearching.value) return

        val minStr = _steamMinPriceInput.value.trim()
        val maxStr = _steamMaxPriceInput.value.trim()
        val exactStr = _steamExactPriceInput.value.trim()
        val exactMode = _isExactPriceMode.value
        val sortOpt = _selectedSteamSort.value

        val minVal = minStr.replace(',', '.').toDoubleOrNull()?.toInt()
        val maxVal = maxStr.replace(',', '.').toDoubleOrNull()?.toInt()
        val exactDouble = exactStr.replace(',', '.').toDoubleOrNull()
        val exactVal = exactDouble?.toInt()
        val exactCents = exactDouble?.let { Math.round(it * 100.0) }
        val currentOffset = _steamSearchOffset.value

        viewModelScope.launch {
            _isSteamLoadingMore.value = true
            try {
                val result = repository.searchSteamGamesByPrice(
                    minPriceUah = if (!exactMode) minVal else null,
                    maxPriceUah = if (!exactMode) maxVal else null,
                    exactPriceUah = if (exactMode) exactVal else null,
                    exactPriceCents = if (exactMode) exactCents else null,
                    sortOption = sortOpt,
                    searchQuery = _searchQuery.value,
                    startOffset = currentOffset
                )

                val currentList = _steamPriceSearchResults.value
                val existingIds = currentList.map { it.id }.toSet()
                val newItems = result.items.filter { !existingIds.contains(it.id) }

                _steamPriceSearchResults.value = currentList + newItems
                _canLoadMoreSteamGames.value = result.hasMore
                _steamSearchOffset.value = currentOffset + 200

                if (newItems.isNotEmpty()) {
                    _userMessage.value = "Завантажено ще ${newItems.size} ігор (всього: ${_steamPriceSearchResults.value.size})"
                } else if (result.hasMore) {
                    _userMessage.value = "Перевірено частину каталогу Steam, доступно завантажити далі"
                } else {
                    _userMessage.value = "Всі доступні ігри зі Steam завантажено (${_steamPriceSearchResults.value.size} ігор)"
                }
            } catch (e: Exception) {
                _userMessage.value = "Помилка завантаження: ${e.localizedMessage ?: "перевірте інтернет"}"
            } finally {
                _isSteamLoadingMore.value = false
            }
        }
    }

    fun resetSteamPriceFilter() {
        _steamMinPriceInput.value = ""
        _steamMaxPriceInput.value = ""
        _steamExactPriceInput.value = ""
        _isExactPriceMode.value = false
        _activePriceFilterSummary.value = null
        _steamPriceSearchResults.value = emptyList()
        _canLoadMoreSteamGames.value = false
        _isSteamLoadingMore.value = false
        _steamSearchOffset.value = 0
        _selectedFilter.value = FilterOption.ALL
    }

    fun setFilter(filter: FilterOption) {
        _selectedFilter.value = filter
        if (filter != FilterOption.STEAM_PRICE_SEARCH) {
            _activePriceFilterSummary.value = null
        }
    }

    fun setSort(sort: SortOption) {
        _selectedSort.value = sort
    }

    fun selectDeal(deal: DealItem?) {
        _selectedDealForDetail.value = deal
    }

    fun openSettings(open: Boolean) {
        _isSettingsOpen.value = open
        if (open) {
            _connectionTestStatus.value = null
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun saveSettings(
        steamId: String,
        apiKey: String,
        notifySteam: Boolean,
        notifyEpic: Boolean,
        minDiscount: Int
    ) {
        preferencesManager.steamId = steamId
        preferencesManager.steamApiKey = apiKey
        preferencesManager.notifySteam = notifySteam
        preferencesManager.notifyEpic = notifyEpic
        preferencesManager.minDiscountPercent = minDiscount

        _isSettingsOpen.value = false
        _userMessage.value = "Налаштування збережено"

        if (steamId.isNotBlank()) {
            refresh(force = true)
        }
    }

    fun sendTestNotification() {
        val sent = notificationManager.sendTestNotification()
        if (sent) {
            _userMessage.value = "Тестове сповіщення надіслано!"
        } else {
            _userMessage.value = "Надайте дозвіл на сповіщення в налаштуваннях Android"
        }
    }

    fun scheduleDelayedNotificationTest() {
        if (!notificationManager.hasNotificationPermission()) {
            _userMessage.value = "Надайте дозвіл на сповіщення в налаштуваннях Android"
            return
        }
        com.example.worker.DealWorkScheduler.scheduleDelayedTestNotification(getApplication(), 1)
        _userMessage.value = "⏰ Сповіщення заплановано через 1 хвилину. Тепер ви можете згорнути або закрити додаток!"
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearCache()
            _userMessage.value = "Кеш очищено"
        }
    }

    private fun isSeasonalDeal(item: DealItem): Boolean {
        if (item.store != "steam") return false
        if (item.isSeasonal) return true
        val seasonalKeywords = listOf(
            "scream", "halloween", "horror", "dead", "evil", "resident", "outlast",
            "phasmophobia", "dying", "zombie", "vampire", "ghost", "dark", "witch",
            "monster", "demon", "fear", "blood", "shadow", "night", "doom", "prey",
            "subnautica", "metro", "alan wake", "cult", "forest", "silent hill",
            "left 4 dead", "killing floor", "freddy", "fnaf", "last of us", "diablo",
            "system shock", "alien", "slay", "project zomboid", "nightmares"
        )
        val lowerTitle = item.title.lowercase()
        return seasonalKeywords.any { lowerTitle.contains(it) }
    }
}
