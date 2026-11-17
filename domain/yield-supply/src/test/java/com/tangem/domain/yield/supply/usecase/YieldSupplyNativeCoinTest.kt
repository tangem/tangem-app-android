package com.tangem.domain.yield.supply.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.status.provider.NonPortfolioCurrenciesProvider
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.mock.yield.MockYieldSupply
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class YieldSupplyNativeCoinTest {

    private val singleAccountListSupplier: SingleAccountListSupplier = mockk()
    private val nonPortfolioCurrenciesProvider: NonPortfolioCurrenciesProvider = mockk()

    private val userWalletId = UserWalletId("011")
    private val network = MockYieldSupply.network()
    private val token = MockYieldSupply.token(network = network)
    private val coin = MockYieldSupply.coin(network = network)

    @BeforeEach
    fun resetMocks() {
        clearMocks(singleAccountListSupplier, nonPortfolioCurrenciesProvider)
    }

    @Test
    fun `GIVEN a portfolio flow WHEN findNativeCoin THEN the portfolio coin without looking outside`() = runTest {
        // Arrange
        coEvery { singleAccountListSupplier.getSyncOrNull(userWalletId) } returns
            AccountList.empty(userWalletId = userWalletId, cryptoCurrencies = listOf(coin, token))

        // Act
        val actual = find(isHeldOutsidePortfolio = false)

        // Assert
        assertThat(actual).isEqualTo(coin)
        coVerify(exactly = 0) { nonPortfolioCurrenciesProvider.getStatuses(any(), any()) }
    }

    @Test
    fun `GIVEN a portfolio flow and the coin only in Money WHEN findNativeCoin THEN it fails`() = runTest {
        // Arrange
        coEvery { singleAccountListSupplier.getSyncOrNull(userWalletId) } returns
            AccountList.empty(userWalletId = userWalletId)

        // Act
        val actual = runCatching { find(isHeldOutsidePortfolio = false) }.exceptionOrNull()

        // Assert
        assertThat(actual).hasMessageThat().startsWith("Unable to find coin for network ID")
        coVerify(exactly = 0) { nonPortfolioCurrenciesProvider.getStatuses(any(), any()) }
    }

    @Test
    fun `GIVEN a Money flow WHEN findNativeCoin THEN the Money coin without looking in the portfolio`() = runTest {
        // Arrange
        coEvery { nonPortfolioCurrenciesProvider.getStatuses(userWalletId, network) } returns listOf(
            MockYieldSupply.loadedStatus(currency = token),
            MockYieldSupply.loadedStatus(currency = coin),
        )

        // Act
        val actual = find(isHeldOutsidePortfolio = true)

        // Assert
        assertThat(actual).isEqualTo(coin)
        coVerify(exactly = 0) { singleAccountListSupplier.getSyncOrNull(userWalletId = any()) }
    }

    @Test
    fun `GIVEN a Money flow and the coin only in the portfolio WHEN findNativeCoin THEN it fails`() = runTest {
        // Arrange
        coEvery { nonPortfolioCurrenciesProvider.getStatuses(userWalletId, network) } returns
            listOf(MockYieldSupply.loadedStatus(currency = token))

        // Act
        val actual = runCatching { find(isHeldOutsidePortfolio = true) }.exceptionOrNull()

        // Assert
        assertThat(actual).hasMessageThat().startsWith("Unable to find coin for network ID")
        coVerify(exactly = 0) { singleAccountListSupplier.getSyncOrNull(userWalletId = any()) }
    }

    private suspend fun find(isHeldOutsidePortfolio: Boolean) = findNativeCoin(
        userWalletId = userWalletId,
        currency = token,
        isHeldOutsidePortfolio = isHeldOutsidePortfolio,
        singleAccountListSupplier = singleAccountListSupplier,
        nonPortfolioCurrenciesProvider = nonPortfolioCurrenciesProvider,
    )
}