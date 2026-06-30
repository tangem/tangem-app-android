package com.tangem.features.send.sendnft.confirm.model

import android.os.SystemClock
import arrow.core.left
import arrow.core.right
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.nft.models.NFTAsset as SdkNFTAsset
import com.tangem.common.routing.AppRouter
import com.tangem.common.ui.navigationButtons.NavigationUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.datasource.local.nft.converter.NFTSdkAssetConverter
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.feedback.SaveBlockchainErrorUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.nft.models.NFTAsset
import com.tangem.domain.settings.IsSendTapHelpEnabledUseCase
import com.tangem.domain.settings.NeverShowTapHelpUseCase
import com.tangem.domain.transaction.usecase.CreateNFTTransferTransactionUseCase
import com.tangem.domain.transaction.usecase.SendTransactionUseCase
import com.tangem.domain.txhistory.usecase.GetExplorerTransactionUrlUseCase
import com.tangem.features.nft.entity.NFTSendSuccessTrigger
import com.tangem.features.send.api.analytics.CommonSendAnalyticEvents
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeExtraInfo
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeItem
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeNonce
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeSelectorUM
import com.tangem.features.send.api.subcomponents.destination.entity.DestinationUM
import com.tangem.features.send.api.subcomponents.feeSelector.FeeSelectorCheckReloadListener
import com.tangem.features.send.api.subcomponents.feeSelector.FeeSelectorCheckReloadTrigger
import com.tangem.features.send.api.subcomponents.feeSelector.FeeSelectorReloadTrigger
import com.tangem.features.send.api.subcomponents.notifications.SendNotificationsUpdateListener
import com.tangem.features.send.api.subcomponents.notifications.SendNotificationsUpdateTrigger
import com.tangem.features.send.common.SendBalanceUpdater
import com.tangem.features.send.common.SendConfirmAlertFactory
import com.tangem.features.send.common.ui.state.ConfirmUM
import com.tangem.features.send.loadedStatus
import com.tangem.features.send.testDispatcherProvider
import com.tangem.features.send.sendnft.analytics.NFTSendAnalyticHelper
import com.tangem.features.send.sendnft.confirm.NFTSendConfirmComponent
import com.tangem.features.send.sendnft.ui.state.NFTSendUM
import com.tangem.test.core.ProvideTestModels
import io.mockk.MockKAnnotations
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
internal class NFTSendConfirmModelTest {

    private val network: Network = mockk(relaxed = true)
    private val nftAsset: NFTAsset = mockk(relaxed = true)
    private val testUserWallet: UserWallet = mockk(relaxed = true)
    private val testCryptoCurrency: CryptoCurrency = mockk(relaxed = true) {
        every { this@mockk.network } returns this@NFTSendConfirmModelTest.network
    }

    private val router: Router = mockk(relaxed = true)
    private val appRouter: AppRouter = mockk(relaxed = true)
    private val isSendTapHelpEnabledUseCase: IsSendTapHelpEnabledUseCase = mockk(relaxed = true)
    private val neverShowTapHelpUseCase: NeverShowTapHelpUseCase = mockk(relaxed = true)
    private val createNFTTransferTransactionUseCase: CreateNFTTransferTransactionUseCase = mockk(relaxed = true)
    private val sendTransactionUseCase: SendTransactionUseCase = mockk(relaxed = true)
    private val getExplorerTransactionUrlUseCase: GetExplorerTransactionUrlUseCase = mockk(relaxed = true)
    private val saveBlockchainErrorUseCase: SaveBlockchainErrorUseCase = mockk(relaxed = true)
    private val getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase = mockk(relaxed = true)
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase = mockk(relaxed = true)
    private val notificationsUpdateTrigger: SendNotificationsUpdateTrigger = mockk(relaxed = true)
    private val notificationsUpdateListener: SendNotificationsUpdateListener = mockk(relaxed = true)
    private val feeSelectorCheckReloadTrigger: FeeSelectorCheckReloadTrigger = mockk(relaxed = true)
    private val feeSelectorCheckReloadListener: FeeSelectorCheckReloadListener = mockk(relaxed = true)
    private val alertFactory: SendConfirmAlertFactory = mockk(relaxed = true)
    private val urlOpener: UrlOpener = mockk(relaxed = true)
    private val shareManager: ShareManager = mockk(relaxed = true)
    private val analyticsEventHandler: AnalyticsEventHandler = mockk(relaxed = true)
    private val nftSendAnalyticHelper: NFTSendAnalyticHelper = mockk(relaxed = true)
    private val nftSendSuccessTrigger: NFTSendSuccessTrigger = mockk(relaxed = true)
    private val feeSelectorReloadTrigger: FeeSelectorReloadTrigger = mockk(relaxed = true)
    private val sendBalanceUpdaterFactory: SendBalanceUpdater.Factory = mockk(relaxed = true)

    private val loadedStatus: CryptoCurrencyStatus get() = loadedStatus(testCryptoCurrency)

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        mockkStatic(SystemClock::class)
        every { SystemClock.elapsedRealtime() } returns 0L
        mockkObject(NFTSdkAssetConverter)
        every { NFTSdkAssetConverter.convertBack(any()) } returns (network to mockk<SdkNFTAsset>(relaxed = true))

        clearMocks(
            createNFTTransferTransactionUseCase,
            sendTransactionUseCase,
            feeSelectorCheckReloadTrigger,
            alertFactory,
            answers = false,
            recordedCalls = true,
            childMocks = false,
        )

        coEvery { isSendTapHelpEnabledUseCase.invokeSync() } returns false.right()
        every { isSendTapHelpEnabledUseCase() } returns emptyFlow<Boolean>().right()
        every { notificationsUpdateListener.hasErrorFlow } returns emptyFlow()
        every { feeSelectorCheckReloadListener.checkReloadResultFlow } returns emptyFlow()
        coEvery {
            createNFTTransferTransactionUseCase(any(), any(), any(), any(), any(), any(), any())
        } returns mockk<TransactionData.Uncompiled>(relaxed = true).right()
        coEvery { sendTransactionUseCase(any(), any(), any()) } returns "txHash".right()
        every { getExplorerTransactionUrlUseCase(any(), any()) } returns "https://explorer/tx".right()
        every { sendBalanceUpdaterFactory.create(any(), any()) } returns mockk(relaxed = true)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(SystemClock::class)
        unmockkObject(NFTSdkAssetConverter)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class OnSendClick {

        @ParameterizedTest
        @ProvideTestModels
        fun `WHEN onSendClick THEN send fresh fee else trigger check reload`(model: OnSendClickModel) = runTest {
            // Arrange
            every { SystemClock.elapsedRealtime() } returns model.elapsedRealtime
            val sut = buildModel()
            advanceUntilIdle()

            // Act
            sut.onSendClick()
            advanceUntilIdle()

            // Assert
            if (model.expectedSendInitiated) {
                coVerify(exactly = 1) { createNFTTransferTransactionUseCase(any(), any(), any(), any(), any(), any(), any()) }
                coVerify(exactly = 0) { feeSelectorCheckReloadTrigger.triggerCheckUpdate() }
            } else {
                coVerify(exactly = 0) { createNFTTransferTransactionUseCase(any(), any(), any(), any(), any(), any(), any()) }
                coVerify(exactly = 1) { feeSelectorCheckReloadTrigger.triggerCheckUpdate() }
            }
        }

        private fun provideTestModels() = listOf(
            OnSendClickModel(elapsedRealtime = 0L, expectedSendInitiated = true),
            OnSendClickModel(elapsedRealtime = 20_000L, expectedSendInitiated = false),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CheckFeeResult {

        @ParameterizedTest
        @ProvideTestModels
        fun `WHEN check reload result emitted THEN send transaction only on success`(model: CheckFeeResultModel) =
            runTest {
                // Arrange
                val resultFlow = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
                every { feeSelectorCheckReloadListener.checkReloadResultFlow } returns resultFlow
                buildModel()
                advanceUntilIdle()

                // Act
                resultFlow.tryEmit(model.checkResult)
                advanceUntilIdle()

                // Assert
                coVerify(exactly = model.expectedCreateNFTTTransferCalls) {
                    createNFTTransferTransactionUseCase(any(), any(), any(), any(), any(), any(), any())
                }
            }

        private fun provideTestModels() = listOf(
            CheckFeeResultModel(checkResult = true, expectedCreateNFTTTransferCalls = 1),
            CheckFeeResultModel(checkResult = false, expectedCreateNFTTTransferCalls = 0),
        )
    }

    @Nested
    inner class VerifyAndSend {

        @Test
        fun `GIVEN successful send WHEN verifyAndSend THEN notify onSendTransaction`() = runTest {
            // Arrange
            val onSendTransaction = mockk<() -> Unit>(relaxed = true)
            val callback = mockk<NFTSendConfirmComponent.ModelCallback>(relaxed = true)
            val resultFlow = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
            every { feeSelectorCheckReloadListener.checkReloadResultFlow } returns resultFlow
            buildModel(
                paramsContainer = MutableParamsContainer(
                    defaultParams().copy(onSendTransaction = onSendTransaction, callback = callback),
                ),
            )
            advanceUntilIdle()

            // Act
            resultFlow.tryEmit(true)
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { onSendTransaction.invoke() }
            verify(exactly = 1) { callback.onResult(any()) }
        }

        @Test
        fun `GIVEN transaction creation fails WHEN verifyAndSend THEN show generic error and do NOT send`() = runTest {
            // Arrange
            val resultFlow = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
            every { feeSelectorCheckReloadListener.checkReloadResultFlow } returns resultFlow
            coEvery {
                createNFTTransferTransactionUseCase(any(), any(), any(), any(), any(), any(), any())
            } returns IllegalStateException("boom").left()
            buildModel()
            advanceUntilIdle()

            // Act
            resultFlow.tryEmit(true)
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { alertFactory.getGenericErrorState(any(), any()) }
            coVerify(exactly = 0) { sendTransactionUseCase(any(), any(), any()) }
        }
    }

    // region fixtures

    private fun TestScope.buildModel(
        paramsContainer: ParamsContainer = MutableParamsContainer(defaultParams()),
    ): NFTSendConfirmModel {
        return NFTSendConfirmModel(
            paramsContainer = paramsContainer,
            dispatchers = testDispatcherProvider(),
            router = router,
            appRouter = appRouter,
            isSendTapHelpEnabledUseCase = isSendTapHelpEnabledUseCase,
            neverShowTapHelpUseCase = neverShowTapHelpUseCase,
            createNFTTransferTransactionUseCase = createNFTTransferTransactionUseCase,
            sendTransactionUseCase = sendTransactionUseCase,
            getExplorerTransactionUrlUseCase = getExplorerTransactionUrlUseCase,
            saveBlockchainErrorUseCase = saveBlockchainErrorUseCase,
            getWalletMetaInfoUseCase = getWalletMetaInfoUseCase,
            sendFeedbackEmailUseCase = sendFeedbackEmailUseCase,
            notificationsUpdateTrigger = notificationsUpdateTrigger,
            notificationsUpdateListener = notificationsUpdateListener,
            feeSelectorCheckReloadTrigger = feeSelectorCheckReloadTrigger,
            feeSelectorCheckReloadListener = feeSelectorCheckReloadListener,
            alertFactory = alertFactory,
            urlOpener = urlOpener,
            shareManager = shareManager,
            analyticsEventHandler = analyticsEventHandler,
            nftSendAnalyticHelper = nftSendAnalyticHelper,
            nftSendSuccessTrigger = nftSendSuccessTrigger,
            feeSelectorReloadTrigger = feeSelectorReloadTrigger,
            sendBalanceUpdaterFactory = sendBalanceUpdaterFactory,
        )
    }

    private fun defaultParams(): NFTSendConfirmComponent.Params = NFTSendConfirmComponent.Params(
        state = contentState(),
        analyticsCategoryName = "test_nft_send",
        analyticsSendSource = CommonSendAnalyticEvents.CommonSendSource.NFT,
        userWallet = testUserWallet,
        appCurrency = AppCurrency.Default,
        nftAsset = nftAsset,
        nftCollectionName = "Collection",
        cryptoCurrencyStatus = loadedStatus,
        feeCryptoCurrencyStatus = loadedStatus,
        account = null,
        isAccountsMode = false,
        callback = mockk(relaxed = true),
        currentRoute = flowOf(),
        isBalanceHidingFlow = kotlinx.coroutines.flow.MutableStateFlow(false),
        onLoadFee = { mockk<com.tangem.blockchain.common.transaction.TransactionFee>(relaxed = true).right() },
        onSendTransaction = {},
    )

    private fun contentState(): NFTSendUM {
        val destination = mockk<DestinationUM.Content>(relaxed = true) {
            every { addressTextField.actualAddress } returns "destinationAddr"
            every { memoTextField } returns null
        }
        val extraInfo = mockk<FeeExtraInfo>(relaxed = true) {
            every { transactionFeeExtended } returns null
            every { feeCryptoCurrencyStatus } returns loadedStatus
        }
        val feeSelector = mockk<FeeSelectorUM.Content>(relaxed = true) {
            every { selectedFeeItem } returns FeeItem.Market(realFee())
            every { feeNonce } returns FeeNonce.None
            every { feeExtraInfo } returns extraInfo
            every { isPrimaryButtonEnabled } returns true
        }
        return NFTSendUM(
            destinationUM = destination,
            feeSelectorUM = feeSelector,
            confirmUM = mockk<ConfirmUM.Content>(relaxed = true),
            navigationUM = NavigationUM.Empty,
        )
    }

    private fun realFee(): Fee = Fee.Common(
        Amount(currencySymbol = "ETH", value = BigDecimal("0.001"), decimals = 18),
    )

    data class OnSendClickModel(val elapsedRealtime: Long, val expectedSendInitiated: Boolean)

    data class CheckFeeResultModel(val checkResult: Boolean, val expectedCreateNFTTTransferCalls: Int)

    // endregion
}