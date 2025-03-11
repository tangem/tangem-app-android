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
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.common.extensions.hexToBytes
import com.tangem.datasource.local.walletmanager.WalletManagersStore
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.transaction.TransactionRepository
import com.tangem.domain.transaction.models.TransactionType
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger
import com.tangem.blockchain.blockchains.tron.TransactionType as SdkTransactionType

internal class DefaultTransactionRepository(
    private val walletManagersFacade: WalletManagersFacade,
    private val walletManagersStore: WalletManagersStore,
    private val coroutineDispatcherProvider: CoroutineDispatcherProvider,
) : TransactionRepository {

    override suspend fun createTransaction(
        amount: Amount,
        fee: Fee,
        memo: String?,
        destination: String,
        userWalletId: UserWalletId,
        network: Network,
        txExtras: TransactionExtras?,
        hash: String?,
    ): TransactionData.Uncompiled = withContext(coroutineDispatcherProvider.io) {
        val blockchain = Blockchain.fromId(network.id.value)
        val walletManager = walletManagersFacade.getOrCreateWalletManager(
            userWalletId = userWalletId,
            blockchain = blockchain,
            derivationPath = network.derivationPath.value,
        ) ?: error("Wallet manager not found")

        return@withContext walletManager.createTransactionDataInternal(
            amount = amount,
            fee = fee,
            memo = memo,
            destination = destination,
            network = network,
            txExtras = txExtras,
            hash = hash,
        )
    }

    override suspend fun createApprovalTransaction(
        amount: Amount,
        approvalAmount: Amount?,
        fee: Fee,
        contractAddress: String,
        spenderAddress: String,
        userWalletId: UserWalletId,
        network: Network,
        hash: String?,
    ): TransactionData.Uncompiled = withContext(coroutineDispatcherProvider.io) {
        val blockchain = Blockchain.fromId(network.id.value)
        val walletManager = walletManagersFacade.getOrCreateWalletManager(
            userWalletId = userWalletId,
            blockchain = blockchain,
            derivationPath = network.derivationPath.value,
        ) ?: error("Wallet manager not found")
        val approver = walletManager as? Approver ?: error("Cannot cast to Approver")

        val approvalData = approver.getApproveData(
            spenderAddress = spenderAddress,
            value = approvalAmount,
        )

        val extras = createTransactionDataExtras(
            data = approvalData,
            network = network,
            transactionType = TransactionType.APPROVE,
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
            hash = hash,
        )
    }

    override suspend fun validateTransaction(
        amount: Amount,
        fee: Fee?,
        memo: String?,
        destination: String,
        userWalletId: UserWalletId,
        network: Network,
        isSwap: Boolean,
        txExtras: TransactionExtras?,
        hash: String?,
    ): Result<Unit> = withContext(coroutineDispatcherProvider.io) {
        val blockchain = Blockchain.fromId(network.id.value)
        val walletManager = walletManagersStore.getSyncOrNull(
            userWalletId = userWalletId,
            blockchain = blockchain,
            derivationPath = network.derivationPath.value,
        )

        val validator = walletManager as? TransactionValidator

        if (validator != null) {
            val transactionData = walletManager.createTransactionDataInternal(
                amount = amount,
                fee = fee ?: Fee.Common(amount = amount),
                memo = memo,
                destination = destination,
                network = network,
                txExtras = txExtras,
                hash = hash,
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
        data: String,
        network: Network,
        transactionType: TransactionType,
        nonce: BigInteger?,
        gasLimit: BigInteger?,
    ): TransactionExtras {
        val blockchain = Blockchain.fromNetworkId(networkId = network.backendId)
            ?: error("Blockchain not found")
        return when {
            blockchain.isEvm() -> {
                EthereumTransactionExtras(
                    data = data.hexToBytes(),
                    gasLimit = gasLimit,
                    nonce = nonce,
                )
            }
            blockchain == Blockchain.Tron -> {
                TronTransactionExtras(
                    data = data.hexToBytes(),
                    txType = convertToSdkTransactionType(transactionType),
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

    private fun convertToSdkTransactionType(transactionType: TransactionType): SdkTransactionType {
        return when (transactionType) {
            TransactionType.APPROVE -> SdkTransactionType.APPROVE
        }
    }

    @Suppress("LongParameterList")
    private fun WalletManager.createTransactionDataInternal(
        amount: Amount,
        fee: Fee,
        memo: String?,
        destination: String,
        network: Network,
        txExtras: TransactionExtras?,
        hash: String?,
    ): TransactionData.Uncompiled {
        if (txExtras != null && memo != null) {
            // throw error for now to avoid programmers errors when use extras
            error("Both txExtras and memo provided, use only one of them")
        }
        val extras = txExtras ?: getMemoExtras(network.id.value, memo)
        return createTransaction(amount, fee, destination).copy(
            hash = hash,
            extras = extras,
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
}