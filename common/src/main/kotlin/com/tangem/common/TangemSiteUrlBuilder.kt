package com.tangem.common

import android.content.res.Resources
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object TangemSiteUrlBuilder {

    suspend fun getUtmTags(campaign: String?): String {
        val langCode = Locale.getDefault().language
        val utmCampaignPart = campaign?.let { "&utm_campaign=$it-$langCode" }.orEmpty()
        val utmContent = deviceLang()?.let { "&utm_content=devicelang-$it" }.orEmpty()
        val appInstanceIdPart = getAppInstanceId()?.let { "&app_instance_id=$it" }.orEmpty()
        return "utm_source=tangem-app&utm_medium=app$utmCampaignPart$utmContent$appInstanceIdPart"
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

    private suspend fun getAppInstanceId(): String? {
        return suspendCoroutine { cont ->
            Firebase.analytics.appInstanceId
                .addOnSuccessListener { id ->
                    cont.resume(id)
                }
                .addOnFailureListener {
                    cont.resume(null)
                }
        }
    }
}