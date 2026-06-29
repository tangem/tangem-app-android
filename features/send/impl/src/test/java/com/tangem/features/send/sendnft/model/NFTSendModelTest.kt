package com.tangem.features.send.sendnft.model

import arrow.core.left
import arrow.core.right
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.account.status.model.AccountCryptoCurrencyStatus
import com.tangem.domain.account.status.usecase.GetAccountCurrencyStatusUseCase
import com.tangem.domain.account.status.usecase.GetFeePaidCryptoCurrencyStatusSyncUseCase
import com.tangem.domain.account.status.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.feedback.SaveBlockchainErrorUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesSupplier
import com.tangem.domain.transaction.usecase.CreateNFTTransferTransactionUseCase
import com.tangem.domain.transaction.usecase.GetFeeUseCase
import com.tangem.domain.wallets.models.errors.GetUserWalletError
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.nft.entity.NFTSendSuccessTrigger
import com.tangem.features.send.api.NFTSendComponent
import com.tangem.features.send.common.CommonSendRoute
import com.tangem.features.send.common.SendConfirmAlertFactory
import com.tangem.features.send.testDispatcherProvider
import com.tangem.test.core.ProvideTestModels
import io.mockk.MockKAnnotations
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@OptIn(ExperimentalCoroutinesApi::class)
internal class NFTSendModelTest {

    private val testUserWalletId = UserWalletId("1234567890ABCDEF")
    private val network: Network = mockk(relaxed = true)
    private val nftAsset: com.tangem.domain.nft.models.NFTAsset = mockk(relaxed = true)
    private val testUserWallet: UserWallet = mockk(relaxed = true)
    private val testCryptoCurrencyStatus: CryptoCurrencyStatus = mockk(relaxed = true)
    private val coin: CryptoCurrency.Coin = mockk(relaxed = true)

    private val router: Router = mockk(relaxed = true)
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase = mockk(relaxed = true)
    private val getUserWalletUseCase: GetUserWalletUseCase = mockk(relaxed = true)
    private val multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier = mockk(relaxed = true)
    private val getFeePaidCryptoCurrencyStatusSyncUseCase: GetFeePaidCryptoCurrencyStatusSyncUseCase =
        mockk(relaxed = true)
    private val createNFTTransferTransactionUseCase: CreateNFTTransferTransactionUseCase = mockk(relaxed = true)
    private val getFeeUseCase: GetFeeUseCase = mockk(relaxed = true)
    private val saveBlockchainErrorUseCase: SaveBlockchainErrorUseCase = mockk(relaxed = true)
    private val getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase = mockk(relaxed = true)
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase = mockk(relaxed = true)
    private val alertFactory: SendConfirmAlertFactory = mockk(relaxed = true)
    private val nftSendSuccessTrigger: NFTSendSuccessTrigger = mockk(relaxed = true)
    private val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase = mockk(relaxed = true)
    private val getAccountCurrencyStatusUseCase: GetAccountCurrencyStatusUseCase = mockk(relaxed = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        // PER_CLASS parameterized nested classes reuse one instance — reset recorded calls between rows.
        clearMocks(router, nftSendSuccessTrigger, alertFactory, answers = false, recordedCalls = true, childMocks = false)

        every { nftAsset.network } returns network
        every { coin.network } returns network
        every { getUserWalletUseCase(testUserWalletId) } returns testUserWallet.right()
        coEvery { getSelectedAppCurrencyUseCase.invokeSync() } returns AppCurrency.Default.right()
        coEvery { multiWalletCryptoCurrenciesSupplier.getSyncOrNull(any(), any()) } returns null
        every { getAccountCurrencyStatusUseCase(any(), any()) } returns emptyFlow()
        coEvery { isAccountsModeEnabledUseCase.invokeSync() } returns false
        coEvery { getFeePaidCryptoCurrencyStatusSyncUseCase(any(), any()) } returns testCryptoCurrencyStatus.right()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class OnNextClick {

        @ParameterizedTest
        @ProvideTestModels
        fun `WHEN onNextClick THEN push Confirm for destination else navigate back`(model: NextClickModel) = runTest {
            // Arrange
            val sut = buildModel()
            advanceUntilIdle()

            // Act
            sut.onNextClick(model.route)
            advanceUntilIdle()

            // Assert
            when (model.expectedAction) {
                NextClickAction.PushConfirm -> {
                    verify(exactly = 1) { router.push(CommonSendRoute.Confirm, any()) }
                    verify(exactly = 0) { router.pop(any()) }
                    verify(exactly = 0) { router.replaceAll(*anyVararg(), onComplete = any()) }
                }
                NextClickAction.ReplaceAllConfirmSuccess -> {
                    // Confirm route → replaceAll(ConfirmSuccess)
                    verify(exactly = 1) { router.replaceAll(CommonSendRoute.ConfirmSuccess, onComplete = any()) }
                    verify(exactly = 0) { router.pop(any()) }
                    verify(exactly = 0) { router.push(any(), any()) }
                }
                NextClickAction.Pop -> {
                    verify(exactly = 1) { router.pop(any()) }
                    verify(exactly = 0) { router.push(any(), any()) }
                    verify(exactly = 0) { router.replaceAll(*anyVararg(), onComplete = any()) }
                }
            }
        }

        private fun provideTestModels() = listOf(
            NextClickModel(route = CommonSendRoute.Destination(isEditMode = false), expectedAction = NextClickAction.PushConfirm),
            NextClickModel(route = CommonSendRoute.Destination(isEditMode = true), expectedAction = NextClickAction.Pop),
            NextClickModel(route = CommonSendRoute.Confirm, expectedAction = NextClickAction.ReplaceAllConfirmSuccess),
        )
    }

    enum class NextClickAction { PushConfirm, ReplaceAllConfirmSuccess, Pop }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class OnBackClick {

        @ParameterizedTest
        @ProvideTestModels
        fun `WHEN onBackClick THEN trigger success only from ConfirmSuccess and always pop`(model: BackClickModel) =
            runTest {
                // Arrange
                val sut = buildModel()
                advanceUntilIdle()

                // Act
                sut.onBackClick(model.route)
                advanceUntilIdle()

                // Assert
                coVerify(exactly = model.expectedTriggerCalls) { nftSendSuccessTrigger.triggerSuccessNFTSend() }
                verify(exactly = 1) { router.pop(any()) }
            }

        private fun provideTestModels() = listOf(
            BackClickModel(route = CommonSendRoute.ConfirmSuccess, expectedTriggerCalls = 1),
            BackClickModel(route = CommonSendRoute.Destination(isEditMode = false), expectedTriggerCalls = 0),
        )
    }

    @Nested
    inner class SubscribeOnCurrencyStatusUpdates {

        @Test
        fun `GIVEN get user wallet fails WHEN init THEN show generic error`() = runTest {
            // Arrange
            every { getUserWalletUseCase(testUserWalletId) } returns GetUserWalletError.UserWalletNotFound.left()

            // Act
            buildModel()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { alertFactory.getGenericErrorState(any(), any()) }
        }

        @Test
        fun `GIVEN currency status loaded with empty destination WHEN init THEN navigate to destination`() = runTest {
            // Arrange
            coEvery { multiWalletCryptoCurrenciesSupplier.getSyncOrNull(any(), any()) } returns setOf(coin)
            val accountStatus = mockk<AccountCryptoCurrencyStatus> {
                every { component1() } returns mockk(relaxed = true)
                every { component2() } returns testCryptoCurrencyStatus
            }
            every { getAccountCurrencyStatusUseCase(testUserWalletId, coin) } returns flowOf(accountStatus)

            // Act
            buildModel()
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) {
                router.replaceAll(CommonSendRoute.Destination(isEditMode = false), onComplete = any())
            }
        }
    }

    // region fixtures

    private fun TestScope.buildModel(): NFTSendModel {
        val params = NFTSendComponent.Params(
            userWalletId = testUserWalletId,
            nftAsset = nftAsset,
            nftCollectionName = "Collection",
        )
        return NFTSendModel(
            paramsContainer = MutableParamsContainer(params),
            dispatchers = testDispatcherProvider(),
            router = router,
            getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
            getUserWalletUseCase = getUserWalletUseCase,
            multiWalletCryptoCurrenciesSupplier = multiWalletCryptoCurrenciesSupplier,
            getFeePaidCryptoCurrencyStatusSyncUseCase = getFeePaidCryptoCurrencyStatusSyncUseCase,
            createNFTTransferTransactionUseCase = createNFTTransferTransactionUseCase,
            getFeeUseCase = getFeeUseCase,
            saveBlockchainErrorUseCase = saveBlockchainErrorUseCase,
            getWalletMetaInfoUseCase = getWalletMetaInfoUseCase,
            sendFeedbackEmailUseCase = sendFeedbackEmailUseCase,
            alertFactory = alertFactory,
            nftSendSuccessTrigger = nftSendSuccessTrigger,
            isAccountsModeEnabledUseCase = isAccountsModeEnabledUseCase,
            getAccountCurrencyStatusUseCase = getAccountCurrencyStatusUseCase,
            analyticsEventHandler = analyticsEventHandler,
        )
    }

    data class NextClickModel(val route: CommonSendRoute, val expectedAction: NextClickAction)

    data class BackClickModel(val route: CommonSendRoute, val expectedTriggerCalls: Int)

    // endregion
}