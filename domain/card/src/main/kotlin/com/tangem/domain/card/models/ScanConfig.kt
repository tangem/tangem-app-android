package com.tangem.domain.card.models

import com.tangem.core.analytics.models.AnalyticsParam

/**
 * Options of a card scan. Prefer the [signIn] and [rescanKnownCard] presets over a custom combination.
 *
 * @property analyticsSource                       screen that started the scan
 * @property shouldConfirmWalletOwnership          lets the SDK confirm that the wallet belongs to the user
 * ("This is my wallet")
 * @property canRequestAccessCodeFromRepository    allows the SDK to use the saved access code
 * @property shouldUpdateCardIdDisplayFormat       resets the card ID display format before the scan and sets it
 * for the scanned card after
 * @property shouldTrackScanFails                  counts failed scans to offer help with scanning
 * @property shouldLaunchOnboarding                launches onboarding (or the twins welcome) when the scanned card needs it
 */
data class ScanConfig(
    val analyticsSource: AnalyticsParam.ScreensSources,
    val shouldConfirmWalletOwnership: Boolean,
    val canRequestAccessCodeFromRepository: Boolean,
    val shouldUpdateCardIdDisplayFormat: Boolean,
    val shouldTrackScanFails: Boolean,
    val shouldLaunchOnboarding: Boolean,
) {

    companion object {

        /** Sign in with or add a card that may be new to the app */
        fun signIn(analyticsSource: AnalyticsParam.ScreensSources): ScanConfig = ScanConfig(
            analyticsSource = analyticsSource,
            shouldConfirmWalletOwnership = true,
            canRequestAccessCodeFromRepository = false,
            shouldUpdateCardIdDisplayFormat = true,
            shouldTrackScanFails = true,
            shouldLaunchOnboarding = true,
        )

        /** Re-scan the card of a wallet already saved in the app, e.g. on the card settings screen */
        fun rescanKnownCard(analyticsSource: AnalyticsParam.ScreensSources): ScanConfig = ScanConfig(
            analyticsSource = analyticsSource,
            shouldConfirmWalletOwnership = false,
            canRequestAccessCodeFromRepository = true,
            shouldUpdateCardIdDisplayFormat = false,
            shouldTrackScanFails = false,
            shouldLaunchOnboarding = false,
        )
    }
}