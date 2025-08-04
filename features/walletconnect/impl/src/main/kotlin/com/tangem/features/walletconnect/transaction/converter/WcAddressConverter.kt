package com.tangem.features.walletconnect.transaction.converter

import com.tangem.domain.walletconnect.usecase.method.WcNetworkDerivationState
import com.tangem.utils.converter.Converter

internal object WcAddressConverter : Converter<WcNetworkDerivationState, String?> {

    override fun convert(value: WcNetworkDerivationState): String? {
        return when (value) {
            is WcNetworkDerivationState.Single -> null
            is WcNetworkDerivationState.Multiple -> value.walletAddress
        }
    }
}