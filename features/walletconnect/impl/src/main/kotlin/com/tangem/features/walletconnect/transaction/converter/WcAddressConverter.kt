package com.tangem.features.walletconnect.transaction.converter

import com.tangem.domain.walletconnect.usecase.method.WcNetworkDerivationState
import com.tangem.features.walletconnect.transaction.entity.common.WcAddressUM
import com.tangem.utils.converter.Converter

private const val ADDRESS_FIRST_PART_LENGTH = 7
private const val ADDRESS_SECOND_PART_LENGTH = 4

internal object WcAddressConverter : Converter<WcNetworkDerivationState, WcAddressUM?> {

    override fun convert(value: WcNetworkDerivationState): WcAddressUM? {
        return when (value) {
            is WcNetworkDerivationState.Single -> null
            is WcNetworkDerivationState.Multiple -> WcAddressUM(
                fullAddress = value.walletAddress,
                shortAddress = value.walletAddress.toShortAddressText(),
            )
        }
    }

    private fun String.toShortAddressText() =
        "${take(ADDRESS_FIRST_PART_LENGTH)}...${takeLast(ADDRESS_SECOND_PART_LENGTH)}"
}