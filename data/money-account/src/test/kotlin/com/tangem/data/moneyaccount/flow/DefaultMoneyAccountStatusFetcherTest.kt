package com.tangem.data.moneyaccount.flow

import com.tangem.data.moneyaccount.store.MoneyAccountStatusesStore
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusFetcher
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DefaultMoneyAccountStatusFetcherTest {

    private val moneyAccountStatusesStore: MoneyAccountStatusesStore = mockk(relaxed = true)
    private val dispatchers = TestingCoroutineDispatcherProvider()

    private val fetcher = DefaultMoneyAccountStatusFetcher(
        moneyAccountStatusesStore = moneyAccountStatusesStore,
        dispatchers = dispatchers,
    )

    private val userWalletId = UserWalletId("011")

    @BeforeEach
    fun resetMocks() {
        clearMocks(moneyAccountStatusesStore)
    }

    @Test
    fun `GIVEN status fetched WHEN invoke THEN status stored`() = runTest {
        // Act
        fetcher.invoke(MoneyAccountStatusFetcher.Params(userWalletId))

        // Assert
        coVerify(exactly = 1) {
            moneyAccountStatusesStore.store(
                userWalletId = userWalletId,
                status = AccountStatus.Money(
                    account = Account.Money(userWalletId),
                    value = MoneyAccountStatusValue.NotCreated,
                ),
            )
        }
        coVerify(exactly = 0) {
            moneyAccountStatusesStore.updateStatusSource(userWalletId, StatusSource.ONLY_CACHE)
        }
    }

    @Test
    fun `GIVEN store throws WHEN invoke THEN source degraded to cache`() = runTest {
        // Arrange
        coEvery { moneyAccountStatusesStore.store(any(), any()) } throws IllegalStateException()

        // Act
        fetcher.invoke(MoneyAccountStatusFetcher.Params(userWalletId))

        // Assert
        coVerify(exactly = 1) {
            moneyAccountStatusesStore.updateStatusSource(
                userWalletId = userWalletId,
                source = StatusSource.ONLY_CACHE,
            )
        }
    }
}