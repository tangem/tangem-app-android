package com.tangem.domain.transaction

import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionSendResult
import com.tangem.blockchain.common.transaction.TransactionsSendResult
import com.tangem.blockchain.nft.models.NFTAsset
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWalletId
import java.math.BigDecimal
import java.math.BigInteger

interface TransactionRepository {

    @Suppress("LongParameterList")
    suspend fun createTransaction(
        amount: Amount,
        fee: Fee?,
        memo: String?,
        destination: String,
        userWalletId: UserWalletId,
        network: Network,
        txExtras: TransactionExtras?,
    ): TransactionData.Uncompiled

    @Suppress("LongParameterList")
    suspend fun createTransferTransaction(
        amount: Amount,
        fee: Fee?,
        memo: String?,
        destination: String,
        userWalletId: UserWalletId,
        network: Network,
    ): TransactionData.Uncompiled

    @Suppress("LongParameterList")
    suspend fun createNFTTransferTransaction(
        ownerAddress: String,
        nftAsset: NFTAsset,
        fee: Fee?,
        memo: String?,
        destinationAddress: String,
        userWalletId: UserWalletId,
        network: Network,
    ): TransactionData.Uncompiled

    @Suppress("LongParameterList")
    suspend fun createApprovalTransaction(
        amount: Amount,
        approvalAmount: Amount?,
        fee: Fee?,
        contractAddress: String,
        spenderAddress: String,
        userWalletId: UserWalletId,
        network: Network,
    ): TransactionData.Uncompiled

    @Suppress("LongParameterList")
    suspend fun validateTransaction(
        amount: Amount,
        fee: Fee?,
        memo: String?,
        destination: String,
        userWalletId: UserWalletId,
        network: Network,
    ): Result<Unit>

    suspend fun sendTransaction(
        txData: TransactionData,
        signer: TransactionSigner,
        userWalletId: UserWalletId,
        network: Network,
    ): com.tangem.blockchain.extensions.Result<TransactionSendResult>

    suspend fun sendMultipleTransactions(
        txsData: List<TransactionData>,
        signer: TransactionSigner,
        userWalletId: UserWalletId,
        network: Network,
        sendMode: TransactionSender.MultipleTransactionSendMode = TransactionSender.MultipleTransactionSendMode.DEFAULT,
    ): com.tangem.blockchain.extensions.Result<TransactionsSendResult>

    fun createTransactionDataExtras(
        callData: SmartContractCallData,
        network: Network,
        nonce: BigInteger?,
        gasLimit: BigInteger?,
    ): TransactionExtras

    suspend fun getAllowance(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency.Token,
        spenderAddress: String,
    ): BigDecimal

    suspend fun prepareForSend(
        transactionData: TransactionData,
        signer: TransactionSigner,
        userWalletId: UserWalletId,
        network: Network,
    ): Result<ByteArray>

    suspend fun prepareForSendMultiple(
        transactionData: List<TransactionData>,
        signer: TransactionSigner,
        userWalletId: UserWalletId,
        network: Network,
    ): Result<List<ByteArray>>
}