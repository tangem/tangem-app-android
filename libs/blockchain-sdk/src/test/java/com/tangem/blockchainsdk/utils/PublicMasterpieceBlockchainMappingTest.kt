package com.tangem.blockchainsdk.utils

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import org.junit.jupiter.api.Test

internal class PublicMasterpieceBlockchainMappingTest {

    @Test
    fun `GIVEN public-masterpiece network id WHEN fromNetworkId THEN returns PublicMasterpiece`() {
        assertThat(Blockchain.fromNetworkId("public-masterpiece")).isEqualTo(Blockchain.PublicMasterpiece)
    }

    @Test
    fun `GIVEN public-masterpiece id WHEN fromId THEN returns PublicMasterpiece`() {
        assertThat(Blockchain.fromId("public-masterpiece")).isEqualTo(Blockchain.PublicMasterpiece)
    }

    @Test
    fun `GIVEN PublicMasterpiece WHEN toNetworkId THEN returns public-masterpiece`() {
        assertThat(Blockchain.PublicMasterpiece.toNetworkId()).isEqualTo("public-masterpiece")
    }

    @Test
    fun `GIVEN PublicMasterpiece WHEN toCoinId THEN returns public-meme-token`() {
        assertThat(Blockchain.PublicMasterpiece.toCoinId()).isEqualTo("public-meme-token")
    }
}