package com.tangem.domain.transaction.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.TransactionRepository
import com.tangem.test.core.ProvideTestModels
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.clearMocks
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

/** Unit tests for the destination barrier of [CreateTransferTransactionUseCase]. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CreateTransferTransactionUseCaseTest {

    private val transactionRepository: TransactionRepository = mockk()
    private val useCase = CreateTransferTransactionUseCase(transactionRepository = transactionRepository)

    private val userWalletId: UserWalletId = mockk()
    private val network: Network = mockk()
    private val transactionData: TransactionData.Uncompiled = mockk()

    private val amount = Amount(blockchain = Blockchain.Ethereum, value = BigDecimal.ONE)

    @BeforeEach
    fun resetMocks() {
        clearMocks(transactionRepository)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `rejected destinations`(model: TestModel) = runTest {
        // Act
        val actual = useCase(
            amount = amount,
            memo = null,
            destination = model.destination,
            userWalletId = userWalletId,
            network = network,
        )

        // Assert
        assertThat(actual.leftOrNull()).isInstanceOf(IllegalArgumentException::class.java)
        coVerify(exactly = 0) {
            transactionRepository.createTransferTransaction(
                amount = any(),
                fee = any(),
                memo = any(),
                nonce = any(),
                destination = any(),
                userWalletId = any(),
                network = any(),
            )
        }
    }

    @Test
    fun `GIVEN regular recipient WHEN invoke THEN transaction is built`() = runTest {
        // Arrange
        val destination = "0xfc9013965447f804042a03ae4b98130a8c300a2f"
        coEvery {
            transactionRepository.createTransferTransaction(
                amount = amount,
                fee = null,
                memo = null,
                nonce = null,
                destination = destination,
                userWalletId = userWalletId,
                network = network,
            )
        } returns transactionData

        // Act
        val actual = useCase(
            amount = amount,
            memo = null,
            destination = destination,
            userWalletId = userWalletId,
            network = network,
        )

        // Assert
        assertThat(actual.getOrNull()).isEqualTo(transactionData)
    }

    internal data class TestModel(val destination: String)

    private fun provideTestModels() = listOf(
        TestModel(destination = ""),
        TestModel(destination = "   "),
        TestModel(destination = "0x0000000000000000000000000000000000000000"),
        TestModel(destination = "0x000000000000000000000000000000000000dEaD"),
    )
}