package com.tangem.features.introduction.impl.model

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.card.analytics.IntroductionProcess
import javax.inject.Inject

internal class IntroductionLegalLinks @Inject constructor(
    private val urlOpener: UrlOpener,
    private val analyticsEventHandler: AnalyticsEventHandler,
) {

    fun openTermsOfService() = open(IntroductionProcess.LegalOpened.Document.TermsOfService, TERMS_OF_SERVICE_URL)

    private fun open(document: IntroductionProcess.LegalOpened.Document, url: String) {
        analyticsEventHandler.send(IntroductionProcess.LegalOpened(document))
        urlOpener.openUrl(url)
    }

    private companion object {
        const val TERMS_OF_SERVICE_URL = "https://tangem.com/tangem_tos.html"
    }
}