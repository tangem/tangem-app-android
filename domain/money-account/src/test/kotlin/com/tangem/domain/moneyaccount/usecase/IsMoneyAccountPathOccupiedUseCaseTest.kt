package com.tangem.domain.moneyaccount.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class IsMoneyAccountPathOccupiedUseCaseTest {

    private val singleAccountListSupplier: SingleAccountListSupplier = mockk()
    private val userWalletId: UserWalletId = mockk()

    private val useCase = IsMoneyAccountPathOccupiedUseCase(
        singleAccountListSupplier = singleAccountListSupplier,
    )

    @BeforeEach
    fun setup() {
        clearMocks(singleAccountListSupplier)
    }

    @Test
    fun `GIVEN no account list WHEN invoke THEN returns false`() = runTest {
        // Arrange
        coEvery { singleAccountListSupplier.getSyncOrNull(userWalletId) } returns null

        // Act
        val result = useCase(userWalletId)

        // Assert
        assertThat(result).isFalse()
    }

    @Test
    fun `GIVEN currency on money account path WHEN invoke THEN returns true`() = runTest {
        // Arrange
        mockCurrencies(derivationPath = "m/44'/60'/999998'/0/0")

        // Act
        val result = useCase(userWalletId)

        // Assert
        assertThat(result).isTrue()
    }

    @Test
    fun `GIVEN currency on regular path WHEN invoke THEN returns false`() = runTest {
        // Arrange
        mockCurrencies(derivationPath = "m/44'/60'/1'/0/0")

        // Act
        val result = useCase(userWalletId)

        // Assert
        assertThat(result).isFalse()
    }

    private fun mockCurrencies(derivationPath: String) {
        val currency = mockk<CryptoCurrency> {
            every { network.derivationPath } returns Network.DerivationPath.Custom(value = derivationPath)
        }
        val accountList = mockk<AccountList> { every { flattenCurrencies() } returns listOf(currency) }
        coEvery { singleAccountListSupplier.getSyncOrNull(userWalletId) } returns accountList
    }
}