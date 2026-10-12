package com.tangem.feature.swap.model

import arrow.core.Either
import arrow.core.right
import com.google.firebase.perf.FirebasePerformance
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.express.models.ExpressError
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.swap.buildSwapCurrencyStatus
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.feature.swap.domain.models.domain.SwapPairLeast
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.feature.swap.domain.models.ui.SwapTransactionState
import com.tangem.feature.swap.models.states.ProviderState
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeItem
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeSelectorUM
import com.tangem.features.swap.SwapComponent
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
internal class SwapModelIntentTest : SwapModelTestBase() {

    private val provider: SwapProvider = swapProvider(id = "changelly", type = ExchangeProviderType.CEX)

    @BeforeEach
    fun setUp() {
        setUpBase()
        mockkStatic(FirebasePerformance::class)
        every { FirebasePerformance.getInstance() } returns mockk(relaxed = true)
        coEvery {
            getFeePaidCryptoCurrencyStatusSyncUseCase(userWalletId = any(), cryptoCurrencyStatus = any())
        } returns Either.Right(null)
        coEvery {
            getMinimumTransactionAmountSyncUseCase(any(), any())
        } returns Either.Right(null)
        every { swapTransferInteractor.shouldTransferInsteadOfSwap(any(), any()) } returns false
        coEvery { swapInteractor.findProvidersForPairWithCheck(any(), any(), any()) } returns listOf(provider)
        coEvery {
            swapInteractor.findBestQuote(any(), any(), any(), any(), any(), any(), any())
        } returns mapOf(provider to quotesLoadedState(provider))
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(FirebasePerformance::class)
    }

    @Test
    fun `GIVEN intent in params WHEN model created THEN quotes are requested for the intent`() = runTest {
        // Arrange
        stubResolvedPair(pairs = Either.Right(emptyList()))

        // Act
        val model = createModel(createParams(fromAmount = BigDecimal.ONE, intentId = INTENT_ID))
        advanceUntilIdle()

        // Assert
        coVerify(atLeast = 1) {
            swapInteractor.findBestQuote(
                any(), any(), any(), any(), any(),
                customSlippage = any(),
                intentId = INTENT_ID,
            )
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN no intent in params WHEN model created THEN quotes are requested without an intent`() = runTest {
        // Arrange
        stubResolvedPair(pairs = Either.Right(emptyList()))

        // Act
        val model = createModel(createParams(fromAmount = BigDecimal.ONE))
        advanceUntilIdle()

        // Assert
        coVerify(atLeast = 1) {
            swapInteractor.findBestQuote(
                any(), any(), any(), any(), any(),
                customSlippage = any(),
                intentId = null,
            )
        }
        model.onDestroy()
    }

    @Test
    fun `GIVEN intent in params WHEN swap button clicked THEN swap is requested for the intent`() = runTest {
        // Arrange
        val model = createModelReadyToSwap(createParams(intentId = INTENT_ID))

        // Act
        model.uiState.swapButton.onClick()
        advanceUntilIdle()

        // Assert
        verifySwapRequested(intentId = INTENT_ID)
        model.onDestroy()
    }

    @Test
    fun `GIVEN no intent in params WHEN swap button clicked THEN swap is requested without an intent`() = runTest {
        // Arrange
        val model = createModelReadyToSwap(createParams())

        // Act
        model.uiState.swapButton.onClick()
        advanceUntilIdle()

        // Assert
        verifySwapRequested(intentId = null)
        model.onDestroy()
    }

    private fun stubResolvedPair(
        pairs: Either<ExpressError, List<SwapPairLeast>> = Either.Left(ExpressError.UnknownError),
    ) {
        val wallet: UserWallet = mockk(relaxed = true) { every { walletId } returns userWalletId }
        coEvery {
            initialCurrenciesResolver.invoke(any(), any(), any(), any(), any())
        } returns (buildSwapCurrencyStatus(wallet) to buildSwapCurrencyStatus(wallet))
        coEvery {
            swapInteractor.getPair(
                fromSwapCurrencyStatus = any(),
                toSwapCurrencyStatus = any(),
                filterProviderTypes = any(),
            )
        } returns pairs
    }

    private fun verifySwapRequested(intentId: String?) = coVerify(exactly = 1) {
        swapInteractor.onSwap(
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
            customSlippage = any(),
            intentId = intentId,
        )
    }

    private fun TestScope.createModelReadyToSwap(params: SwapComponent.Params): SwapModel {
        stubResolvedPair()
        coEvery { swapInteractor.getUnfulfilledReceiveRequirement(any()) } returns null
        coEvery {
            swapInteractor.onSwap(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                customSlippage = any(),
                intentId = any(),
            )
        } returns SwapTransactionState.TxSent(txHash = TX_HASH, timestamp = 0L)
        every { getExplorerTransactionUrlUseCase(txHash = any(), currency = any()) } returns EXPLORER_URL.right()

        val model = createModel(params, dispatchers = deferredDispatchers())
        advanceUntilIdle()
        model.dataState = model.dataState.copy(
            amount = "5",
            selectedProvider = provider,
            lastLoadedSwapStates = mapOf(provider to quotesLoadedState(provider)),
        )
        model.uiState = model.uiState.copy(providerState = providerContentState(provider))
        model.feeSelectorRepository.state.value = mockk<FeeSelectorUM.Content>(relaxed = true) {
            every { selectedFeeItem } returns FeeItem.Market(fee = mockk(relaxed = true))
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

    private fun providerContentState(provider: SwapProvider) = ProviderState.Content(
        id = provider.providerId,
        name = provider.name,
        type = provider.type.providerName,
        iconUrl = "",
        subtitle = stringReference(""),
        selectionType = ProviderState.SelectionType.CLICK,
        additionalBadge = ProviderState.AdditionalBadge.Empty,
        namePrefix = ProviderState.PrefixType.NONE,
        onProviderClick = {},
    )

    private companion object {
        const val INTENT_ID = "intent-id"
        const val TX_HASH = "0xTxHash"
        const val EXPLORER_URL = "https://etherscan.io/tx/0xTxHash"
    }
}