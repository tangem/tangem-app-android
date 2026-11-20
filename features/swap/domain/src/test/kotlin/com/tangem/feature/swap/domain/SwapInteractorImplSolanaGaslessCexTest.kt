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
import com.tangem.domain.express.models.ExpressOperationType
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.domain.transaction.error.SolanaGaslessError
import com.tangem.domain.transaction.models.GaslessProtocolData
import com.tangem.domain.transaction.models.TransactionFeeExtended
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

/**
 * Tests for [SwapInteractorImpl.onSwap] sending a sponsored Solana CEX deposit transfer.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SwapInteractorImplSolanaGaslessCexTest : SwapInteractorImplTestBase() {

    private val solanaNetwork = Blockchain.Solana.toNetworkId()
    private val depositTransfer = mockk<TransactionData.Uncompiled>(relaxed = true) {
        every { extras } returns null
    }

    @BeforeEach
    fun setup() {
        clearMocks(
            sendTransactionUseCase,
            createAndSendGaslessTransactionUseCase,
            createTransferTransactionUseCase,
            repository,
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
        } returns depositTransfer.right()
        coEvery {
            repository.exchangeSent(
                userWallet = any(), txId = any(), fromNetwork = any(), fromAddress = any(),
                payInAddress = any(), txHash = any(), payInExtraId = any(),
            )
        } returns Unit.right()
        coEvery { sendTransactionUseCase(txData = any(), userWallet = any(), network = any()) } returns
            NATIVE_TX_HASH.right()
    }

    @Test
    fun `GIVEN sponsored fee picked AND transfer sponsored WHEN onSwap CEX THEN deposit sent with the sponsor`() =
        runTest {
            // Arrange
            val from = hotSolanaTokenStatus()
            coEvery { solanaGaslessSwapResolver.resolveTransferSponsorship(any(), depositTransfer) } returns
                feeOnlySponsorship(from).right()
            coEvery { createAndSendGaslessTransactionUseCase(any(), any(), any()) } returns SIGNATURE.right()

            // Act
            val result = onSwapCex(from, solanaSwapFee(isSponsored = true))

            // Assert
            assertThat((result as SwapTransactionState.TxSent).txHash).isEqualTo(SIGNATURE)
            val sponsorship = feeOnlySponsorship(from)
            coVerify(exactly = 1) {
                createAndSendGaslessTransactionUseCase(
                    transactionData = depositTransfer,
                    userWallet = from.userWallet,
                    fee = match {
                        it.gaslessProtocolData == sponsorship
                    },
                )
            }
            coVerify(exactly = 0) { sendTransactionUseCase(txData = any(), userWallet = any(), network = any()) }
            coVerify(exactly = 1) {
                repository.exchangeSent(
                    userWallet = any(), txId = CEX_TX_ID, fromNetwork = any(), fromAddress = any(),
                    payInAddress = any(), txHash = SIGNATURE, payInExtraId = any(),
                )
            }
        }

    @Test
    fun `GIVEN sponsored fee picked AND deposit transfer rejected WHEN onSwap CEX THEN rejection shown, not sent`() =
        runTest {
            // Arrange
            val from = hotSolanaTokenStatus()
            coEvery {
                solanaGaslessSwapResolver.resolveTransferSponsorship(any(), any())
            } returns SolanaGaslessError.Rejected(code = null).left()

            // Act
            val result = onSwapCex(from, solanaSwapFee(isSponsored = true))

            // Assert
            assertThat(result).isEqualTo(
                SwapTransactionState.Error.SolanaGaslessError(SolanaGaslessError.Rejected(code = null)),
            )
            coVerify(exactly = 0) { createAndSendGaslessTransactionUseCase(any(), any(), any()) }
            coVerify(exactly = 0) { sendTransactionUseCase(txData = any(), userWallet = any(), network = any()) }
        }

    @Test
    fun `GIVEN service may have sent with signature WHEN onSwap CEX THEN deposit tracked as sent`() = runTest {
        // Arrange
        val from = hotSolanaTokenStatus()
        coEvery { solanaGaslessSwapResolver.resolveTransferSponsorship(any(), any()) } returns
            feeOnlySponsorship(from).right()
        coEvery { createAndSendGaslessTransactionUseCase(any(), any(), any()) } returns
            SendTransactionError.SolanaGasless(SolanaGaslessError.MaybeSent(signature = SIGNATURE)).left()

        // Act
        val result = onSwapCex(from, solanaSwapFee(isSponsored = true))

        // Assert
        assertThat((result as SwapTransactionState.TxSent).txHash).isEqualTo(SIGNATURE)
        coVerify(exactly = 1) {
            repository.exchangeSent(
                userWallet = any(), txId = CEX_TX_ID, fromNetwork = any(), fromAddress = any(),
                payInAddress = any(), txHash = SIGNATURE, payInExtraId = any(),
            )
        }
    }

    @Test
    fun `GIVEN service rejects the transfer WHEN onSwap CEX THEN Solana gasless error`() = runTest {
        // Arrange
        val from = hotSolanaTokenStatus()
        coEvery { solanaGaslessSwapResolver.resolveTransferSponsorship(any(), any()) } returns
            feeOnlySponsorship(from).right()
        coEvery { createAndSendGaslessTransactionUseCase(any(), any(), any()) } returns
            SendTransactionError.SolanaGasless(SolanaGaslessError.BlockhashExpired).left()

        // Act
        val result = onSwapCex(from, solanaSwapFee(isSponsored = true))

        // Assert
        assertThat(result)
            .isEqualTo(SwapTransactionState.Error.SolanaGaslessError(SolanaGaslessError.BlockhashExpired))
        coVerify(exactly = 0) {
            repository.exchangeSent(
                userWallet = any(), txId = any(), fromNetwork = any(), fromAddress = any(),
                payInAddress = any(), txHash = any(), payInExtraId = any(),
            )
        }
    }

    @Test
    fun `GIVEN fee paid in SOL WHEN onSwap CEX THEN regular transfer and no sponsor resolved`() = runTest {
        // Arrange
        val from = hotSolanaTokenStatus()

        // Act
        val result = onSwapCex(from, solanaSwapFee(isSponsored = false))

        // Assert
        assertThat((result as SwapTransactionState.TxSent).txHash).isEqualTo(NATIVE_TX_HASH)
        coVerify(exactly = 1) {
            sendTransactionUseCase(txData = depositTransfer, userWallet = from.userWallet, network = any())
        }
        coVerify(exactly = 0) { solanaGaslessSwapResolver.resolveTransferSponsorship(any(), any()) }
        coVerify(exactly = 0) { createAndSendGaslessTransactionUseCase(any(), any(), any()) }
    }

    private suspend fun onSwapCex(from: SwapCurrencyStatus, fee: SwapFee): SwapTransactionState = sut.onSwap(
        fromSwapCurrencyStatus = from,
        toSwapCurrencyStatus = buildSwapCurrencyStatus(),
        swapProvider = buildSwapProvider(ExchangeProviderType.CEX),
        swapData = null,
        amountToSwap = "10",
        balanceStatus = SwapBalanceStatus.Sufficient,
        fee = fee,
        expressOperationType = ExpressOperationType.SWAP,
        isTangemPayWithdrawal = false,
    )

    private fun solanaSwapFee(isSponsored: Boolean): SwapFee {
        val fee = Fee.Common(Amount(if (isSponsored) BigDecimal.ZERO else BigDecimal("0.0002"), Blockchain.Solana))
        return SwapFee(
            fee = fee,
            transactionFeeResult = TransactionFeeResult.Loaded(fee = TransactionFee.Single(normal = fee)),
            selectedFeeToken = buildSwapCurrencyStatus(networkRawId = solanaNetwork, decimals = 9).status,
            otherNativeFee = BigDecimal.ZERO,
            feeBucket = FeeBucket.MARKET,
            isSponsored = isSponsored,
        )
    }

    private fun hotSolanaTokenStatus(): SwapCurrencyStatus {
        val status = buildSwapCurrencyStatus(
            networkRawId = solanaNetwork,
            contractAddress = USDC_MINT,
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
            txId = CEX_TX_ID,
            txTo = CEX_DEPOSIT_ADDRESS,
            txExtraId = null,
            externalTxId = "ext-id",
            externalTxUrl = "https://explorer/tx",
            txExtraIdName = null,
        ),
    )

    private fun feeOnlySponsorship(from: SwapCurrencyStatus) = GaslessProtocolData.Solana(
        feePayer = FEE_PAYER,
        network = from.currency.network,
        isRentSponsored = false,
    )

    private companion object {
        const val USDC_MINT = "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v"
        const val CEX_DEPOSIT_ADDRESS = "A9Jn6HqVd5oPdT3YTJjnDMh3vZDVpz8j1HnxygsNxdBo"
        const val CEX_TX_ID = "cex-tx-id"
        const val FEE_PAYER = "FeePayer123"
        const val SIGNATURE = "sig"
        const val NATIVE_TX_HASH = "native-hash"
    }
}