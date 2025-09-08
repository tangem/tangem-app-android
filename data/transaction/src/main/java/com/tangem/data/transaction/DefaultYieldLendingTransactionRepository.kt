package com.tangem.data.transaction

import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.blockchains.tron.TronTransactionExtras
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.yieldlending.YieldLendingContractCallDataProviderFactory
import com.tangem.blockchain.yieldlending.providers.ethereum.EthereumLendingStatus
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.yieldlending.YieldLendingStatus
import com.tangem.domain.transaction.YieldLendingTransactionRepository
import com.tangem.domain.utils.convertToSdkAmount
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import java.math.BigDecimal

internal class DefaultYieldLendingTransactionRepository(
    private val walletManagersFacade: WalletManagersFacade,
    private val dispatchers: CoroutineDispatcherProvider,
) : YieldLendingTransactionRepository {

    override suspend fun getYieldContractAddress(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): String = withContext(dispatchers.io) {
        require(cryptoCurrency is CryptoCurrency.Token)

        val walletManager = walletManagersFacade.getOrCreateWalletManager(
            userWalletId = userWalletId,
            blockchain = cryptoCurrency.network.toBlockchain(),
            derivationPath = cryptoCurrency.network.derivationPath.value,
        ) ?: error("Wallet manager not found")

        walletManager.getYieldContract()
    }

    override suspend fun getYieldTokenStatus(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): EthereumLendingStatus? = withContext(dispatchers.io) {
        require(cryptoCurrency is CryptoCurrency.Token)

        val walletManager = walletManagersFacade.getOrCreateWalletManager(
            userWalletId = userWalletId,
            blockchain = cryptoCurrency.network.toBlockchain(),
            derivationPath = cryptoCurrency.network.derivationPath.value,
        ) ?: error("Wallet manager not found")

        walletManager.getLendingStatus(tokenContractAddress = cryptoCurrency.contractAddress)
    }

    override suspend fun createDeployTransaction(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
        yieldLendingStatus: YieldLendingStatus,
        fee: Fee?,
    ): TransactionData.Uncompiled = withContext(dispatchers.io) {
        require(cryptoCurrency is CryptoCurrency.Token)

        val walletManager = walletManagersFacade.getOrCreateWalletManager(
            userWalletId = userWalletId,
            blockchain = cryptoCurrency.network.toBlockchain(),
            derivationPath = cryptoCurrency.network.derivationPath.value,
        ) ?: error("Wallet manager not found")

        val callData = YieldLendingContractCallDataProviderFactory.getDeployCallData(
            tokenContractAddress = cryptoCurrency.contractAddress,
            walletAddress = walletManager.wallet.address,
            maxNetworkFee = BigDecimal.TEN.convertToSdkAmount(cryptoCurrency), // TODO for TESTNET only
        )

        createTransaction(
            walletManager = walletManager,
            cryptoCurrency = cryptoCurrency,
            callData = callData,
            destinationAddress = walletManager.factoryContractAddress,
            yieldLendingStatus = yieldLendingStatus,
            fee = fee,
        )
    }

    override suspend fun createInitTokenTransaction(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
        yieldContractAddress: String,
        yieldLendingStatus: YieldLendingStatus,
        fee: Fee?,
    ): TransactionData.Uncompiled = withContext(dispatchers.io) {
        require(cryptoCurrency is CryptoCurrency.Token)

        val walletManager = walletManagersFacade.getOrCreateWalletManager(
            userWalletId = userWalletId,
            blockchain = cryptoCurrency.network.toBlockchain(),
            derivationPath = cryptoCurrency.network.derivationPath.value,
        ) ?: error("Wallet manager not found")

        val callData = YieldLendingContractCallDataProviderFactory.getInitTokenCallData(
            tokenContractAddress = cryptoCurrency.contractAddress,
            maxNetworkFee = BigDecimal.TEN.convertToSdkAmount(cryptoCurrency), // TODO for TESTNET only
        )

        createTransaction(
            walletManager = walletManager,
            cryptoCurrency = cryptoCurrency,
            callData = callData,
            destinationAddress = yieldContractAddress,
            yieldLendingStatus = yieldLendingStatus,
            fee = fee,
        )
    }

    override suspend fun createEnterTransaction(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
        yieldLendingStatus: YieldLendingStatus,
        fee: Fee?,
    ): TransactionData.Uncompiled = withContext(dispatchers.io) {
        require(cryptoCurrency is CryptoCurrency.Token)

        val walletManager = walletManagersFacade.getOrCreateWalletManager(
            userWalletId = userWalletId,
            blockchain = cryptoCurrency.network.toBlockchain(),
            derivationPath = cryptoCurrency.network.derivationPath.value,
        ) ?: error("Wallet manager not found")

        val callData = YieldLendingContractCallDataProviderFactory.getEnterCallData(
            tokenContractAddress = cryptoCurrency.contractAddress,
        )

        createTransaction(
            walletManager = walletManager,
            cryptoCurrency = cryptoCurrency,
            callData = callData,
            destinationAddress = walletManager.getYieldContract(),
            yieldLendingStatus = yieldLendingStatus,
            fee = fee,
        )
    }

    override suspend fun createExitTransaction(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
        yieldLendingStatus: YieldLendingStatus,
        fee: Fee?,
    ): TransactionData.Uncompiled = withContext(dispatchers.io) {
        require(cryptoCurrency is CryptoCurrency.Token)

        val walletManager = walletManagersFacade.getOrCreateWalletManager(
            userWalletId = userWalletId,
            blockchain = cryptoCurrency.network.toBlockchain(),
            derivationPath = cryptoCurrency.network.derivationPath.value,
        ) ?: error("Wallet manager not found")

        val callData = YieldLendingContractCallDataProviderFactory.getExitCallData(
            tokenContractAddress = cryptoCurrency.contractAddress,
        )

        createTransaction(
            walletManager = walletManager,
            cryptoCurrency = cryptoCurrency,
            callData = callData,
            destinationAddress = walletManager.getYieldContract(),
            yieldLendingStatus = yieldLendingStatus,
            fee = fee,
        )
    }

    private fun createTransaction(
        walletManager: WalletManager,
        cryptoCurrency: CryptoCurrency,
        callData: SmartContractCallData,
        destinationAddress: String,
        yieldLendingStatus: YieldLendingStatus,
        fee: Fee?,
    ): TransactionData.Uncompiled {
        requireNotNull(cryptoCurrency as? CryptoCurrency.Token)
        val blockchain = cryptoCurrency.network.toBlockchain()

        val extras = createTransactionDataExtras(
            callData = callData,
            blockchain = blockchain,
        )

        val amount = getYieldLendingAmount(cryptoCurrency, yieldLendingStatus)

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

    private fun getYieldLendingAmount(
        cryptoCurrency: CryptoCurrency.Token,
        yieldLendingStatus: YieldLendingStatus,
    ) = BigDecimal.ZERO.convertToSdkAmount(
        cryptoCurrency = cryptoCurrency,
        AmountType.YieldLend(
            token = Token(
                symbol = cryptoCurrency.symbol,
                contractAddress = cryptoCurrency.contractAddress,
                decimals = cryptoCurrency.decimals,
            ),
            isActive = yieldLendingStatus.isActive,
            isInitialized = yieldLendingStatus.isInitialized,
            isAllowedToSpend = yieldLendingStatus.isAllowedToSpend,
        ),
    )
}