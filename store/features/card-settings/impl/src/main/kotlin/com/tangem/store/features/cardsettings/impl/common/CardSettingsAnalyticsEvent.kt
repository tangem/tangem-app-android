package com.tangem.store.features.cardsettings.impl.common

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam

internal sealed class CardSettingsAnalyticsEvent(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category = "Settings / Card Settings", event = event, params = params) {

    class ButtonFactoryReset : CardSettingsAnalyticsEvent("Button - Factory Reset")
    class FactoryResetFinished(cardsCount: Int? = null) : CardSettingsAnalyticsEvent(
        event = "Factory Reset Finished",
        params = buildMap {
            cardsCount?.let { put("Cards Count", "$it") }
        },
    )

    class FactoryResetCanceled(cardsCount: Int) : CardSettingsAnalyticsEvent(
        event = "Factory Reset Canceled",
        params = mapOf("Cards Count" to "$cardsCount"),
    )

    class UserCodeChanged : CardSettingsAnalyticsEvent("User Code Changed")
    class ButtonChangeSecurityMode : CardSettingsAnalyticsEvent("Button - Change Security Mode")

    class ButtonChangeUserCode(type: AnalyticsParam.UserCode) : CardSettingsAnalyticsEvent(
        event = "Button - Change User Code",
        params = mapOf("Type" to type.value),
    )

    class SecurityModeChanged(mode: Mode) : CardSettingsAnalyticsEvent(
        event = "Security Mode Changed",
        params = mapOf("Mode" to mode.value),
    ) {
        enum class Mode(val value: String) {
            AccessCode("Access Code"),
            Passcode("Passcode"),
            LongTap("Long Tap"),
        }
    }

    class AccessCodeRecoveryChanged(status: AnalyticsParam.AccessCodeRecoveryStatus) : CardSettingsAnalyticsEvent(
        event = "Access Code Recovery Changed",
        params = mapOf(status.key to status.value),
    )
}