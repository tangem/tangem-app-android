package com.tangem.data.marketing

import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.datasource.api.marketing.models.BindMarketingUidWalletsBody
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.marketing.MarketingFeatureToggles
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.confirmVerified
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultMarketingUidBindingRepositoryTest {

    private val tangemTechApi: TangemTechApi = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val featureToggles: MarketingFeatureToggles = mockk()

    private val defaultWallet = wallet(WALLET_ID)
    private val userWallets = MutableStateFlow<List<UserWallet>?>(null)

    private val repository = DefaultMarketingUidBindingRepository(
        tangemTechApi = tangemTechApi,
        userWalletsListRepository = userWalletsListRepository,
        featureToggles = featureToggles,
    )

    @BeforeEach
    fun setUp() {
        clearMocks(tangemTechApi, userWalletsListRepository, featureToggles)
        userWallets.value = listOf(defaultWallet)

        every { featureToggles.isUidBindingEnabled } returns true
        every { userWalletsListRepository.userWallets } returns userWallets
        coEvery { tangemTechApi.bindMarketingUidWallets(any(), any()) } returns success()
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

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN no wallet yet WHEN the first wallet appears THEN the binding is sent with it`(
        model: NoWalletModel,
    ) = runTest {
        // Arrange
        userWallets.value = model.wallets
        launch { repository.bind(UID) }
        runCurrent()

        // Act
        userWallets.value = listOf(defaultWallet)
        runCurrent()

        // Assert
        coVerify(exactly = 1) {
            tangemTechApi.bindMarketingUidWallets(UID, BindMarketingUidWalletsBody(walletIds = listOf(WALLET_ID)))
        }
        confirmVerified(tangemTechApi)
    }

    private fun provideTestModels() = listOf(
        NoWalletModel(name = "list not loaded", wallets = null),
        NoWalletModel(name = "empty list", wallets = emptyList()),
    )

    data class NoWalletModel(val name: String, val wallets: List<UserWallet>?) {
        override fun toString(): String = name
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