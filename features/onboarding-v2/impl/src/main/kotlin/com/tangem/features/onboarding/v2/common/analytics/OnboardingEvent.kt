package com.tangem.features.onboarding.v2.common.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam

sealed class OnboardingEvent(
    category: String,
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent(category, event, params) {

    sealed class Backup(
        event: String,
        params: Map<String, String> = mapOf(),
    ) : OnboardingEvent("Onboarding / Backup", event, params) {

        class ScreenOpened : Backup("Backup Screen Opened")
        class Started : Backup("Backup Started")
        class Skipped : Backup("Backup Skipped")
        class SettingAccessCodeStarted : Backup("Setting Access Code Started")
        class AccessCodeEntered : Backup("Access Code Entered")
        class AccessCodeReEntered : Backup("Access Code Re-entered")

        class Finished(cardsCount: Int) : Backup(
            event = "Backup Finished",
            params = mapOf("Cards count" to "$cardsCount"),
        )

        class ResetCancelEvent : Backup(
            event = "Reset Card Notification",
            params = mapOf("Option" to "Cancel"),
        )

        class ResetPerformEvent : Backup(
            event = "Reset Card Notification",
            params = mapOf("Option" to "Reset"),
        )

        class ResumeInterruptedBackup : Backup(
            event = "Notice - Backup Canceled",
            params = mapOf("Action" to "Resume"),
        )

        class CancelInterruptedBackup : Backup(
            event = "Notice - Backup Canceled",
            params = mapOf("Action" to "Cancel"),
        )
    }

    sealed class Twins(
        event: String,
        params: Map<String, String> = mapOf(),
    ) : OnboardingEvent("Onboarding / Twins", event, params) {

        class ScreenOpened : Twins("Twinning Screen Opened")
        class SetupFinished : Twins("Twin Setup Finished")
    }

    data class OfflineAttestationFailed(
        val source: AnalyticsParam.ScreensSources,
    ) : OnboardingEvent(
        category = "Error",
        event = "Offline Attestation Failed",
        params = mapOf(AnalyticsParam.SOURCE to source.value),
    )
}