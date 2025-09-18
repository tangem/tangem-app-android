package com.tangem.data.yield.supply

import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.blockchain.blockchains.ethereum.tokenmethods.ApprovalERC20TokenCallData
import com.tangem.blockchain.blockchains.tron.TronTransactionExtras
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.yieldsupply.YieldSupplyContractCallDataProviderFactory
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.yield.supply.YieldSupplyStatus
import com.tangem.domain.utils.convertToSdkAmount
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.yield.supply.YieldSupplyTransactionRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.math.BigDecimal

@Suppress("LargeClass")
internal class DefaultYieldSupplyTransactionRepository(
    private val walletManagersFacade: WalletManagersFacade,
    private val dispatchers: CoroutineDispatcherProvider,
) : YieldSupplyTransactionRepository {

    override suspend fun createEnterTransactions(
        userWalletId: UserWalletId,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
    ): List<TransactionData.Uncompiled> {
        val cryptoCurrency = cryptoCurrencyStatus.currency

        require(cryptoCurrency is CryptoCurrency.Token)

        val walletManager = walletManagersFacade.getOrCreateWalletManager(
            userWalletId = userWalletId,
            blockchain = cryptoCurrency.network.toBlockchain(),
            derivationPath = cryptoCurrency.network.derivationPath.value,
        ) ?: error("Wallet manager not found")

        val existingYieldContractAddress = getYieldContractAddress(
            userWalletId = userWalletId,
            cryptoCurrency = cryptoCurrency,
        )

        val calculatedYieldContractAddress = calculateYieldContractAddress(
            userWalletId = userWalletId,
            cryptoCurrency = cryptoCurrency,
        ) ?: error("Calculated yield contract address is null")

        val yieldTokenStatus = cryptoCurrencyStatus.value.yieldSupplyStatus ?: getYieldTokenStatus(
            walletManager = walletManager,
            cryptoCurrency = cryptoCurrency,
        )

        return buildEnterTransactions(
            walletManager = walletManager,
            cryptoCurrency = cryptoCurrency,
            existingYieldContractAddress = existingYieldContractAddress,
            calculatedYieldContractAddress = calculatedYieldContractAddress,
            yieldTokenStatus = yieldTokenStatus,
        )
    }

    override suspend fun createExitTransaction(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
        yieldSupplyStatus: YieldSupplyStatus,
        fee: Fee?,
    ): TransactionData.Uncompiled = withContext(dispatchers.io) {
        require(cryptoCurrency is CryptoCurrency.Token)

        val walletManager = walletManagersFacade.getOrCreateWalletManager(
            userWalletId = userWalletId,
            blockchain = cryptoCurrency.network.toBlockchain(),
            derivationPath = cryptoCurrency.network.derivationPath.value,
        ) ?: error("Wallet manager not found")

        val callData = YieldSupplyContractCallDataProviderFactory.getExitCallData(
            tokenContractAddress = cryptoCurrency.contractAddress,
        )

        createTransaction(
            walletManager = walletManager,
            cryptoCurrency = cryptoCurrency,
            callData = callData,
            destinationAddress = walletManager.getYieldContract(),
            yieldSupplyStatus = yieldSupplyStatus,
            fee = fee,
        )
    }

    private fun buildEnterTransactions(
        walletManager: WalletManager,
        cryptoCurrency: CryptoCurrency.Token,
        existingYieldContractAddress: String?,
        calculatedYieldContractAddress: String,
        yieldTokenStatus: YieldSupplyStatus?,
    ): MutableList<TransactionData.Uncompiled> {
        val enterTransactions = mutableListOf<TransactionData.Uncompiled>()

        when {
            existingYieldContractAddress == null || existingYieldContractAddress == EthereumUtils.ZERO_ADDRESS -> {
                enterTransactions.add(
                    createDeployTransaction(
                        walletManager = walletManager,
                        cryptoCurrency = cryptoCurrency,
                    ),
                )
            }
            yieldTokenStatus == null -> error("Yield token status is null")
            !yieldTokenStatus.isInitialized -> enterTransactions.add(
                createInitTokenTransaction(
                    walletManager = walletManager,
                    cryptoCurrency = cryptoCurrency,
                    yieldSupplyStatus = yieldTokenStatus,
                    yieldContractAddress = calculatedYieldContractAddress,
                ),
            )
            !yieldTokenStatus.isActive -> enterTransactions.add(
                createReactivateTokenTransaction(
                    walletManager = walletManager,
                    cryptoCurrency = cryptoCurrency,
                    yieldSupplyStatus = yieldTokenStatus,
                    yieldContractAddress = calculatedYieldContractAddress,
                ),
            )
            else -> Unit
        }

        if (yieldTokenStatus?.isAllowedToSpend == false) {
            enterTransactions.add(
                createTransaction(
                    walletManager = walletManager,
                    cryptoCurrency = cryptoCurrency,
                    callData = ApprovalERC20TokenCallData(
                        spenderAddress = calculatedYieldContractAddress,
                        amount = null,
                    ),
                    destinationAddress = cryptoCurrency.contractAddress,
                    yieldSupplyStatus = yieldTokenStatus,
                    fee = null,
                ),
            )
        }

        enterTransactions.add(
            createEnterTransaction(
                walletManager = walletManager,
                cryptoCurrency = cryptoCurrency,
                yieldSupplyStatus = yieldTokenStatus,
                yieldContractAddress = calculatedYieldContractAddress,
            ),
        )

        return enterTransactions
    }

    private suspend fun calculateYieldContractAddress(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): String? = withContext(dispatchers.io) {
        require(cryptoCurrency is CryptoCurrency.Token)
        runCatching {
            val walletManager = walletManagersFacade.getOrCreateWalletManager(
                userWalletId = userWalletId,
                blockchain = cryptoCurrency.network.toBlockchain(),
                derivationPath = cryptoCurrency.network.derivationPath.value,
            ) ?: error("Wallet manager not found")
            walletManager.calculateYieldContract()
        }.onFailure(Timber::e)
            .getOrNull()
    }

    private suspend fun getYieldContractAddress(userWalletId: UserWalletId, cryptoCurrency: CryptoCurrency): String? =
        withContext(dispatchers.io) {
            require(cryptoCurrency is CryptoCurrency.Token)
            runCatching {
                val walletManager = walletManagersFacade.getOrCreateWalletManager(
                    userWalletId = userWalletId,
                    blockchain = cryptoCurrency.network.toBlockchain(),
                    derivationPath = cryptoCurrency.network.derivationPath.value,
                ) ?: error("Wallet manager not found")
                walletManager.getYieldContract()
            }.onFailure(Timber::e)
                .getOrNull()
        }

    private suspend fun getYieldTokenStatus(
        walletManager: WalletManager,
        cryptoCurrency: CryptoCurrency,
    ): YieldSupplyStatus? = withContext(dispatchers.io) {
        require(cryptoCurrency is CryptoCurrency.Token)
        runCatching {
            val sdkSupplyStatus = walletManager.getYieldSupplyStatus(cryptoCurrency.contractAddress)
            val isAllowedToSpend = walletManager.isAllowedToSpend(cryptoCurrency.contractAddress)

            YieldSupplyStatus(
                isActive = sdkSupplyStatus?.isActive == true,
                isInitialized = sdkSupplyStatus?.isInitialized == true,
                isAllowedToSpend = isAllowedToSpend,
            )
        }.onFailure(Timber::e).getOrNull()
    }

    private fun createDeployTransaction(
        walletManager: WalletManager,
        cryptoCurrency: CryptoCurrency.Token,
    ): TransactionData.Uncompiled {
        val callData = YieldSupplyContractCallDataProviderFactory.getDeployCallData(
            tokenContractAddress = cryptoCurrency.contractAddress,
            walletAddress = walletManager.wallet.address,
            maxNetworkFee = MAX_NETWORK_FEE.convertToSdkAmount(cryptoCurrency),
        )

        val factoryContractAddress = walletManager.getYieldSupplyContractAddresses()?.factoryContractAddress
            ?: error("Factory contract address is null")

        return createTransaction(
            walletManager = walletManager,
            cryptoCurrency = cryptoCurrency,
            callData = callData,
            destinationAddress = factoryContractAddress,
            yieldSupplyStatus = null,
            fee = null,
        )
    }

    private fun createInitTokenTransaction(
        walletManager: WalletManager,
        cryptoCurrency: CryptoCurrency.Token,
        yieldContractAddress: String,
        yieldSupplyStatus: YieldSupplyStatus,
    ): TransactionData.Uncompiled {
        val callData = YieldSupplyContractCallDataProviderFactory.getInitTokenCallData(
            tokenContractAddress = cryptoCurrency.contractAddress,
            maxNetworkFee = MAX_NETWORK_FEE.convertToSdkAmount(cryptoCurrency),
        )

        return createTransaction(
            walletManager = walletManager,
            cryptoCurrency = cryptoCurrency,
            callData = callData,
            destinationAddress = yieldContractAddress,
            yieldSupplyStatus = yieldSupplyStatus,
            fee = null,
        )
    }

    private fun createReactivateTokenTransaction(
        walletManager: WalletManager,
        cryptoCurrency: CryptoCurrency.Token,
        yieldContractAddress: String,
        yieldSupplyStatus: YieldSupplyStatus,
    ): TransactionData.Uncompiled {
        val callData = YieldSupplyContractCallDataProviderFactory.getReactivateTokenCallData(
            tokenContractAddress = cryptoCurrency.contractAddress,
            maxNetworkFee = MAX_NETWORK_FEE.convertToSdkAmount(cryptoCurrency),
        )

        return createTransaction(
            walletManager = walletManager,
            cryptoCurrency = cryptoCurrency,
            callData = callData,
            destinationAddress = yieldContractAddress,
            yieldSupplyStatus = yieldSupplyStatus,
            fee = null,
        )
    }

    private fun createEnterTransaction(
        walletManager: WalletManager,
        cryptoCurrency: CryptoCurrency.Token,
        yieldSupplyStatus: YieldSupplyStatus?,
        yieldContractAddress: String,
    ): TransactionData.Uncompiled {
        val callData = YieldSupplyContractCallDataProviderFactory.getEnterCallData(
            tokenContractAddress = cryptoCurrency.contractAddress,
        )

        return createTransaction(
            walletManager = walletManager,
            cryptoCurrency = cryptoCurrency,
            callData = callData,
            destinationAddress = yieldContractAddress,
            yieldSupplyStatus = yieldSupplyStatus,
            fee = null,
        )
    }

    @Suppress("LongParameterList")
    private fun createTransaction(
        walletManager: WalletManager,
        cryptoCurrency: CryptoCurrency,
        callData: SmartContractCallData,
        destinationAddress: String,
        yieldSupplyStatus: YieldSupplyStatus?,
        fee: Fee?,
    ): TransactionData.Uncompiled {
        requireNotNull(cryptoCurrency as? CryptoCurrency.Token)
        val blockchain = cryptoCurrency.network.id.toBlockchain()

        val extras = createTransactionDataExtras(
            callData = callData,
            blockchain = blockchain,
        )

        val amount = getYieldSupplyAmount(cryptoCurrency, yieldSupplyStatus)

        return if (fee != null) {
            walletManager.createTransaction(
                amount = amount,
                fee = fee,
                destination = destinationAddress,
            ).copy(
                extras = extras,
            )
        } else {
            TransactionData.Uncompiled(
                amount = amount,
                sourceAddress = walletManager.wallet.address,
                destinationAddress = destinationAddress,
                extras = extras,
                fee = null,
            )
        }
    }

    private fun createTransactionDataExtras(
        callData: SmartContractCallData,
        blockchain: Blockchain,
    ): TransactionExtras {
        return when {
            blockchain.isEvm() -> {
                EthereumTransactionExtras(
                    callData = callData,
                    gasLimit = null,
                    nonce = null,
                )
            }
            blockchain == Blockchain.Tron -> {
                TronTransactionExtras(
                    callData = callData,
                )
            }
            else -> error("Data extras not supported for $blockchain")
        }
    }

    private fun getYieldSupplyAmount(cryptoCurrency: CryptoCurrency.Token, yieldSupplyStatus: YieldSupplyStatus?) =
        BigDecimal.ZERO.convertToSdkAmount(
            cryptoCurrency = cryptoCurrency,
            amountType = AmountType.TokenYieldSupply(
                token = Token(
                    symbol = cryptoCurrency.symbol,
                    contractAddress = cryptoCurrency.contractAddress,
                    decimals = cryptoCurrency.decimals,
                ),
                isActive = yieldSupplyStatus?.isActive ?: false,
                isInitialized = yieldSupplyStatus?.isInitialized ?: false,
                isAllowedToSpend = yieldSupplyStatus?.isAllowedToSpend ?: false,
            ),
        )

    private companion object {
        val MAX_NETWORK_FEE: BigDecimal = BigDecimal.TEN // TODO for TESTNET only
    }
}