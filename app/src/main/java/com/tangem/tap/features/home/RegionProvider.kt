package com.tangem.tap.features.home

import android.content.Context
import android.telephony.TelephonyManager
import android.telephony.TelephonyManager.PHONE_TYPE_CDMA
import androidx.compose.ui.text.intl.Locale
import java.lang.ref.WeakReference

/**
 * Created by Anton Zhilenkov on 25/03/2022.
 */
interface RegionProvider {
    fun getRegion(): String?
}

class RegionService(
    private val providers: List<RegionProvider>,
) : RegionProvider {
    override fun getRegion(): String? {
        for (provider in providers) {
            val region = provider.getRegion()
            if (region != null) return region
        }
        return null
    }
}

class TelephonyManagerRegionProvider(context: Context) : RegionProvider {

    private val wContext: WeakReference<Context> = WeakReference(context)

    override fun getRegion(): String? {
        val tm = wContext.get()?.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return null

        val region = when (tm.phoneType) {
            PHONE_TYPE_CDMA -> {
                // Result may be unreliable
                tm.networkCountryIso
            }
            else -> tm.networkCountryIso
        }
        return region.ifEmpty { return null }
    }
}

class LocaleRegionProvider : RegionProvider {
    override fun getRegion(): String = Locale.current.region
}

const val RUSSIA_COUNTRY_CODE = "ru"
const val BELARUS_COUNTRY_CODE = "by"
