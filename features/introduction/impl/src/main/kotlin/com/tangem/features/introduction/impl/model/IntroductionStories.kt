package com.tangem.features.introduction.impl.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.card.analytics.Shop
import com.tangem.domain.wallets.usecase.GenerateBuyTangemCardLinkUseCase
import com.tangem.features.storiesv2.StoriesV2PendingAction
import com.tangem.features.storiesv2.StoriesV2Result
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The onboarding story of the welcome screen: its slot and the routing of everything the player hands back.
 */
internal class IntroductionStories(
    private val host: Host,
    private val scope: CoroutineScope,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val urlOpener: UrlOpener,
    private val generateBuyTangemCardLinkUseCase: GenerateBuyTangemCardLinkUseCase,
) {

    interface Host {

        /** The story covers the background video, so every change of [isShown] re-decides whether it may run. */
        fun onStoryVisibilityChanged()

        /** Starts the scan a slide asked for; its outcome returns to [onScanNavigatedAway] / [onScanFailed]. */
        fun scanCardFromStory()
    }

    val navigation = SlotNavigation<Unit>()

    var isShown: Boolean = false
        private set

    /** Armed by the tap that asks for the story, released by the collapse of the sheet that same tap dismissed. */
    var isOpening: Boolean = false
        private set

    /** An ended story kept on screen because navigation is covering it; dismissed on the next resume. */
    private var isDismissPendingResume = false

    /** The scan tap being served; cleared once settled or once the viewer closes the story around it. */
    private var pendingScanAction: StoriesV2PendingAction? = null

    /** The store tap being served; the browser covers the story, so the tap settles on the next resume. */
    private var pendingStoreAction: StoriesV2PendingAction? = null

    fun openAfterSheetCollapse() {
        if (isShown) return
        isOpening = true
    }

    /**
     * Activating the slot swaps the whole screen composition, the sheet included, so a story opened on the tap
     * itself cuts the sheet away in a single frame. The screen reports the end of that collapse here.
     */
    fun onSheetCollapsed() {
        if (!isOpening) return
        isOpening = false
        isShown = true
        host.onStoryVisibilityChanged()
        navigation.activate(Unit)
    }

    /**
     * The story hands back every terminal decision instead of navigating, so this owns the routing table for it.
     * Kept short on purpose: for a bundled composition the result can arrive re-entrantly, inside `activate`.
     */
    fun onResult(result: StoriesV2Result) {
        when (result) {
            // A looping story reports neither on its own; both mean the viewer left it alone.
            StoriesV2Result.Dismissed,
            StoriesV2Result.Completed,
            -> {
                pendingScanAction = null
                pendingStoreAction = null
                close()
            }
            // Nothing to show, so the tap that opened it must still do what its scan slide would have, the way
            // iOS does — otherwise the card row answers a tap with a blank flash.
            is StoriesV2Result.Unavailable -> {
                close()
                startCardScan(action = null)
            }
            // A scan that navigated away is covered by the next screen: tearing the player down now would flash
            // the welcome screen behind the closing NFC sheet, so that teardown waits for the next resume.
            is StoriesV2Result.ActionInvoked -> when (result.actionId) {
                SCAN_WALLET_ACTION_ID -> isDismissPendingResume = true
                else -> close()
            }
        }
    }

    /**
     * Keyed on the action id rather than the target type: the id is what the composition promises, while several
     * actions can share a target type. A remotely delivered action this build does not know must land the viewer
     * back where they acted from instead of on a bare screen.
     */
    fun onAction(action: StoriesV2PendingAction) {
        when (action.actionId) {
            LEARN_MORE_ACTION_ID -> openTangemStore(action)
            SCAN_WALLET_ACTION_ID -> startCardScan(action)
            else -> {
                TangemLogger.e("Unhandled story action: id=${action.actionId}, target=${action.target}")
                action.resume()
            }
        }
    }

    /** The closing analytics must beat the navigation that kills the tree. */
    fun onScanNavigatedAway() {
        pendingScanAction?.end()
        pendingScanAction = null
    }

    fun onScanFailed() {
        val action = pendingScanAction
        pendingScanAction = null
        action?.resume()
    }

    /**
     * Settles whatever left the screen and has now handed it back. A store tap picks its slide up where it paused;
     * a story that ended under a covering screen the viewer backed out of is taken down, so they land on the
     * welcome screen without the sheet the story replaced. The resume re-applies the video on its own.
     */
    fun onScreenResumed() {
        pendingStoreAction?.let { action ->
            pendingStoreAction = null
            action.resume()
        }
        if (!isDismissPendingResume) return
        isDismissPendingResume = false
        isShown = false
        navigation.dismiss()
    }

    private fun close() {
        isShown = false
        host.onStoryVisibilityChanged()
        navigation.dismiss()
    }

    /** [action] is `null` when no story is left to settle around the scan. */
    private fun startCardScan(action: StoriesV2PendingAction?) {
        pendingScanAction = action
        host.scanCardFromStory()
    }

    private fun openTangemStore(action: StoriesV2PendingAction) {
        analyticsEventHandler.send(Basic.ButtonBuy(source = AnalyticsParam.ScreensSources.Stories))
        analyticsEventHandler.send(Shop.ScreenOpened())
        // The browser is a layer over the screen rather than a flow that replaces it, so the story stays suspended
        // on its slide: closing the store puts the viewer back where they were reading, not on a bare welcome.
        pendingStoreAction = action
        scope.launch {
            // The url the story carries is the bare storefront; the generated one adds the language, the utm
            // campaign and the referral promocode, which is what every other buy entry point reports on.
            urlOpener.openUrl(generateBuyTangemCardLinkUseCase(GenerateBuyTangemCardLinkUseCase.Source.Creation))
        }
    }

    private companion object {
        // Declared by the bundled composition in BundledStoryV2ContentSource; a remote one must reuse them.
        const val LEARN_MORE_ACTION_ID = "learn_more"
        const val SCAN_WALLET_ACTION_ID = "scan_wallet"
    }
}