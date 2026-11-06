package com.tangem.common

import android.content.res.Resources
import com.tangem.core.analytics.AppInstanceIdProvider
import java.util.Locale
import javax.inject.Inject

class TangemSiteUrlBuilder @Inject constructor(
    private val appInstanceIdProvider: AppInstanceIdProvider,
) {

    suspend fun getUtmTags(campaign: String?): String {
        val langCode = Locale.getDefault().language
        val utmCampaignPart = campaign?.let { "&utm_campaign=$it-$langCode" }.orEmpty()
        val utmContent = deviceLang()?.let { "&utm_content=devicelang-$it" }.orEmpty()
        val appInstanceIdPart = appInstanceIdProvider.getAppInstanceId()?.let { "&app_instance_id=$it" }.orEmpty()
        return "utm_source=tangem-app&utm_medium=app$utmCampaignPart$utmContent$appInstanceIdPart"
    }

    fun getRefCodeTag(refCode: String?): String {
        return refCode?.let { "promocode=$it" }.orEmpty()
    }

    suspend fun url(path: String, campaign: String): String {
        val normalizedPath = path.trim('/')
        return "https://tangem.com/$normalizedPath?${getUtmTags(campaign)}"
    }

    private fun deviceLang(): String? {
        return runCatching {
            Resources.getSystem().configuration.locales.get(0).toLanguageTag()
        }.getOrNull()
    }

    companion object {

        const val NOTE_MIGRATION_URL = "https://tangem.com/en/?promocode=Note10"

        const val HELP_CENTER_SWAP_URL =
            "https://tangem.com/en/help-center/tangem-wallet-core-functionality/how-to-swap-coins-and-tokens/"

        const val YIELD_MODE_TERMS_URL = "https://tangem.com/docs/en/yield-mode-terms.pdf"
    }
}