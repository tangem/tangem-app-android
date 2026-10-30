package com.tangem.feature.swap.model

import arrow.core.Either
import com.google.common.truth.Truth.assertThat
import com.google.firebase.perf.FirebasePerformance
import com.tangem.domain.express.models.ExpressError
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.swap.buildSwapCurrencyStatus
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.feature.swap.domain.models.domain.SwapBalanceStatus
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeItem
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeSelectorUM
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SwapModelSolanaGaslessSuppressionTest : SwapModelTestBase() {

    private val jupiter: SwapProvider = swapProvider(id = "jupiter", type = ExchangeProviderType.DEX)

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
    fun `GIVEN new swap screen WHEN created THEN gasless is not suppressed`() {
        // Act
        val model = createModel()

        // Assert
        assertThat(model.isSolanaGaslessSuppressed).isFalse()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN swap send throws WHEN swap clicked THEN gasless stays suppressed only after a sponsored send`(
        model: ThrowingSendModel,
    ) = runTest {
        // Arrange
        val swapModel = createModelThrowingOnSwap(isSponsoredSend = model.isSponsoredSend)

        // Act
        swapModel.uiState.swapButton.onClick()
        runCurrent()

        // Assert
        assertThat(swapModel.isSolanaGaslessSuppressed).isEqualTo(model.isSuppressed)
        swapModel.onDestroy()
    }

    data class ThrowingSendModel(val isSponsoredSend: Boolean, val isSuppressed: Boolean)

    private fun provideTestModels() = listOf(
        ThrowingSendModel(isSponsoredSend = true, isSuppressed = true),
        ThrowingSendModel(isSponsoredSend = false, isSuppressed = false),
    )

    private fun TestScope.createModelThrowingOnSwap(isSponsoredSend: Boolean): SwapModel {
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
        } throws IllegalStateException("storing the sent swap failed")

        val quote = quotesLoadedState(jupiter)
        every { quote.preparedSwapConfigState.balanceStatus } returns if (isSponsoredSend) {
            SwapBalanceStatus.SponsoredFee(feePayer = "FeePayer")
        } else {
            SwapBalanceStatus.Sufficient
        }

        val model = createModel(createParams(), dispatchers = deferredDispatchers())
        advanceUntilIdle()
        model.dataState = model.dataState.copy(
            amount = "5",
            selectedPairProviders = listOf(jupiter),
            selectedProvider = jupiter,
            lastLoadedSwapStates = mapOf(jupiter to quote),
        )
        model.feeSelectorRepository.state.value = mockk<FeeSelectorUM.Content>(relaxed = true) {
            every { selectedFeeItem } returns FeeItem.Market(fee = mockk(relaxed = true))
            every { feeExtraInfo.sponsoredFee.isSelected } returns isSponsoredSend
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