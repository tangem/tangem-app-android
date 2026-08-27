package com.tangem.feature.swap.domain

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
import com.tangem.domain.transaction.models.AllowanceInfo
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.feature.swap.domain.models.domain.ExpressTransactionModel
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
 * Approval in-progress lifecycle in [SwapInteractorImpl.findBestQuote].
 *
 * After an approve transaction is sent, the token stays "in progress" (permission state
 * `PermissionLoading`) until the on-chain allowance reflects it. The in-progress marker must clear
 * once the allowance reaches the amount the approval was given for — even when the entered swap
 * amount has grown past it since (`NotEnough` for the entered amount). Otherwise increasing the
 * amount right after approving locks the screen in the approval-in-progress error with no way to
 * request a new approval.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SwapInteractorImplApprovalInProgressTest : SwapInteractorImplTestBase() {

    private val tronNetwork = Blockchain.Tron.toNetworkId()
    private val btcNetwork = Blockchain.Bitcoin.toNetworkId()

    private val spender = "TDexRouterSpender"
    private val tokenContract = "TRegularTokenContract"
    private val approvedAmount = BigDecimal("5")

    private var inProgress = true

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
        coEvery { getFeePaidCryptoCurrencyStatusSyncUseCase.invoke(any(), any()) } returns null.right()
        coEvery { currenciesRepository.createCoinCurrency(any()) } returns buildCoinCurrency()
        coEvery { walletManagersFacade.getNativeTokenBalance(any(), any(), any()) } returns BigDecimal("10")

        // stateful in-progress marker: starts set, cleared by removeAddressFromProgress
        inProgress = true
        every { allowPermissionsHandler.isAddressAllowanceInProgress(any()) } answers { inProgress }
        every { allowPermissionsHandler.removeAddressFromProgress(any()) } answers { inProgress = false }
        every { allowPermissionsHandler.getApprovedAmount(any()) } returns approvedAmount

        // Separate-approval fee coverage: a small approve fee against the native balance of 10
        // keeps the coverage check passing so the permission state stays the subject under test.
        coEvery {
            createApprovalTransactionUseCase.invoke(
                cryptoCurrencyStatus = any(),
                userWalletId = any(),
                amount = any(),
                contractAddress = any(),
                spenderAddress = any(),
            )
        } returns mockk<TransactionData.Uncompiled>(relaxed = true).right()
        val approveFeeAmount = mockk<Amount>(relaxed = true) {
            every { value } returns BigDecimal("0.001")
        }
        val approveFee = mockk<Fee.Common>(relaxed = true) {
            every { this@mockk.amount } returns approveFeeAmount
        }
        coEvery {
            getFeeUseCase.invoke(transactionData = any(), userWallet = any(), network = any())
        } returns (TransactionFee.Single(normal = approveFee) as TransactionFee).right()
    }

    @Test
    fun `GIVEN allowance reached approved amount and amount increased WHEN findBestQuote THEN approve re-requested`() =
        runTest {
            // Arrange — approve for 5 landed (allowance = 5), the user entered 8
            stubAllowance(AllowanceInfo.NotEnough(allowance = approvedAmount, requiredAmount = BigDecimal("8")))
            val provider = stubTronDexQuote()

            // Act
            val result = invokeTronTokenQuote(provider, amountToSwap = "8")

            // Assert — the marker is cleared and a new approval is offered for the larger amount
            coVerify(exactly = 1) { allowPermissionsHandler.removeAddressFromProgress(tokenContract) }
            val loaded = result[provider] as SwapState.QuotesLoadedState
            assertThat(loaded.permissionState).isInstanceOf(PermissionDataState.PermissionRequired::class.java)
        }

    @Test
    fun `GIVEN approve not yet on chain WHEN findBestQuote with increased amount THEN stays in progress`() = runTest {
        // Arrange — allowance still below the approved 5: the approve transaction has not landed
        stubAllowance(AllowanceInfo.NotEnough(allowance = BigDecimal.ZERO, requiredAmount = BigDecimal("8")))
        val provider = stubTronDexQuote()

        // Act
        val result = invokeTronTokenQuote(provider, amountToSwap = "8")

        // Assert
        coVerify(exactly = 0) { allowPermissionsHandler.removeAddressFromProgress(any()) }
        val loaded = result[provider] as SwapState.QuotesLoadedState
        assertThat(loaded.permissionState).isEqualTo(PermissionDataState.PermissionLoading)
    }

    @Test
    fun `GIVEN allowance enough for entered amount WHEN findBestQuote THEN marker cleared as before`() = runTest {
        // Arrange — the pre-existing happy path: allowance covers the entered amount
        stubAllowance(AllowanceInfo.Enough(allowance = approvedAmount))
        val provider = stubTronDexQuote()

        // Act
        invokeTronTokenQuote(provider, amountToSwap = "5")

        // Assert
        coVerify(exactly = 1) { allowPermissionsHandler.removeAddressFromProgress(tokenContract) }
    }

    @Test
    fun `GIVEN approved amount unknown WHEN findBestQuote with increased amount THEN stays in progress`() = runTest {
        // Arrange — no recorded approve amount: NotEnough cannot be distinguished from a pending tx
        every { allowPermissionsHandler.getApprovedAmount(any()) } returns null
        stubAllowance(AllowanceInfo.NotEnough(allowance = approvedAmount, requiredAmount = BigDecimal("8")))
        val provider = stubTronDexQuote()

        // Act
        val result = invokeTronTokenQuote(provider, amountToSwap = "8")

        // Assert
        coVerify(exactly = 0) { allowPermissionsHandler.removeAddressFromProgress(any()) }
        val loaded = result[provider] as SwapState.QuotesLoadedState
        assertThat(loaded.permissionState).isEqualTo(PermissionDataState.PermissionLoading)
    }

    @Test
    fun `GIVEN reset needed allowance WHEN findBestQuote THEN stays in progress`() = runTest {
        // Arrange — reset-approval flows keep the conservative in-progress state
        stubAllowance(AllowanceInfo.ResetNeeded(allowance = approvedAmount, requiredAmount = BigDecimal("8")))
        val provider = stubTronDexQuote()

        // Act
        invokeTronTokenQuote(provider, amountToSwap = "8")

        // Assert
        coVerify(exactly = 0) { allowPermissionsHandler.removeAddressFromProgress(any()) }
    }

    // region — stubs & helpers

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

    private fun stubTronDexQuote(): SwapProvider {
        val provider = buildSwapProvider(ExchangeProviderType.DEX)
        coEvery {
            repository.findBestQuote(
                userWallet = any(), fromContractAddress = any(), fromNetwork = tronNetwork,
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

    private suspend fun invokeTronTokenQuote(provider: SwapProvider, amountToSwap: String) = sut.findBestQuote(
        fromSwapCurrencyStatus = buildSwapCurrencyStatus(
            networkRawId = tronNetwork,
            contractAddress = tokenContract,
            isCoin = false,
            amount = BigDecimal("100"),
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