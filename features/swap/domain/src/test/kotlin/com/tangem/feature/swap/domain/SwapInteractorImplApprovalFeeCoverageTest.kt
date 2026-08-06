package com.tangem.feature.swap.domain

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.tokens.model.FeePaidCurrency
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyCheck
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.domain.transaction.models.AllowanceInfo
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.feature.swap.domain.models.domain.ExpressTransactionModel
import com.tangem.feature.swap.domain.models.domain.SwapBalanceStatus
import com.tangem.feature.swap.domain.models.domain.SwapDataModel
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.feature.swap.domain.models.ui.PermissionDataState
import com.tangem.feature.swap.domain.models.ui.SwapState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Separate-approval fee coverage in [SwapInteractorImpl.findBestQuote].
 *
 * When the quote resolves to [PermissionDataState.PermissionRequired] (the separate-approval sheet,
 * TRON being the only network without integrated approve), the approve transaction is paid in the
 * network's fee coin immediately — so the coin balance must cover the estimated approve fee.
 * When it cannot, `preparedSwapConfigState.balanceStatus` becomes [SwapBalanceStatus.InsufficientFee],
 * which surfaces the insufficient-fee error and suppresses the approve prompt on the UI side.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SwapInteractorImplApprovalFeeCoverageTest : SwapInteractorImplTestBase() {

    private val tronNetwork = Blockchain.Tron.toNetworkId()
    private val ethNetwork = Blockchain.Ethereum.toNetworkId()
    private val btcNetwork = Blockchain.Bitcoin.toNetworkId()

    private val spender = "TDexRouterSpender"
    private val tokenContract = "TRegularTokenContract"

    @BeforeEach
    fun setup() {
        coEvery { currenciesRepository.getFeePaidCurrency(any(), any()) } returns FeePaidCurrency.Coin
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
        } returns buildCurrencyCheck()
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
        coEvery { quotesRepository.getMultiQuoteSyncOrNull(any()) } answers {
            firstArg<Set<CryptoCurrency.RawID>>().map { rawId ->
                QuoteStatus(
                    rawCurrencyId = rawId,
                    value = QuoteStatus.Data(
                        source = StatusSource.ACTUAL,
                        fiatRate = BigDecimal.ONE,
                        fiatRateUSD = BigDecimal.ONE,
                        priceChange = BigDecimal.ZERO,
                    ),
                )
            }.toSet()
        }
        coEvery { multiQuoteStatusFetcher.invoke(any()) } returns Unit.right()
        every { allowPermissionsHandler.isAddressAllowanceInProgress(any()) } returns false
        val notEnough: AllowanceInfo = AllowanceInfo.NotEnough(
            allowance = BigDecimal.ZERO,
            requiredAmount = BigDecimal.ONE,
        )
        coEvery {
            getAllowanceInfoUseCase.invoke(
                userWalletId = any(),
                cryptoCurrency = any(),
                spenderAddress = any(),
                requiredAmount = any(),
            )
        } returns notEnough.right()
    }

    @Test
    fun `GIVEN approve fee exceeds fee coin balance WHEN findBestQuote THEN balance status is InsufficientFee`() =
        runTest {
            // Arrange
            stubNativeFeeCoinBalance(balance = BigDecimal("5"))
            stubApproveFeeEstimation(feeValue = BigDecimal("13"))
            val provider = stubTronDexQuote()

            // Act
            val result = invokeTronTokenQuote(provider)

            // Assert
            val loaded = result[provider] as SwapState.QuotesLoadedState
            assertThat(loaded.preparedSwapConfigState.balanceStatus)
                .isInstanceOf(SwapBalanceStatus.InsufficientFee::class.java)
            assertThat(loaded.permissionState).isInstanceOf(PermissionDataState.PermissionRequired::class.java)
        }

    @Test
    fun `GIVEN fee coin covers approve fee WHEN findBestQuote THEN balance status stays Pending`() = runTest {
        // Arrange
        stubNativeFeeCoinBalance(balance = BigDecimal("50"))
        stubApproveFeeEstimation(feeValue = BigDecimal("13"))
        val provider = stubTronDexQuote()

        // Act
        val result = invokeTronTokenQuote(provider)

        // Assert
        val loaded = result[provider] as SwapState.QuotesLoadedState
        assertThat(loaded.preparedSwapConfigState.balanceStatus).isEqualTo(SwapBalanceStatus.Pending)
        assertThat(loaded.permissionState).isInstanceOf(PermissionDataState.PermissionRequired::class.java)
    }

    @Test
    fun `GIVEN fee estimation fails and fee coin balance is zero WHEN findBestQuote THEN InsufficientFee`() = runTest {
        // Arrange — a TRON address that never held TRX: estimation fails, balance is zero
        stubNativeFeeCoinBalance(balance = BigDecimal.ZERO)
        stubApprovalTransactionCreation()
        coEvery {
            getFeeUseCase.invoke(transactionData = any(), userWallet = any(), network = any())
        } returns GetFeeError.UnknownError.left()
        val provider = stubTronDexQuote()

        // Act
        val result = invokeTronTokenQuote(provider)

        // Assert
        val loaded = result[provider] as SwapState.QuotesLoadedState
        assertThat(loaded.preparedSwapConfigState.balanceStatus)
            .isInstanceOf(SwapBalanceStatus.InsufficientFee::class.java)
    }

    @Test
    fun `GIVEN fee estimation fails and fee coin balance is positive WHEN findBestQuote THEN stays Pending`() =
        runTest {
            // Arrange — estimation failure alone is inconclusive and must not block the approval
            stubNativeFeeCoinBalance(balance = BigDecimal("30"))
            stubApprovalTransactionCreation()
            coEvery {
                getFeeUseCase.invoke(transactionData = any(), userWallet = any(), network = any())
            } returns GetFeeError.UnknownError.left()
            val provider = stubTronDexQuote()

            // Act
            val result = invokeTronTokenQuote(provider)

            // Assert
            val loaded = result[provider] as SwapState.QuotesLoadedState
            assertThat(loaded.preparedSwapConfigState.balanceStatus).isEqualTo(SwapBalanceStatus.Pending)
        }

    @Test
    fun `GIVEN integrated approve path on EVM WHEN findBestQuote THEN approve fee is not estimated`() = runTest {
        // Arrange — EVM + NotEnough goes to PermissionSettings; coverage is owned by applySwapFee there
        stubNativeFeeCoinBalance(balance = BigDecimal.ZERO)
        val provider = stubDexQuoteAndExchangeData(networkRawId = ethNetwork)

        // Act
        val result = invokeTokenQuote(provider, networkRawId = ethNetwork)

        // Assert
        val loaded = result[provider] as SwapState.QuotesLoadedState
        assertThat(loaded.permissionState).isInstanceOf(PermissionDataState.PermissionSettings::class.java)
        coVerify(exactly = 0) {
            getFeeUseCase.invoke(transactionData = any(), userWallet = any(), network = any())
        }
    }

    @Test
    fun `GIVEN amount exceeds token balance on TRON WHEN findBestQuote THEN InsufficientAmount without estimation`() =
        runTest {
            // Arrange — the amount error takes priority; no approve-fee request is spent on it
            stubNativeFeeCoinBalance(balance = BigDecimal("50"))
            val provider = stubTronDexQuote()

            // Act
            val result = invokeTokenQuote(provider, networkRawId = tronNetwork, amountToSwap = "100")

            // Assert
            val loaded = result[provider] as SwapState.QuotesLoadedState
            assertThat(loaded.preparedSwapConfigState.balanceStatus)
                .isInstanceOf(SwapBalanceStatus.InsufficientAmount::class.java)
            coVerify(exactly = 0) {
                getFeeUseCase.invoke(transactionData = any(), userWallet = any(), network = any())
            }
        }

    // region — stubs & helpers

    private fun stubNativeFeeCoinBalance(balance: BigDecimal) {
        val coinStatus = buildSwapCurrencyStatus(
            networkRawId = tronNetwork,
            isCoin = true,
            amount = balance,
        ).status
        coEvery {
            getFeePaidCryptoCurrencyStatusSyncUseCase.invoke(userWalletId = any(), cryptoCurrencyStatus = any())
        } returns coinStatus.right()
    }

    private fun stubApprovalTransactionCreation() {
        coEvery {
            createApprovalTransactionUseCase.invoke(
                cryptoCurrencyStatus = any(),
                userWalletId = any(),
                amount = any(),
                contractAddress = any(),
                spenderAddress = any(),
            )
        } returns mockk<TransactionData.Uncompiled>(relaxed = true).right()
    }

    private fun stubApproveFeeEstimation(feeValue: BigDecimal) {
        stubApprovalTransactionCreation()
        val amount = mockk<Amount>(relaxed = true) {
            every { value } returns feeValue
        }
        val fee = mockk<Fee.Common>(relaxed = true) {
            every { this@mockk.amount } returns amount
        }
        coEvery {
            getFeeUseCase.invoke(transactionData = any(), userWallet = any(), network = any())
        } returns (TransactionFee.Single(normal = fee) as TransactionFee).right()
    }

    private fun stubTronDexQuote(): SwapProvider = stubDexQuoteAndExchangeData(networkRawId = tronNetwork)

    private fun stubDexQuoteAndExchangeData(networkRawId: String): SwapProvider {
        val provider = buildSwapProvider(ExchangeProviderType.DEX)
        coEvery {
            repository.findBestQuote(
                userWallet = any(), fromContractAddress = any(), fromNetwork = networkRawId,
                toContractAddress = any(), toNetwork = any(), fromAmount = any(),
                fromDecimals = any(), toDecimals = any(),
                providerId = provider.providerId, rateType = any(),
            )
        } returns buildQuoteModel(allowanceContract = spender).right()
        coEvery {
            repository.getExchangeData(
                userWallet = any(), fromContractAddress = any(), fromNetwork = any(),
                toContractAddress = any(), fromAddress = any(), toNetwork = any(),
                fromAmount = any(), fromDecimals = any(), toDecimals = any(),
                providerId = provider.providerId, rateType = any(), toAddress = any(),
                expressOperationType = any(), refundAddress = any(),
            )
        } returns buildDexSwapData().right()
        return provider
    }

    private fun buildDexSwapData(): SwapDataModel = SwapDataModel(
        toTokenAmount = SwapAmount(BigDecimal("0.5"), 18),
        transaction = ExpressTransactionModel.DEX(
            fromAmount = SwapAmount(BigDecimal.ONE, 18),
            toAmount = SwapAmount(BigDecimal("0.5"), 18),
            txValue = "0",
            txId = "tx-id-123",
            txTo = "0xRecipient",
            txExtraId = null,
            txFrom = "0xSender",
            txData = "dGVzdA==",
            otherNativeFeeWei = null,
            gas = BigInteger.valueOf(21_000L),
            allowanceContract = spender,
        ),
    )

    private suspend fun invokeTronTokenQuote(provider: SwapProvider) =
        invokeTokenQuote(provider, networkRawId = tronNetwork)

    private suspend fun invokeTokenQuote(
        provider: SwapProvider,
        networkRawId: String,
        amountToSwap: String = "1.0",
    ) = sut.findBestQuote(
        fromSwapCurrencyStatus = buildSwapCurrencyStatus(
            networkRawId = networkRawId,
            contractAddress = tokenContract,
            isCoin = false,
            amount = BigDecimal("10"),
        ),
        toSwapCurrencyStatus = buildSwapCurrencyStatus(networkRawId = btcNetwork),
        providers = listOf(provider),
        amountToSwap = amountToSwap,
        reduceBalanceBy = BigDecimal.ZERO,
    )

    private fun buildCurrencyCheck(): CryptoCurrencyCheck = CryptoCurrencyCheck(
        dustValue = null,
        reserveAmount = null,
        minimumSendAmount = null,
        existentialDeposit = null,
        utxoAmountLimit = null,
        isAccountFunded = true,
        rentWarning = null,
        isMemoRequired = false,
    )

    // endregion
}