package com.tangem.domain.transaction.usecase.gasless

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.transaction.TronGaslessTransactionRepository
import com.tangem.domain.transaction.models.tron.TronGaslessToken
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class IsTronGaslessSupportedUseCaseTest {

    private val repository: TronGaslessTransactionRepository = mockk()
    private val useCase = IsTronGaslessSupportedUseCase(repository)

    private val factory = MockCryptoCurrencyFactory()
    private val usdtContract = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t"
    private val usdtToken = factory.createToken(blockchain = Blockchain.Tron, contractAddress = usdtContract)
    private val tronCoin = factory.createCoin(blockchain = Blockchain.Tron)
    private val usdt = TronGaslessToken(contractAddress = usdtContract, symbol = "USDT", decimals = 6)

    @Test
    fun `GIVEN non-token currency WHEN invoke THEN false`() = runTest {
        // Act
        val result = useCase(network = tronCoin.network, currency = tronCoin)

        // Assert
        assertThat(result).isFalse()
    }

    @Test
    fun `GIVEN tron usdt token and supported WHEN invoke THEN true`() = runTest {
        // Arrange
        coEvery { repository.getSupportedTokens() } returns listOf(usdt)

        // Act
        val result = useCase(network = usdtToken.network, currency = usdtToken)

        // Assert
        assertThat(result).isTrue()
    }

    @Test
    fun `GIVEN supported list lacks token WHEN invoke THEN false`() = runTest {
        // Arrange
        coEvery { repository.getSupportedTokens() } returns emptyList()

        // Act
        val result = useCase(network = usdtToken.network, currency = usdtToken)

        // Assert
        assertThat(result).isFalse()
    }
}