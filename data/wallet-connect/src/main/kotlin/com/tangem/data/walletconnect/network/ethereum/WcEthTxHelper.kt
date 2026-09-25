package com.tangem.data.walletconnect.network.ethereum

import arrow.core.getOrElse
import com.domain.blockaid.models.transaction.CheckTransactionResult
import com.domain.blockaid.models.transaction.SimulationResult
import com.domain.blockaid.models.transaction.simultation.ApproveInfo
import com.domain.blockaid.models.transaction.simultation.SimulationData
import com.tangem.blockchain.blockchains.ethereum.EthereumTransactionExtras
import com.tangem.blockchain.blockchains.ethereum.tokenmethods.ApprovalERC20TokenCallData
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.HEX_PREFIX
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.smartcontract.CompiledSmartContractCallData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.blockchainsdk.utils.toCoinId
import com.tangem.common.extensions.hexToBytes
import com.tangem.data.common.currency.getCoinId
import com.tangem.domain.account.status.utils.CryptoCurrencyOperations.getCryptoCurrency
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.transaction.usecase.GetEthSpecificFeeUseCase
import com.tangem.domain.walletconnect.model.WcApprovedAmount
import com.tangem.domain.walletconnect.model.WcEthTransactionParams
import java.math.BigInteger
import javax.inject.Inject

internal class WcEthTxHelper @Inject constructor(
    private val singleAccountListSupplier: SingleAccountListSupplier,
    private val ethSpecificFee: GetEthSpecificFeeUseCase,
) {

    suspend fun getDAppFee(txParams: WcEthTransactionParams, userWallet: UserWallet, network: Network): Fee? {
        // A gas / gasPrice the dApp did not encode as a hex QUANTITY is treated as absent: the wallet estimates
        // the fee itself instead of building one from a zero that the lenient parser used to substitute.
        val gasLimit = txParams.gas?.toHexQuantityOrNull() ?: return null
        val gasPrice = txParams.gasPrice?.toHexQuantityOrNull()
        val coinId = getCoinId(network, network.toBlockchain().toCoinId())

        val currency = singleAccountListSupplier.getSyncOrNull(userWalletId = userWallet.walletId)
            .getCryptoCurrency(currencyId = coinId, network = network)
            .getOrElse {
                return null
            }

        return ethSpecificFee(
            userWallet = userWallet,
            cryptoCurrency = currency,
            gasLimit = gasLimit,
            gasPrice = gasPrice,
        )
            .map { it.minimum }
            .getOrNull()
    }

    fun createTransactionData(
        dAppFee: Fee?,
        network: Network,
        txParams: WcEthTransactionParams,
    ): TransactionData.Uncompiled? {
        val destinationAddress = txParams.to ?: return null
        val blockchain = network.toBlockchain()

        // The fields below end up in the signed transaction. They must be what the dApp encoded, or nothing:
        // the lenient `hexToBigDecimal(default = 0)` turned a malformed `value` into 0 and a decimal string into
        // a hex number, and `hexToBytes` silently dropped the last nibble of odd-length `data`.
        val value = when (val rawValue = txParams.value) {
            null -> BigInteger.ZERO
            else -> rawValue.toHexQuantityOrNull() ?: return null
        }
        val rawData = txParams.data
        val callData = when {
            rawData == null || rawData.isEmptyHexData() -> null
            else -> rawData.toHexBytesOrNull()?.let { CompiledSmartContractCallData(it) } ?: return null
        }
        val nonce = when (val rawNonce = txParams.nonce) {
            null -> null
            else -> rawNonce.toHexQuantityOrNull() ?: return null
        }

        return TransactionData.Uncompiled(
            amount = Amount(value.toBigDecimal().movePointLeft(blockchain.decimals()), blockchain),
            fee = dAppFee,
            sourceAddress = txParams.from,
            destinationAddress = destinationAddress,
            extras = EthereumTransactionExtras(
                callData = callData,
                nonce = nonce,
            ),
        )
    }

    fun getApprovedAmount(txData: String?, result: CheckTransactionResult): ApproveInfo.Amount? {
        val approvalMethodId = ApprovalERC20TokenCallData("", null).methodId
        val isApprovalWcMethod = txData?.startsWith(approvalMethodId)
        if (isApprovalWcMethod != true) return null
        val simulation = result.simulation as? SimulationResult.Success
            ?: return null
        val approves = (simulation.data as? SimulationData.Approve)
            ?.items
            ?.filterIsInstance<ApproveInfo.Amount>()
            ?: return null
        if (approves.isEmpty()) return null
        val amount = approves.first()
        return amount
    }

    private companion object {

        /** Ethereum JSON-RPC QUANTITY: `0x` followed by hex digits. A bare `0x` is accepted as zero. */
        fun String.toHexQuantityOrNull(): BigInteger? {
            if (!startsWith(HEX_PREFIX, ignoreCase = true)) return null
            val digits = substring(HEX_PREFIX.length)
            if (digits.isEmpty()) return BigInteger.ZERO
            if (!digits.all { it.isAsciiHexDigit() }) return null
            return BigInteger(digits, HEX_RADIX)
        }

        /** Ethereum JSON-RPC DATA: `0x` followed by an even number of hex digits. */
        fun String.toHexBytesOrNull(): ByteArray? {
            if (!startsWith(HEX_PREFIX, ignoreCase = true)) return null
            val digits = substring(HEX_PREFIX.length)
            if (digits.length % 2 != 0 || !digits.all { it.isAsciiHexDigit() }) return null
            return digits.hexToBytes()
        }

        fun String.isEmptyHexData(): Boolean = isEmpty() || equals(HEX_PREFIX, ignoreCase = true)

        private fun Char.isAsciiHexDigit(): Boolean = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

        private const val HEX_RADIX = 16
    }
}

sealed interface WcEthTxAction {

    data class UpdateFee(val fee: Fee) : WcEthTxAction
    data class UpdateApprovalAmount(val amount: WcApprovedAmount?) : WcEthTxAction
}