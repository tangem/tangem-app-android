package com.tangem.domain.models.account

import com.google.common.truth.Truth
import com.tangem.domain.models.account.Account.CryptoPortfolio
import com.tangem.domain.models.account.Account.CryptoPortfolio.Error.AccountNameError
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccountTest {

    @Test
    fun `Account userWalletId`() {
        // Arrange
        val userWalletId = UserWalletId("011")

        // Act
        val actual = createCryptoPortfolioStub(userWalletId = userWalletId).userWalletId

        // Assert
        Truth.assertThat(actual).isEqualTo(userWalletId)
    }

    @Test
    fun `CryptoPortfolio isMainAccount`() {
        // Arrange
        val derivationIndex0 = 0
        val derivationIndex1 = 1

        // Act
        val actual1 = createCryptoPortfolioStub(derivationIndex = derivationIndex0)
            .isMainAccount

        val actual2 = createCryptoPortfolioStub(derivationIndex = derivationIndex1)
            .isMainAccount

        // Assert
        Truth.assertThat(actual1).isTrue()
        Truth.assertThat(actual2).isFalse()
    }

    @Test
    fun `CryptoPortfolio tokensCount`() {
        // Arrange
        val emptyCurrencies = emptySet<CryptoCurrency>()
        val filledCurrencies = setOf(mockk<CryptoCurrency>())

        // Act
        val actual1 = createCryptoPortfolioStub(currencies = emptyCurrencies)
            .tokensCount

        val actual2 = createCryptoPortfolioStub(currencies = filledCurrencies)
            .tokensCount

        // Assert
        Truth.assertThat(actual1).isEqualTo(0)
        Truth.assertThat(actual2).isEqualTo(1)
    }

    @Test
    fun `CryptoPortfolio networksCount`() {
        // Arrange
        val emptyCurrencies = emptySet<CryptoCurrency>()
        val filledCurrencies = setOf(
            mockk<CryptoCurrency> {
                every { network } returns mockk()
            },
        )

        // Act
        val actual1 = createCryptoPortfolioStub(currencies = emptyCurrencies)
            .networksCount

        val actual2 = createCryptoPortfolioStub(currencies = filledCurrencies)
            .networksCount

        // Assert
        Truth.assertThat(actual1).isEqualTo(0)
        Truth.assertThat(actual2).isEqualTo(1)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CreateCryptoPortfolio {

        @Test
        fun `invoke returns AccountNameError`() {
            // Arrange
            val name = ""

            // Act
            val actual = CryptoPortfolio.invoke(
                accountId = mockk(),
                name = name,
                icon = mockk(),
                derivationIndex = 0,
                cryptoCurrencies = emptySet(),
            )
                .leftOrNull()!!

            // Assert
            val expected = AccountNameError(cause = AccountName.Error.Empty)
            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `invoke returns CryptoPortfolio`() {
            // Act
            val derivationIndex = DerivationIndex.Main
            val actual = CryptoPortfolio(
                accountId = AccountId.forCryptoPortfolio(
                    userWalletId = UserWalletId("011"),
                    derivationIndex = derivationIndex,
                ),
                name = "Test Account",
                icon = CryptoPortfolioIcon.ofMainAccount(userWalletId = UserWalletId("011")),
                derivationIndex = derivationIndex.value,
                cryptoCurrencies = emptySet(),
            )
                .getOrNull()!!

            // Assert
            val expected = createCryptoPortfolioStub()
            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun createMainAccount() {
            // Arrange
            val userWalletId = UserWalletId("011")
            val derivationIndex = DerivationIndex.Main

            // Act
            val actual = CryptoPortfolio.createMainAccount(userWalletId = userWalletId)

            // Assert
            val expected = CryptoPortfolio(
                accountId = AccountId.forCryptoPortfolio(
                    userWalletId = userWalletId,
                    derivationIndex = derivationIndex,
                ),
                accountName = AccountName.DefaultMain,
                icon = CryptoPortfolioIcon.ofMainAccount(userWalletId),
                derivationIndex = derivationIndex,
                cryptoCurrencies = emptySet(),
            )

            Truth.assertThat(actual).isEqualTo(expected)
        }
    }

    private fun createCryptoPortfolioStub(
        userWalletId: UserWalletId = UserWalletId("011"),
        name: String = "Test Account",
        derivationIndex: Int = 0,
        currencies: Set<CryptoCurrency> = emptySet(),
    ): CryptoPortfolio {
        val accountIndex = DerivationIndex(value = derivationIndex).getOrNull()!!

        return CryptoPortfolio.invoke(
            accountId = AccountId.forCryptoPortfolio(userWalletId = userWalletId, derivationIndex = accountIndex),
            name = name,
            icon = CryptoPortfolioIcon.ofMainAccount(userWalletId),
            derivationIndex = derivationIndex,
            cryptoCurrencies = currencies,
        )
            .getOrNull()!!
    }
}