package com.tangem.lib.crypto.converter

import com.tangem.lib.crypto.models.XrpTaggedAddress
import com.tangem.utils.converter.Converter
import com.tangem.blockchain.blockchains.xrp.XrpTaggedAddress as BlockchainXrpTaggedAddress

internal class XrpTaggedAddressConverter : Converter<BlockchainXrpTaggedAddress, XrpTaggedAddress> {

    override fun convert(value: BlockchainXrpTaggedAddress): XrpTaggedAddress {
        return XrpTaggedAddress(
            address = value.address,
            destinationTag = value.destinationTag,
        )
    }
}