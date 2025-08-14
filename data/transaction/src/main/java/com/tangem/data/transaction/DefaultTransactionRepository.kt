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
import com.tangem.blockchain.nft.models.NFTAsset
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.datasource.local.walletmanager.WalletManagersStore
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.TransactionRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger

@Suppress("LargeClass")
internal class DefaultTransactionRepository(
    private val walletManagersFacade: WalletManagersFacade,
    private val walletManagersStore: WalletManagersStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : TransactionRepository {

    override suspend fun createTransaction(
        amount: Amount,
        fee: Fee?,
        memo: String?,
        destination: String,
        userWalletId: UserWalletId,
        network: Network,
        txExtras: TransactionExtras?,
    ): TransactionData.Uncompiled = withContext(dispatchers.io) {
        val blockchain = network.toBlockchain()
        val walletManager = walletManagersFacade.getOrCreateWalletManager(
            userWalletId = userWalletId,
            blockchain = blockchain,
            derivationPath = network.derivationPath.value,
        ) ?: error("Wallet manager not found")

        val extras = txExtras ?: getMemoExtras(networkId = network.rawId, memo)

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
        fee: Fee?,
        memo: String?,
        destination: String,
        userWalletId: UserWalletId,
        network: Network,
        nonce: BigInteger?,
    ): TransactionData.Uncompiled = withContext(dispatchers.io) {
        val blockchain = network.toBlockchain()
        val walletManager = walletManagersFacade.getOrCreateWalletManager(
            userWalletId = userWalletId,
            blockchain = blockchain,
            derivationPath = network.derivationPath.value,
        ) ?: error("Wallet manager not found")

        val callData = SmartContractCallDataProviderFactory.getTokenTransferCallData(
            destinationAddress = destination,
            amount = amount,
            blockchain = blockchain,
        )

        val extras = if (amount.type is AmountType.Token && callData != null) {
            createTransactionDataExtras(
                callData = callData,
                network = network,
                nonce = nonce,
                gasLimit = null,
            )
        } else {
            null
        }

        return@withContext if (fee != null) {
            createTransaction(
                amount = amount,
                fee = fee,
                memo = null,
                destination = destination,
                userWalletId = userWalletId,
                network = network,
                txExtras = getMemoExtras(networkId = network.rawId, memo = memo) ?: extras,
            )
        } else {
            TransactionData.Uncompiled(
                amount = amount,
                sourceAddress = walletManager.wallet.address,
                destinationAddress = destination,
                extras = getMemoExtras(networkId = network.rawId, memo = memo) ?: extras,
                fee = null,
            )
        }
    }

    override suspend fun createApprovalTransaction(
        amount: Amount,
        approvalAmount: Amount?,
        fee: Fee?,
        contractAddress: String,
        spenderAddress: String,
        userWalletId: UserWalletId,
        network: Network,
    ): TransactionData.Uncompiled = withContext(dispatchers.io) {
        val blockchain = network.toBlockchain()

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

    override suspend fun createNFTTransferTransaction(
        ownerAddress: String,
        nftAsset: NFTAsset,
        fee: Fee?,
        memo: String?,
        destinationAddress: String,
        userWalletId: UserWalletId,
        network: Network,
    ): TransactionData.Uncompiled = withContext(dispatchers.io) {
        val blockchain = network.toBlockchain()

        // For now transfer one nft asset at a time
        val updatedNFTAsset = nftAsset.copy(
            amount = BigInteger.ONE,
        )

        val nftTransferCallData = SmartContractCallDataProviderFactory.getNFTTransferCallData(
            destinationAddress = destinationAddress,
            ownerAddress = ownerAddress,
            nftAsset = updatedNFTAsset,
            blockchain = blockchain,
        )

        val extras = if (nftTransferCallData != null) {
            createTransactionDataExtras(
                callData = nftTransferCallData,
                network = network,
                nonce = null,
                gasLimit = null,
            )
        } else {
            null
        }

        val contractAddress = when (val identifier = nftAsset.identifier) {
            is NFTAsset.Identifier.EVM -> identifier.tokenAddress
            is NFTAsset.Identifier.Solana -> identifier.tokenAddress
            is NFTAsset.Identifier.TON -> identifier.tokenAddress
            NFTAsset.Identifier.Unknown -> ""
        }

        return@withContext createTransaction(
            amount = Amount(
                value = updatedNFTAsset.amount?.toBigDecimal() ?: error("Invalid amount"),
                token = Token(
                    symbol = blockchain.currency,
                    contractAddress = contractAddress,
                    decimals = nftAsset.decimals ?: error("Invalid decimals"),
                ),
            ),
            fee = fee,
            memo = memo,
            destination = destinationAddress,
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
    ): Result<Unit> = withContext(dispatchers.io) {
        val blockchain = network.toBlockchain()
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
                extras = getMemoExtras(networkId = network.rawId, memo = memo),
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
    ) = withContext(dispatchers.io) {
        val blockchain = network.toBlockchain()
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
        sendMode: TransactionSender.MultipleTransactionSendMode,
    ) = withContext(dispatchers.io) {
        val blockchain = network.toBlockchain()
        val walletManager = walletManagersFacade.getOrCreateWalletManager(
            userWalletId = userWalletId,
            blockchain = blockchain,
            derivationPath = network.derivationPath.value,
        )
        (walletManager as TransactionSender).sendMultiple(txsData, signer, sendMode)
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
        val blockchain = cryptoCurrency.network.toBlockchain()
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
    ) = withContext(dispatchers.io) {
        val preparer = getPreparer(network, userWalletId)
        preparer.prepareForSend(transactionData, signer)
    }

    override suspend fun prepareForSendMultiple(
        transactionData: List<TransactionData>,
        signer: TransactionSigner,
        userWalletId: UserWalletId,
        network: Network,
    ) = withContext(dispatchers.io) {
        val preparer = getPreparer(network, userWalletId)
        preparer.prepareForSendMultiple(transactionData, signer)
    }

    override suspend fun prepareAndSign(
        transactionData: TransactionData,
        signer: TransactionSigner,
        userWalletId: UserWalletId,
        network: Network,
    ) = withContext(dispatchers.io) {
        val preparer = getPreparer(network, userWalletId)
        preparer.prepareAndSign(transactionData, signer)
    }

    override suspend fun prepareAndSignMultiple(
        transactionData: List<TransactionData>,
        signer: TransactionSigner,
        userWalletId: UserWalletId,
        network: Network,
    ) = withContext(dispatchers.io) {
        val preparer = getPreparer(network, userWalletId)
        preparer.prepareAndSignMultiple(transactionData, signer)
    }

    private suspend fun getPreparer(network: Network, userWalletId: UserWalletId): TransactionPreparer {
        val blockchain = network.toBlockchain()
        val walletManager = walletManagersFacade.getOrCreateWalletManager(
            userWalletId = userWalletId,
            blockchain = blockchain,
            derivationPath = network.derivationPath.value,
        )
        val preparer = walletManager as? TransactionPreparer ?: run {
            Timber.e("${walletManager?.wallet?.blockchain} does not support TransactionBuilder")
            error("Wallet manager does not support TransactionPreparer")
        }
        return preparer
    }
}