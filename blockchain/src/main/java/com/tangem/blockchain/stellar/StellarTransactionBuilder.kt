package com.tangem.blockchain.stellar

import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.stellar.StellarWalletManager.Companion.BASE_FEE
import com.tangem.blockchain.stellar.StellarWalletManager.Companion.STROOPS_IN_XLM
import com.tangem.common.extensions.hexToBytes
import org.stellar.sdk.*
import org.stellar.sdk.xdr.AccountID
import org.stellar.sdk.xdr.DecoratedSignature
import org.stellar.sdk.xdr.Signature
import org.stellar.sdk.xdr.SignatureHint
import java.util.*

class StellarTransactionBuilder(private val newtorkManager: StellarNetworkManager, private val publicKey: ByteArray) {

    private lateinit var transaction: Transaction

    suspend fun buildToSign(transactionData: TransactionData, sequence: Long, fee: Int): List<ByteArray> {

        val destinationKeyPair = KeyPair.fromAccountId(transactionData.destinationAddress)
        val sourceKeyPair = KeyPair.fromAccountId(transactionData.sourceAddress)

        if (transactionData.amount.type == AmountType.Coin) {
            val operation = if (newtorkManager.checkIsAccountCreated(transactionData.sourceAddress)) {
                PaymentOperation.Builder(destinationKeyPair.accountId,
                        AssetTypeNative(),
                        transactionData.amount.value.toString())
                        .build()
            } else {
                CreateAccountOperation.Builder(destinationKeyPair.accountId, transactionData.amount.value.toString()).build()
            }
            return serializeOperation(operation, sourceKeyPair, sequence, fee)

        } else if (transactionData.amount.type == AmountType.Token) {
            val keyPair = KeyPair.fromAccountId(transactionData.amount.address)
            val asset = Asset.createNonNativeAsset(transactionData.amount.currencySymbol, keyPair.accountId)
            val operation: Operation = if (transactionData.amount.value != null) {
                PaymentOperation.Builder(
                        destinationKeyPair.accountId,
                        asset,
                        transactionData.amount.value!!.toPlainString())
                        .build()
            } else {
                ChangeTrustOperation.Builder(asset, "900000000000.0000000")
                        .setSourceAccount(sourceKeyPair.accountId)
                        .build()
            }
            return serializeOperation(operation, sourceKeyPair, sequence, fee)
        } else {
            return emptyList()
        }
    }

    private fun serializeOperation(
            operation: Operation, sourceKeyPair: KeyPair,
            sequence: Long, fee: Int
    ): List<ByteArray> {

        val accountID = AccountID()
        accountID.accountID = sourceKeyPair.xdrPublicKey
        val currentTime = Calendar.getInstance().timeInMillis / 1000
        val minTime = 0L
        val maxTime = currentTime + 120

        transaction = Transaction.Builder(
                Account(sourceKeyPair.accountId, sequence), newtorkManager.network)
                .addOperation(operation)
                .addTimeBounds(TimeBounds(minTime, maxTime))
                .setOperationFee(fee)
                .build()
        return listOf<ByteArray>(transaction.hash())
    }

    fun buildToSend(signature: ByteArray): String {
        val hint = publicKey.takeLast(4).toByteArray()
        val decoratedSignature = DecoratedSignature().apply {
            this.hint = SignatureHint().apply { signatureHint = hint }
            this.signature = Signature().apply { this.signature = signature }
        }
        transaction.signatures.add(decoratedSignature)
        return transaction.toEnvelopeXdrBase64()
    }
}