package com.tangem.tap.features.home

import androidx.compose.ui.text.intl.Locale

/**
 * Created by Anton Zhilenkov on 25/03/2022.
 */
interface RegionProvider {
    fun getRegion(): String?
}

class LocaleRegionProvider : RegionProvider {
    override fun getRegion(): String = Locale.current.region
}

const val RUSSIA_COUNTRY_CODE = "ru"
