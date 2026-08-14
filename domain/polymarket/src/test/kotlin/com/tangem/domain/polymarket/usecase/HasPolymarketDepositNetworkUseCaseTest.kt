package com.tangem.domain.polymarket.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class HasPolymarketDepositNetworkUseCaseTest {

    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier = mockk()

    private val useCase = HasPolymarketDepositNetworkUseCase(
        singleAccountStatusListSupplier = singleAccountStatusListSupplier,
    )

    private val userWalletId = UserWalletId("011")

    @BeforeEach
    fun resetMocks() {
        clearMocks(singleAccountStatusListSupplier)
    }

    @Test
    fun `GIVEN a currency on the deposit chain WHEN invoked THEN the wallet holds it`() = runTest {
        // Arrange
        stubCurrencies(POLYGON_NETWORK_ID)

        // Act
        val actual = useCase(userWalletId)

        // Assert
        assertThat(actual).isTrue()
    }

    @Test
    fun `GIVEN only currencies on other chains WHEN invoked THEN the wallet does not hold it`() = runTest {
        // Arrange
        stubCurrencies("ethereum", "bitcoin")

        // Act
        val actual = useCase(userWalletId)

        // Assert
        assertThat(actual).isFalse()
    }

    @Test
    fun `GIVEN a token on the deposit chain WHEN invoked THEN the wallet holds it`() = runTest {
        // Arrange — any currency on the chain answers the question, native or not
        stubCurrencies("ethereum", POLYGON_NETWORK_ID)

        // Act
        val actual = useCase(userWalletId)

        // Assert
        assertThat(actual).isTrue()
    }

    @Test
    fun `GIVEN no accounts WHEN invoked THEN the wallet does not hold it`() = runTest {
        // Arrange
        stubCurrencies()

        // Act
        val actual = useCase(userWalletId)

        // Assert
        assertThat(actual).isFalse()
    }

    @Test
    fun `GIVEN the account list is unavailable WHEN invoked THEN the wallet does not hold it`() = runTest {
        // Arrange
        coEvery { singleAccountStatusListSupplier.getSyncOrNull(userWalletId) } returns null

        // Act
        val actual = useCase(userWalletId)

        // Assert
        assertThat(actual).isFalse()
    }

    private fun stubCurrencies(vararg networkRawIds: String) {
        val statuses = networkRawIds.map { rawId ->
            val statusNetwork: Network = mockk { every { this@mockk.rawId } returns rawId }
            val statusCurrency: CryptoCurrency = mockk { every { network } returns statusNetwork }
            mockk<CryptoCurrencyStatus> { every { currency } returns statusCurrency }
        }
        val accountStatusList: AccountStatusList = mockk { every { flattenCurrencies() } returns statuses }
        coEvery { singleAccountStatusListSupplier.getSyncOrNull(userWalletId) } returns accountStatusList
    }

    private companion object {
        /** What `Blockchain.Polygon.toNetworkId()` resolves to; spelled out so a change to it fails here. */
        const val POLYGON_NETWORK_ID = "polygon-pos"
    }
}