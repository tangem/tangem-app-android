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
import com.tangem.blockchain.extensions.hexToBigDecimal
import com.tangem.blockchain.extensions.hexToBigInteger
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.blockchainsdk.utils.toCoinId
import com.tangem.common.extensions.hexToBytes
import com.tangem.data.common.currency.getCoinId
import com.tangem.domain.account.status.utils.CryptoCurrencyOperations.getCryptoCurrency
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.tokens.model.Amount as DomainAmount
import com.tangem.domain.transaction.usecase.GetEthSpecificFeeUseCase
import com.tangem.domain.walletconnect.model.WcApprovedAmount
import com.tangem.domain.walletconnect.model.WcEthTransactionParams
import javax.inject.Inject

internal class WcEthTxHelper @Inject constructor(
    private val singleAccountListSupplier: SingleAccountListSupplier,
    private val ethSpecificFee: GetEthSpecificFeeUseCase,
) {

    suspend fun getDAppFee(txParams: WcEthTransactionParams, userWallet: UserWallet, network: Network): Fee? {
        val gasLimit = txParams.gas?.hexToBigInteger() ?: return null
        val gasPrice = txParams.gasPrice?.hexToBigInteger()
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
        val value = (txParams.value ?: "0")
            .hexToBigDecimal()
            .movePointLeft(blockchain.decimals())

        val callData = txParams.data?.removePrefix(HEX_PREFIX)?.hexToBytes()?.let {
            CompiledSmartContractCallData(it)
        }
        return TransactionData.Uncompiled(
            amount = Amount(value, blockchain),
            fee = dAppFee,
            sourceAddress = txParams.from,
            destinationAddress = destinationAddress,
            extras = EthereumTransactionExtras(
                callData = callData,
                nonce = txParams.nonce?.hexToBigDecimal()?.toBigInteger(),
            ),
        )
    }

    /**
     * The allowance is decoded from the calldata that is going to be signed: BlockAid exposure reflects the balance
     * currently at risk, not the requested allowance, so it's used only for token metadata.
     */
    fun getApprovedAmount(txData: String?, result: CheckTransactionResult): WcApprovedAmount? {
        val approvalCallData = txData?.decodeApprovalCallData() ?: return null
        val simulation = result.simulation as? SimulationResult.Success ?: return null
        val tokenInfo = (simulation.data as? SimulationData.Approve)
            ?.items
            ?.filterIsInstance<ApproveInfo.Amount>()
            ?.firstOrNull()
            ?.tokenInfo
            ?: return null

        return WcApprovedAmount(
            amount = DomainAmount(
                currencySymbol = tokenInfo.symbol,
                decimals = tokenInfo.decimals,
                value = approvalCallData.amount?.value?.movePointLeft(tokenInfo.decimals)?.stripTrailingZeros(),
            ),
            logoUrl = tokenInfo.logoUrl,
            chainId = tokenInfo.chainId,
        )
    }

    private fun String.decodeApprovalCallData(): ApprovalERC20TokenCallData? {
        val bytes = runCatching { removePrefix(HEX_PREFIX).hexToBytes() }.getOrNull() ?: return null
        return ApprovalERC20TokenCallData(bytes)
    }
}

sealed interface WcEthTxAction {

    data class UpdateFee(val fee: Fee) : WcEthTxAction
    data class UpdateApprovalAmount(val amount: WcApprovedAmount?) : WcEthTxAction
}