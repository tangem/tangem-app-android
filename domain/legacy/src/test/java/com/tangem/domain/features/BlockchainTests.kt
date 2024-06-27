package com.tangem.domain.features

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.blockchainsdk.utils.toNetworkId
import org.junit.Test

class BlockchainTests {
    @Test
    fun allNetworkIdsAreImplemented() {
        val unimplementedIds = Blockchain.entries
            .toMutableList()
            .apply {
                remove(Blockchain.Unknown)
            }
            .map { it to Blockchain.fromNetworkId(it.toNetworkId()) }
            .mapNotNull { if (it.second == null) it.first else null }

        Truth.assertThat(unimplementedIds).isEmpty()
    }
}