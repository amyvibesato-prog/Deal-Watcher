package com.example.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("deal_watcher_prefs", Context.MODE_PRIVATE)

    var steamId: String
        get() = prefs.getString(KEY_STEAM_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_STEAM_ID, value.trim()).apply()

    var steamApiKey: String
        get() = prefs.getString(KEY_STEAM_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_STEAM_API_KEY, value.trim()).apply()

    var notifySteam: Boolean
        get() = prefs.getBoolean(KEY_NOTIFY_STEAM, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFY_STEAM, value).apply()

    var notifyEpic: Boolean
        get() = prefs.getBoolean(KEY_NOTIFY_EPIC, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFY_EPIC, value).apply()

    var minDiscountPercent: Int
        get() = prefs.getInt(KEY_MIN_DISCOUNT, 0)
        set(value) = prefs.edit().putInt(KEY_MIN_DISCOUNT, value).apply()

    var currency: String
        get() = prefs.getString(KEY_CURRENCY, "UAH") ?: "UAH"
        set(value) = prefs.edit().putString(KEY_CURRENCY, value).apply()

    var lastUpdatedTime: Long
        get() = prefs.getLong(KEY_LAST_UPDATED, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_UPDATED, value).apply()

    fun getNotifiedDealIds(): Set<String> {
        return prefs.getStringSet(KEY_NOTIFIED_DEAL_IDS, emptySet()) ?: emptySet()
    }

    fun saveNotifiedDealIds(ids: Set<String>) {
        prefs.edit().putStringSet(KEY_NOTIFIED_DEAL_IDS, ids).apply()
    }

    companion object {
        private const val KEY_STEAM_ID = "steam_id"
        private const val KEY_STEAM_API_KEY = "steam_api_key"
        private const val KEY_NOTIFY_STEAM = "notify_steam"
        private const val KEY_NOTIFY_EPIC = "notify_epic"
        private const val KEY_MIN_DISCOUNT = "min_discount"
        private const val KEY_CURRENCY = "currency"
        private const val KEY_LAST_UPDATED = "last_updated"
        private const val KEY_NOTIFIED_DEAL_IDS = "notified_deal_ids"
    }
}
