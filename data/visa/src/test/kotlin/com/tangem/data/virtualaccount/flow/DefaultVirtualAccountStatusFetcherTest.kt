package com.tangem.data.virtualaccount.flow

import com.tangem.data.virtualaccount.store.VirtualAccountStatusesStore
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.VirtualAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.virtualaccount.flow.VirtualAccountStatusFetcher
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DefaultVirtualAccountStatusFetcherTest {

    private val virtualAccountStatusesStore: VirtualAccountStatusesStore = mockk(relaxed = true)
    private val dispatchers = TestingCoroutineDispatcherProvider()

    private val fetcher = DefaultVirtualAccountStatusFetcher(
        virtualAccountStatusesStore = virtualAccountStatusesStore,
        dispatchers = dispatchers,
    )

    private val userWalletId = UserWalletId("011")

    @BeforeEach
    fun resetMocks() {
        clearMocks(virtualAccountStatusesStore)
    }

    @Test
    fun `GIVEN status fetched WHEN invoke THEN status stored`() = runTest {
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