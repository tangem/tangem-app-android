package com.tangem.features.staking.impl.presentation.state.helpers

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.features.staking.impl.fixtures.p2pIntegration
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.Provider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class P2PEthPoolFeeLoaderTest {

    private val transactionCreator: P2PEthPoolTransactionCreator = mockk()
    private var currentStatus = status(currency = ethereumCoin)

    private val loader = P2PEthPoolFeeLoader(
        transactionCreator = transactionCreator,
        cryptoCurrencyStatusProvider = Provider { currentStatus },
        integration = p2pIntegration(),
    )

    @BeforeEach
    fun setUp() {
        clearMocks(transactionCreator)
        currentStatus = status(currency = ethereumCoin)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN gas limit and max fee per gas WHEN getFee THEN fee in ETH with eighteen decimals`(model: FeeModel) =
        runTest {
            // Arrange
            coEvery {
                transactionCreator.createTransaction(any())
            } returns unsignedTx(gasLimit = model.gasLimit, maxFeePerGas = model.maxFeePerGas).right()
            val captured = Captured()

            // Act
            loader.load(captured)

            // Assert
            val expected = Fee.Common(Amount(currencySymbol = "ETH", value = model.expected, decimals = 18))
            assertThat(captured.fees).containsExactly(expected to false)
        }

    @Test
    fun `GIVEN creator left WHEN getFee THEN staking fee error forwarded`() = runTest {
        // Arrange
        val error = StakingError.DomainError("Invalid state for transaction creation")
        coEvery { transactionCreator.createTransaction(any()) } returns error.left()
        val captured = Captured()

        // Act
        loader.load(captured)

        // Assert
        assertThat(captured.stakingErrors).containsExactly(error)
        assertThat(captured.fees).isEmpty()
    }

    @Test
    fun `GIVEN status changes between calls WHEN getFee twice THEN creator receives the fresh status`() = runTest {
        // Arrange
        coEvery { transactionCreator.createTransaction(any()) } returns unsignedTx().right()
        val oldStatus = status(ethereumCoin, address = "0xold")
        val newStatus = status(ethereumCoin, address = "0xnew")

        // Act
        currentStatus = oldStatus
        loader.load(Captured())
        currentStatus = newStatus
        loader.load(Captured())

        // Assert
        coVerifyOrder {
            transactionCreator.createTransaction(oldStatus)
            transactionCreator.createTransaction(newStatus)
        }
    }

    private fun provideTestModels() = listOf(
        FeeModel(BigDecimal("21000"), BigDecimal("30000000000"), BigDecimal("0.000630000000000000")),
        FeeModel(BigDecimal("150000"), BigDecimal("12345678901"), BigDecimal("0.001851851835150000")),
    )

    private class Captured {
        val fees = mutableListOf<Pair<Fee, Boolean>>()
        val stakingErrors = mutableListOf<StakingError>()
    }

    private suspend fun P2PEthPoolFeeLoader.load(captured: Captured) {
        getFee(
            onStakingFee = { fee, isApproximate -> captured.fees += fee to isApproximate },
            onStakingFeeError = { captured.stakingErrors += it },
            onApprovalFee = {},
            onFeeError = {},
        )
    }

    internal data class FeeModel(val gasLimit: BigDecimal, val maxFeePerGas: BigDecimal, val expected: BigDecimal)
}