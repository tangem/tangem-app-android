package com.tangem.feature.swap.model

import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.feature.swap.domain.models.domain.PreparedSwapConfigState
import com.tangem.feature.swap.domain.models.domain.SwapBalanceStatus
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.feature.swap.domain.models.ui.PermissionDataState
import com.tangem.feature.swap.models.states.SwapNotificationUM
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeSelectorUM
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@OptIn(ExperimentalCoroutinesApi::class)
internal class SwapModelFeeCurrencyInStackTest : SwapModelTestBase() {

    private val fromToken: CryptoCurrency.Token = mockk(relaxed = true) {
        every { id } returns CryptoCurrency.ID(
            prefix = CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
            body = CryptoCurrency.ID.Body.NetworkId("ethereum"),
            suffix = CryptoCurrency.ID.Suffix.RawID("0xusdc"),
        )
    }
    private val feeCoin: CryptoCurrency.Coin = mockk(relaxed = true) {
        every { id } returns CryptoCurrency.ID(
            prefix = CryptoCurrency.ID.Prefix.COIN_PREFIX,
            body = CryptoCurrency.ID.Body.NetworkId("ethereum"),
            suffix = CryptoCurrency.ID.Suffix.RawID("ethereum"),
        )
    }

    @BeforeEach
    fun setUp() {
        setUpBase()
        every { isEvmGaslessFeeSupportedForNetwork(any()) } returns false
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `GIVEN fee currency details in stack or not WHEN fee error shown THEN go to fee currency is hidden only when in stack`(
        isFeeCurrencyInStack: Boolean,
    ) = runTest {
        // Arrange
        val feeCurrencyRoute = AppRoute.CurrencyDetails(userWalletId = userWalletId, currency = feeCoin)
        coEvery { router.contains(any()) } returns false
        coEvery { router.contains(feeCurrencyRoute) } returns isFeeCurrencyInStack
        val model = createModel()
        val provider = swapProvider()
        val fromStatus = swapCurrencyStatus(currency = fromToken).also {
            every { it.userWalletId } returns userWalletId
        }
        model.dataState = model.dataState.copy(
            fromSwapCurrencyStatus = fromStatus,
            toSwapCurrencyStatus = swapCurrencyStatus(),
            selectedProvider = provider,
            feePaidCryptoCurrency = mockk<CryptoCurrencyStatus>(relaxed = true) {
                every { currency } returns feeCoin
            },
            lastLoadedSwapStates = mapOf(provider to insufficientFeeState(provider, fromStatus)),
        )

        // Act
        model.feeSelectorRepository.onResult(FeeSelectorUM.Error(error = GetFeeError.UnknownError, isHidden = false))

        // Assert
        val warning = model.uiState.notifications
            .filterIsInstance<SwapNotificationUM.Error.UnableToCoverFeeWarning>()
            .single()
        assertThat(warning.onConfirmClick == null).isEqualTo(isFeeCurrencyInStack)
        coVerify(exactly = 1) { router.contains(feeCurrencyRoute) }
    }

    private fun insufficientFeeState(provider: SwapProvider, fromStatus: SwapCurrencyStatus) =
        quotesLoadedState(provider = provider).also { state ->
            every { state.fromTokenInfo.swapCurrencyStatus } returns fromStatus
            every { state.permissionState } returns PermissionDataState.Empty
            every { state.preparedSwapConfigState } returns PreparedSwapConfigState(
                balanceStatus = SwapBalanceStatus.InsufficientFee(
                    feeCurrencyName = "Ethereum",
                    feeCurrencySymbol = "ETH",
                ),
                hasOutgoingTransaction = false,
            )
        }
}