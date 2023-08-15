package com.tangem.data.tokens.utils

import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.tokens.models.Network
import com.tangem.utils.converter.Converter
import timber.log.Timber

internal class NetworkConverter : Converter<Network.ID, Network?> {

    override fun convert(value: Network.ID): Network? {
        val blockchain = Blockchain.fromId(value.value)

        if (blockchain == Blockchain.Unknown) {
            Timber.e("Unable to convert Unknown blockchain to the domain network model")
            return null
        }

        return Network(
            id = value,
            name = blockchain.fullName,
        )
    }

    override fun convertList(input: Collection<Network.ID>): List<Network> {
        return input.mapNotNull(::convert)
    }

    override fun convertSet(input: Collection<Network.ID>): Set<Network> {
        return input.mapNotNullTo(hashSetOf(), ::convert)
    }
}