package com.tangem.features.yield.supply.impl.subcomponents.stopearning.model

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.network.Network
import com.tangem.test.core.ProvideTestModels
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class YieldSupplyWithdrawalClaimBatchingTest {

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN a network WHEN asked about batching THEN only Avalanche is left out`(model: BatchingModel) {
        // Arrange
        val network: Network = mockk { every { rawId } returns model.networkId }

        // Act
        val actual = YieldSupplyWithdrawalClaimBatching.isSupported(network)

        // Assert
        assertThat(actual).isEqualTo(model.isSupported)
    }

    internal data class BatchingModel(val networkId: String, val isSupported: Boolean) {
        override fun toString(): String = networkId
    }

    private fun provideTestModels() = listOf(
        BatchingModel(networkId = "ethereum", isSupported = true),
        BatchingModel(networkId = "polygon-pos", isSupported = true),
        BatchingModel(networkId = "base", isSupported = true),
        BatchingModel(networkId = "avalanche", isSupported = false),
        BatchingModel(networkId = "avalanche-2", isSupported = false),
        BatchingModel(networkId = "avalanche/test", isSupported = false),
    )
}