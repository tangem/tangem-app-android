package com.tangem.data.virtualaccount.flow

import arrow.core.left
import arrow.core.right
import com.tangem.data.virtualaccount.store.VirtualAccountStatusesStore
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.VirtualAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.usecase.GetMoneyAccountBalanceUseCase
import com.tangem.domain.virtualaccount.flow.VirtualAccountStatusFetcher
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class DefaultVirtualAccountStatusFetcherTest {

    private val virtualAccountStatusesStore: VirtualAccountStatusesStore = mockk(relaxed = true)
    private val dispatchers = TestingCoroutineDispatcherProvider()
    private val getMoneyAccountBalanceUseCase: GetMoneyAccountBalanceUseCase = mockk()

    private val fetcher = DefaultVirtualAccountStatusFetcher(
        virtualAccountStatusesStore = virtualAccountStatusesStore,
        dispatchers = dispatchers,
        getMoneyAccountBalanceUseCase = getMoneyAccountBalanceUseCase,
    )

    private val userWalletId = UserWalletId("011")

    @BeforeEach
    fun resetMocks() {
        clearMocks(virtualAccountStatusesStore, getMoneyAccountBalanceUseCase)
    }

    @Test
    fun `GIVEN balance loaded WHEN invoke THEN status stored`() = runTest {
        // Arrange
        coEvery { getMoneyAccountBalanceUseCase(userWalletId) } returns BigDecimal.TEN.right()

        // Act
        fetcher.invoke(VirtualAccountStatusFetcher.Params(userWalletId))

        // Assert
        coVerify(exactly = 1) { getMoneyAccountBalanceUseCase(userWalletId) }
        coVerify(exactly = 1) {
            virtualAccountStatusesStore.store(
                userWalletId = userWalletId,
                status = AccountStatus.Virtual(
                    account = Account.Virtual(userWalletId),
                    value = VirtualAccountStatusValue.NotCreated,
                ),
            )
        }
    }

    @Test
    fun `GIVEN balance unavailable WHEN invoke THEN status still stored`() = runTest {
        // Arrange
        coEvery { getMoneyAccountBalanceUseCase(userWalletId) } returns IllegalStateException().left()

        // Act
        fetcher.invoke(VirtualAccountStatusFetcher.Params(userWalletId))

        // Assert
        coVerify(exactly = 1) {
            virtualAccountStatusesStore.store(
                userWalletId = userWalletId,
                status = AccountStatus.Virtual(
                    account = Account.Virtual(userWalletId),
                    value = VirtualAccountStatusValue.NotCreated,
                ),
            )
        }
        coVerify(exactly = 0) {
            virtualAccountStatusesStore.updateStatusSource(userWalletId, StatusSource.ONLY_CACHE)
        }
    }

    @Test
    fun `GIVEN store throws WHEN invoke THEN source degraded to cache`() = runTest {
        // Arrange
        coEvery { getMoneyAccountBalanceUseCase(userWalletId) } returns BigDecimal.TEN.right()
        coEvery { virtualAccountStatusesStore.store(any(), any()) } throws IllegalStateException()

        // Act
        fetcher.invoke(VirtualAccountStatusFetcher.Params(userWalletId))

        // Assert
        coVerify(exactly = 1) {
            virtualAccountStatusesStore.updateStatusSource(
                userWalletId = userWalletId,
                source = StatusSource.ONLY_CACHE,
            )
        }
    }
}