package com.tangem.tap.features.home

import androidx.compose.ui.text.intl.Locale

/**
[REDACTED_AUTHOR]
 */
interface RegionProvider {
    fun getRegion(): String?
}

class LocaleRegionProvider : RegionProvider {
    override fun getRegion(): String = Locale.current.region
}

const val RUSSIA_COUNTRY_CODE = "ru"