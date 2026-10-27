package com.tangem.data.marketing

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.data.common.wallet.WalletServerBinder
import com.tangem.datasource.api.marketing.models.BindMarketingUidWalletsBody
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.marketing.MarketingFeatureToggles
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultMarketingUidBindingRepositoryTest {

    private val tangemTechApi: TangemTechApi = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val featureToggles: MarketingFeatureToggles = mockk()
    private val walletServerBinder: WalletServerBinder = mockk()

    private val defaultWallet = wallet(WALLET_ID)
    private val userWallets = MutableStateFlow<List<UserWallet>?>(null)

    private val repository = DefaultMarketingUidBindingRepository(
        tangemTechApi = tangemTechApi,
        userWalletsListRepository = userWalletsListRepository,
        featureToggles = featureToggles,
        walletServerBinder = walletServerBinder,
    )

    @BeforeEach
    fun setUp() {
        clearMocks(tangemTechApi, userWalletsListRepository, featureToggles, walletServerBinder)
        userWallets.value = listOf(defaultWallet)

        every { featureToggles.isUidBindingEnabled } returns true
        every { userWalletsListRepository.userWallets } returns userWallets
        coEvery { tangemTechApi.bindMarketingUidWallets(any(), any()) } returns success()
        coEvery { walletServerBinder.awaitBound(any()) } just Runs
    }

    @Test
    fun `GIVEN toggle off WHEN bind THEN no request is made`() = runTest {
        // Arrange
        every { featureToggles.isUidBindingEnabled } returns false

        // Act
        repository.bind(UID)

        // Assert
        verify(exactly = 0) { userWalletsListRepository.userWallets }
        coVerify(exactly = 0) { tangemTechApi.bindMarketingUidWallets(any(), any()) }
    }

    @Test
    fun `GIVEN wallets WHEN bind THEN every wallet id is sent as is`() = runTest {
        // Arrange
        val otherId = "cd".repeat(32)
        val shortId = "ab12"
        userWallets.value = listOf(defaultWallet, wallet(otherId), wallet(shortId))

        // Act
        repository.bind(UID)

        // Assert
        coVerify(exactly = 1) {
            tangemTechApi.bindMarketingUidWallets(
                uid = UID,
                body = BindMarketingUidWalletsBody(walletIds = listOf(WALLET_ID, otherId, shortId)),
            )
        }
        coVerify(exactly = 0) { walletServerBinder.awaitBound(any()) }
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

        // Assert
        coVerify(exactly = 1) { tangemTechApi.bindMarketingUidWallets(UID, any()) }
    }

    @Test
    fun `GIVEN list not loaded WHEN it loads with a wallet THEN sent without asking the server`() = runTest {
        // Arrange
        userWallets.value = null
        launch { repository.bind(UID) }
        runCurrent()

        // Act
        userWallets.value = listOf(defaultWallet)
        runCurrent()

        // Assert
        coVerify(exactly = 1) {
            tangemTechApi.bindMarketingUidWallets(UID, BindMarketingUidWalletsBody(walletIds = listOf(WALLET_ID)))
        }
        coVerify(exactly = 0) { walletServerBinder.awaitBound(any()) }
    }

    @Test
    fun `GIVEN no wallet WHEN the first wallet appears THEN sent once the server has it`() = runTest {
        // Arrange
        val serverHasWallet = CompletableDeferred<Unit>()
        val sentBodies = mutableListOf<BindMarketingUidWalletsBody>()
        coEvery { walletServerBinder.awaitBound(UserWalletId(WALLET_ID)) } coAnswers { serverHasWallet.await() }
        coEvery { tangemTechApi.bindMarketingUidWallets(UID, capture(sentBodies)) } returns success()
        userWallets.value = emptyList()
        launch { repository.bind(UID) }
        runCurrent()

        // Act
        userWallets.value = listOf(defaultWallet)
        runCurrent()
        val sentBeforeServerHasWallet = sentBodies.toList()
        serverHasWallet.complete(Unit)
        runCurrent()

        // Assert
        assertThat(sentBeforeServerHasWallet).isEmpty()
        assertThat(sentBodies).containsExactly(BindMarketingUidWalletsBody(walletIds = listOf(WALLET_ID)))
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