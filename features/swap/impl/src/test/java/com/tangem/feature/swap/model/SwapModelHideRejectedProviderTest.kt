package com.tangem.feature.swap.model

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.google.firebase.perf.FirebasePerformance
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.express.models.ExpressError
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.swap.analytics.SwapEvents
import com.tangem.feature.swap.buildSwapCurrencyStatus
import com.tangem.feature.swap.domain.models.ExpressDataError
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.feature.swap.domain.models.ui.SwapTransactionState
import com.tangem.feature.swap.models.states.ProviderState
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeItem
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeSelectorUM
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class SwapModelHideRejectedProviderTest : SwapModelTestBase() {

    private val changeHero: SwapProvider = swapProvider(id = "changehero", type = ExchangeProviderType.CEX)
    private val changelly: SwapProvider = swapProvider(id = "changelly", type = ExchangeProviderType.CEX)

    @BeforeEach
    fun setUp() {
        setUpBase()
        mockkStatic(FirebasePerformance::class)
        every { FirebasePerformance.getInstance() } returns mockk(relaxed = true)
        coEvery {
            getFeePaidCryptoCurrencyStatusSyncUseCase(userWalletId = any(), cryptoCurrencyStatus = any())
        } returns Either.Right(null)
        coEvery { getMinimumTransactionAmountSyncUseCase(any(), any()) } returns Either.Right(null)
        every { swapTransferInteractor.shouldTransferInsteadOfSwap(any(), any()) } returns false
        coEvery {
            swapInteractor.findBestQuote(any(), any(), any(), any(), any(), any(), any())
        } answers { thirdArg<List<SwapProvider>>().associateWith { provider -> quotesLoadedState(provider) } }
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(FirebasePerformance::class)
    }

    @Test
    fun `GIVEN hiding enabled and provider rejected WHEN swap clicked THEN provider removed from pair`() = runTest {
        // Arrange
        every { swapFeatureToggles.isHideRejectedCexProviderEnabled } returns true
        val model = createModelFailingWith(
            selectedProvider = changeHero,
            errorCode = REJECTED_CODE,
            pairProviders = listOf(changeHero, changelly),
        )

        // Act
        model.uiState.swapButton.onClick()
        runCurrent()

        // Assert
        assertThat(model.dataState.selectedPairProviders).containsExactly(changelly)
        model.onDestroy()
    }

    @Test
    fun `GIVEN hiding disabled and provider rejected WHEN swap clicked THEN pair providers unchanged`() = runTest {
        // Arrange
        every { swapFeatureToggles.isHideRejectedCexProviderEnabled } returns false
        val model = createModelFailingWith(
            selectedProvider = changeHero,
            errorCode = REJECTED_CODE,
            pairProviders = listOf(changeHero, changelly),
        )

        // Act
        model.uiState.swapButton.onClick()
        runCurrent()

        // Assert
        assertThat(model.dataState.selectedPairProviders).containsExactly(changeHero, changelly)
        model.onDestroy()
    }

    @Test
    fun `GIVEN hiding enabled and Changelly fails with user-fixable code WHEN swap clicked THEN pair providers unchanged`() =
        runTest {
            // Arrange
            every { swapFeatureToggles.isHideRejectedCexProviderEnabled } returns true
            val model = createModelFailingWith(
                selectedProvider = changelly,
                errorCode = TOO_SMALL_AMOUNT_CODE,
                pairProviders = listOf(changeHero, changelly),
            )

            // Act
            model.uiState.swapButton.onClick()
            runCurrent()

            // Assert
            assertThat(model.dataState.selectedPairProviders).containsExactly(changeHero, changelly)
            model.onDestroy()
        }

    @Test
    fun `GIVEN hiding enabled and last provider rejected WHEN swap clicked THEN pair is unsupported`() = runTest {
        // Arrange
        every { swapFeatureToggles.isHideRejectedCexProviderEnabled } returns true
        val model = createModelFailingWith(
            selectedProvider = changeHero,
            errorCode = REJECTED_CODE,
            pairProviders = listOf(changeHero),
        )

        // Act
        model.uiState.swapButton.onClick()
        runCurrent()

        // Assert
        assertThat(model.dataState.selectedPairProviders).isEmpty()
        verify { analyticsEventHandler.send(match { it is SwapEvents.NoticeUnavailableToSwapPair }) }
        model.onDestroy()
    }

    private fun TestScope.createModelFailingWith(
        selectedProvider: SwapProvider,
        errorCode: Int,
        pairProviders: List<SwapProvider>,
    ): SwapModel {
        val wallet: UserWallet = mockk(relaxed = true) { every { walletId } returns userWalletId }
        coEvery {
            initialCurrenciesResolver.invoke(any(), any(), any(), any(), any())
        } returns (buildSwapCurrencyStatus(wallet) to buildSwapCurrencyStatus(wallet))
        coEvery {
            swapInteractor.getPair(fromSwapCurrencyStatus = any(), toSwapCurrencyStatus = any(), filterProviderTypes = any())
        } returns Either.Left(ExpressError.UnknownError)
        coEvery { swapInteractor.getUnfulfilledReceiveRequirement(any()) } returns null
        coEvery {
            swapInteractor.onSwap(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                customSlippage = any(),
                intentId = any(),
            )
        } returns SwapTransactionState.Error.ExpressError(ExpressDataError.UnknownErrorWithCode(errorCode))

        val model = createModel(createParams(), dispatchers = deferredDispatchers())
        advanceUntilIdle()
        model.dataState = model.dataState.copy(
            amount = "5",
            selectedPairProviders = pairProviders,
            selectedProvider = selectedProvider,
            lastLoadedSwapStates = mapOf(selectedProvider to quotesLoadedState(selectedProvider)),
        )
        model.uiState = model.uiState.copy(providerState = providerContentState(selectedProvider))
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
        const val REJECTED_CODE = 2264
        const val TOO_SMALL_AMOUNT_CODE = 2250
    }
}