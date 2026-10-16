package com.tangem.domain.card.models

import com.tangem.common.core.TangemError
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.models.scan.ScanResponse

/**
 * Options of a card scan. The defaults sign in with or add a card that may be new to the app; override only what a
 * screen does differently.
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
    val shouldConfirmWalletOwnership: Boolean = true,
    val canRequestAccessCodeFromRepository: Boolean = false,
    val shouldUpdateCardIdDisplayFormat: Boolean = true,
    val shouldTrackScanFails: Boolean = true,
    val shouldLaunchOnboarding: Boolean = true,
    val callback: Callback,
) {

    /** Scan outcome callbacks; implement only the ones the caller needs */
    interface Callback {

        /** Called to show the progress when the scan starts and to hide it when the app leaves the scan flow */
        suspend fun onProgressStateChange(showProgress: Boolean) {}

        /** The scanned card needs onboarding (or the twins welcome); it is launched right after this call */
        suspend fun onOnboardingRequired() {}

        /** The card failed attestation; called after the user has closed the warning dialog */
        suspend fun onCardVerificationFailed() {}

        /** Any card SDK error except a card attestation failure, including user cancellation */
        suspend fun onFailure(error: TangemError) {}

        suspend fun onSuccess(scanResponse: ScanResponse) {}
    }
}