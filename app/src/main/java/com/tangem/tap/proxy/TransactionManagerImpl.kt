package com.tangem.tap.proxy

import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.blockchains.optimism.EthereumOptimisticRollupWalletManager
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.externallinkprovider.TxExploreState
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.lib.crypto.TransactionManager
import com.tangem.lib.crypto.models.*
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode

@Suppress("LargeClass")
class TransactionManagerImpl(
    private val walletManagersFacade: WalletManagersFacade,
    private val userWalletsListManager: UserWalletsListManager,
) : TransactionManager {

    override fun getExplorerTransactionLink(networkId: String, txAddress: String): String {
        val blockchain = Blockchain.fromNetworkId(networkId) ?: error("blockchain not found")
        return when (val txUrlState = blockchain.getExploreTxUrl(txAddress)) {
            TxExploreState.Unsupported -> ""
            is TxExploreState.Url -> txUrlState.url
        }
    }

    override suspend fun updateWalletManager(networkId: String, derivationPath: String?) {
        val blockchain = requireNotNull(Blockchain.fromNetworkId(networkId)) { "blockchain not found" }
        getActualWalletManager(blockchain, derivationPath).update()
    }

    @Throws(IllegalStateException::class)
    override suspend fun getFee(
        networkId: String,
        amountToSend: Amount,
        currencyToSend: Currency,
        destinationAddress: String,
        increaseBy: Int?,
        callData: SmartContractCallData?,
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
                    callData = callData,
                )
            }
            return getFeeForEthereumBlockchain(
                walletManager = walletManager,
                blockchain = blockchain,
                amountToSend = amountToSend,
                destinationAddress = destinationAddress,
                callData = callData,
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
                        val singleFee = when (val normalFee = (fee.data as TransactionFee.Single).normal) {
                            is Fee.CardanoToken -> {
                                ProxyFee.CardanoToken(
                                    gasLimit = BigInteger.ZERO,
                                    fee = convertToProxyAmount(amount = normalFee.amount),
                                    minAdaValue = normalFee.minAdaValue,
                                )
                            }
                            is Fee.Filecoin -> {
                                ProxyFee.Filecoin(
                                    gasLimit = BigInteger.ZERO,
                                    fee = convertToProxyAmount(amount = normalFee.amount),
                                    gasPremium = normalFee.gasPremium,
                                )
                            }
                            is Fee.Sui -> {
                                ProxyFee.Sui(
                                    gasLimit = BigInteger.ZERO,
                                    fee = convertToProxyAmount(amount = normalFee.amount),
                                    gasPrice = normalFee.gasPrice,
                                    gasBudget = normalFee.gasBudget,
                                )
                            }
                            else -> {
                                ProxyFee.Common(
                                    gasLimit = BigInteger.ZERO,
                                    fee = convertToProxyAmount(amount = normalFee.amount),
                                )
                            }
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
        callData: SmartContractCallData?,
        increaseBy: Int?,
    ): ProxyFees {
        val gasLimit = getGasLimit(
            evmWalletManager = walletManager,
            amount = amountToSend,
            destinationAddress = destinationAddress,
            callData = callData,
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
        callData: SmartContractCallData?,
    ): ProxyFees {
        val fee = if (callData == null) {
            walletManager.getFee(amount, destinationAddress)
        } else {
            walletManager.getFee(amount, destinationAddress, callData)
        }
        return when (fee) {
            is Result.Success -> {
                val choosableFee = fee.data as? TransactionFee.Choosable ?: error("Incorrect fee type")

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
        callData: SmartContractCallData?,
    ): BigInteger {
        val result = if (callData == null) {
            evmWalletManager.getGasLimit(
                amount = amount,
                destination = destinationAddress,
            )
        } else {
            evmWalletManager.getGasLimit(
                amount = amount,
                destination = destinationAddress,
                callData = callData,
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

    /**
     * Create proxy fees
     *
     * @param gasPrice min fee gasPrice
     * @param gasLimit
     * @param blockchain
     */
    private fun createMultipleProxyFees(gasPrice: BigInteger, gasLimit: BigInteger, blockchain: Blockchain): ProxyFees {
        val patchedGasLimit = gasLimit.toBigDecimal().increaseForMantleIfNeeded(blockchain).toBigInteger()
        val gasPriceNormal = gasPrice
            .increaseBigIntegerByPercents(MULTIPLIER_GAS_PRICE_FOR_NORMAL_FEE)
        val gasPricePriority = gasPrice.increaseBigIntegerByPercents(MULTIPLIER_GAS_PRICE_FOR_PRIORITY_FEE)
        val feeMin = patchedGasLimit.multiply(gasPrice).toBigDecimal(
            scale = blockchain.decimals(),
            mathContext = MathContext(blockchain.decimals(), RoundingMode.HALF_EVEN),
        ).increaseForMantleIfNeeded(blockchain)
        val feeNormal = patchedGasLimit.multiply(gasPriceNormal).toBigDecimal(
            scale = blockchain.decimals(),
            mathContext = MathContext(blockchain.decimals(), RoundingMode.HALF_EVEN),
        ).increaseForMantleIfNeeded(blockchain)
        val feePriority = patchedGasLimit.multiply(gasPricePriority).toBigDecimal(
            scale = blockchain.decimals(),
            mathContext = MathContext(blockchain.decimals(), RoundingMode.HALF_EVEN),
        ).increaseForMantleIfNeeded(blockchain)
        val minFee = ProxyFee.Common(
            gasLimit = patchedGasLimit,
            fee = ProxyAmount(
                currencySymbol = blockchain.currency,
                value = feeMin,
                decimals = blockchain.decimals(),
            ),
        )
        val normalFee = ProxyFee.Common(
            gasLimit = patchedGasLimit,
            fee = ProxyAmount(
                currencySymbol = blockchain.currency,
                value = feeNormal,
                decimals = blockchain.decimals(),
            ),
        )
        val priorityFee = ProxyFee.Common(
            gasLimit = patchedGasLimit,
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

    // TODO Workaround for Mantle. Remove after [REDACTED_JIRA]
    private fun BigDecimal.increaseForMantleIfNeeded(blockchain: Blockchain): BigDecimal {
        return if (blockchain == Blockchain.Mantle) {
            this.multiply(MANTLE_FEE_ESTIMATE_MULTIPLIER)
        } else {
            this
        }
    }

    companion object {
        private const val MULTIPLIER_GAS_PRICE_FOR_NORMAL_FEE = 150 // 50%
        private const val MULTIPLIER_GAS_PRICE_FOR_PRIORITY_FEE = 200 // 50%
        private val MANTLE_FEE_ESTIMATE_MULTIPLIER = BigDecimal("1.8")
    }
}