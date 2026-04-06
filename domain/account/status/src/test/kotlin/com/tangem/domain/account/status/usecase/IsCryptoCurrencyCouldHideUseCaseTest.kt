package com.tangem.domain.account.status.usecase

import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.mock.MockAccounts
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.*

/**
 * Tests for [IsCryptoCurrencyCouldHideUseCase]
 *
 * Test cases:
 * 1. Token → always returns true (can hide)
 * 2. Coin without tokens in the same network → returns true (can hide)
 * 3. Coin with tokens in the same network → returns false (cannot hide)
 * 4. Coin with tokens only in other networks → returns true (can hide)
 * 5. Supplier returns null → returns true (can hide)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class IsCryptoCurrencyCouldHideUseCaseTest {

    private val cryptoCurrencyFactory = MockCryptoCurrencyFactory()
    private val singleAccountListSupplier: SingleAccountListSupplier = mockk()

    private val useCase = IsCryptoCurrencyCouldHideUseCase(
        singleAccountListSupplier = singleAccountListSupplier,
    )

    private val userWalletId = MockAccounts.userWalletId

    @AfterEach
    fun tearDown() {
        clearMocks(singleAccountListSupplier)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class TokenTests {

        @Test
        fun `Token always returns true`() = runTest {
            // Arrange
            val coin = cryptoCurrencyFactory.ethereum
            val token = cryptoCurrencyFactory.createToken(com.tangem.blockchain.common.Blockchain.Ethereum)
            val accountList = createAccountList(listOf(coin, token))
            coEvery { singleAccountListSupplier.getSyncOrNull(userWalletId) } returns accountList

            // Act
            val result = useCase(userWalletId = userWalletId, cryptoCurrency = token)

            // Assert
            Truth.assertThat(result).isTrue()
        }

        @Test
        fun `Token returns true even when supplier returns null`() = runTest {
            // Arrange
            val token = cryptoCurrencyFactory.createToken(com.tangem.blockchain.common.Blockchain.Ethereum)
            coEvery { singleAccountListSupplier.getSyncOrNull(userWalletId) } returns null

            // Act
            val result = useCase(userWalletId = userWalletId, cryptoCurrency = token)

            // Assert
            Truth.assertThat(result).isTrue()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CoinTests {

        @Test
        fun `Coin without tokens in the same network returns true`() = runTest {
            // Arrange
            val coin = cryptoCurrencyFactory.ethereum
            val accountList = createAccountList(listOf(coin))
            coEvery { singleAccountListSupplier.getSyncOrNull(userWalletId) } returns accountList

            // Act
            val result = useCase(userWalletId = userWalletId, cryptoCurrency = coin)

            // Assert
            Truth.assertThat(result).isTrue()
        }

        @Test
        fun `Coin with tokens in the same network returns false`() = runTest {
            // Arrange
            val coin = cryptoCurrencyFactory.ethereum
            val tokenOnSameNetwork = cryptoCurrencyFactory.createToken(com.tangem.blockchain.common.Blockchain.Ethereum)
            val accountList = createAccountList(listOf(coin, tokenOnSameNetwork))
            coEvery { singleAccountListSupplier.getSyncOrNull(userWalletId) } returns accountList

            // Act
            val result = useCase(userWalletId = userWalletId, cryptoCurrency = coin)

            // Assert
            Truth.assertThat(result).isFalse()
        }

        @Test
        fun `Coin with multiple tokens in the same network returns false`() = runTest {
            // Arrange
            val coin = cryptoCurrencyFactory.ethereum
            val token1 = cryptoCurrencyFactory.createToken(com.tangem.blockchain.common.Blockchain.Ethereum)
            val token2 = cryptoCurrencyFactory.createToken(com.tangem.blockchain.common.Blockchain.Ethereum)
            val accountList = createAccountList(listOf(coin, token1, token2))
            coEvery { singleAccountListSupplier.getSyncOrNull(userWalletId) } returns accountList

            // Act
            val result = useCase(userWalletId = userWalletId, cryptoCurrency = coin)

            // Assert
            Truth.assertThat(result).isFalse()
        }

        @Test
        fun `Coin with tokens only in other networks returns true`() = runTest {
            // Arrange
            val ethereumCoin = cryptoCurrencyFactory.ethereum
            val stellarCoin = cryptoCurrencyFactory.stellar
            val stellarToken = cryptoCurrencyFactory.createToken(com.tangem.blockchain.common.Blockchain.Stellar)
            val accountList = createAccountList(listOf(ethereumCoin, stellarCoin, stellarToken))
            coEvery { singleAccountListSupplier.getSyncOrNull(userWalletId) } returns accountList

            // Act
            val result = useCase(userWalletId = userWalletId, cryptoCurrency = ethereumCoin)

            // Assert
            Truth.assertThat(result).isTrue()
        }

        @Test
        fun `Coin returns true when supplier returns null`() = runTest {
            // Arrange
            val coin = cryptoCurrencyFactory.ethereum
            coEvery { singleAccountListSupplier.getSyncOrNull(userWalletId) } returns null

            // Act
            val result = useCase(userWalletId = userWalletId, cryptoCurrency = coin)

            // Assert
            Truth.assertThat(result).isTrue()
        }

        @Test
        fun `Coin returns true when account list has no currencies`() = runTest {
            // Arrange
            val coin = cryptoCurrencyFactory.ethereum
            val accountList = createAccountList(emptyList())
            coEvery { singleAccountListSupplier.getSyncOrNull(userWalletId) } returns accountList

            // Act
            val result = useCase(userWalletId = userWalletId, cryptoCurrency = coin)

            // Assert
            Truth.assertThat(result).isTrue()
        }
    }

    private fun createAccountList(currencies: List<CryptoCurrency>): AccountList {
        return MockAccounts.createAccountList(
            activeAccounts = 1,
            userWalletId = userWalletId,
        ).let { accountList ->
            val mainAccount = accountList.mainAccount.copy(cryptoCurrencies = currencies)
            AccountList(
                userWalletId = userWalletId,
                accounts = listOf(mainAccount),
                totalAccounts = 1,
                totalArchivedAccounts = 0,
            ).getOrNull()!!
        }
    }
}