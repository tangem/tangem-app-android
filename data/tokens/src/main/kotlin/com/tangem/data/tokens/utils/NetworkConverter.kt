package com.tangem.data.tokens.utils

import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.tokens.models.Network
import com.tangem.utils.converter.Converter

internal class NetworkConverter : Converter<Network.ID, Network?> {

    override fun convert(value: Network.ID): Network? {
        val blockchain = Blockchain.fromId(value.value)

        return getNetwork(blockchain)
    }

    override fun convertList(input: Collection<Network.ID>): List<Network> {
        return input.mapNotNull(::convert)
    }

    override fun convertSet(input: Collection<Network.ID>): Set<Network> {
        return input.mapNotNullTo(hashSetOf(), ::convert)
    }
}