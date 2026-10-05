package com.tangem.data.walletmanager.utils

import com.tangem.domain.models.currency.isJoint
import com.tangem.blockchain.blockchains.ethereum.tokenmethods.TransferERC20TokenCallData
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.transactionhistory.models.TransactionHistoryItem
import com.tangem.domain.jointaccount.model.SafeExecution
import com.tangem.domain.jointaccount.model.SafeGasToken
import com.tangem.domain.jointaccount.model.SafeRefund
import com.tangem.domain.jointaccount.model.SafeTx
import com.tangem.domain.jointaccount.safe.SafeExecTransactionDecoder
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.SdkAmount
import com.tangem.domain.models.network.SdkAmountType
import com.tangem.domain.models.network.TxInfo
import com.tangem.utils.extensions.addHexPrefix
import java.math.BigDecimal

/** Reads what a joint account's own Safe did in a history record of [currency] */
internal class SafeExecutionTypeDecoder(
    private val currency: CryptoCurrency,
    private val networkTokens: Set<CryptoCurrency.Token>,
    private val safeAddress: String?,
    private val jointRefundReceiver: String?,
) {

    private val isJointAccount = currency.isJoint

    /** `null` for a record this account's Safe did not produce */
    fun decode(callData: String?, item: TransactionHistoryItem): TxInfo.TransactionType.Joint? =
        decodeSafeExecution(callData = callData, source = item.sourceType, recordFee = item.fee)

    /**
     * The relayer's zero-value call that ran this account's Safe. In the coin history the Safe's internal calls of the
     * same transaction already show what it moved and what it refunded, so the call itself is no event.
     */
    fun isOwnSafeZeroCall(item: TransactionHistoryItem): Boolean {
        if (!isJointAccount || currency !is CryptoCurrency.Coin || safeAddress == null) return false
        val recipient = (item.destinationType as? TransactionHistoryItem.DestinationType.Single)?.addressType?.address
            ?: return false
        return !item.isOutgoing && recipient.equals(safeAddress, ignoreCase = true) && item.amount.value?.signum() == 0
    }

    /**
     * The Safe refunds the relayer with a transfer of the gas currency, the coin or a token, which the history lists
     * as one more outgoing transfer of the same transaction. It is the fee the operation cost, as the gasless fee
     * transfer is; unlike gasless the method is not checked, the providers report the record under different ones.
     */
    fun refund(item: TransactionHistoryItem): TxInfo.TransactionType.GaslessFee? =
        TxInfo.TransactionType.GaslessFee.takeIf { isRefund(item) }

    /** The refund is paid even when the operation failed, and Blockbook gives it the transaction's status */
    fun withRefundStatus(item: TransactionHistoryItem): TransactionHistoryItem {
        val isFailedRefund = item.status == TransactionHistoryItem.TransactionStatus.Failed && isRefund(item)
        return if (isFailedRefund) item.copy(status = TransactionHistoryItem.TransactionStatus.Confirmed) else item
    }

    private fun isRefund(item: TransactionHistoryItem): Boolean {
        if (!isJointAccount || safeAddress == null) return false
        val sender = (item.sourceType as? TransactionHistoryItem.SourceType.Single)?.address ?: return false
        val recipient = (item.destinationType as? TransactionHistoryItem.DestinationType.Single)?.addressType?.address
            ?: return false
        return sender.equals(safeAddress, ignoreCase = true) && recipient.equals(jointRefundReceiver, ignoreCase = true)
    }

    /**
     * The Safe execution in [callData] when this joint account's own Safe ran it: another Safe paying this account
     * lands in its history too. A direct `execTransaction` names no Safe, and its token transfer record is sent by the
     * Safe that ran it.
     */
    private fun readOwnSafeExecution(callData: String?, source: TransactionHistoryItem.SourceType): SafeExecution? {
        if (!isJointAccount) return null
        val execution = SafeExecTransactionDecoder.decode(callData ?: return null) ?: return null
        val executingSafe = execution.safe ?: (source as? TransactionHistoryItem.SourceType.Single)?.address
        return execution.takeIf {
            execution.operation == SafeTx.OPERATION_CALL &&
                safeAddress != null && safeAddress.equals(executingSafe, ignoreCase = true)
        }
    }

    /** Only the token's history still reads the calldata: the coin's history reads the Safe's internal transfers */
    private fun decodeSafeExecution(
        callData: String?,
        source: TransactionHistoryItem.SourceType,
        recordFee: Amount,
    ): TxInfo.TransactionType.Joint? {
        if (currency !is CryptoCurrency.Token) return null
        val execution = readOwnSafeExecution(callData, source) ?: return null
        val transfer = TransferERC20TokenCallData(compiledData = execution.call.data) ?: return null
        val refundCap = refundCapOf(refund = execution.refund, recordFee = recordFee) ?: return null

        return TxInfo.TransactionType.Joint.Transfer(
            recipient = transfer.destination,
            amount = transfer.amount.value?.movePointLeft(currency.decimals) ?: return null,
            refundCap = refundCap,
            signaturesCount = execution.signaturesCount,
        )
    }

    /** In the gas token the operation was signed with; `null` for a token the wallet does not track */
    private fun refundCapOf(refund: SafeRefund, recordFee: Amount): SdkAmount? {
        val maxPayment = refund.maxPayment.toBigDecimal()
        if (refund.isInCoin()) {
            return SdkAmount(
                currencySymbol = recordFee.currencySymbol,
                value = maxPayment.movePointLeft(recordFee.decimals),
                decimals = recordFee.decimals,
                type = SdkAmountType.Coin,
            )
        }
        val gasToken = refund.gasToken.lowercase().addHexPrefix()
        return networkTokens
            .firstOrNull { it.contractAddress.lowercase().addHexPrefix() == gasToken }
            ?.toSdkAmount(rawValue = maxPayment)
    }

    private fun SafeRefund.isInCoin(): Boolean = gasToken.lowercase().addHexPrefix() == SafeGasToken.Coin.address
}

internal fun CryptoCurrency.Token.toSdkAmount(rawValue: BigDecimal?): SdkAmount = SdkAmount(
    currencySymbol = symbol,
    value = rawValue?.movePointLeft(decimals),
    decimals = decimals,
    type = SdkAmountType.Token(contractAddress = contractAddress, id = id.rawCurrencyId?.value),
)