package com.tangem.tap.proxy

import androidx.core.text.isDigitsOnly
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.tangem.Message
import com.tangem.blockchain.blockchains.algorand.AlgorandTransactionExtras
import com.tangem.blockchain.blockchains.binance.BinanceTransactionExtras
import com.tangem.blockchain.blockchains.cosmos.CosmosTransactionExtras
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.blockchains.hedera.HederaTransactionExtras
import com.tangem.blockchain.blockchains.optimism.EthereumOptimisticRollupWalletManager
import com.tangem.blockchain.blockchains.stellar.StellarMemo
import com.tangem.blockchain.blockchains.stellar.StellarTransactionExtras
import com.tangem.blockchain.blockchains.ton.TonTransactionExtras
import com.tangem.blockchain.blockchains.xrp.XrpTransactionBuilder
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.common.transaction.TransactionSendResult
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.externallinkprovider.TxExploreState
import com.tangem.blockchain.network.ResultChecker
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.hexToBytes
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.lib.crypto.TransactionManager
import com.tangem.lib.crypto.models.*
import com.tangem.lib.crypto.models.transactions.SendTxResult
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.TangemSigner
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode

@Suppress("LargeClass")
class TransactionManagerImpl(
    private val appStateHolder: AppStateHolder,
    private val cardSdkConfigRepository: CardSdkConfigRepository,
    private val walletManagersFacade: WalletManagersFacade,
    private val userWalletsListManager: UserWalletsListManager,
) : TransactionManager {

    override suspend fun sendApproveTransaction(
        txData: ApproveTxData,
        derivationPath: String?,
        analyticsData: AnalyticsData,
    ): SendTxResult {
        val blockchain = requireNotNull(Blockchain.fromNetworkId(txData.networkId)) { "blockchain not found" }
        val walletManager = getActualWalletManager(blockchain, derivationPath)
        walletManager.update()
        val amount = Amount(value = BigDecimal.ZERO, blockchain = blockchain)
        return sendTransactionInternal(
            walletManager = walletManager,
            amount = amount,
            blockchain = blockchain,
            feeAmount = txData.feeAmount,
            gasLimit = txData.gasLimit,
            destinationAddress = txData.destinationAddress,
            dataToSign = txData.dataToSign,
        )
    }

    override suspend fun sendTransaction(
        txData: SwapTxData,
        isSwap: Boolean,
        derivationPath: String?,
        analyticsData: AnalyticsData,
    ): SendTxResult {
        val blockchain = requireNotNull(Blockchain.fromNetworkId(txData.networkId)) { "blockchain not found" }
        val walletManager = getActualWalletManager(blockchain, derivationPath)
        walletManager.update()
        val amount = if (isSwap) {
            createAmountForSwap(txData.amountToSend, txData.currencyToSend, blockchain)
        } else {
            createAmount(txData.amountToSend, txData.currencyToSend, blockchain)
        }
        return sendTransactionInternal(
            walletManager = walletManager,
            amount = amount,
            blockchain = blockchain,
            feeAmount = txData.feeAmount,
            gasLimit = txData.gasLimit,
            destinationAddress = txData.destinationAddress,
            dataToSign = txData.dataToSign,
        )
    }

    @Suppress("LongParameterList")
    private suspend fun sendTransactionInternal(
        walletManager: WalletManager,
        amount: Amount,
        blockchain: Blockchain,
        feeAmount: BigDecimal,
        gasLimit: Int,
        destinationAddress: String,
        dataToSign: String,
    ): SendTxResult {
        val txData = walletManager.createTransaction(
            amount = amount,
            fee = Fee.Common(Amount(value = feeAmount, blockchain = blockchain)),
            destination = destinationAddress,
        ).copy(hash = dataToSign, extras = createExtras(walletManager, gasLimit, dataToSign))

        val signer = transactionSigner(walletManager)

        val sendResult = try {
            (walletManager as? TransactionSender)?.send(txData, signer) ?: error("Cannot cast to TransactionSender")
        } catch (ex: Exception) {
            FirebaseCrashlytics.getInstance().recordException(ex)
            return SendTxResult.UnknownError(ex)
        }
        return handleSendResult(result = sendResult)
    }

    override fun getExplorerTransactionLink(networkId: String, txAddress: String): String {
        val blockchain = Blockchain.fromNetworkId(networkId) ?: error("blockchain not found")
        return when (val txUrlState = blockchain.getExploreTxUrl(txAddress)) {
            TxExploreState.Unsupported -> ""
            is TxExploreState.Url -> txUrlState.url
        }
    }

    override fun getMemoExtras(networkId: String, memo: String?): TransactionExtras? {
        val blockchain = Blockchain.fromNetworkId(networkId)
        if (memo == null) return null
        return when (blockchain) {
            Blockchain.Stellar -> {
                val xlmMemo = if (memo.isNotEmpty() && memo.isDigitsOnly()) {
                    StellarMemo.Id(memo.toBigInteger())
                } else {
                    StellarMemo.Text(memo)
                }
                StellarTransactionExtras(xlmMemo)
            }
            Blockchain.Binance -> BinanceTransactionExtras(memo)
            Blockchain.XRP -> memo.toLongOrNull()?.let { XrpTransactionBuilder.XrpTransactionExtras(it) }
            Blockchain.Cosmos -> CosmosTransactionExtras(memo)
            Blockchain.TON -> TonTransactionExtras(memo)
            Blockchain.Hedera -> HederaTransactionExtras(memo)
            Blockchain.Algorand -> AlgorandTransactionExtras(memo)
            else -> null
        }
    }

    override suspend fun updateWalletManager(networkId: String, derivationPath: String?) {
        val blockchain = requireNotNull(Blockchain.fromNetworkId(networkId)) { "blockchain not found" }
        getActualWalletManager(blockchain, derivationPath).update()
    }

    override fun calculateFee(networkId: String, gasPrice: String, estimatedGas: Int): BigDecimal {
        val blockchain = requireNotNull(Blockchain.fromNetworkId(networkId)) { "blockchain not found" }
        val gasPriceValue = requireNotNull(gasPrice.toLongOrNull()) { "gasprice should be Long" }
        return (gasPriceValue * estimatedGas).toBigDecimal().movePointLeft(blockchain.decimals())
    }

    @Throws(IllegalStateException::class)
    override suspend fun getFee(
        networkId: String,
        amountToSend: Amount,
        currencyToSend: Currency,
        destinationAddress: String,
        increaseBy: Int?,
        data: String?,
        derivationPath: String?,
    ): ProxyFees {
        val blockchain = requireNotNull(Blockchain.fromNetworkId(networkId)) { "blockchain not found" }
        val walletManager = getActualWalletManager(blockchain, derivationPath)
        if (walletManager is EthereumWalletManager) {
            if (walletManager is EthereumOptimisticRollupWalletManager) {
                return getFeeForOptimismBlockchain(
                    walletManager = walletManager,
                    amount = amountToSend,
                    destinationAddress = destinationAddress,
                    data = data,
                )
            }
            return getFeeForEthereumBlockchain(
                walletManager = walletManager,
                blockchain = blockchain,
                amountToSend = amountToSend,
                destinationAddress = destinationAddress,
                data = data,
                increaseBy = increaseBy,
            )
        } else {
            return getFeeForBlockchain(
                walletManager = walletManager,
                amountToSend = amountToSend,
                destinationAddress = destinationAddress,
            )
        }
    }

    override fun getBlockchainInfo(networkId: String): ProxyNetworkInfo {
        val blockchain = requireNotNull(Blockchain.fromNetworkId(networkId)) { "blockchain not found" }
        return ProxyNetworkInfo(
            name = blockchain.fullName,
            blockchainId = blockchain.id,
        )
    }

    private suspend fun getFeeForBlockchain(
        walletManager: WalletManager,
        amountToSend: Amount,
        destinationAddress: String,
    ): ProxyFees {
        val fee = (walletManager as? TransactionSender)?.getFee(
            amount = amountToSend,
            destination = destinationAddress,
        ) ?: error("Cannot cast to TransactionSender")
        return when (fee) {
            is Result.Success -> {
                // for not EVM blockchains set gasLimit ZERO for now
                when (fee.data) {
                    is TransactionFee.Single -> {
                        val normalFee = (fee.data as TransactionFee.Single).normal
                        val singleFee = if (normalFee as? Fee.CardanoToken != null) {
                            ProxyFee.CardanoToken(
                                gasLimit = BigInteger.ZERO,
                                fee = convertToProxyAmount(amount = normalFee.amount),
                                minAdaValue = normalFee.minAdaValue,
                            )
                        } else {
                            ProxyFee.Common(
                                gasLimit = BigInteger.ZERO,
                                fee = convertToProxyAmount(amount = normalFee.amount),
                            )
                        }

                        ProxyFees.SingleFee(singleFee = singleFee)
                    }
                    is TransactionFee.Choosable -> {
                        val choosableFee = fee.data as TransactionFee.Choosable
                        ProxyFees.MultipleFees(
                            minFee = ProxyFee.Common(
                                gasLimit = BigInteger.ZERO,
                                fee = convertToProxyAmount(amount = choosableFee.minimum.amount),
                            ),
                            normalFee = ProxyFee.Common(
                                gasLimit = BigInteger.ZERO,
                                fee = convertToProxyAmount(amount = choosableFee.normal.amount),
                            ),
                            priorityFee = ProxyFee.Common(
                                gasLimit = BigInteger.ZERO,
                                fee = convertToProxyAmount(amount = choosableFee.priority.amount),
                            ),
                        )
                    }
                }
            }
            is Result.Failure -> {
                error(fee.error.message ?: fee.error.customMessage)
            }
        }
    }

    @Suppress("LongParameterList")
    private suspend fun getFeeForEthereumBlockchain(
        walletManager: EthereumWalletManager,
        blockchain: Blockchain,
        amountToSend: Amount,
        destinationAddress: String,
        data: String?,
        increaseBy: Int?,
    ): ProxyFees {
        val gasLimit = getGasLimit(
            evmWalletManager = walletManager,
            amount = amountToSend,
            destinationAddress = destinationAddress,
            data = data,
        ).increaseBigIntegerByPercents(increaseBy)
        return when (val gasPrice = walletManager.getGasPrice()) {
            is Result.Success -> {
                createMultipleProxyFees(gasPrice = gasPrice.data, gasLimit = gasLimit, blockchain = blockchain)
            }
            is Result.Failure -> {
                error(gasPrice.error.message ?: gasPrice.error.customMessage)
            }
        }
    }

    private suspend fun getFeeForOptimismBlockchain(
        walletManager: EthereumOptimisticRollupWalletManager,
        amount: Amount,
        destinationAddress: String,
        data: String?,
    ): ProxyFees {
        val fee = if (data.isNullOrEmpty()) {
            walletManager.getFee(amount, destinationAddress)
        } else {
            walletManager.getFee(amount, destinationAddress, data)
        }
        return when (fee) {
            is Result.Success -> {
                val choosableFee = fee.data

                val minProxyFee = ProxyFee.Common(
                    gasLimit = (choosableFee.minimum as Fee.Ethereum).gasLimit,
                    fee = convertToProxyAmount(amount = choosableFee.minimum.amount),
                )
                val normalProxyFee = ProxyFee.Common(
                    gasLimit = (choosableFee.normal as Fee.Ethereum).gasLimit,
                    fee = convertToProxyAmount(amount = choosableFee.normal.amount),
                )
                val priorityProxyFee = ProxyFee.Common(
                    gasLimit = (choosableFee.priority as Fee.Ethereum).gasLimit,
                    fee = convertToProxyAmount(amount = choosableFee.priority.amount),
                )

                ProxyFees.MultipleFees(
                    minFee = minProxyFee,
                    normalFee = normalProxyFee,
                    priorityFee = priorityProxyFee,
                )
            }
            is Result.Failure -> {
                error(fee.error.message ?: fee.error.customMessage)
            }
        }
    }

    private suspend fun getGasLimit(
        evmWalletManager: EthereumWalletManager,
        amount: Amount,
        destinationAddress: String,
        data: String?,
    ): BigInteger {
        val result = if (data.isNullOrEmpty()) {
            evmWalletManager.getGasLimit(
                amount = amount,
                destination = destinationAddress,
            )
        } else {
            evmWalletManager.getGasLimit(
                amount = amount,
                destination = destinationAddress,
                data = data,
            )
        }
        when (result) {
            is Result.Success -> {
                return result.data
            }
            is Result.Failure -> {
                error(result.error.message ?: result.error.customMessage)
            }
        }
    }

    override suspend fun getFeeForGas(networkId: String, gas: BigInteger, derivationPath: String?): ProxyFees {
        val blockchain = requireNotNull(Blockchain.fromNetworkId(networkId)) { "blockchain not found" }
        val walletManager = getActualWalletManager(blockchain, derivationPath)
        val gasPriceResult = (walletManager as? EthereumWalletManager)?.getGasPrice()
            ?: error("not supported for $blockchain")
        val gasPrice = when (gasPriceResult) {
            is Result.Failure -> error("fail to receive gasPrice")
            is Result.Success -> gasPriceResult.data
        }
        return createMultipleProxyFees(gasPrice, gas, blockchain)
    }

    private fun handleSendResult(result: Result<TransactionSendResult>): SendTxResult {
        when (result) {
            is Result.Success -> {
                return SendTxResult.Success
            }
            is Result.Failure -> {
                if (ResultChecker.isNetworkError(result)) return SendTxResult.NetworkError(result.error)
                val error = result.error as? BlockchainSdkError ?: return SendTxResult.UnknownError()
                when (error) {
                    is BlockchainSdkError.WrappedTangemError -> {
                        val errorByCode = mapErrorByCode(error)
                        if (errorByCode != null) {
                            return errorByCode
                        }
                        val tangemSdkError = error.tangemError as? TangemSdkError ?: return SendTxResult.UnknownError()
                        if (tangemSdkError is TangemSdkError.UserCancelled) return SendTxResult.UserCancelledError
                        return SendTxResult.TangemSdkError(tangemSdkError.code, tangemSdkError.cause)
                    }
                    else -> {
                        return SendTxResult.TangemSdkError(error.code, error.cause)
                    }
                }
            }
        }
    }

    private fun mapErrorByCode(error: BlockchainSdkError.WrappedTangemError): SendTxResult? {
        return when (error.code) {
            USER_CANCELLED_ERROR_CODE -> {
                return SendTxResult.UserCancelledError
            }
            else -> {
                null
            }
        }
    }

    private fun transactionSigner(walletManager: WalletManager): TransactionSigner {
        val actualCard = requireNotNull(appStateHolder.getActualCard()) { "no card found" }
        return TangemSigner(
            card = actualCard,
            tangemSdk = cardSdkConfigRepository.sdk,
            initialMessage = Message(),
        ) { signResponse ->
            appStateHolder.mainStore?.dispatch(
                GlobalAction.UpdateWalletSignedHashes(
                    walletSignedHashes = signResponse.totalSignedHashes,
                    walletPublicKey = walletManager.wallet.publicKey.seedKey,
                    remainingSignatures = signResponse.remainingSignatures,
                ),
            )
        }
    }

    private suspend fun getActualWalletManager(blockchain: Blockchain, derivationPath: String?): WalletManager {
        val selectedUserWallet = requireNotNull(
            userWalletsListManager.selectedUserWalletSync,
        ) { "userWallet or userWalletsListManager is null" }
        val walletManager = walletManagersFacade.getOrCreateWalletManager(
            selectedUserWallet.walletId,
            blockchain,
            derivationPath,
        )

        return requireNotNull(walletManager) { "no wallet manager found" }
    }

    private fun createExtras(
        walletManager: WalletManager,
        gasLimit: Int,
        transactionHash: String,
    ): TransactionExtras? {
        return when (walletManager) {
            is EthereumWalletManager -> {
                return EthereumTransactionExtras(
                    data = transactionHash.removePrefix(HEX_PREFIX).hexToBytes(),
                    gasLimit = gasLimit.toBigInteger(),
                )
            }
            else -> {
                null
            }
        }
    }

    /**
     * Create proxy fees
     *
     * @param gasPrice min fee gasPrice
     * @param gasLimit
     * @param blockchain
     */
    private fun createMultipleProxyFees(gasPrice: BigInteger, gasLimit: BigInteger, blockchain: Blockchain): ProxyFees {
        val gasPriceNormal = gasPrice.increaseBigIntegerByPercents(MULTIPLIER_GAS_PRICE_FOR_NORMAL_FEE)
        val gasPricePriority = gasPrice.increaseBigIntegerByPercents(MULTIPLIER_GAS_PRICE_FOR_PRIORITY_FEE)
        val feeMin = gasLimit.multiply(gasPrice).toBigDecimal(
            scale = blockchain.decimals(),
            mathContext = MathContext(blockchain.decimals(), RoundingMode.HALF_EVEN),
        )
        val feeNormal = gasLimit.multiply(gasPriceNormal).toBigDecimal(
            scale = blockchain.decimals(),
            mathContext = MathContext(blockchain.decimals(), RoundingMode.HALF_EVEN),
        )
        val feePriority = gasLimit.multiply(gasPricePriority).toBigDecimal(
            scale = blockchain.decimals(),
            mathContext = MathContext(blockchain.decimals(), RoundingMode.HALF_EVEN),
        )
        val minFee = ProxyFee.Common(
            gasLimit = gasLimit,
            fee = ProxyAmount(
                currencySymbol = blockchain.currency,
                value = feeMin,
                decimals = blockchain.decimals(),
            ),
        )
        val normalFee = ProxyFee.Common(
            gasLimit = gasLimit,
            fee = ProxyAmount(
                currencySymbol = blockchain.currency,
                value = feeNormal,
                decimals = blockchain.decimals(),
            ),
        )
        val priorityFee = ProxyFee.Common(
            gasLimit = gasLimit,
            fee = ProxyAmount(
                currencySymbol = blockchain.currency,
                value = feePriority,
                decimals = blockchain.decimals(),
            ),
        )
        return ProxyFees.MultipleFees(
            minFee = minFee,
            normalFee = normalFee,
            priorityFee = priorityFee,
        )
    }

    private fun createAmount(amount: BigDecimal, currency: Currency, blockchain: Blockchain): Amount {
        return when (currency) {
            is Currency.NativeToken -> {
                Amount(value = amount, blockchain = blockchain)
            }
            is Currency.NonNativeToken -> {
                Amount(convertNonNativeToken(currency), amount)
            }
        }
    }

    private fun createAmountForSwap(amount: BigDecimal, currency: Currency?, blockchain: Blockchain): Amount {
        return when (currency) {
            is Currency.NativeToken,
            null,
            -> {
                Amount(value = amount, blockchain = blockchain)
            }
            is Currency.NonNativeToken -> {
                // 1. when creates swap amount for NonNativeToken, amount should be ZERO
                // 2. Amount has .Coin type, as workaround to use destinationAddress in bsdk, not contractAddress
                Amount(
                    currencySymbol = currency.symbol,
                    value = BigDecimal.ZERO,
                    decimals = currency.decimalCount,
                )
            }
        }
    }

    private fun convertNonNativeToken(token: Currency.NonNativeToken): Token {
        return Token(
            name = token.name,
            symbol = token.symbol,
            contractAddress = token.contractAddress,
            decimals = token.decimalCount,
            id = token.id,
        )
    }

    private fun convertToProxyAmount(amount: Amount): ProxyAmount {
        return ProxyAmount(
            currencySymbol = amount.currencySymbol,
            value = amount.value ?: BigDecimal.ZERO,
            decimals = amount.decimals,
        )
    }

    /**
     * Increase big integer by percents
     *
     * @param percents in format 150 -> 50%
     * @return increased value
     */
    private fun BigInteger.increaseBigIntegerByPercents(percents: Int?): BigInteger {
        return if (percents != null && percents != 0) {
            this.multiply(percents.toBigInteger()).divide(BigInteger("100"))
        } else {
            this
        }
    }

    companion object {
        private const val HEX_PREFIX = "0x"
        private const val USER_CANCELLED_ERROR_CODE = 50002
        private const val MULTIPLIER_GAS_PRICE_FOR_NORMAL_FEE = 150 // 50%
        private const val MULTIPLIER_GAS_PRICE_FOR_PRIORITY_FEE = 200 // 50%
    }
}
