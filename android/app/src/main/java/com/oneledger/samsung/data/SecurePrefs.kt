package com.oneledger.samsung.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.UUID

/**
 * 使用 Android Keystore AES256-GCM 加密存放敏感設定（Gemini API Key、雲端同步 Token 等）。
 */
class SecurePrefs(context: Context) {
    private val prefs: SharedPreferences = run {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "oneledger_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (_: Exception) {
            context.getSharedPreferences("oneledger_fallback_prefs", Context.MODE_PRIVATE)
        }
    }

    val deviceId: String
        get() {
            val existing = prefs.getString(KEY_DEVICE_ID, null)
            if (existing != null) return existing
            val created = "galaxy-${UUID.randomUUID().toString().take(8)}"
            prefs.edit().putString(KEY_DEVICE_ID, created).apply()
            return created
        }

    var currency: String
        get() = prefs.getString(KEY_CURRENCY, "NT$") ?: "NT$"
        set(value) = prefs.edit().putString(KEY_CURRENCY, value).apply()

    var themeMode: String // "system" | "light" | "dark"
        get() = prefs.getString(KEY_THEME, "system") ?: "system"
        set(value) = prefs.edit().putString(KEY_THEME, value).apply()

    var geminiApiKey: String
        get() = prefs.getString(KEY_GEMINI_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GEMINI_API_KEY, value.trim()).apply()

    var syncServerUrl: String
        get() = prefs.getString(KEY_SYNC_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SYNC_URL, value.trim().trimEnd('/')).apply()

    var syncToken: String
        get() = prefs.getString(KEY_SYNC_TOKEN, "oneledger-dev-token") ?: "oneledger-dev-token"
        set(value) = prefs.edit().putString(KEY_SYNC_TOKEN, value.trim()).apply()

    var lastSyncAt: Long
        get() = prefs.getLong(KEY_LAST_SYNC_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC_AT, value).apply()

    var listenerEnabled: Boolean
        get() = prefs.getBoolean(KEY_LISTENER_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_LISTENER_ENABLED, value).apply()

    var maskCardLast4: Boolean
        get() = prefs.getBoolean(KEY_MASK_CARD_LAST4, true)
        set(value) = prefs.edit().putBoolean(KEY_MASK_CARD_LAST4, value).apply()

    var monitoredAppsCsv: String
        get() = prefs.getString(
            KEY_MONITORED_APPS,
            "Samsung Wallet,玉山 Wallet,國泰 CUBE,LINE Pay,街口支付,台新 Richart,中信 Home Bank"
        ) ?: ""
        set(value) = prefs.edit().putString(KEY_MONITORED_APPS, value).apply()

    var listenerKeywords: String
        get() = prefs.getString(
            KEY_LISTENER_KEYWORDS,
            "刷卡,消費,扣款,付款成功,交易,轉帳,入帳"
        ) ?: ""
        set(value) = prefs.edit().putString(KEY_LISTENER_KEYWORDS, value).apply()

    var pushOnAutoRecord: Boolean
        get() = prefs.getBoolean(KEY_PUSH_AUTO, true)
        set(value) = prefs.edit().putBoolean(KEY_PUSH_AUTO, value).apply()

    var dailyReminderEnabled: Boolean
        get() = prefs.getBoolean(KEY_REMINDER_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_REMINDER_ENABLED, value).apply()

    var dailyReminderHour: Int
        get() = prefs.getInt(KEY_REMINDER_HOUR, 21)
        set(value) = prefs.edit().putInt(KEY_REMINDER_HOUR, value.coerceIn(0, 23)).apply()

    var dailyReminderMinute: Int
        get() = prefs.getInt(KEY_REMINDER_MINUTE, 30)
        set(value) = prefs.edit().putInt(KEY_REMINDER_MINUTE, value.coerceIn(0, 59)).apply()

    var seeded: Boolean
        get() = prefs.getBoolean(KEY_SEEDED, false)
        set(value) = prefs.edit().putBoolean(KEY_SEEDED, value).apply()

    fun getMonitoredAppList(): List<String> =
        monitoredAppsCsv.split(",", "，", "\n").map { it.trim() }.filter { it.isNotEmpty() }

    companion object {
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_CURRENCY = "currency"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val KEY_SYNC_URL = "sync_server_url"
        private const val KEY_SYNC_TOKEN = "sync_token"
        private const val KEY_LAST_SYNC_AT = "last_sync_at"
        private const val KEY_LISTENER_ENABLED = "listener_enabled"
        private const val KEY_MASK_CARD_LAST4 = "mask_card_last4"
        private const val KEY_MONITORED_APPS = "monitored_apps"
        private const val KEY_LISTENER_KEYWORDS = "listener_keywords"
        private const val KEY_PUSH_AUTO = "push_on_auto_record"
        private const val KEY_REMINDER_ENABLED = "daily_reminder_enabled"
        private const val KEY_REMINDER_HOUR = "daily_reminder_hour"
        private const val KEY_REMINDER_MINUTE = "daily_reminder_minute"
        private const val KEY_SEEDED = "seeded_v1"
    }
}
