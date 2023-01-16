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
import java.util.concurrent.atomic.AtomicLong

/**
 * Created by Anton Zhilenkov on 06.10.2022.
 */
class GnosisRegistrator(
    val walletManager: EthereumWalletManager,
) {

    private val walletAddress = walletManager.wallet.address
    private val token = walletManager.wallet.getFirstToken().guard {
        throw NullPointerException("WalletManager for GnosisRegistrator must contain token")
    }

    private val otpProcessorContractAddress: String = when (walletManager.wallet.blockchain) {
        Blockchain.SaltPay -> "0x3B4397C817A26521Df8bD01a949AFDE2251d91C2"
        else -> throw IllegalArgumentException("GnosisRegistrator supports only the SaltPay blockchain")
    }

    private val addressTreasureSafe = "0x24A3c2382497075b6D93258f5938f7B661c06318"

    private val atomicNonce = AtomicLong()

    suspend fun checkHasGas(): Result<Boolean> {
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
        return walletManager.getAllowance(addressTreasureSafe, token)
    }

    suspend fun transferFrom(amountToClaim: BigDecimal, signer: TransactionSigner): Result<Unit> {
        val amount = Amount(token, amountToClaim)
        val feeAmount = walletManager.getFeeToTransferFrom(amount, addressTreasureSafe)
            .extractFeeAmount().successOr { return it }

        val transactionData = walletManager.createTransferFromTransaction(amount, feeAmount, addressTreasureSafe)
        walletManager.transferFrom(transactionData, signer).successOr { return Result.Failure(it.error) }

        return Result.Success(Unit)
    }

    @Suppress("MagicNumber")
    private fun Result<List<Amount>>.extractFeeAmount(): Result<Amount> {
        return when (this) {
            is Result.Success -> {
                if (this.data.size != 3) {
                    Result.Failure(BlockchainSdkError.FailedToLoadFee)
                } else {
                    Result.Success(this.data[1])
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
