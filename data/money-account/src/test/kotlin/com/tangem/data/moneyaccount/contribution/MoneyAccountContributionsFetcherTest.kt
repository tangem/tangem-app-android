package com.tangem.data.moneyaccount.contribution

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.balance.BalanceContributionFetcher
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.MoneyAccountFeatureToggles
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class MoneyAccountContributionsFetcherTest {

    private val moneyAccountFeatureToggles: MoneyAccountFeatureToggles = mockk()

    private val currency: CryptoCurrency.Token = mockk()
    private val yieldFetcher = FakeFetcher()

    private val fetcher = MoneyAccountContributionsFetcher(
        contributionFetchers = listOf(yieldFetcher),
        policy = MoneyAccountContributionPolicy(moneyAccountFeatureToggles = moneyAccountFeatureToggles),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(moneyAccountFeatureToggles)
        every { moneyAccountFeatureToggles.isYieldModeEnabled } returns true
        yieldFetcher.calls.clear()
        yieldFetcher.result = Unit.right()
    }

    @Test
    fun `GIVEN contribution mode WHEN fetch THEN the Money account fetchers run for the account currency`() = runTest {
        // Act
        fetcher.fetch(USER_WALLET_ID, currency)

        // Assert
        assertThat(yieldFetcher.calls).containsExactly(USER_WALLET_ID to setOf(currency))
    }

    @Test
    fun `GIVEN a failing fetcher WHEN fetch THEN it completes`() = runTest {
        // Arrange
        yieldFetcher.result = IllegalStateException("RPC").left()

        // Act
        val actual = runCatching { fetcher.fetch(USER_WALLET_ID, currency) }

        // Assert
        assertThat(actual.isSuccess).isTrue()
    }

    @Test
    fun `GIVEN yield mode disabled WHEN fetch THEN nothing runs`() = runTest {
        // Arrange
        every { moneyAccountFeatureToggles.isYieldModeEnabled } returns false

        // Act
        fetcher.fetch(USER_WALLET_ID, currency)

        // Assert
        assertThat(yieldFetcher.calls).isEmpty()
    }

    private class FakeFetcher : BalanceContributionFetcher {

        val calls = mutableListOf<Pair<UserWalletId, Collection<CryptoCurrency>>>()
        var result: Either<Throwable, Unit> = Unit.right()

        override suspend fun fetch(
            userWalletId: UserWalletId,
            currencies: Collection<CryptoCurrency>,
        ): Either<Throwable, Unit> {
            calls += userWalletId to currencies
            return result
        }
    }

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
    }
}