package com.tangem.feature.swap.domain

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.domain.tokens.model.FeePaidCurrency
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyCheck
import com.tangem.domain.transaction.models.AllowanceInfo
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.feature.swap.domain.models.domain.ExpressTransactionModel
import com.tangem.feature.swap.domain.models.domain.SwapBalanceStatus
import com.tangem.feature.swap.domain.models.domain.SwapDataModel
import com.tangem.feature.swap.domain.models.ui.PermissionDataState
import com.tangem.feature.swap.domain.models.ui.SwapState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Tests for `loadDexSwapDataNoFee` — the replacement for the legacy `loadDexSwapData`.
 *
 * Verifies:
 *  - `dexSwapFeeCalculator.calculate` is NEVER called during quote loading (fee is owned by
 *    the fee selector now).
 *  - The returned `preparedSwapConfigState.balanceStatus` is [SwapBalanceStatus.Pending].
 *  - `swapDataModel` is populated from the Express response so `applySwapFee` (and
 *    `FeeSelectorRepository.loadFeeExtended`) can consume it later.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SwapInteractorImplLoadDexSwapDataNoFeeTest : SwapInteractorImplTestBase() {

    private val ethNetwork = Blockchain.Ethereum.toNetworkId()

    @BeforeEach
    fun setup() {
        coEvery { currenciesRepository.getFeePaidCurrency(any(), any()) } returns FeePaidCurrency.Coin
        coEvery { walletManagersFacade.getNativeTokenBalance(any(), any(), any()) } returns BigDecimal("10")
        coEvery {
            getCurrencyCheckUseCase.invoke(
                userWalletId = any(),
                currencyStatus = any(),
                feeCurrencyStatus = any(),
                amount = any(),
                fee = any(),
                feeCurrencyBalanceAfterTransaction = any(),
                recipientAddress = any(),
            )
        } returns CryptoCurrencyCheck(
            dustValue = null,
            reserveAmount = null,
            minimumSendAmount = null,
            existentialDeposit = null,
            utxoAmountLimit = null,
            isAccountFunded = true,
            rentWarning = null,
            isMemoRequired = false,
        )
        coEvery {
            validateTransactionUseCase.invoke(
                amount = any(),
                fee = any(),
                memo = any(),
                destination = any(),
                userWalletId = any(),
                network = any(),
            )
        } returns Unit.right()
        coEvery { quotesRepository.getMultiQuoteSyncOrNull(any()) } returns emptySet()
        coEvery { multiQuoteStatusFetcher.invoke(any()) } returns Unit.right()
        coEvery { getFeePaidCryptoCurrencyStatusSyncUseCase.invoke(any(), any()) } returns null.right()
        coEvery { currenciesRepository.createCoinCurrency(any()) } returns buildCoinCurrency()
        coEvery {
            getAllowanceInfoUseCase.invoke(
                userWalletId = any(),
                cryptoCurrency = any(),
                spenderAddress = any(),
                requiredAmount = any(),
            )
        } returns (AllowanceInfo.Enough(allowance = BigDecimal("100")) as AllowanceInfo).right()
    }

    @Test
    fun `DEX findBestQuote returns QuotesLoadedState without invoking DexSwapFeeCalculator`() = runTest {
        val dexProvider = buildSwapProvider(ExchangeProviderType.DEX)
        val from = buildSwapCurrencyStatus(networkRawId = ethNetwork, isCoin = true, amount = BigDecimal("10"))
        val to = buildSwapCurrencyStatus(networkRawId = ethNetwork)
        val quoteModel = buildQuoteModel(allowanceContract = null)
        val swapDataModel = SwapDataModel(
            toTokenAmount = SwapAmount(BigDecimal("0.5"), 18),
            transaction = ExpressTransactionModel.DEX(
                fromAmount = SwapAmount(BigDecimal("1.0"), 18),
                toAmount = SwapAmount(BigDecimal("0.5"), 18),
                txValue = "1000000000000000000",
                txId = "tx-id",
                txTo = "0xToAddress",
                txExtraId = null,
                txFrom = "0xFromAddress",
                txData = "0xdata",
                otherNativeFeeWei = null,
                gas = BigInteger.valueOf(21_000L),
                allowanceContract = null,
            ),
        )

        coEvery {
            repository.findBestQuote(
                userWallet = any(),
                fromContractAddress = any(),
                fromNetwork = any(),
                toContractAddress = any(),
                toNetwork = any(),
                fromAmount = any(),
                fromDecimals = any(),
                toDecimals = any(),
                providerId = dexProvider.providerId,
                rateType = any(),
            )
        } returns quoteModel.right()

        coEvery {
            repository.getExchangeData(
                userWallet = any(),
                fromContractAddress = any(),
                fromNetwork = any(),
                toContractAddress = any(),
                fromAddress = any(),
                toNetwork = any(),
                fromAmount = any(),
                fromDecimals = any(),
                toDecimals = any(),
                providerId = dexProvider.providerId,
                rateType = any(),
                toAddress = any(),
                expressOperationType = any(),
                refundAddress = any(),
            )
        } returns swapDataModel.right()

        val result = sut.findBestQuote(
            fromSwapCurrencyStatus = from,
            toSwapCurrencyStatus = to,
            providers = listOf(dexProvider),
            amountToSwap = "1.0",
            reduceBalanceBy = BigDecimal.ZERO,
        )

        assertThat(result).hasSize(1)
        val state = result[dexProvider]
        assertThat(state).isInstanceOf(SwapState.QuotesLoadedState::class.java)
        val quotesState = state as SwapState.QuotesLoadedState
        // Fee not computed yet — balanceStatus is Pending until applySwapFee patches the state.
        assertThat(quotesState.preparedSwapConfigState.balanceStatus)
            .isInstanceOf(SwapBalanceStatus.Pending::class.java)
        // swapDataModel is propagated so the fee selector can later call loadSwapFee with it.
        assertThat(quotesState.swapDataModel).isEqualTo(swapDataModel)
        // Fee calculator must not be invoked during quote loading.
        coVerify(exactly = 0) { dexSwapFeeCalculator.calculate(any(), any(), any()) }
    }

    // region permission-state selection on AllowanceInfo.NotEnough

    @Test
    fun `GIVEN NotEnough allowance AND integrated active THEN permissionState is PermissionSettings`() = runTest {
        every { swapFeatureToggles.isSwapIntegratedApproveEnabled } returns true
        stubAllowance(AllowanceInfo.NotEnough(allowance = BigDecimal.ZERO, requiredAmount = BigDecimal.ONE))

        val state = runFindBestQuoteForToken()

        val permission = state.permissionState
        assertThat(permission).isInstanceOf(PermissionDataState.PermissionSettings::class.java)
        assertThat((permission as PermissionDataState.PermissionSettings).spenderAddress).isEqualTo(SPENDER)
    }

    @Test
    fun `GIVEN Enough allowance THEN permissionState is Empty`() = runTest {
        every { swapFeatureToggles.isSwapIntegratedApproveEnabled } returns true
        stubAllowance(AllowanceInfo.Enough(allowance = BigDecimal("100")))

        val state = runFindBestQuoteForToken()

        assertThat(state.permissionState).isEqualTo(PermissionDataState.Empty)
    }

    @Test
    fun `GIVEN NotEnough allowance AND integrated toggle OFF THEN does not reach loadDexSwapDataNoFee`() = runTest {
        // With the integrated toggle off, NotEnough is not allowance-satisfied (requires Enough),
        // so manageDex does NOT enter loadDexSwapDataNoFee — getExchangeData is never called.
        every { swapFeatureToggles.isSwapIntegratedApproveEnabled } returns false
        stubAllowance(AllowanceInfo.NotEnough(allowance = BigDecimal.ZERO, requiredAmount = BigDecimal.ONE))

        runFindBestQuoteForTokenRaw()

        coVerify(exactly = 0) {
            repository.getExchangeData(
                userWallet = any(), fromContractAddress = any(), fromNetwork = any(),
                toContractAddress = any(), fromAddress = any(), toNetwork = any(),
                fromAmount = any(), fromDecimals = any(), toDecimals = any(),
                providerId = any(), rateType = any(), toAddress = any(),
                expressOperationType = any(), refundAddress = any(),
            )
        }
    }

    // endregion

    private fun stubAllowance(info: AllowanceInfo) {
        coEvery {
            getAllowanceInfoUseCase.invoke(
                userWalletId = any(),
                cryptoCurrency = any(),
                spenderAddress = any(),
                requiredAmount = any(),
            )
        } returns info.right()
    }

    /** Runs findBestQuote for a token from-currency whose quote carries [SPENDER] as allowanceContract. */
    private suspend fun runFindBestQuoteForToken(): SwapState.QuotesLoadedState {
        val dexProvider = stubDexQuoteAndExchangeData()
        val result = invokeFindBestQuote(dexProvider)
        return result[dexProvider] as SwapState.QuotesLoadedState
    }

    private suspend fun runFindBestQuoteForTokenRaw() {
        val dexProvider = stubDexQuoteAndExchangeData()
        invokeFindBestQuote(dexProvider)
    }

    private fun stubDexQuoteAndExchangeData(): com.tangem.feature.swap.domain.models.domain.SwapProvider {
        val dexProvider = buildSwapProvider(ExchangeProviderType.DEX)
        val quoteModel = buildQuoteModel(allowanceContract = SPENDER)
        val swapDataModel = SwapDataModel(
            toTokenAmount = SwapAmount(BigDecimal("0.5"), 18),
            transaction = ExpressTransactionModel.DEX(
                fromAmount = SwapAmount(BigDecimal("1.0"), 18),
                toAmount = SwapAmount(BigDecimal("0.5"), 18),
                txValue = "1000000000000000000",
                txId = "tx-id",
                txTo = "0xToAddress",
                txExtraId = null,
                txFrom = "0xFromAddress",
                txData = "0xdata",
                otherNativeFeeWei = null,
                gas = BigInteger.valueOf(21_000L),
                allowanceContract = SPENDER,
            ),
        )
        coEvery {
            repository.findBestQuote(
                userWallet = any(), fromContractAddress = any(), fromNetwork = any(),
                toContractAddress = any(), toNetwork = any(), fromAmount = any(),
                fromDecimals = any(), toDecimals = any(),
                providerId = dexProvider.providerId, rateType = any(),
            )
        } returns quoteModel.right()
        coEvery {
            repository.getExchangeData(
                userWallet = any(), fromContractAddress = any(), fromNetwork = any(),
                toContractAddress = any(), fromAddress = any(), toNetwork = any(),
                fromAmount = any(), fromDecimals = any(), toDecimals = any(),
                providerId = dexProvider.providerId, rateType = any(), toAddress = any(),
                expressOperationType = any(), refundAddress = any(),
            )
        } returns swapDataModel.right()
        return dexProvider
    }

    private suspend fun invokeFindBestQuote(
        dexProvider: com.tangem.feature.swap.domain.models.domain.SwapProvider,
    ) = sut.findBestQuote(
        fromSwapCurrencyStatus = buildSwapCurrencyStatus(
            networkRawId = ethNetwork,
            isCoin = false,
            contractAddress = "0xToken",
            amount = BigDecimal("10"),
        ),
        toSwapCurrencyStatus = buildSwapCurrencyStatus(networkRawId = ethNetwork),
        providers = listOf(dexProvider),
        amountToSwap = "1.0",
        reduceBalanceBy = BigDecimal.ZERO,
    )

    private companion object {
        const val SPENDER = "0xSpender"
    }
}