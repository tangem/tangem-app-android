package com.tangem.domain.features

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.common.extensions.fromNetworkId
import com.tangem.domain.common.extensions.toNetworkId
import org.junit.Test

class BlockchainNetworkIdTests {
    @Test
    fun checkAppropriateImplementationForNetworkIds() {
        val unimplementedIds = Blockchain.values()
            .toMutableList()
            .apply { remove(Blockchain.Unknown) }
            .map { it to Blockchain.fromNetworkId(it.toNetworkId()) }
            .filter { it.second == null }

        if (unimplementedIds.isEmpty()) return

        val message = unimplementedIds.joinToString("\n") { it.first.fullName }
        Truth.assert_().withMessage(message).fail()
    }
}