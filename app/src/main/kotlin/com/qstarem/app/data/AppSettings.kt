package com.qstarem.app.data

enum class AdBlockerChoice {
    UBLOCK,
    ADGUARD,
    NONE,
}

data class AppSettings(
    val homeUrl: String = DEFAULT_HOME_URL,
    val adBlocker: AdBlockerChoice = AdBlockerChoice.UBLOCK,
    val pStreamEnabled: Boolean = true,
    val appIconId: Int = DEFAULT_APP_ICON_ID,
) {
    companion object {
        const val DEFAULT_HOME_URL = "https://zstream.mov"
        const val DEFAULT_APP_ICON_ID = 1
        const val MIN_APP_ICON_ID = 1
        const val MAX_APP_ICON_ID = 6
    }
}
