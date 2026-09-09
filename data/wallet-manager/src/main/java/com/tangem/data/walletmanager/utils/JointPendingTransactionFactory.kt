package com.tangem.data.walletmanager.utils

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionStatus
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointOperation
import com.tangem.domain.jointaccount.model.SafeTx
import com.tangem.domain.jointaccount.safe.JointOperationTransferResolver
import com.tangem.domain.jointaccount.supplier.SingleJointAccountListSupplier
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.firstOrNull
import java.math.BigInteger
import java.time.Instant
import java.util.Calendar
import javax.inject.Inject

/**
 * Only an operation that still collects signatures is synthesized: one that reached the threshold is already being
 * broadcast, and its real transaction arrives through the ordinary history. Synthesizing that one too would show the
 * same transfer twice, and the second row would be keyed by an operation id no explorer knows.
 */
internal class JointPendingTransactionFactory @Inject constructor(
    private val jointAccountListSupplier: SingleJointAccountListSupplier,
    private val transferResolver: JointOperationTransferResolver,
) {

    private val sdkTokenConverter = SdkTokenConverter()

    suspend fun create(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
        safeAddress: String,
        blockchain: Blockchain,
    ): TransactionData.Uncompiled? {
        val chainId = blockchain.getChainId()?.toLong() ?: return null
        val account = findAccount(userWalletId = userWalletId, safeAddress = safeAddress) ?: return null
        val operation = account.inFlightOperations[chainId]
            ?.takeIf { it.status == JointOperation.Status.AWAITING_SIGNATURES }
            ?: return null

        val transfer = transferResolver.resolve(
            safeTx = operation.safeTx,
            currencies = listOf(currency),
            network = currency.network,
            safeAddress = safeAddress,
        ) ?: return null

        return TransactionData.Uncompiled(
            amount = amountOf(currency = currency, amountWei = transfer.intent.amountWei, blockchain = blockchain),
            fee = feeOf(safeTx = operation.safeTx, blockchain = blockchain),
            sourceAddress = safeAddress,
            destinationAddress = transfer.intent.recipient,
            status = TransactionStatus.Unconfirmed,
            hash = operation.operationId,
            date = operation.startedAt(),
            contractAddress = (currency as? CryptoCurrency.Token)?.contractAddress,
        )
    }

    private suspend fun findAccount(userWalletId: UserWalletId, safeAddress: String): JointAccount? {
        return jointAccountListSupplier(userWalletId = userWalletId)
            .firstOrNull()
            .orEmpty()
            .firstOrNull { it.safeAddress.equals(safeAddress, ignoreCase = true) }
    }

    private fun amountOf(currency: CryptoCurrency, amountWei: BigInteger, blockchain: Blockchain): Amount {
        val value = amountWei.toBigDecimal().movePointLeft(currency.decimals)

        return when (currency) {
            is CryptoCurrency.Coin -> Amount(value = value, blockchain = blockchain)
            is CryptoCurrency.Token -> Amount(token = sdkTokenConverter.convert(currency), value = value)
        }
    }

    private fun feeOf(safeTx: SafeTx, blockchain: Blockchain): Fee {
        val gasLimit = safeTx.safeTxGas + safeTx.baseGas
        val feeWei = safeTx.gasPrice * gasLimit

        return Fee.Ethereum.Legacy(
            amount = Amount(
                value = feeWei.toBigDecimal().movePointLeft(blockchain.decimals()),
                blockchain = blockchain,
            ),
            gasLimit = gasLimit,
            gasPrice = safeTx.gasPrice,
        )
    }

    private fun JointOperation.startedAt(): Calendar {
        val createdAt = signatures
            .firstOrNull { it.address.equals(initiator, ignoreCase = true) }
            ?.at
            ?.let { at -> runCatching { Instant.parse(at).toEpochMilli() }.getOrNull() }

        return Calendar.getInstance().apply { timeInMillis = createdAt ?: System.currentTimeMillis() }
    }
}