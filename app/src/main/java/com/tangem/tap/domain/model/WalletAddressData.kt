package com.tangem.tap.domain.model

import com.tangem.blockchain.common.address.AddressType

internal data class WalletAddressData(
    val address: String,
    val type: AddressType,
    val shareUrl: String,
    val exploreUrl: String,
)