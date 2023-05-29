package com.tangem.tap.features.onboarding.products.wallet.saltPay

import com.tangem.blockchain.blockchains.ethereum.CompiledEthereumTransaction
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.blockchains.ethereum.SignedEthereumTransaction
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.BlockchainSdkError
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.successOr
import com.tangem.common.extensions.guard
import com.tangem.domain.common.extensions.successOr
import com.tangem.tap.common.extensions.isPositive
import com.tangem.tap.common.extensions.safeUpdate
import com.tangem.tap.common.extensions.stub
import com.tangem.tap.domain.getFirstToken
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode
import java.util.concurrent.atomic.AtomicLong

/**
[REDACTED_AUTHOR]
 */
class GnosisRegistrator(
    val walletManager: EthereumWalletManager,
) {

    private val walletAddress = walletManager.wallet.address
    private val blockchain = walletManager.wallet.blockchain
    private val token = walletManager.wallet.getFirstToken().guard {
        throw NullPointerException("WalletManager for GnosisRegistrator must contain token")
    }

    private val otpProcessorContractAddress: String = when (blockchain) {
        Blockchain.SaltPay -> "0xc659f4FEd7A84a188F54cBA4A7a49D77c1a20522"
        else -> throw IllegalArgumentException("GnosisRegistrator supports only the SaltPay blockchain")
    }

    private val addressTreasureSafe = "0x24A3c2382497075b6D93258f5938f7B661c06318"

    private val atomicNonce = AtomicLong()

    suspend fun checkHasGas(): Result<Boolean> {
        // return Result.Success(true) // test
        val wallet = walletManager.safeUpdate().successOr {
            return Result.Failure(BlockchainSdkError.WrappedThrowable(it.error))
        }

        val hasGas = wallet.amounts[AmountType.Coin]?.value?.isPositive() ?: false
        return Result.Success(hasGas)
    }

    suspend fun prepareBeforeMakingTxs(): Result<Unit> {
        walletManager.safeUpdate().successOr {
            return Result.Failure(BlockchainSdkError.WrappedThrowable(it.error))
        }
        atomicNonce.set(walletManager.txCount)

        return Result.Success(Unit)
    }

    suspend fun sendTransactions(transactions: List<SignedEthereumTransaction>): Result<List<String>> {
        val results = transactions.map {
            coroutineScope { async { walletManager.sendRaw(it.compiledTransaction, it.signature) } }
        }.awaitAll()

        val errors = results.filterIsInstance<SimpleResult.Failure>()
        return if (errors.isEmpty()) {
            Result.Success(listOf())
        } else {
            Result.Failure(errors[0].error)
        }
    }

    suspend fun makeSetSpendLimitTx(value: BigDecimal): Result<CompiledEthereumTransaction> {
        val limitAmount = Amount(walletManager.cardTokens.first(), value)
        val feeAmount = walletManager.getFeeToSetSpendLimit(otpProcessorContractAddress, limitAmount)
            .extractFeeAmount().successOr { return it }

        val compiledEthereumTransaction = walletManager.transactionBuilder.buildSetSpendLimitToSign(
            processorContractAddress = otpProcessorContractAddress,
            cardAddress = walletAddress,
            amount = limitAmount,
            transactionFee = feeAmount,
            gasLimit = walletManager.gasLimitToSetSpendLimit,
            nonce = atomicNonce.getAndIncrement().toBigInteger(),
        ) ?: return Result.Failure(BlockchainSdkError.CustomError("Can't create the 'setSpendLimit' transaction"))

        return Result.Success(compiledEthereumTransaction)
    }

    suspend fun makeInitOtpTx(rootOTP: ByteArray, rootOTPCounter: Int): Result<CompiledEthereumTransaction> {
        val feeAmount = walletManager.getFeeToInitOTP(otpProcessorContractAddress, rootOTP, rootOTPCounter)
            .extractFeeAmount().successOr { return it }

        val compiledEthereumTransaction = walletManager.transactionBuilder.buildInitOTPToSign(
            processorContractAddress = otpProcessorContractAddress,
            cardAddress = walletAddress,
            otp = rootOTP,
            otpCounter = rootOTPCounter,
            transactionFee = feeAmount,
            gasLimit = walletManager.gasLimitToInitOTP,
            nonce = atomicNonce.getAndIncrement().toBigInteger(),
        ) ?: return Result.Failure(BlockchainSdkError.CustomError("Can't create the 'initOtp' transaction"))

        return Result.Success(compiledEthereumTransaction)
    }

    suspend fun makeSetWalletTx(): Result<CompiledEthereumTransaction> {
        val feeAmount = walletManager.getFeeToSetWallet(otpProcessorContractAddress)
            .extractFeeAmount().successOr { return it }

        val compiledEthereumTransaction = walletManager.transactionBuilder.buildSetWalletToSign(
            processorContractAddress = otpProcessorContractAddress,
            cardAddress = walletAddress,
            transactionFee = feeAmount,
            gasLimit = walletManager.gasLimitToSetWallet,
            nonce = atomicNonce.getAndIncrement().toBigInteger(),
        ) ?: return Result.Failure(BlockchainSdkError.CustomError("Can't create the 'setWallet' transaction"))

        return Result.Success(compiledEthereumTransaction)
    }

    suspend fun makeApprovalTx(value: BigDecimal): Result<CompiledEthereumTransaction> {
        val approveAmount = Amount(token, value)
        val feeAmount = walletManager.getFeeToApprove(approveAmount, otpProcessorContractAddress)
            .extractFeeAmount().successOr { return it }

        val compiledEthereumTransaction = walletManager.transactionBuilder.buildApproveToSign(
            transactionData = walletManager.createTransaction(
                amount = approveAmount,
                fee = feeAmount,
                destination = otpProcessorContractAddress,
            ),
            nonce = atomicNonce.getAndIncrement().toBigInteger(),
            gasLimit = walletManager.gasLimitToApprove,
        ) ?: return Result.Failure(BlockchainSdkError.CustomError("Can't create the 'approval' transaction"))

        return Result.Success(compiledEthereumTransaction)
    }

    suspend fun getAllowance(): Result<Amount> {
        // return Result.Success(Amount(BigDecimal(0.1), walletManager.wallet.blockchain)) // test
        return walletManager.getAllowance(addressTreasureSafe, token)
    }

    suspend fun transferFrom(amountToClaim: BigDecimal, signer: TransactionSigner): Result<Unit> {
        if (walletManager.txCount == -1L) {
            walletManager.safeUpdate().successOr {
                return Result.Failure(BlockchainSdkError.WrappedThrowable(it.error))
            }
        }

        // return transferFromStandardWay(amountToClaim, signer)
        return transferFromHardcodeWay(amountToClaim, signer)
    }

    @Suppress("UnusedPrivateMember")
    private suspend fun transferFromStandardWay(amountToClaim: BigDecimal, signer: TransactionSigner): Result<Unit> {
        val amount = Amount(token, amountToClaim)
        val feeAmount = walletManager.getFeeToTransferFrom(amount, addressTreasureSafe)
            .extractFeeAmount().successOr { return it }

        val transactionData = walletManager.createTransferFromTransaction(amount, feeAmount, addressTreasureSafe)
        walletManager.transferFrom(transactionData, signer).successOr { return Result.Failure(it.error) }

        return Result.Success(Unit)
    }

    private suspend fun transferFromHardcodeWay(amountToClaim: BigDecimal, signer: TransactionSigner): Result<Unit> {
        val amount = Amount(token, amountToClaim)
        val gasPrice = walletManager.getGasPrice().successOr { return it }

        val hardcodeGasLimit = BigInteger("300000")
        val hardcodeFeeValue = (hardcodeGasLimit * gasPrice).toBigDecimal(
            scale = blockchain.decimals(),
            mathContext = MathContext(blockchain.decimals(), RoundingMode.HALF_EVEN),
        ).movePointLeft(blockchain.decimals())
        val hardcodeFeeAmount = Amount(hardcodeFeeValue, blockchain)

        val transactionData = walletManager.createTransferFromTransaction(
            amount = amount,
            fee = hardcodeFeeAmount,
            source = addressTreasureSafe,
        )
        val transactionToSign = walletManager.transactionBuilder.buildTransferFromToSign(
            transactionData,
            walletManager.txCount.toBigInteger(),
            hardcodeGasLimit,
        ) ?: return Result.Failure(BlockchainSdkError.CustomError("Not enough data"))

        return when (val result = walletManager.signAndSend(transactionToSign, signer)) {
            SimpleResult.Success -> Result.Success(Unit)
            is SimpleResult.Failure -> Result.Failure(result.error)
        }
    }

    @Suppress("MagicNumber")
    private fun Result<TransactionFee>.extractFeeAmount(): Result<Amount> {
        return when (this) {
            is Result.Success -> {
                when(data) {
                    is TransactionFee.Single -> {
                        Result.Failure(BlockchainSdkError.FailedToLoadFee)
                    }
                    is TransactionFee.Choosable -> {
                        Result.Success((data as TransactionFee.Choosable).normal)
                    }
                }
            }
            is Result.Failure -> this
        }
    }

    private fun Long?.toBigInteger(): BigInteger? = this?.let { BigInteger.valueOf(it) }

    private fun Long.toBigInteger(): BigInteger = BigInteger.valueOf(this)

    companion object {
        fun stub(): GnosisRegistrator {
            return GnosisRegistrator(WalletManager.stub())
        }
    }
}