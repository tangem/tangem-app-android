package com.tangem.feature.swap

import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRouter
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.express.models.ProviderFilterType
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.domain.transaction.usecase.gasless.IsGaslessFeeSupportedForNetwork
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.ui.TokenSwapInfo
import com.tangem.feature.swap.models.SwapStateHolder
import com.tangem.feature.swap.models.UiActions
import com.tangem.feature.swap.models.states.ChooseProviderBottomSheetConfig
import com.tangem.feature.swap.models.states.PercentDifference
import com.tangem.feature.swap.models.states.ProviderState
import com.tangem.feature.swap.ui.StateBuilder
import com.tangem.utils.Provider
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * Tests for [StateBuilder.updateProvidersBottomSheetContent] — the periodic refresh that rewrites the
 * rows of an already open provider picker.
 */
internal class StateBuilderProvidersBottomSheetTest {

    private val actions: UiActions = mockk(relaxed = true)
    private val isBalanceHiddenProvider: Provider<Boolean> = mockk()
    private val appCurrencyProvider: Provider<AppCurrency> = mockk()
    private val isAccountsModeProvider: Provider<Boolean> = mockk()
    private val isGaslessFeeSupportedForNetwork: IsGaslessFeeSupportedForNetwork = mockk()
    private val appRouter: AppRouter = mockk()

    private lateinit var sut: StateBuilder

    @BeforeEach
    fun setup() {
        every { isBalanceHiddenProvider() } returns false
        every { appCurrencyProvider() } returns AppCurrency.Default
        every { isAccountsModeProvider() } returns false

        sut = StateBuilder(
            actions = actions,
            isBalanceHiddenProvider = isBalanceHiddenProvider,
            appCurrencyProvider = appCurrencyProvider,
            isAccountsModeProvider = isAccountsModeProvider,
            isGaslessFeeSupportedForNetwork = isGaslessFeeSupportedForNetwork,
            appRouter = appRouter,
            isChooseTokenPulseEnabled = false,
        )
    }

    @Test
    fun `GIVEN unavailable provider row WHEN quotes refresh THEN percent difference stays hidden`() {
        // Arrange
        val holder = holderWithProviderRow(badge = ProviderState.AdditionalBadge.Unavailable)

        // Act
        val updated = sut.updateProvidersBottomSheetContent(
            uiState = holder,
            pricesLowerBest = mapOf(PROVIDER_ID to 0.5f),
            tokenSwapInfoForProviders = mapOf(PROVIDER_ID to tokenInfo()),
        )

        // Assert
        assertThat(refreshedRow(updated).percentLowerThenBest).isEqualTo(PercentDifference.Empty)
    }

    @Test
    fun `GIVEN ordinary provider row WHEN quotes refresh THEN percent difference is applied`() {
        // Arrange
        val holder = holderWithProviderRow(badge = ProviderState.AdditionalBadge.Empty)

        // Act
        val updated = sut.updateProvidersBottomSheetContent(
            uiState = holder,
            pricesLowerBest = mapOf(PROVIDER_ID to 0.5f),
            tokenSwapInfoForProviders = mapOf(PROVIDER_ID to tokenInfo()),
        )

        // Assert
        assertThat(refreshedRow(updated).percentLowerThenBest).isEqualTo(PercentDifference.Value(0.5f))
    }

    private fun refreshedRow(holder: SwapStateHolder): ProviderState.Content {
        val config = holder.bottomSheetConfig?.content as ChooseProviderBottomSheetConfig
        return config.providers.single() as ProviderState.Content
    }

    private fun holderWithProviderRow(badge: ProviderState.AdditionalBadge): SwapStateHolder {
        val row = ProviderState.Content(
            id = PROVIDER_ID,
            name = "Provider",
            type = "CEX",
            iconUrl = "",
            subtitle = stringReference("1 BTC"),
            selectionType = ProviderState.SelectionType.SELECT,
            additionalBadge = badge,
            percentLowerThenBest = PercentDifference.Empty,
            namePrefix = ProviderState.PrefixType.NONE,
            onProviderClick = {},
        )
        val config = ChooseProviderBottomSheetConfig(
            selectedProviderId = PROVIDER_ID,
            providers = persistentListOf(row),
            allProviders = persistentListOf(row),
            notification = null,
            selectedFilter = ProviderFilterType.ALL,
            availableFilters = persistentListOf(),
            onFilterSelect = {},
        )
        return sut.createInitialLoadingState().copy(
            bottomSheetConfig = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = {},
                content = config,
            ),
        )
    }

    private fun tokenInfo(): TokenSwapInfo {
        val currency = mockk<CryptoCurrency.Coin> {
            every { symbol } returns "BTC"
            every { decimals } returns 8
            every { displayDecimals } returns 8
        }
        val swapStatus = mockk<SwapCurrencyStatus> {
            every { this@mockk.currency } returns currency
        }
        return TokenSwapInfo(
            tokenAmount = SwapAmount(value = BigDecimal.ONE, decimals = 8),
            amountFiat = BigDecimal.ZERO,
            swapCurrencyStatus = swapStatus,
        )
    }

    private companion object {
        const val PROVIDER_ID = "1inch"
    }
}