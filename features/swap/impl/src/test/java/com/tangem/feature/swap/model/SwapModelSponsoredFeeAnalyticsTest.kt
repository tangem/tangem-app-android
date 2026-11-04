package com.tangem.feature.swap.model

import arrow.core.Either
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.Basic
import com.tangem.domain.express.models.ExpressError
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.swap.buildSwapCurrencyStatus
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.feature.swap.domain.models.ui.SwapTransactionState
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeItem
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeSelectorUM
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SwapModelSponsoredFeeAnalyticsTest : SwapModelTestBase() {

    private val jupiter: SwapProvider = swapProvider(id = "jupiter", type = ExchangeProviderType.DEX)

    @BeforeEach
    fun setUp() {
        setUpBase()
        coEvery {
            getFeePaidCryptoCurrencyStatusSyncUseCase(userWalletId = any(), cryptoCurrencyStatus = any())
        } returns Either.Right(null)
        coEvery { getMinimumTransactionAmountSyncUseCase(any(), any()) } returns Either.Right(null)
        every { swapTransferInteractor.shouldTransferInsteadOfSwap(any(), any()) } returns false
        coEvery {
            swapInteractor.findBestQuote(any(), any(), any(), any(), any(), any(), any())
        } answers { thirdArg<List<SwapProvider>>().associateWith { provider -> quotesLoadedState(provider) } }
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN swap sent WHEN swap clicked THEN Fee Asset Type is Sponsored only for the sponsored fee`(
        model: SentSwapModel,
    ) = runTest {
        // Arrange
        val events = mutableListOf<AnalyticsEvent>()
        every { analyticsEventHandler.send(capture(events)) } returns Unit
        every { getExplorerTransactionUrlUseCase(txHash = any(), currency = any()) } returns "".right()
        val swapModel = createSwapReadyModel(isSponsoredFeeSelected = model.isSponsoredFeeSelected)

        // Act
        swapModel.uiState.swapButton.onClick()
        advanceUntilIdle()

        // Assert
        val transactionSent = events.filterIsInstance<Basic.TransactionSent>()
        assertThat(transactionSent).hasSize(1)
        assertThat(transactionSent.single().params).containsAtLeast(
            "Source", "Swap",
            "Fee Asset Type", model.expectedFeeAssetType,
        )
        swapModel.onDestroy()
    }

    data class SentSwapModel(val isSponsoredFeeSelected: Boolean, val expectedFeeAssetType: String)

    private fun provideTestModels() = listOf(
        SentSwapModel(isSponsoredFeeSelected = true, expectedFeeAssetType = "Sponsored"),
        SentSwapModel(isSponsoredFeeSelected = false, expectedFeeAssetType = "Coin"),
    )

    private fun TestScope.createSwapReadyModel(isSponsoredFeeSelected: Boolean): SwapModel {
        val wallet: UserWallet = mockk(relaxed = true) { every { walletId } returns userWalletId }
        coEvery {
            initialCurrenciesResolver.invoke(any(), any(), any(), any(), any())
        } returns (buildSwapCurrencyStatus(wallet) to buildSwapCurrencyStatus(wallet))
        coEvery {
            swapInteractor.getPair(fromSwapCurrencyStatus = any(), toSwapCurrencyStatus = any(), filterProviderTypes = any())
        } returns Either.Left(ExpressError.UnknownError())
        coEvery { swapInteractor.getUnfulfilledReceiveRequirement(any()) } returns null
        coEvery {
            swapInteractor.onSwap(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                customSlippage = any(),
                intentId = any(),
            )
        } returns SwapTransactionState.TxSent(txHash = "tx-hash", timestamp = 0L)

        val model = createModel(createParams(), dispatchers = deferredDispatchers())
        advanceUntilIdle()
        model.dataState = model.dataState.copy(
            amount = "5",
            selectedPairProviders = listOf(jupiter),
            selectedProvider = jupiter,
            lastLoadedSwapStates = mapOf(jupiter to quotesLoadedState(jupiter)),
        )
        model.feeSelectorRepository.state.value = mockk<FeeSelectorUM.Content>(relaxed = true) {
            every { selectedFeeItem } returns FeeItem.Market(fee = mockk(relaxed = true))
            every { feeExtraInfo.sponsoredFee.isSelected } returns isSponsoredFeeSelected
        }
        return model
    }

    private fun TestScope.deferredDispatchers(): TestingCoroutineDispatcherProvider {
        val deferred = StandardTestDispatcher(testScheduler)
        return TestingCoroutineDispatcherProvider(
            main = deferred,
            mainImmediate = deferred,
            io = deferred,
            default = deferred,
        )
    }
}