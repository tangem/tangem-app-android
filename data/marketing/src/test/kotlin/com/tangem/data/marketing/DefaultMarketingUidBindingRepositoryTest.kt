package com.tangem.data.marketing

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.datasource.api.marketing.models.BindMarketingUidWalletsBody
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.marketing.MarketingFeatureToggles
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultMarketingUidBindingRepositoryTest {

    private val tangemTechApi: TangemTechApi = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val featureToggles: MarketingFeatureToggles = mockk()

    private val defaultWallet = wallet(WALLET_ID)

    private lateinit var repository: DefaultMarketingUidBindingRepository

    @BeforeEach
    fun setUp() {
        clearMocks(tangemTechApi, userWalletsListRepository, featureToggles)

        every { featureToggles.isUidBindingEnabled } returns true
        coEvery { userWalletsListRepository.userWalletsSync() } returns listOf(defaultWallet)
        coEvery { tangemTechApi.bindMarketingUidWallets(any(), any()) } returns success()

        repository = DefaultMarketingUidBindingRepository(
            tangemTechApi = tangemTechApi,
            userWalletsListRepository = userWalletsListRepository,
            featureToggles = featureToggles,
            dispatchers = TestingCoroutineDispatcherProvider(),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Bind {

        @Test
        fun `GIVEN toggle off WHEN bind THEN neither wallets nor backend are touched`() = runTest {
            // Arrange
            every { featureToggles.isUidBindingEnabled } returns false

            // Act
            repository.bind(UID)

            // Assert
            coVerify(exactly = 0) { userWalletsListRepository.userWalletsSync() }
            coVerify(exactly = 0) { tangemTechApi.bindMarketingUidWallets(any(), any()) }
        }

        @Test
        fun `GIVEN wallets WHEN bind THEN every wallet id is sent as is`() = runTest {
            // Arrange
            val otherId = "cd".repeat(32)
            val shortId = "ab12"
            coEvery { userWalletsListRepository.userWalletsSync() } returns
                listOf(defaultWallet, wallet(otherId), wallet(shortId))

            // Act
            repository.bind(UID)

            // Assert
            coVerify(exactly = 1) {
                tangemTechApi.bindMarketingUidWallets(
                    uid = UID,
                    body = BindMarketingUidWalletsBody(walletIds = listOf(WALLET_ID, otherId, shortId)),
                )
            }
        }

        @Test
        fun `GIVEN wallets WHEN the same uid is bound twice THEN each open sends a request`() = runTest {
            // Act
            repository.bind(UID)
            repository.bind(UID)

            // Assert
            coVerify(exactly = 2) { tangemTechApi.bindMarketingUidWallets(UID, any()) }
        }

        @Test
        fun `GIVEN backend fails WHEN bind THEN the binding is not retried`() = runTest {
            // Arrange
            coEvery { tangemTechApi.bindMarketingUidWallets(any(), any()) } returns
                errorResponse(ApiResponseError.NetworkException())

            // Act
            repository.bind(UID)
            repository.bindPending()

            // Assert
            coVerify(exactly = 1) { tangemTechApi.bindMarketingUidWallets(UID, any()) }
        }

        @Test
        fun `GIVEN no wallets WHEN bind THEN no request is made`() = runTest {
            // Arrange
            coEvery { userWalletsListRepository.userWalletsSync() } returns emptyList()

            // Act
            repository.bind(UID)

            // Assert
            coVerify(exactly = 0) { tangemTechApi.bindMarketingUidWallets(any(), any()) }
        }

        @Test
        fun `GIVEN wallet list fails to load WHEN bind THEN nothing is thrown and the uid waits`() = runTest {
            // Arrange
            coEvery { userWalletsListRepository.userWalletsSync() } throws IllegalStateException()

            // Act
            val actual = runCatching { repository.bind(UID) }
            coEvery { userWalletsListRepository.userWalletsSync() } returns listOf(defaultWallet)
            repository.bindPending()

            // Assert
            assertThat(actual.exceptionOrNull()).isNull()
            coVerify(exactly = 1) { tangemTechApi.bindMarketingUidWallets(UID, any()) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class BindPending {

        @Test
        fun `GIVEN uid waits for a wallet WHEN bindPending with a wallet THEN it is sent once`() = runTest {
            // Arrange
            coEvery { userWalletsListRepository.userWalletsSync() } returns emptyList()
            repository.bind(UID)
            coEvery { userWalletsListRepository.userWalletsSync() } returns listOf(defaultWallet)

            // Act
            repository.bindPending()
            repository.bindPending()

            // Assert
            coVerify(exactly = 1) { tangemTechApi.bindMarketingUidWallets(UID, any()) }
        }

        @Test
        fun `GIVEN the same uid opened twice without wallets WHEN bindPending THEN one request is sent`() = runTest {
            // Arrange
            coEvery { userWalletsListRepository.userWalletsSync() } returns emptyList()
            repository.bind(UID)
            repository.bind(UID)
            coEvery { userWalletsListRepository.userWalletsSync() } returns listOf(defaultWallet)

            // Act
            repository.bindPending()

            // Assert
            coVerify(exactly = 1) { tangemTechApi.bindMarketingUidWallets(UID, any()) }
        }

        @Test
        fun `GIVEN toggle off WHEN bindPending THEN no request is made`() = runTest {
            // Arrange
            coEvery { userWalletsListRepository.userWalletsSync() } returns emptyList()
            repository.bind(UID)
            coEvery { userWalletsListRepository.userWalletsSync() } returns listOf(defaultWallet)
            every { featureToggles.isUidBindingEnabled } returns false

            // Act
            repository.bindPending()

            // Assert
            coVerify(exactly = 0) { tangemTechApi.bindMarketingUidWallets(any(), any()) }
        }

        @Test
        fun `GIVEN nothing pending WHEN bindPending THEN wallets are not read`() = runTest {
            // Act
            repository.bindPending()

            // Assert
            coVerify(exactly = 0) { userWalletsListRepository.userWalletsSync() }
        }

        @Test
        fun `GIVEN concurrent calls WHEN the wallets load THEN a uid is sent once`() = runTest {
            // Arrange
            coEvery { userWalletsListRepository.userWalletsSync() } returns emptyList()
            repository.bind(UID)
            val wallets = CompletableDeferred<List<UserWallet>>()
            coEvery { userWalletsListRepository.userWalletsSync() } coAnswers { wallets.await() }
            coEvery { tangemTechApi.bindMarketingUidWallets(any(), any()) } coAnswers {
                yield()
                success()
            }
            launch { repository.bindPending() }
            launch { repository.bindPending() }
            runCurrent()

            // Act
            wallets.complete(listOf(defaultWallet))
            advanceUntilIdle()

            // Assert
            coVerify(exactly = 1) { tangemTechApi.bindMarketingUidWallets(UID, any()) }
        }
    }

    private fun wallet(id: String): UserWallet = mockk<UserWallet.Hot> {
        every { walletId } returns UserWalletId(id)
    }

    private fun success(): ApiResponse<Unit> = ApiResponse.Success(Unit)

    @Suppress("UNCHECKED_CAST")
    private fun errorResponse(cause: ApiResponseError): ApiResponse<Unit> =
        ApiResponse.Error(cause) as ApiResponse<Unit>

    private companion object {
        const val UID = "spring-campaign"
        val WALLET_ID = "AB".repeat(32)
    }
}