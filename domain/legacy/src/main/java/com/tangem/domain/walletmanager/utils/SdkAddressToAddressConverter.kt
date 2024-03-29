package com.tangem.domain.walletmanager.utils

import com.tangem.blockchain.common.address.AddressType
import com.tangem.domain.walletmanager.model.Address
import com.tangem.utils.converter.Converter
import com.tangem.blockchain.common.address.Address as SdkAddress

/**
 * Convert [SdkAddress] to [Address]
 *
 * @author Andrew Khokhlov on 26/12/2023
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
