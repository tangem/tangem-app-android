package com.tangem.domain.walletmanager.utils

import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchainsdk.models.UpdateWalletManagerResult.Address
import com.tangem.utils.converter.Converter
import com.tangem.blockchain.common.address.Address as SdkAddress

/**
 * Convert [SdkAddress] to [Address]
 *
[REDACTED_AUTHOR]
 */
internal object SdkAddressToAddressConverter : Converter<SdkAddress, Address> {

    override fun convert(value: SdkAddress): Address {
        return Address(
            value = value.value,
            type = when (value.type) {
                AddressType.Default -> Address.Type.Primary
                AddressType.Legacy -> Address.Type.Secondary
            },
        )
    }
}