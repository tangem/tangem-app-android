package com.tangem.feature.swap.domain.transfer

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.account.status.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.ui.SwapState
import com.tangem.feature.swap.domain.models.ui.TokenSwapInfo
import com.tangem.features.swap.SwapFeatureToggles
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SwapTransferInteractorImplTest {

    private val swapFeatureToggles: SwapFeatureToggles = mockk()
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase = mockk()
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase = mockk()
    private val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase = mockk()

    private val sut = SwapTransferInteractorImpl(
        swapFeatureToggles = swapFeatureToggles,
        getSelectedAppCurrencyUseCase = getSelectedAppCurrencyUseCase,
        getBalanceHidingSettingsUseCase = getBalanceHidingSettingsUseCase,
        isAccountsModeEnabledUseCase = isAccountsModeEnabledUseCase,
    )

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    // region updateTransfer

    @Test
    fun `GIVEN unparsable amount WHEN updateTransfer THEN return EmptyAmountState in transfer mode`() = runTest {
        val appCurrency = AppCurrency(code = "EUR", name = "Euro", symbol = "€")
        val fromCurrencyStatus = buildCurrencyStatus(
            rawCurrencyId = FROM_RAW_CURRENCY_ID,
            decimals = FROM_DECIMALS,
        )
        val toCurrencyStatus = buildCurrencyStatus(
            rawCurrencyId = TO_RAW_CURRENCY_ID,
            decimals = TO_DECIMALS,
        )
        every { getSelectedAppCurrencyUseCase() } returns flowOf(appCurrency.right())
        every { getBalanceHidingSettingsUseCase.isBalanceHidden() } returns flowOf(false)
        coEvery { isAccountsModeEnabledUseCase.invokeSync() } returns false

        val result = sut.updateTransfer(
            fromSwapCurrencyStatus = fromCurrencyStatus,
            toSwapCurrencyStatus = toCurrencyStatus,
            fromTokenAmount = "abc",
        )

        assertThat(result).isInstanceOf(SwapState.EmptyAmountState::class.java)
        assertThat((result as SwapState.EmptyAmountState).isTransferMode).isTrue()
        verify { getSelectedAppCurrencyUseCase() }
        verify { getBalanceHidingSettingsUseCase.isBalanceHidden() }
        coVerify { isAccountsModeEnabledUseCase.invokeSync() }
    }

    @Test
    fun `GIVEN valid amount WHEN updateTransfer THEN return Transfer state with mirrored from-and-to swap info`() =
        runTest {
            val appCurrency = AppCurrency(code = "USD", name = "US Dollar", symbol = "$")
            val userWallet: UserWallet = mockk()
            val fromCurrencyStatus = buildCurrencyStatus(
                rawCurrencyId = FROM_RAW_CURRENCY_ID,
                decimals = FROM_DECIMALS,
                fiatRate = BigDecimal.TEN,
            )
            val toCurrencyStatus = buildCurrencyStatus(
                rawCurrencyId = TO_RAW_CURRENCY_ID,
                decimals = TO_DECIMALS,
                userWallet = userWallet,
            )
            every { getSelectedAppCurrencyUseCase() } returns flowOf(appCurrency.right())
            every { getBalanceHidingSettingsUseCase.isBalanceHidden() } returns flowOf(true)
            coEvery { isAccountsModeEnabledUseCase.invokeSync() } returns true

            val result = sut.updateTransfer(
                fromSwapCurrencyStatus = fromCurrencyStatus,
                toSwapCurrencyStatus = toCurrencyStatus,
                fromTokenAmount = "1,5",
            )

            val expectedAmount = BigDecimal("1.5")
            val expectedFiat = BigDecimal("15.0")
            val expected = SwapState.Transfer(
                userWallet = userWallet,
                fromTokenInfo = TokenSwapInfo(
                    tokenAmount = SwapAmount(expectedAmount, FROM_DECIMALS),
                    swapCurrencyStatus = fromCurrencyStatus,
                    amountFiat = expectedFiat,
                ),
                toTokenInfo = TokenSwapInfo(
                    tokenAmount = SwapAmount(expectedAmount, TO_DECIMALS),
                    swapCurrencyStatus = toCurrencyStatus,
                    amountFiat = expectedFiat,
                ),
                appCurrency = appCurrency,
                isBalanceHidden = true,
                isAccountsMode = true,
            )
            assertThat(result).isEqualTo(expected)
            coVerify { isAccountsModeEnabledUseCase.invokeSync() }
            verify { getBalanceHidingSettingsUseCase.isBalanceHidden() }
        }

    // endregion

    // region shouldTransferInsteadOfSwap

    @Test
    fun `GIVEN feature toggle disabled WHEN shouldTransferInsteadOfSwap THEN return false`() {
        every { swapFeatureToggles.isSwapSwitchToTransferEnabled } returns false

        val result = sut.shouldTransferInsteadOfSwap(
            fromSwapCurrency = buildCoin(networkRawId = ETHEREUM),
            toSwapCurrency = buildCoin(networkRawId = ETHEREUM),
        )

        assertThat(result).isFalse()
        verify { swapFeatureToggles.isSwapSwitchToTransferEnabled }
    }

    @Test
    fun `GIVEN both coins on the same network WHEN shouldTransferInsteadOfSwap THEN return true`() {
        every { swapFeatureToggles.isSwapSwitchToTransferEnabled } returns true

        val result = sut.shouldTransferInsteadOfSwap(
            fromSwapCurrency = buildCoin(networkRawId = ETHEREUM),
            toSwapCurrency = buildCoin(networkRawId = ETHEREUM),
        )

        assertThat(result).isTrue()
    }

    @Test
    fun `GIVEN coins on different networks WHEN shouldTransferInsteadOfSwap THEN return false`() {
        every { swapFeatureToggles.isSwapSwitchToTransferEnabled } returns true

        val result = sut.shouldTransferInsteadOfSwap(
            fromSwapCurrency = buildCoin(networkRawId = ETHEREUM),
            toSwapCurrency = buildCoin(networkRawId = POLYGON),
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `GIVEN tokens with same network and same contract WHEN shouldTransferInsteadOfSwap THEN return true`() {
        every { swapFeatureToggles.isSwapSwitchToTransferEnabled } returns true

        val result = sut.shouldTransferInsteadOfSwap(
            fromSwapCurrency = buildToken(networkRawId = ETHEREUM, contractAddress = USDT_CONTRACT),
            toSwapCurrency = buildToken(networkRawId = ETHEREUM, contractAddress = USDT_CONTRACT),
        )

        assertThat(result).isTrue()
    }

    @Test
    fun `GIVEN tokens with same network but different contract WHEN shouldTransferInsteadOfSwap THEN return false`() {
        every { swapFeatureToggles.isSwapSwitchToTransferEnabled } returns true

        val result = sut.shouldTransferInsteadOfSwap(
            fromSwapCurrency = buildToken(networkRawId = ETHEREUM, contractAddress = USDT_CONTRACT),
            toSwapCurrency = buildToken(networkRawId = ETHEREUM, contractAddress = USDC_CONTRACT),
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `GIVEN tokens with same contract but different network WHEN shouldTransferInsteadOfSwap THEN return false`() {
        every { swapFeatureToggles.isSwapSwitchToTransferEnabled } returns true

        val result = sut.shouldTransferInsteadOfSwap(
            fromSwapCurrency = buildToken(networkRawId = ETHEREUM, contractAddress = USDT_CONTRACT),
            toSwapCurrency = buildToken(networkRawId = POLYGON, contractAddress = USDT_CONTRACT),
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `GIVEN coin from and token to WHEN shouldTransferInsteadOfSwap THEN return false`() {
        every { swapFeatureToggles.isSwapSwitchToTransferEnabled } returns true

        val result = sut.shouldTransferInsteadOfSwap(
            fromSwapCurrency = buildCoin(networkRawId = ETHEREUM),
            toSwapCurrency = buildToken(networkRawId = ETHEREUM, contractAddress = USDT_CONTRACT),
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `GIVEN token from and coin to WHEN shouldTransferInsteadOfSwap THEN return false`() {
        every { swapFeatureToggles.isSwapSwitchToTransferEnabled } returns true

        val result = sut.shouldTransferInsteadOfSwap(
            fromSwapCurrency = buildToken(networkRawId = ETHEREUM, contractAddress = USDT_CONTRACT),
            toSwapCurrency = buildCoin(networkRawId = ETHEREUM),
        )

        assertThat(result).isFalse()
    }

    // endregion

    // region helpers

    private fun buildCoin(networkRawId: String): CryptoCurrency.Coin {
        val network: Network = mockk { every { rawId } returns networkRawId }
        return mockk {
            every { this@mockk.network } returns network
        }
    }

    private fun buildToken(networkRawId: String, contractAddress: String): CryptoCurrency.Token {
        val network: Network = mockk { every { rawId } returns networkRawId }
        return mockk {
            every { this@mockk.network } returns network
            every { this@mockk.contractAddress } returns contractAddress
        }
    }

    private fun buildCurrencyStatus(
        rawCurrencyId: CryptoCurrency.RawID?,
        decimals: Int,
        fiatRate: BigDecimal = BigDecimal.ZERO,
        userWallet: UserWallet = mockk(),
    ): SwapCurrencyStatus {
        val currencyId: CryptoCurrency.ID = mockk {
            every { this@mockk.rawCurrencyId } returns rawCurrencyId
        }
        val currency: CryptoCurrency.Coin = mockk {
            every { this@mockk.id } returns currencyId
            every { this@mockk.decimals } returns decimals
        }
        val currencyValue: CryptoCurrencyStatus.Value = mockk {
            every { this@mockk.fiatRate } returns fiatRate
        }
        val status: CryptoCurrencyStatus = mockk {
            every { this@mockk.value } returns currencyValue
        }
        return mockk {
            every { this@mockk.currency } returns currency
            every { this@mockk.userWallet } returns userWallet
            every { this@mockk.status } returns status
        }
    }

    // endregion

    private companion object {
        const val ETHEREUM = "ethereum"
        const val POLYGON = "polygon"
        const val USDT_CONTRACT = "0xdAC17F958D2ee523a2206206994597C13D831ec7"
        const val USDC_CONTRACT = "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48"
        const val FROM_DECIMALS = 18
        const val TO_DECIMALS = 6
        val USD_QUOTE: BigDecimal = BigDecimal("2000")
        val FROM_RAW_CURRENCY_ID = CryptoCurrency.RawID(value = "eth")
        val TO_RAW_CURRENCY_ID = CryptoCurrency.RawID(value = "matic")
    }
}