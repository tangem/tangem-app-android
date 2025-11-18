package com.tangem.feature.tokendetails.presentation.tokendetails.route

import com.tangem.core.decompose.navigation.Route
import com.tangem.domain.models.TokenReceiveConfig
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.tokens.model.details.TokenAction
import kotlinx.serialization.Serializable

@Serializable
sealed class TokenDetailsBottomSheetConfig : Route {

    @Serializable
    data class Receive(val tokenReceiveConfig: TokenReceiveConfig) : TokenDetailsBottomSheetConfig()

    @Serializable
    data class YieldSupplyWarning(
        val cryptoCurrency: CryptoCurrency,
        val tokenAction: TokenAction,
    ) : TokenDetailsBottomSheetConfig()
}