package com.tangem.data.transaction

import androidx.core.text.isDigitsOnly
import com.tangem.blockchain.blockchains.algorand.AlgorandTransactionExtras
import com.tangem.blockchain.blockchains.binance.BinanceTransactionExtras
import com.tangem.blockchain.blockchains.casper.CasperTransactionExtras
import com.tangem.blockchain.blockchains.cosmos.CosmosTransactionExtras
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.blockchains.hedera.HederaTransactionExtras
import com.tangem.blockchain.blockchains.icp.ICPTransactionExtras
import com.tangem.blockchain.blockchains.stellar.StellarMemo
import com.tangem.blockchain.blockchains.stellar.StellarTransactionExtras
import com.tangem.blockchain.blockchains.ton.TonTransactionExtras
import com.tangem.blockchain.blockchains.tron.TronTransactionExtras
import com.tangem.blockchain.blockchains.xrp.XrpTransactionBuilder
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.common.smartcontract.SmartContractCallDataProviderFactory
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.datasource.local.walletmanager.WalletManagersStore
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.transaction.TransactionRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger

internal class DefaultTransactionRepository(
    private val walletManagersFacade: WalletManagersFacade,
    private val walletManagersStore: WalletManagersStore,
    private val coroutineDispatcherProvider: CoroutineDispatcherProvider,
) : TransactionRepository {

    override suspend fun createTransaction(
        amount: Amount,
        fee: Fee?,
        memo: String?,
        destination: String,
        userWalletId: UserWalletId,
        network: Network,
        txExtras: TransactionExtras?,
    ): TransactionData.Uncompiled = withContext(coroutineDispatcherProvider.io) {
        val blockchain = Blockchain.fromId(network.id.value)
        val walletManager = walletManagersFacade.getOrCreateWalletManager(
            userWalletId = userWalletId,
            blockchain = blockchain,
            derivationPath = network.derivationPath.value,
        ) ?: error("Wallet manager not found")

        val extras = txExtras ?: getMemoExtras(networkId = network.id.value, memo)

        return@withContext if (fee != null) {
            walletManager.createTransaction(
                amount = amount,
                fee = fee,
                destination = destination,
            ).copy(
                extras = extras,
            )
        } else {
            TransactionData.Uncompiled(
                amount = amount,
                sourceAddress = walletManager.wallet.address,
                destinationAddress = destination,
                extras = extras,
                fee = null,
            )
        }
    }

    override suspend fun createTransferTransaction(
        amount: Amount,
        fee: Fee,
        memo: String?,
        destination: String,
        userWalletId: UserWalletId,
        network: Network,
    ): TransactionData.Uncompiled = withContext(coroutineDispatcherProvider.io) {
        val blockchain = Blockchain.fromId(network.id.value)

        val callData = SmartContractCallDataProviderFactory.getTokenTransferCallData(
            destinationAddress = destination,
            amount = amount,
            blockchain = blockchain,
        )

        val extras = if (amount.type is AmountType.Token && callData != null) {
            createTransactionDataExtras(
                callData = callData,
                network = network,
                nonce = null,
                gasLimit = null,
            )
        } else {
            null
        }

        return@withContext createTransaction(
            amount = amount,
            fee = fee,
            memo = null,
            destination = destination,
            userWalletId = userWalletId,
            network = network,
            txExtras = getMemoExtras(networkId = network.id.value, memo = memo) ?: extras,
        )
    }

    override suspend fun createApprovalTransaction(
        amount: Amount,
        approvalAmount: Amount?,
        fee: Fee?,
        contractAddress: String,
        spenderAddress: String,
        userWalletId: UserWalletId,
        network: Network,
    ): TransactionData.Uncompiled = withContext(coroutineDispatcherProvider.io) {
        val blockchain = Blockchain.fromId(network.id.value)

        val extras = createTransactionDataExtras(
            callData = SmartContractCallDataProviderFactory.getApprovalCallData(
                spenderAddress = spenderAddress,
                amount = approvalAmount,
                blockchain = blockchain,
            ),
            network = network,
            nonce = null,
            gasLimit = null,
        )

        return@withContext createTransaction(
            amount = amount,
            fee = fee,
            memo = null,
            destination = contractAddress,
            userWalletId = userWalletId,
            network = network,
            txExtras = extras,
        )
    }

    override suspend fun validateTransaction(
        amount: Amount,
        fee: Fee?,
        memo: String?,
        destination: String,
        userWalletId: UserWalletId,
        network: Network,
    ): Result<Unit> = withContext(coroutineDispatcherProvider.io) {
        val blockchain = Blockchain.fromId(network.id.value)
        val walletManager = walletManagersStore.getSyncOrNull(
            userWalletId = userWalletId,
            blockchain = blockchain,
            derivationPath = network.derivationPath.value,
        )

        val validator = walletManager as? TransactionValidator

        if (validator != null) {
            val transactionData = walletManager.createTransaction(
                amount = amount,
                fee = fee ?: Fee.Common(amount = amount),
                destination = destination,
            ).copy(
                extras = getMemoExtras(networkId = network.id.value, memo = memo),
            )

            validator.validate(transactionData = transactionData)
        } else {
            Timber.e("${walletManager?.wallet?.blockchain} does not support transaction validation")
            Result.success(Unit)
        }
    }

    override suspend fun sendTransaction(
        txData: TransactionData,
        signer: TransactionSigner,
        userWalletId: UserWalletId,
        network: Network,
    ) = withContext(coroutineDispatcherProvider.io) {
        val blockchain = Blockchain.fromId(network.id.value)
        val walletManager = walletManagersFacade.getOrCreateWalletManager(
            userWalletId = userWalletId,
            blockchain = blockchain,
            derivationPath = network.derivationPath.value,
        )
        (walletManager as TransactionSender).send(txData, signer)
    }

    override suspend fun sendMultipleTransactions(
        txsData: List<TransactionData>,
        signer: TransactionSigner,
        userWalletId: UserWalletId,
        network: Network,
        mode: TransactionSender.MultipleTransactionSendMode,
    ) = withContext(coroutineDispatcherProvider.io) {
        val blockchain = Blockchain.fromId(network.id.value)
        val walletManager = walletManagersFacade.getOrCreateWalletManager(
            userWalletId = userWalletId,
            blockchain = blockchain,
            derivationPath = network.derivationPath.value,
        )
        (walletManager as TransactionSender).sendMultiple(txsData, signer, mode)
    }

    override fun createTransactionDataExtras(
        callData: SmartContractCallData,
        network: Network,
        nonce: BigInteger?,
        gasLimit: BigInteger?,
    ): TransactionExtras {
        val blockchain = Blockchain.fromNetworkId(networkId = network.backendId)
            ?: error("Blockchain not found")
        return when {
            blockchain.isEvm() -> {
                EthereumTransactionExtras(
                    callData = callData,
                    gasLimit = gasLimit,
                    nonce = nonce,
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

    override suspend fun getAllowance(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency.Token,
        spenderAddress: String,
    ): BigDecimal {
        val walletManager = walletManagersFacade.getOrCreateWalletManager(userWalletId, cryptoCurrency.network)
        val blockchain = Blockchain.fromId(cryptoCurrency.network.id.value)
        val allowanceResult = (walletManager as? Approver)?.getAllowance(
            spenderAddress,
            Token(
                symbol = blockchain.currency,
                contractAddress = cryptoCurrency.contractAddress,
                decimals = cryptoCurrency.decimals,
            ),
        ) ?: error("Cannot cast to Approver")

        return allowanceResult.fold(
            onSuccess = { it },
            onFailure = { error(it) },
        )
    }

    @Suppress("CyclomaticComplexMethod")
    private fun getMemoExtras(networkId: String, memo: String?): TransactionExtras? {
        val blockchain = Blockchain.fromId(networkId)
        if (memo == null) return null
        return when (blockchain) {
            Blockchain.Stellar -> {
                val xmlMemo = when {
                    memo.isNotEmpty() && memo.isDigitsOnly() -> StellarMemo.Id(memo.toBigInteger())
                    else -> StellarMemo.Text(memo)
                }
                StellarTransactionExtras(xmlMemo)
            }
            Blockchain.Binance -> BinanceTransactionExtras(memo)
            Blockchain.XRP -> memo.toLongOrNull()?.let { XrpTransactionBuilder.XrpTransactionExtras(it) }
            Blockchain.Cosmos,
            Blockchain.Sei,
            Blockchain.TerraV1,
            Blockchain.TerraV2,
            -> CosmosTransactionExtras(memo)
            Blockchain.TON -> TonTransactionExtras(memo)
            Blockchain.Hedera -> HederaTransactionExtras(memo)
            Blockchain.Algorand -> AlgorandTransactionExtras(memo)
            Blockchain.InternetComputer -> memo.toLongOrNull()?.let { ICPTransactionExtras(it) }
            Blockchain.Casper -> memo.toLongOrNull()?.let { CasperTransactionExtras(it) }
            else -> null
        }
    }

    override suspend fun prepareForSend(
        transactionData: TransactionData,
        signer: TransactionSigner,
        userWalletId: UserWalletId,
        network: Network,
    ): Result<ByteArray> = withContext(coroutineDispatcherProvider.io) {
        val blockchain = Blockchain.fromId(network.id.value)
        val walletManager = walletManagersFacade.getOrCreateWalletManager(
            userWalletId = userWalletId,
            blockchain = blockchain,
            derivationPath = network.derivationPath.value,
        )
        val preparer = walletManager as? TransactionPreparer ?: kotlin.run {
            Timber.e("${walletManager?.wallet?.blockchain} does not support TransactionBuilder")
            error("Wallet manager does not support TransactionPreparer")
        }

        when (val prepareForSend = preparer.prepareForSend(transactionData, signer)) {
            is com.tangem.blockchain.extensions.Result.Failure -> Result.failure(prepareForSend.error)
            is com.tangem.blockchain.extensions.Result.Success -> Result.success(prepareForSend.data)
        }
    }
}