package com.tangem.features.earn.yield.impl

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.routing.AppRoute
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.currency.yieldSupplyKey
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.usecase.YieldSupplyApyFlowUseCase
import com.tangem.features.earn.api.overview.EarnBadgeAccent
import com.tangem.features.earn.yield.impl.fixtures.coin
import com.tangem.features.earn.yield.impl.fixtures.currencyStatus
import com.tangem.features.earn.yield.impl.fixtures.token
import com.tangem.features.earn.yield.impl.fixtures.yieldStatus
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class YieldModeRateSourceTest {

    private val accountStatusListSupplier: SingleAccountStatusListSupplier = mockk()
    private val yieldSupplyApyFlowUseCase: YieldSupplyApyFlowUseCase = mockk()

    private val source = YieldModeRateSource(
        accountStatusListSupplier = accountStatusListSupplier,
        yieldSupplyApyFlowUseCase = yieldSupplyApyFlowUseCase,
        mapper = YieldModeRateMapper(),
    )

    private val userWalletId = UserWalletId("1234567890ABCDEF")
    private val token = token()
    private val key = token.yieldSupplyKey()
    private val apy = mapOf(key to BigDecimal("5.5"))

    @Test
    fun `GIVEN source WHEN reading the badge accent THEN Success`() {
        // Assert
        assertThat(source.badgeAccent).isEqualTo(EarnBadgeAccent.Success)
    }

    @Test
    fun `GIVEN source WHEN reading metadata THEN id yield_mode and rank 3`() {
        // Assert
        assertThat(source.id).isEqualTo(EarnProductId("yield_mode"))
        assertThat(source.rank).isEqualTo(3)
    }

    @Test
    fun `GIVEN two tokens and a coin WHEN rates THEN entries only for tokens in the map`() = runTest {
        // Arrange
        val otherToken = token(contractAddress = "0xdAC17F958D2ee523a2206206994597C13D831ec7")
        every { accountStatusListSupplier(userWalletId) } returns flowOf(
            accountList(
                currencyStatus(token),
                currencyStatus(otherToken),
                currencyStatus(coin(Blockchain.Ethereum)),
            ),
        )
        every { yieldSupplyApyFlowUseCase() } returns flowOf(apy)

        // Act
        val actual = source.rates(userWalletId).first()

        // Assert
        assertThat(actual.keys).containsExactly(token.id)
    }

    @Test
    fun `GIVEN the same token in two accounts WHEN rates THEN an entry per currency id`() = runTest {
        // Arrange
        val second = secondAccountOf(token)
        every { accountStatusListSupplier(userWalletId) } returns
            flowOf(accountList(currencyStatus(token), currencyStatus(second)))
        every { yieldSupplyApyFlowUseCase() } returns flowOf(apy)

        // Act
        val actual = source.rates(userWalletId).first()

        // Assert
        assertThat(actual.keys).containsExactly(token.id, second.id)
        assertThat(actual.getValue(token.id).route).isEqualTo(AppRoute.YieldSupplyEntry(userWalletId, token, "5.5"))
        assertThat(actual.getValue(second.id).route).isEqualTo(AppRoute.YieldSupplyEntry(userWalletId, second, "5.5"))
    }

    @Test
    fun `GIVEN no accounts WHEN rates THEN empty map`() = runTest {
        // Arrange
        every { accountStatusListSupplier(userWalletId) } returns flowOf(accountList())
        every { yieldSupplyApyFlowUseCase() } returns flowOf(apy)

        // Act
        val actual = source.rates(userWalletId).first()

        // Assert
        assertThat(actual).isEmpty()
    }

    @Test
    fun `GIVEN apy map updates WHEN rates THEN a new map follows each update`() = runTest {
        // Arrange
        val apyFlow = MutableSharedFlow<Map<String, BigDecimal>>(replay = 1)
        apyFlow.tryEmit(apy)
        every { accountStatusListSupplier(userWalletId) } returns flowOf(accountList(currencyStatus(token)))
        every { yieldSupplyApyFlowUseCase() } returns apyFlow

        // Act & Assert
        source.rates(userWalletId).test {
            assertThat(awaitItem().getValue(token.id).rate.fraction).isEqualTo(BigDecimal("0.055"))
            apyFlow.emit(emptyMap())
            assertThat(awaitItem()).isEmpty()
            apyFlow.emit(mapOf(key to BigDecimal("6")))
            assertThat(awaitItem().getValue(token.id).rate.fraction).isEqualTo(BigDecimal("0.06"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GIVEN a status tick WHEN rates THEN activity follows the new status`() = runTest {
        // Arrange
        val ticks = MutableSharedFlow<AccountStatusList>(replay = 1)
        ticks.tryEmit(accountList(currencyStatus(token, yieldStatus(isActive = false))))
        every { accountStatusListSupplier(userWalletId) } returns ticks
        every { yieldSupplyApyFlowUseCase() } returns flowOf(apy)

        // Act & Assert
        source.rates(userWalletId).test {
            assertThat(awaitItem().getValue(token.id).isActive).isFalse()
            ticks.emit(accountList(currencyStatus(token, yieldStatus(isActive = true))))
            assertThat(awaitItem().getValue(token.id).isActive).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun accountList(vararg statuses: CryptoCurrencyStatus): AccountStatusList = mockk {
        every { flattenCurrencies() } returns statuses.toList()
    }

    private fun secondAccountOf(token: CryptoCurrency.Token): CryptoCurrency.Token = token.copy(
        id = token.id.copy(
            body = CryptoCurrency.ID.Body.NetworkIdWithDerivationPath(
                rawId = token.id.rawNetworkId,
                derivationPath = SECOND_ACCOUNT_PATH,
            ),
        ),
    )

    private companion object {
        const val SECOND_ACCOUNT_PATH = "m/44'/60'/1'/0/0"
    }
}