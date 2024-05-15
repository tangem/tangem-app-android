package com.tangem.data.transaction

import androidx.core.text.isDigitsOnly
import com.tangem.blockchain.blockchains.algorand.AlgorandTransactionExtras
import com.tangem.blockchain.blockchains.binance.BinanceTransactionExtras
import com.tangem.blockchain.blockchains.cosmos.CosmosTransactionExtras
import com.tangem.blockchain.blockchains.hedera.HederaTransactionBuilder
import com.tangem.blockchain.blockchains.stellar.StellarMemo
import com.tangem.blockchain.blockchains.stellar.StellarTransactionExtras
import com.tangem.blockchain.blockchains.ton.TonTransactionExtras
import com.tangem.blockchain.blockchains.xrp.XrpTransactionBuilder
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.datasource.local.walletmanager.WalletManagersStore
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.transaction.TransactionRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.math.BigDecimal

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
        isSwap: Boolean,
        hash: String?,
    ): TransactionData? = withContext(coroutineDispatcherProvider.io) {
        val blockchain = Blockchain.fromId(network.id.value)
        val walletManager = walletManagersFacade.getOrCreateWalletManager(
            userWalletId = userWalletId,
            blockchain = blockchain,
            derivationPath = network.derivationPath.value,
        )

        return@withContext walletManager?.createTransactionInternal(
            amount = amount,
            fee = fee,
            memo = memo,
            destination = destination,
            network = network,
            isSwap = isSwap,
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
        hash: String?,
    ): Result<Unit> {
        val blockchain = Blockchain.fromId(network.id.value)
        val walletManager = walletManagersStore.getSyncOrNull(
            userWalletId = userWalletId,
            blockchain = blockchain,
            derivationPath = network.derivationPath.value,
        )

        val validator = walletManager as? TransactionValidator

        return if (validator != null) {
            val transaction = walletManager.createTransactionInternal(
                amount = amount,
                fee = fee ?: Fee.Common(amount = amount),
                memo = memo,
                destination = destination,
                network = network,
                isSwap = isSwap,
                hash = hash,
            )

            validator.validate(transaction = transaction)
        } else {
            Timber.e("${walletManager?.wallet?.blockchain} does not support transaction validation")
            Result.success(Unit)
        }
    }

    override suspend fun sendTransaction(
        txData: TransactionData,
        signer: CommonSigner,
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

    @Suppress("LongParameterList")
    private fun WalletManager.createTransactionInternal(
        amount: Amount,
        fee: Fee,
        memo: String?,
        destination: String,
        network: Network,
        isSwap: Boolean,
        hash: String?,
    ): TransactionData {
        val txAmount = if (isSwap) {
            createAmountForSwap(amount)
        } else {
            amount
        }

        return createTransaction(txAmount, fee, destination).copy(
            hash = hash,
            extras = getMemoExtras(network.id.value, memo),
        )
    }

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
            Blockchain.TerraV1,
            Blockchain.TerraV2,
            -> CosmosTransactionExtras(memo)
            Blockchain.TON -> TonTransactionExtras(memo)
            Blockchain.Hedera -> HederaTransactionBuilder.HederaTransactionExtras(memo)
            Blockchain.Algorand -> AlgorandTransactionExtras(memo)
            else -> null
        }
    }

    private fun createAmountForSwap(amount: Amount): Amount {
        return when (amount.type) {
            is AmountType.Coin -> amount
            else -> {
                // 1. when creates swap amount for NonNativeToken, amount should be ZERO
                // 2. Amount has .Coin type, as workaround to use destinationAddress in bsdk, not contractAddress
                Amount(
                    currencySymbol = amount.currencySymbol,
                    value = BigDecimal.ZERO,
                    decimals = amount.decimals,
                )
            }
        }
    }
}