package com.tangem.domain.features

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.common.extensions.fromNetworkId
import com.tangem.domain.common.extensions.toNetworkId
import org.junit.Test

class BlockchainTests {
    @Test
    fun allNetworkIdsAreImplemented() {
        val unimplementedIds = Blockchain.values()
            .toMutableList()
            .apply {
                remove(Blockchain.Unknown)
                remove(Blockchain.Optimism)
            }
            .map { it to Blockchain.fromNetworkId(it.toNetworkId()) }
            .mapNotNull { if (it.second == null) it.first else null }

        Truth.assertThat(unimplementedIds).isEmpty()
    }
}
