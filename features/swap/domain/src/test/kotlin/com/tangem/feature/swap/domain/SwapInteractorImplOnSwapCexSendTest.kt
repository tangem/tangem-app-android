package com.tangem.feature.swap.domain

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.domain.express.models.ExpressOperationType
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.domain.transaction.models.TransactionFeeExtended
import com.tangem.domain.transaction.models.tron.TronGaslessQuote
import com.tangem.feature.swap.domain.fee.TransactionFeeResult
import com.tangem.feature.swap.domain.models.SwapAmount
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.feature.swap.domain.models.domain.ExpressTransactionModel
import com.tangem.feature.swap.domain.models.domain.SwapBalanceStatus
import com.tangem.feature.swap.domain.models.domain.SwapDataModel
import com.tangem.feature.swap.domain.models.ui.FeeBucket
import com.tangem.feature.swap.domain.models.ui.SwapFee
import com.tangem.feature.swap.domain.models.ui.SwapTransactionState
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SwapInteractorImplOnSwapCexSendTest : SwapInteractorImplTestBase() {

    private val tronNetwork = Blockchain.Tron.toNetworkId()
    private val txData = mockk<TransactionData.Uncompiled>(relaxed = true) {
        every { extras } returns null
    }

    @BeforeEach
    fun setup() {
        clearMocks(
            sendTransactionUseCase,
            createAndSendGaslessTransactionUseCase,
            createAndSendTronGaslessTransactionUseCase,
        )
        every { isDemoCardUseCase(any()) } returns false
        coEvery {
            repository.getExchangeData(
                userWallet = any(), fromContractAddress = any(), fromNetwork = any(),
                toContractAddress = any(), fromAddress = any(), toNetwork = any(),
                fromAmount = any(), fromDecimals = any(), toDecimals = any(),
                providerId = any(), rateType = any(), toAddress = any(),
                expressOperationType = any(), refundAddress = any(),
            )
        } returns cexSwapData().right()
        coEvery {
            createTransferTransactionUseCase(
                amount = any(), fee = any(), memo = any(),
                destination = any(), userWalletId = any(), network = any(),
            )
        } returns txData.right()
        coEvery {
            repository.exchangeSent(
                userWallet = any(), txId = any(), fromNetwork = any(), fromAddress = any(),
                payInAddress = any(), txHash = any(), payInExtraId = any(),
            )
        } returns Unit.right()
        coEvery {
            createAndSendTronGaslessTransactionUseCase(
                userWallet = any(),
                network = any(),
                transactionData = any(),
                fee = any(),
            )
        } returns TRON_TX_HASH.right()
        coEvery { createAndSendGaslessTransactionUseCase(any(), any(), any()) } returns EVM_TX_HASH.right()
        coEvery { sendTransactionUseCase(txData = any(), userWallet = any(), network = any()) } returns
            NATIVE_TX_HASH.right()
    }

    @Test
    fun `GIVEN fee with Tron gasless quote WHEN onSwap CEX THEN sends via Tron gasless`() = runTest {
        // Arrange
        val fromStatus = hotTronTokenStatus()
        val feeExtended = TransactionFeeExtended(
            transactionFee = TransactionFee.Single(normal = mockk<Fee.Common>(relaxed = true)),
            feeTokenId = fromStatus.currency.id,
            tronGaslessQuote = mockk<TronGaslessQuote>(relaxed = true),
        )

        // Act
        val result = onSwapCex(fromStatus, swapFee(feeExtended, feeToken = fromStatus.status))

        // Assert
        assertThat(result).isInstanceOf(SwapTransactionState.TxSent::class.java)
        val network = fromStatus.currency.network
        coVerify(exactly = 1) {
            createAndSendTronGaslessTransactionUseCase(
                userWallet = fromStatus.userWallet,
                network = network,
                transactionData = txData,
                fee = feeExtended,
            )
        }
        coVerify(exactly = 0) {
            createAndSendGaslessTransactionUseCase(any(), any(), any())
            sendTransactionUseCase(txData = any(), userWallet = any(), network = any())
        }
    }

    @Test
    fun `GIVEN token fee without Tron quote WHEN onSwap CEX THEN sends via EVM gasless`() = runTest {
        // Arrange
        val fromStatus = hotTronTokenStatus()
        val feeExtended = TransactionFeeExtended(
            transactionFee = TransactionFee.Single(normal = mockk<Fee.Ethereum.TokenCurrency>(relaxed = true)),
            feeTokenId = fromStatus.currency.id,
        )

        // Act
        onSwapCex(fromStatus, swapFee(feeExtended, feeToken = fromStatus.status))

        // Assert
        coVerify(exactly = 1) {
            createAndSendGaslessTransactionUseCase(
                transactionData = txData,
                userWallet = fromStatus.userWallet,
                fee = feeExtended,
            )
        }
        coVerify(exactly = 0) {
            createAndSendTronGaslessTransactionUseCase(
                userWallet = any(),
                network = any(),
                transactionData = any(),
                fee = any(),
            )
        }
    }

    @Test
    fun `GIVEN native fee WHEN onSwap CEX THEN sends a regular transaction`() = runTest {
        // Arrange
        val fromStatus = hotTronTokenStatus()
        val nativeFee = SwapFee(
            fee = mockk<Fee.Common>(relaxed = true),
            transactionFeeResult = TransactionFeeResult.Loaded(fee = mockk<TransactionFee.Single>(relaxed = true)),
            selectedFeeToken = buildSwapCurrencyStatus(networkRawId = tronNetwork).status,
            otherNativeFee = BigDecimal.ZERO,
            feeBucket = FeeBucket.MARKET,
        )

        // Act
        onSwapCex(fromStatus, nativeFee)

        // Assert
        coVerify(exactly = 1) {
            sendTransactionUseCase(txData = txData, userWallet = fromStatus.userWallet, network = any())
        }
        coVerify(exactly = 0) {
            createAndSendTronGaslessTransactionUseCase(
                userWallet = any(),
                network = any(),
                transactionData = any(),
                fee = any(),
            )
            createAndSendGaslessTransactionUseCase(any(), any(), any())
        }
    }

    private suspend fun onSwapCex(fromStatus: SwapCurrencyStatus, fee: SwapFee): SwapTransactionState = sut.onSwap(
        fromSwapCurrencyStatus = fromStatus,
        toSwapCurrencyStatus = buildSwapCurrencyStatus(),
        swapProvider = buildSwapProvider(ExchangeProviderType.CEX),
        swapData = null,
        amountToSwap = "10",
        balanceStatus = SwapBalanceStatus.Sufficient,
        fee = fee,
        expressOperationType = ExpressOperationType.SWAP,
        isTangemPayWithdrawal = false,
    )

    private fun swapFee(feeExtended: TransactionFeeExtended, feeToken: CryptoCurrencyStatus): SwapFee = SwapFee(
        fee = feeExtended.transactionFee.normal,
        transactionFeeResult = TransactionFeeResult.LoadedExtended(feeExtended),
        selectedFeeToken = feeToken,
        otherNativeFee = BigDecimal.ZERO,
        feeBucket = FeeBucket.MARKET,
    )

    private fun hotTronTokenStatus(): SwapCurrencyStatus {
        val status = buildSwapCurrencyStatus(
            networkRawId = tronNetwork,
            contractAddress = USDT_CONTRACT,
            isCoin = false,
            decimals = 6,
        )
        return SwapCurrencyStatus(
            userWallet = mockk<UserWallet.Hot>(relaxed = true),
            status = status.status,
            account = status.account,
        )
    }

    private fun cexSwapData(): SwapDataModel = SwapDataModel(
        toTokenAmount = SwapAmount(BigDecimal("9"), 6),
        transaction = ExpressTransactionModel.CEX(
            fromAmount = SwapAmount(BigDecimal("10"), 6),
            toAmount = SwapAmount(BigDecimal("9"), 6),
            txValue = null,
            txId = "cex-tx-id",
            txTo = CEX_DEPOSIT_ADDRESS,
            txExtraId = null,
            externalTxId = "ext-id",
            externalTxUrl = "https://explorer/tx",
            txExtraIdName = null,
        ),
    )

    private companion object {
        const val USDT_CONTRACT = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t"
        const val CEX_DEPOSIT_ADDRESS = "TCexDepositAddress"
        const val TRON_TX_HASH = "tron-original-hash"
        const val EVM_TX_HASH = "evm-gasless-hash"
        const val NATIVE_TX_HASH = "native-hash"
    }
}