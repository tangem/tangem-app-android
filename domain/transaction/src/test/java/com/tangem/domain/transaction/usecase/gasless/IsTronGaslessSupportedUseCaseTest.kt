package com.tangem.domain.transaction.usecase.gasless

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.TronGaslessTransactionRepository
import com.tangem.domain.transaction.models.tron.TronGaslessToken
import com.tangem.domain.walletmanager.WalletManagersFacade
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class IsTronGaslessSupportedUseCaseTest {

    private val repository: TronGaslessTransactionRepository = mockk()
    private val walletManagersFacade: WalletManagersFacade = mockk()
    private val useCase = IsTronGaslessSupportedUseCase(repository, walletManagersFacade)

    private val factory = MockCryptoCurrencyFactory()
    private val userWalletId = UserWalletId("011")
    private val usdtContract = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t"
    private val usdtToken = factory.createToken(blockchain = Blockchain.Tron, contractAddress = usdtContract)
    private val tronCoin = factory.createCoin(blockchain = Blockchain.Tron)
    private val ethereumToken = factory.createToken(blockchain = Blockchain.Ethereum, contractAddress = usdtContract)
    private val usdt = TronGaslessToken(contractAddress = usdtContract, symbol = "USDT", decimals = 6)

    @BeforeEach
    fun resetMocks() {
        clearMocks(repository, walletManagersFacade)
        coEvery { walletManagersFacade.isTronAccountActivated(any(), any()) } returns true
    }

    @Test
    fun `GIVEN non-token currency WHEN invoke THEN false`() = runTest {
        // Act
        val result = useCase(userWalletId = userWalletId, network = tronCoin.network, currency = tronCoin)

        // Assert
        assertThat(result).isFalse()
        coVerify(exactly = 0) { walletManagersFacade.isTronAccountActivated(any(), any()) }
    }

    @Test
    fun `GIVEN tron usdt token and supported WHEN invoke THEN true`() = runTest {
        // Arrange
        coEvery { repository.getSupportedTokens() } returns listOf(usdt)

        // Act
        val result = useCase(userWalletId = userWalletId, network = usdtToken.network, currency = usdtToken)

        // Assert
        assertThat(result).isTrue()
        coVerify(exactly = 1) { walletManagersFacade.isTronAccountActivated(userWalletId, usdtToken.network) }
    }

    @Test
    fun `GIVEN supported list lacks token WHEN invoke THEN false`() = runTest {
        // Arrange
        coEvery { repository.getSupportedTokens() } returns emptyList()

        // Act
        val result = useCase(userWalletId = userWalletId, network = usdtToken.network, currency = usdtToken)

        // Assert
        assertThat(result).isFalse()
        coVerify(exactly = 0) { walletManagersFacade.isTronAccountActivated(any(), any()) }
    }

    @Test
    fun `GIVEN supported token but account not activated WHEN invoke THEN false`() = runTest {
        // Arrange
        coEvery { repository.getSupportedTokens() } returns listOf(usdt)
        coEvery { walletManagersFacade.isTronAccountActivated(userWalletId, usdtToken.network) } returns false

        // Act
        val result = useCase(userWalletId = userWalletId, network = usdtToken.network, currency = usdtToken)

        // Assert
        assertThat(result).isFalse()
    }

    @Test
    fun `GIVEN supported token WHEN isTokenSupported THEN true without asking for activation`() = runTest {
        // Arrange
        coEvery { repository.getSupportedTokens() } returns listOf(usdt)

        // Act
        val result = useCase.isTokenSupported(network = usdtToken.network, currency = usdtToken)

        // Assert
        assertThat(result).isTrue()
        coVerify(exactly = 0) { walletManagersFacade.isTronAccountActivated(any(), any()) }
    }

    @Test
    fun `GIVEN supported list lacks token WHEN isTokenSupported THEN false`() = runTest {
        // Arrange
        coEvery { repository.getSupportedTokens() } returns emptyList()

        // Act
        val result = useCase.isTokenSupported(network = usdtToken.network, currency = usdtToken)

        // Assert
        assertThat(result).isFalse()
    }

    @Test
    fun `GIVEN token on another blockchain WHEN isTokenSupported THEN false and backend is not queried`() = runTest {
        // Act
        val result = useCase.isTokenSupported(network = ethereumToken.network, currency = ethereumToken)

        // Assert
        assertThat(result).isFalse()
        coVerify(exactly = 0) { repository.getSupportedTokens() }
    }
}