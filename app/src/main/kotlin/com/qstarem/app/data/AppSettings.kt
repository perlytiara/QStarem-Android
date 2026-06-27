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
) {
    companion object {
        const val DEFAULT_HOME_URL = "https://zstream.mov"
    }
}
