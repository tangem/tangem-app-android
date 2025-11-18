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
import com.tangem.utils.extensions.orZero
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
        maxNetworkFee: BigDecimal,
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

        val maxNetworkFee = maxNetworkFee.convertToSdkAmount(cryptoCurrencyStatus)

        return buildEnterTransactions(
            walletManager = walletManager,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            existingYieldAddress = existingYieldContractAddress,
            calculatedYieldContractAddress = calculatedYieldContractAddress,
            maxNetworkFee = maxNetworkFee,
        )
    }

    override suspend fun createExitTransaction(
        userWalletId: UserWalletId,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        fee: Fee?,
    ): TransactionData.Uncompiled = withContext(dispatchers.io) {
        val cryptoCurrency = cryptoCurrencyStatus.currency as CryptoCurrency.Token

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
            destinationAddress = walletManager.getYieldModuleAddress(),
            amount = BigDecimal.ZERO.convertToSdkAmount(cryptoCurrencyStatus),
            fee = fee,
        )
    }

    override suspend fun getEffectiveProtocolBalance(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): BigDecimal? = withContext(dispatchers.io) {
        require(cryptoCurrency is CryptoCurrency.Token)
        runCatching {
            val walletManager = walletManagersFacade.getOrCreateWalletManager(
                userWalletId = userWalletId,
                blockchain = cryptoCurrency.network.toBlockchain(),
                derivationPath = cryptoCurrency.network.derivationPath.value,
            ) ?: error("Wallet manager not found")
            walletManager.getEffectiveProtocolBalance(
                token = Token(
                    symbol = cryptoCurrency.symbol,
                    contractAddress = cryptoCurrency.contractAddress,
                    decimals = cryptoCurrency.decimals,
                ),
            )
        }.onFailure(Timber::e).getOrThrow()
    }

    @Suppress("LongParameterList")
    private suspend fun buildEnterTransactions(
        walletManager: WalletManager,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        existingYieldAddress: String?,
        calculatedYieldContractAddress: String,
        maxNetworkFee: Amount,
    ): MutableList<TransactionData.Uncompiled> {
        val enterTransactions = mutableListOf<TransactionData.Uncompiled>()
        val cryptoCurrency = cryptoCurrencyStatus.currency as CryptoCurrency.Token
        val yieldSupplyStatus = getYieldTokenStatus(walletManager, cryptoCurrency)

        val amount = getEnterAmount(cryptoCurrency, yieldSupplyStatus)

        val emptyContractAddress = existingYieldAddress == null || existingYieldAddress == EthereumUtils.ZERO_ADDRESS

        when {
            yieldSupplyStatus == null || emptyContractAddress -> {
                enterTransactions.add(
                    createDeployTransaction(
                        walletManager = walletManager,
                        cryptoCurrency = cryptoCurrency,
                        amount = amount,
                        maxNetworkFee = maxNetworkFee,
                    ),
                )
            }
            !yieldSupplyStatus.isInitialized -> enterTransactions.add(
                createInitTokenTransaction(
                    walletManager = walletManager,
                    cryptoCurrency = cryptoCurrency,
                    yieldContractAddress = calculatedYieldContractAddress,
                    amount = amount,
                    maxNetworkFee = maxNetworkFee,
                ),
            )
            !yieldSupplyStatus.isActive -> enterTransactions.add(
                createReactivateTokenTransaction(
                    walletManager = walletManager,
                    cryptoCurrency = cryptoCurrency,
                    yieldContractAddress = calculatedYieldContractAddress,
                    amount = amount,
                    maxNetworkFee = maxNetworkFee,
                ),
            )
            else -> Unit
        }

        if (yieldSupplyStatus?.isAllowedToSpend != true) {
            enterTransactions.add(
                createTransaction(
                    walletManager = walletManager,
                    cryptoCurrency = cryptoCurrency,
                    callData = ApprovalERC20TokenCallData(
                        spenderAddress = calculatedYieldContractAddress,
                        amount = null,
                    ),
                    destinationAddress = cryptoCurrency.contractAddress,
                    amount = amount,
                    fee = null,
                ),
            )
        }

        if (cryptoCurrencyStatus.value.amount.orZero() > BigDecimal.ZERO) {
            enterTransactions.add(
                createEnterTransaction(
                    walletManager = walletManager,
                    cryptoCurrency = cryptoCurrency,
                    amount = amount,
                    yieldContractAddress = calculatedYieldContractAddress,
                ),
            )
        }

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
            walletManager.calculateYieldModuleAddress()
        }.onFailure(Timber::e).getOrThrow()
    }

    override suspend fun getYieldContractAddress(userWalletId: UserWalletId, cryptoCurrency: CryptoCurrency): String? =
        withContext(dispatchers.io) {
            require(cryptoCurrency is CryptoCurrency.Token)
            runCatching {
                val walletManager = walletManagersFacade.getOrCreateWalletManager(
                    userWalletId = userWalletId,
                    blockchain = cryptoCurrency.network.toBlockchain(),
                    derivationPath = cryptoCurrency.network.derivationPath.value,
                ) ?: error("Wallet manager not found")
                walletManager.getYieldModuleAddress()
            }.onFailure(Timber::e).getOrThrow()
        }

    private suspend fun getYieldTokenStatus(
        walletManager: WalletManager,
        cryptoCurrency: CryptoCurrency.Token,
    ): YieldSupplyStatus? = withContext(dispatchers.io) {
        runCatching {
            val sdkSupplyStatus = walletManager.getYieldSupplyStatus(cryptoCurrency.contractAddress)
            val protocolBalance = if (sdkSupplyStatus?.isActive == true) {
                walletManager.getEffectiveProtocolBalance(
                    token = Token(
                        symbol = cryptoCurrency.symbol,
                        contractAddress = cryptoCurrency.contractAddress,
                        decimals = cryptoCurrency.decimals,
                    ),
                )
            } else {
                null
            }
            val isAllowedToSpend = walletManager.isAllowedToSpend(
                Token(
                    symbol = cryptoCurrency.symbol,
                    contractAddress = cryptoCurrency.contractAddress,
                    decimals = cryptoCurrency.decimals,
                ),
            )

            YieldSupplyStatus(
                isActive = sdkSupplyStatus?.isActive == true,
                isInitialized = sdkSupplyStatus?.isInitialized == true,
                isAllowedToSpend = isAllowedToSpend,
                effectiveProtocolBalance = protocolBalance,
            )
        }.onFailure(Timber::e).getOrNull()
    }

    private fun createDeployTransaction(
        walletManager: WalletManager,
        cryptoCurrency: CryptoCurrency.Token,
        amount: Amount,
        maxNetworkFee: Amount,
    ): TransactionData.Uncompiled {
        val callData = YieldSupplyContractCallDataProviderFactory.getDeployCallData(
            tokenContractAddress = cryptoCurrency.contractAddress,
            walletAddress = walletManager.wallet.address,
            maxNetworkFee = maxNetworkFee,
        )

        val factoryContractAddress = walletManager.getYieldSupplyContractAddresses()?.factoryContractAddress
            ?: error("Factory contract address is null")

        return createTransaction(
            walletManager = walletManager,
            cryptoCurrency = cryptoCurrency,
            callData = callData,
            destinationAddress = factoryContractAddress,
            amount = amount,
            fee = null,
        )
    }

    private fun createInitTokenTransaction(
        walletManager: WalletManager,
        cryptoCurrency: CryptoCurrency.Token,
        yieldContractAddress: String,
        amount: Amount,
        maxNetworkFee: Amount,
    ): TransactionData.Uncompiled {
        val callData = YieldSupplyContractCallDataProviderFactory.getInitTokenCallData(
            tokenContractAddress = cryptoCurrency.contractAddress,
            maxNetworkFee = maxNetworkFee,
        )

        return createTransaction(
            walletManager = walletManager,
            cryptoCurrency = cryptoCurrency,
            callData = callData,
            destinationAddress = yieldContractAddress,
            amount = amount,
            fee = null,
        )
    }

    private fun createReactivateTokenTransaction(
        walletManager: WalletManager,
        cryptoCurrency: CryptoCurrency.Token,
        yieldContractAddress: String,
        amount: Amount,
        maxNetworkFee: Amount,
    ): TransactionData.Uncompiled {
        val callData = YieldSupplyContractCallDataProviderFactory.getReactivateTokenCallData(
            tokenContractAddress = cryptoCurrency.contractAddress,
            maxNetworkFee = maxNetworkFee,
        )

        return createTransaction(
            walletManager = walletManager,
            cryptoCurrency = cryptoCurrency,
            callData = callData,
            destinationAddress = yieldContractAddress,
            amount = amount,
            fee = null,
        )
    }

    private fun createEnterTransaction(
        walletManager: WalletManager,
        cryptoCurrency: CryptoCurrency.Token,
        amount: Amount,
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
            amount = amount,
            fee = null,
        )
    }

    @Suppress("LongParameterList")
    private fun createTransaction(
        walletManager: WalletManager,
        cryptoCurrency: CryptoCurrency,
        callData: SmartContractCallData,
        destinationAddress: String,
        amount: Amount,
        fee: Fee?,
    ): TransactionData.Uncompiled {
        requireNotNull(cryptoCurrency as? CryptoCurrency.Token)
        val blockchain = cryptoCurrency.network.id.toBlockchain()

        val extras = createTransactionDataExtras(
            callData = callData,
            blockchain = blockchain,
        )

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

    private fun getEnterAmount(cryptoCurrency: CryptoCurrency.Token, yieldSupplyStatus: YieldSupplyStatus?) = Amount(
        currencySymbol = cryptoCurrency.symbol,
        value = BigDecimal.ZERO,
        decimals = cryptoCurrency.decimals,
        type = AmountType.TokenYieldSupply(
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
}