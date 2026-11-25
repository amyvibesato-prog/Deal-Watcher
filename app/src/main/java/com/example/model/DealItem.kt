package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class StoreType {
    STEAM,
    EPIC
}

enum class SortOption(val label: String) {
    DISCOUNT_DESC("За знижкою (max → min)"),
    DISCOUNT_ASC("За знижкою (min → max)"),
    PRICE_ASC("За ціною (дешевші спочатку)"),
    PRICE_DESC("За ціною (дорожчі спочатку)"),
    TITLE_ASC("За назвою (А → Я)")
}

enum class SteamSearchSortOption(val label: String) {
    POPULARITY("За популярністю / лідерами продажу"),
    DISCOUNT_DESC("За розміром знижки"),
    PRICE_ASC("Від найдешевших до найдорожчих"),
    PRICE_DESC("Від найдорожчих до найдешевших"),
    REVIEWS_DESC("За відгуками гравців"),
    RELEASED_DESC("За датою виходу"),
    NAME_ASC("За назвою (А → Я)")
}

enum class FilterOption(val label: String) {
    ALL("Всі"),
    SEASONAL("Сезонні знижки 🔥"),
    CRAZY_DISCOUNTS("Шалені знижки (-75%+)"),
    STEAM_PRICE_SEARCH("Фільтр цін Steam 🔍"),
    UPCOMING("Майбутні роздачі"),
    STEAM_FREE("Знижки -100% у Steam"),
    EPIC("Epic Games"),
    STEAM("Steam Wishlist")
}

@Entity(tableName = "deals")
data class DealItem(
    @PrimaryKey
    val id: String,
    val store: String, // "steam" or "epic"
    val appId: String? = null,
    val title: String,
    val imageUrl: String,
    val storeUrl: String,
    val discountPercent: Int = 0,
    val initialPrice: Long = 0L, // In currency cents/kopecks (e.g. 100000 = 1000.00 UAH)
    val finalPrice: Long = 0L,
    val currency: String = "UAH",
    val formattedFinalPrice: String = "",
    val formattedInitialPrice: String = "",
    val isFree: Boolean = false,
    val isUpcoming: Boolean = false,
    val isWishlist: Boolean = false,
    val isSeasonal: Boolean = false,
    val isHistoricalLow: Boolean = false,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)

data class SteamPriceSearchResult(
    val items: List<DealItem> = emptyList(),
    val hasMore: Boolean = false
)
