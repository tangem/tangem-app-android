package com.tangem.data.jointaccount.fetcher

import com.google.common.truth.Truth.assertThat
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.remote.response.ETAG_HEADER
import com.tangem.data.common.cache.etag.ETagsStore
import com.tangem.data.jointaccount.converter.JointAccountDMConverter
import com.tangem.data.jointaccount.converter.JointAccountDtoConverter
import com.tangem.data.jointaccount.converter.JointOperationConverter
import com.tangem.data.jointaccount.converter.SafeTxConverter
import com.tangem.data.jointaccount.createJointAccount
import com.tangem.data.jointaccount.createJointAccountDto
import com.tangem.data.jointaccount.store.JointAccountsStore
import com.tangem.data.jointaccount.store.WalletIdWithJointAccountsDM
import com.tangem.store.datasource.jointaccount.JointAccountApi
import com.tangem.store.datasource.jointaccount.models.GetJointAccountsResponse
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
    private val eTagsStore: ETagsStore = mockk(relaxUnitFun = true)
    private val walletId = UserWalletId("011121314151617181910A0B0C0D0E0F011121314151617181910A0B0C0D0E0F")

    @BeforeEach
    fun resetMocks() {
        clearMocks(jointAccountApi, eTagsStore)
        coEvery { eTagsStore.getSyncOrNull(walletId, ETagsStore.Key.JointAccounts) } returns STORED_ETAG
    }

    private fun TestScopeContext.createFetcher(): DefaultSingleJointAccountListFetcher {
        return DefaultSingleJointAccountListFetcher(
            jointAccountApi = jointAccountApi,
            store = store,
            converter = JointAccountDtoConverter(operationConverter = JointOperationConverter(safeTxConverter = SafeTxConverter())),
            eTagsStore = eTagsStore,
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
        coEvery {
            jointAccountApi.getJointAccounts(walletId = walletId.stringValue, eTag = STORED_ETAG)
        } returns ApiResponse.Success(
            data = GetJointAccountsResponse(jointAccounts = listOf(createJointAccountDto())),
            headers = mapOf(ETAG_HEADER to listOf(NEW_ETAG)),
        )

        // Act
        val result = fetcher(walletId)

        // Assert
        assertThat(result.isRight()).isTrue()
        assertThat(context.store.get(walletId).first())
            .containsExactly(createJointAccount(source = StatusSource.ACTUAL))
        coVerify(exactly = 1) {
            jointAccountApi.getJointAccounts(walletId = walletId.stringValue, eTag = STORED_ETAG)
            eTagsStore.store(userWalletId = walletId, key = ETagsStore.Key.JointAccounts, value = NEW_ETAG)
        }
    }

    @Test
    fun `GIVEN request fails and cache exists WHEN invoke THEN cache downgraded to ONLY_CACHE`() = runTest {
        // Arrange
        val context = TestScopeContext(TestAppCoroutineScope(testScope = this))
        advanceUntilIdle()
        val fetcher = with(context) { createFetcher() }
        val cached = createJointAccount(source = StatusSource.ACTUAL)
        context.store.store(walletId, listOf(cached))
        coEvery { jointAccountApi.getJointAccounts(any(), any()) } throws IllegalStateException("network down")

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
        coEvery { jointAccountApi.getJointAccounts(any(), any()) } throws IllegalStateException("network down")

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
        coEvery { jointAccountApi.getJointAccounts(any(), any()) } returns ApiResponse.Success(
            data = GetJointAccountsResponse(jointAccounts = emptyList()),
        )

        // Act
        val result = fetcher(walletId)

        // Assert
        assertThat(result.isRight()).isTrue()
        assertThat(context.store.get(walletId).first()).isEmpty()
        assertThat(context.store.contains(walletId)).isTrue()
    }

    @Test
    fun `GIVEN nothing moved since the stored tag WHEN invoke THEN the cache is kept and marked ACTUAL`() = runTest {
        // Arrange
        val context = TestScopeContext(TestAppCoroutineScope(testScope = this))
        advanceUntilIdle()
        val fetcher = with(context) { createFetcher() }
        val cached = createJointAccount(source = StatusSource.CACHE)
        context.store.store(walletId, listOf(cached))
        coEvery { jointAccountApi.getJointAccounts(any(), any()) } returns notModified()

        // Act
        val result = fetcher(walletId)

        // Assert
        assertThat(result.isRight()).isTrue()
        assertThat(context.store.get(walletId).first())
            .containsExactly(cached.copy(source = StatusSource.ACTUAL))
        coVerify(exactly = 0) { eTagsStore.store(any(), any(), any()) }
    }

    @Test
    fun `GIVEN a 304 but no cache WHEN invoke THEN the tag is dropped and the list refetched without it`() = runTest {
        // Arrange
        val context = TestScopeContext(TestAppCoroutineScope(testScope = this))
        advanceUntilIdle()
        val fetcher = with(context) { createFetcher() }
        coEvery {
            jointAccountApi.getJointAccounts(walletId = walletId.stringValue, eTag = STORED_ETAG)
        } returns notModified()
        coEvery { jointAccountApi.getJointAccounts(walletId = walletId.stringValue) } returns ApiResponse.Success(
            data = GetJointAccountsResponse(jointAccounts = listOf(createJointAccountDto())),
            headers = mapOf(ETAG_HEADER to listOf(NEW_ETAG)),
        )

        // Act
        val result = fetcher(walletId)

        // Assert
        assertThat(result.isRight()).isTrue()
        assertThat(context.store.get(walletId).first())
            .containsExactly(createJointAccount(source = StatusSource.ACTUAL))
        coVerify(exactly = 1) {
            eTagsStore.clear(userWalletId = walletId, key = ETagsStore.Key.JointAccounts)
            eTagsStore.store(userWalletId = walletId, key = ETagsStore.Key.JointAccounts, value = NEW_ETAG)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun notModified(): ApiResponse<GetJointAccountsResponse> = ApiResponse.Error(
        cause = ApiResponseError.HttpException(
            code = ApiResponseError.HttpException.Code.NOT_MODIFIED,
            message = null,
            errorBody = null,
        ),
    ) as ApiResponse<GetJointAccountsResponse>

    private companion object {
        const val STORED_ETAG = "stored-etag"
        const val NEW_ETAG = "new-etag"
    }
}