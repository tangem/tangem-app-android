package com.tangem.domain.features

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.common.extensions.fromNetworkId
import com.tangem.domain.common.extensions.toNetworkId
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class BlockchainNetworkIdTests {
    @Test
    fun checkAppropriateImplementationForNetworkIds() {
        val unimplementedIds = Blockchain.values()
            .toMutableList()
            .apply { remove(Blockchain.Unknown) }.map {
                val networkId = it.toNetworkId()
                networkId to Blockchain.fromNetworkId(networkId)
            }.filter { it.second == null }

        if (unimplementedIds.isEmpty()) return

        val message = unimplementedIds.joinToString("\n") { it.first }
        Truth.assert_().withMessage(message).fail()
    }
}