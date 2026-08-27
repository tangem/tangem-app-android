package com.tangem.domain.models.account

import arrow.core.left
import com.google.common.truth.Truth
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.Account.Personal
import com.tangem.domain.models.account.Account.Personal.Error.AccountNameError
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
        val actual = createPersonalStub(userWalletId = userWalletId).userWalletId

        // Assert
        Truth.assertThat(actual).isEqualTo(userWalletId)
    }

    @Test
    fun `Personal isMainAccount`() {
        // Arrange
        val derivationIndex0 = 0
        val derivationIndex1 = 1

        // Act
        val actual1 = createPersonalStub(derivationIndex = derivationIndex0)
            .isMainAccount

        val actual2 = createPersonalStub(derivationIndex = derivationIndex1)
            .isMainAccount

        // Assert
        Truth.assertThat(actual1).isTrue()
        Truth.assertThat(actual2).isFalse()
    }

    @Test
    fun `Personal tokensCount`() {
        // Arrange
        val emptyCurrencies = emptyList<CryptoCurrency>()
        val filledCurrencies = listOf(mockk<CryptoCurrency>())

        // Act
        val actual1 = createPersonalStub(currencies = emptyCurrencies)
            .tokensCount

        val actual2 = createPersonalStub(currencies = filledCurrencies)
            .tokensCount

        // Assert
        Truth.assertThat(actual1).isEqualTo(0)
        Truth.assertThat(actual2).isEqualTo(1)
    }

    @Test
    fun `Personal networksCount`() {
        // Arrange
        val emptyCurrencies = emptyList<CryptoCurrency>()
        val filledCurrencies = listOf(
            mockk<CryptoCurrency> {
                every { network } returns mockk()
            },
        )

        // Act
        val actual1 = createPersonalStub(currencies = emptyCurrencies)
            .networksCount

        val actual2 = createPersonalStub(currencies = filledCurrencies)
            .networksCount

        // Assert
        Truth.assertThat(actual1).isEqualTo(0)
        Truth.assertThat(actual2).isEqualTo(1)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CreatePersonal {

        @Test
        fun `invoke returns AccountNameError`() {
            // Arrange
            val name = ""

            // Act
            val actual = Personal.invoke(
                accountId = mockk(),
                name = name,
                icon = mockk(),
                derivationIndex = 0,
                cryptoCurrencies = emptyList(),
            )
                .leftOrNull()!!

            // Assert
            val expected = AccountNameError(cause = AccountName.Error.Empty)
            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `invoke returns Personal`() {
            // Act
            val derivationIndex = DerivationIndex.Main
            val actual = Personal(
                accountId = AccountId.forCryptoPortfolio(
                    userWalletId = UserWalletId("011"),
                    derivationIndex = derivationIndex,
                ),
                name = "Test Account",
                icon = CryptoPortfolioIcon.ofMainAccount(userWalletId = UserWalletId("011")),
                derivationIndex = derivationIndex.value,
                cryptoCurrencies = emptyList(),
            )
                .getOrNull()!!

            // Assert
            val expected = createPersonalStub()
            Truth.assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun createMainAccount() {
            // Arrange
            val userWalletId = UserWalletId("011")
            val derivationIndex = DerivationIndex.Main

            // Act
            val actual = Personal.createMainAccount(userWalletId = userWalletId)

            // Assert
            val expected = Personal(
                accountId = AccountId.forCryptoPortfolio(
                    userWalletId = userWalletId,
                    derivationIndex = derivationIndex,
                ),
                accountName = AccountName.DefaultMain,
                icon = CryptoPortfolioIcon.ofMainAccount(userWalletId),
                derivationIndex = derivationIndex,
                cryptoCurrencies = emptyList(),
            )

            Truth.assertThat(actual).isEqualTo(expected)
        }
    }

    @Test
    fun `GIVEN negative owner key index WHEN Joint invoke THEN returns OwnerKeyIndexError`() {
        // Arrange
        val userWalletId = UserWalletId("011")
        val accountId = AccountId.forJointAccount(userWalletId = userWalletId, value = "1".padStart(64, '0'))
            .getOrNull()!!

        // Act
        val actual = Account.Joint(
            accountId = accountId,
            name = "Family",
            icon = CryptoPortfolioIcon.ofDefaultCustomAccount(),
            ownerKeyIndex = -1,
        )

        // Assert
        val expected = Account.Joint.Error.OwnerKeyIndexError(
            cause = OwnerKeyIndex.Error.NegativeOwnerKeyIndex(ownerKeyIndex = -1),
        ).left()
        Truth.assertThat(actual).isEqualTo(expected)
    }

    private fun createPersonalStub(
        userWalletId: UserWalletId = UserWalletId("011"),
        name: String = "Test Account",
        derivationIndex: Int = 0,
        currencies: List<CryptoCurrency> = emptyList(),
    ): Personal {
        val accountIndex = DerivationIndex(value = derivationIndex).getOrNull()!!

        return Personal.invoke(
            accountId = AccountId.forCryptoPortfolio(userWalletId = userWalletId, derivationIndex = accountIndex),
            name = name,
            icon = CryptoPortfolioIcon.ofMainAccount(userWalletId),
            derivationIndex = derivationIndex,
            cryptoCurrencies = currencies,
        )
            .getOrNull()!!
    }
}