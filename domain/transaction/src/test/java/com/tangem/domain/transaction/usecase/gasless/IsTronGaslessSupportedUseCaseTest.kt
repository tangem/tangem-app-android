package com.tangem.domain.transaction.usecase.gasless

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.transaction.TronGaslessTransactionRepository
import com.tangem.domain.transaction.models.tron.TronGaslessToken
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class IsTronGaslessSupportedUseCaseTest {

    private val repository: TronGaslessTransactionRepository = mockk()
    private val useCase = IsTronGaslessSupportedUseCase(repository = repository)

    private val factory = MockCryptoCurrencyFactory()
    private val usdtContract = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t"
    private val usdtToken = factory.createToken(blockchain = Blockchain.Tron, contractAddress = usdtContract)
    private val tronCoin = factory.createCoin(blockchain = Blockchain.Tron)
    private val ethereumToken = factory.createToken(blockchain = Blockchain.Ethereum, contractAddress = usdtContract)
    private val usdt = TronGaslessToken(contractAddress = usdtContract, symbol = "USDT", decimals = 6)

    @BeforeEach
    fun resetMocks() {
        clearMocks(repository)
    }

    @Test
    fun `GIVEN non-token currency WHEN invoke THEN false`() = runTest {
        // Act
        val result = useCase(network = tronCoin.network, currency = tronCoin)

        // Assert
        assertThat(result).isFalse()
        coVerify(exactly = 0) { repository.getSupportedTokens() }
    }

    @Test
    fun `GIVEN supported token WHEN invoke THEN true`() = runTest {
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

    @Test
    fun `GIVEN token on another blockchain WHEN invoke THEN false and backend is not queried`() = runTest {
        // Act
        val result = useCase(network = ethereumToken.network, currency = ethereumToken)

        // Assert
        assertThat(result).isFalse()
        coVerify(exactly = 0) { repository.getSupportedTokens() }
    }
}