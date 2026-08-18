package com.tangem.data.jointaccount.fetcher

import com.google.common.truth.Truth.assertThat
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.core.remote.response.ApiResponse
import com.tangem.data.jointaccount.converter.JointAccountDMConverter
import com.tangem.data.jointaccount.converter.JointAccountDtoConverter
import com.tangem.data.jointaccount.createJointAccount
import com.tangem.data.jointaccount.createJointAccountDto
import com.tangem.data.jointaccount.store.JointAccountsStore
import com.tangem.data.jointaccount.store.WalletIdWithJointAccountsDM
import com.tangem.datasource.api.jointaccount.JointAccountApi
import com.tangem.datasource.api.jointaccount.models.GetJointAccountsResponse
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.test.core.datastore.MockStateDataStore
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DefaultSingleJointAccountListFetcherTest {

    private val jointAccountApi: JointAccountApi = mockk()
    private val walletId = UserWalletId("011121314151617181910A0B0C0D0E0F011121314151617181910A0B0C0D0E0F")

    @BeforeEach
    fun resetMocks() {
        clearMocks(jointAccountApi)
    }

    private fun TestScopeContext.createFetcher(): DefaultSingleJointAccountListFetcher {
        return DefaultSingleJointAccountListFetcher(
            jointAccountApi = jointAccountApi,
            store = store,
            converter = JointAccountDtoConverter(),
            dispatchers = TestingCoroutineDispatcherProvider(),
        )
    }

    private class TestScopeContext(scope: TestAppCoroutineScope) {
        val persistence = MockStateDataStore<WalletIdWithJointAccountsDM>(default = emptyMap())
        val store = JointAccountsStore(
            runtimeStore = RuntimeSharedStore(),
            persistenceDataStore = persistence,
            converter = JointAccountDMConverter(),
            scope = scope,
        )
    }

    @Test
    fun `GIVEN backend responds WHEN invoke THEN accounts stored as ACTUAL`() = runTest {
        // Arrange
        val context = TestScopeContext(TestAppCoroutineScope(testScope = this))
        advanceUntilIdle()
        val fetcher = with(context) { createFetcher() }
        coEvery { jointAccountApi.getJointAccounts(walletId.stringValue) } returns ApiResponse.Success(
            data = GetJointAccountsResponse(jointAccounts = listOf(createJointAccountDto())),
        )

        // Act
        val result = fetcher(walletId)

        // Assert
        assertThat(result.isRight()).isTrue()
        assertThat(context.store.get(walletId).first())
            .containsExactly(createJointAccount(source = StatusSource.ACTUAL))
        coVerify(exactly = 1) { jointAccountApi.getJointAccounts(walletId.stringValue) }
    }

    @Test
    fun `GIVEN request fails and cache exists WHEN invoke THEN cache downgraded to ONLY_CACHE`() = runTest {
        // Arrange
        val context = TestScopeContext(TestAppCoroutineScope(testScope = this))
        advanceUntilIdle()
        val fetcher = with(context) { createFetcher() }
        val cached = createJointAccount(source = StatusSource.ACTUAL)
        context.store.store(walletId, listOf(cached))
        coEvery { jointAccountApi.getJointAccounts(any()) } throws IllegalStateException("network down")

        // Act
        val result = fetcher(walletId)

        // Assert
        assertThat(result.isLeft()).isTrue()
        assertThat(context.store.get(walletId).first())
            .containsExactly(cached.copy(source = StatusSource.ONLY_CACHE))
    }

    @Test
    fun `GIVEN request fails and no cache WHEN invoke THEN store untouched and error returned`() = runTest {
        // Arrange
        val context = TestScopeContext(TestAppCoroutineScope(testScope = this))
        advanceUntilIdle()
        val fetcher = with(context) { createFetcher() }
        coEvery { jointAccountApi.getJointAccounts(any()) } throws IllegalStateException("network down")

        // Act
        val result = fetcher(walletId)

        // Assert
        assertThat(result.isLeft()).isTrue()
        assertThat(context.store.get(walletId).first()).isNull()
        assertThat(context.persistence.data.first()).isEmpty()
    }

    @Test
    fun `GIVEN empty backend list WHEN invoke THEN empty list stored as data`() = runTest {
        // Arrange
        val context = TestScopeContext(TestAppCoroutineScope(testScope = this))
        advanceUntilIdle()
        val fetcher = with(context) { createFetcher() }
        coEvery { jointAccountApi.getJointAccounts(any()) } returns ApiResponse.Success(
            data = GetJointAccountsResponse(jointAccounts = emptyList()),
        )

        // Act
        val result = fetcher(walletId)

        // Assert
        assertThat(result.isRight()).isTrue()
        assertThat(context.store.get(walletId).first()).isEmpty()
        assertThat(context.store.contains(walletId)).isTrue()
    }
}