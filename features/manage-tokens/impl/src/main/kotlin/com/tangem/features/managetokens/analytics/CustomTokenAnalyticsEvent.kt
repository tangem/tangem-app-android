package com.tangem.features.managetokens.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.features.managetokens.component.ManageTokensSource

internal sealed class CustomTokenAnalyticsEvent(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent(
    category = "Manage Tokens / Custom",
    event = event,
    params = params,
) {

    class ScreenOpened(source: ManageTokensSource) : CustomTokenAnalyticsEvent(
        event = "Custom Token Screen Opened",
        params = mapOf(AnalyticsParam.Key.SOURCE to source.analyticsName),
    )

    class CustomTokenWasAdded(
        currencySymbol: String,
        derivationPath: String,
        source: ManageTokensSource,
    ) : CustomTokenAnalyticsEvent(
        event = "Custom Token Was Added",
        params = mapOf(
            AnalyticsParam.Key.TOKEN_PARAM to currencySymbol,
            AnalyticsParam.Key.DERIVATION to derivationPath,
            AnalyticsParam.Key.SOURCE to source.analyticsName,
        ),
    )

    class NetworkSelected(networkName: String, source: ManageTokensSource) : CustomTokenAnalyticsEvent(
        event = "Custom Token Network Selected",
        params = mapOf(
            AnalyticsParam.Key.BLOCKCHAIN to networkName,
            AnalyticsParam.Key.SOURCE to source.analyticsName,
        ),
    )

    class DerivationSelected(derivationName: String, source: ManageTokensSource) : CustomTokenAnalyticsEvent(
        event = "Custom Token Derivation Selected",
        params = mapOf(
            AnalyticsParam.Key.DERIVATION to derivationName,
            AnalyticsParam.Key.SOURCE to source.analyticsName,
        ),
    )

    class Address(isValid: Boolean, source: ManageTokensSource) : CustomTokenAnalyticsEvent(
        event = "Custom Token Address",
        params = mapOf(
            AnalyticsParam.Key.VALIDATION to AnalyticsParam.Validation.from(isValid),
            AnalyticsParam.Key.SOURCE to source.analyticsName,
        ),
    )

    class Name(source: ManageTokensSource) : CustomTokenAnalyticsEvent(
        event = "Custom Token Name",
        params = mapOf(AnalyticsParam.Key.SOURCE to source.analyticsName),
    )

    class Symbol(source: ManageTokensSource) : CustomTokenAnalyticsEvent(
        event = "Custom Token Symbol",
        params = mapOf(AnalyticsParam.Key.SOURCE to source.analyticsName),
    )

    class Decimals(source: ManageTokensSource) : CustomTokenAnalyticsEvent(
        event = "Custom Token Decimals",
        params = mapOf(AnalyticsParam.Key.SOURCE to source.analyticsName),
    )

    class ButtonCustomToken(source: ManageTokensSource) : CustomTokenAnalyticsEvent(
        event = "Button - Custom Token",
        params = mapOf(AnalyticsParam.Key.SOURCE to source.analyticsName),
    )
}