package com.tangem.features.introduction.impl.model

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.card.analytics.IntroductionProcess
import com.tangem.domain.card.analytics.Shop
import com.tangem.domain.wallets.usecase.GenerateBuyTangemCardLinkUseCase
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

internal class IntroductionStore(
    private val scope: CoroutineScope,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val urlOpener: UrlOpener,
    private val generateBuyTangemCardLinkUseCase: GenerateBuyTangemCardLinkUseCase,
) {

    /**
     * Armed by the tap rather than by the browser appearing: the link is built asynchronously, and a second tap
     * landing before the browser covers the screen must not open it twice.
     */
    private var isOpen = false

    fun open() {
        if (isOpen) return
        isOpen = true
        analyticsEventHandler.send(IntroductionProcess.ButtonBuyTangem())
        analyticsEventHandler.send(Shop.ScreenOpened())
        scope.launch {
            val link = runSuspendCatching {
                generateBuyTangemCardLinkUseCase(GenerateBuyTangemCardLinkUseCase.Source.Welcome)
            }.onFailure { error ->
                TangemLogger.e("Failed to build the Buy Tangem link", error)
            }.getOrNull()
            if (link != null) {
                urlOpener.openUrl(link)
                // In split screen or with no browser nothing pauses this screen, so no resume would release it.
                delay(BROWSER_COVER_TIMEOUT)
            }
            isOpen = false
        }
    }

    fun onScreenResumed() {
        isOpen = false
    }

    companion object {
        val BROWSER_COVER_TIMEOUT = 1.seconds
    }
}