package com.tangem.domain.card.models

import com.tangem.common.core.TangemError
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.models.scan.ScanResponse

/**
 * Options of a card scan. Prefer the [signIn] and [rescanKnownCard] presets over a custom combination.
 *
 * @property analyticsSource                    screen that started the scan
 * @property cardId                             ID of the card to scan; any card is accepted when `null`
 * @property shouldConfirmWalletOwnership       lets the SDK confirm that the wallet belongs to the user
 * ("This is my wallet")
 * @property canRequestAccessCodeFromRepository allows the SDK to use the saved access code
 * @property shouldUpdateCardIdDisplayFormat    resets the card ID display format before the scan and sets it
 * for the scanned card after
 * @property shouldTrackScanFails               counts failed scans to offer help with scanning
 * @property shouldLaunchOnboarding             launches onboarding (or the twins welcome) when the scanned card
 * needs it
 * @property callback                           receives the scan outcome
 */
data class ScanConfig(
    val analyticsSource: AnalyticsParam.ScreensSources,
    val cardId: String? = null,
    val shouldConfirmWalletOwnership: Boolean,
    val canRequestAccessCodeFromRepository: Boolean,
    val shouldUpdateCardIdDisplayFormat: Boolean,
    val shouldTrackScanFails: Boolean,
    val shouldLaunchOnboarding: Boolean,
    val callback: Callback,
) {

    /** Scan outcome callbacks; implement only the ones the caller needs */
    interface Callback {

        /** Called to show the progress when the scan starts and to hide it when the app leaves the scan flow */
        suspend fun onProgressStateChange(showProgress: Boolean) {}

        /** The card has no wallet (or the twins welcome wasn't shown); onboarding is launched right after */
        suspend fun onWalletNotCreated() {}

        /** The card attestation warning dialog was closed */
        suspend fun onCancel() {}

        /** Any card SDK error except a card attestation failure, including user cancellation */
        suspend fun onFailure(error: TangemError) {}

        suspend fun onSuccess(scanResponse: ScanResponse) {}
    }

    companion object {

        /** Sign in with or add a card that may be new to the app */
        fun signIn(analyticsSource: AnalyticsParam.ScreensSources, callback: Callback): ScanConfig = ScanConfig(
            analyticsSource = analyticsSource,
            shouldConfirmWalletOwnership = true,
            canRequestAccessCodeFromRepository = false,
            shouldUpdateCardIdDisplayFormat = true,
            shouldTrackScanFails = true,
            shouldLaunchOnboarding = true,
            callback = callback,
        )

        /** Re-scan the card of a wallet already saved in the app, e.g. on the card settings screen */
        fun rescanKnownCard(analyticsSource: AnalyticsParam.ScreensSources, callback: Callback): ScanConfig =
            ScanConfig(
                analyticsSource = analyticsSource,
                shouldConfirmWalletOwnership = false,
                canRequestAccessCodeFromRepository = true,
                shouldUpdateCardIdDisplayFormat = false,
                shouldTrackScanFails = false,
                shouldLaunchOnboarding = false,
                callback = callback,
            )
    }
}