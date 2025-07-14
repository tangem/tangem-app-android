package com.tangem.data.walletconnect.network.ethereum

import com.domain.blockaid.models.transaction.CheckTransactionResult
import com.domain.blockaid.models.transaction.SimulationResult
import com.domain.blockaid.models.transaction.simultation.ApprovedAmount
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
import com.tangem.domain.models.network.Network
import com.tangem.domain.tokens.GetSingleCryptoCurrencyStatusUseCase
import com.tangem.domain.transaction.usecase.GetEthSpecificFeeUseCase
import com.tangem.domain.walletconnect.model.WcApprovedAmount
import com.tangem.domain.walletconnect.model.WcEthTransactionParams
import com.tangem.domain.wallets.models.UserWallet
import javax.inject.Inject

internal class WcEthTxHelper @Inject constructor(
    private val getSingleCryptoCurrency: GetSingleCryptoCurrencyStatusUseCase,
    private val ethSpecificFee: GetEthSpecificFeeUseCase,
) {

    suspend fun getDAppFee(txParams: WcEthTransactionParams, userWallet: UserWallet, network: Network): Fee? {
        val gasLimit = txParams.gas?.hexToBigInteger() ?: return null
        val gasPrice = txParams.gasPrice?.hexToBigInteger()
        val coinId = getCoinId(network, network.toBlockchain().toCoinId())
        val currency = getSingleCryptoCurrency.invokeMultiWalletSync(userWallet.walletId, coinId)
            .map { it.currency }
            .getOrNull() ?: return null
        return ethSpecificFee(userWallet, currency, gasLimit, gasPrice)
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

    fun getApprovedAmount(txData: String?, result: CheckTransactionResult): ApprovedAmount? {
        val approvalMethodId = ApprovalERC20TokenCallData("", null).methodId
        val isApprovalWcMethod = txData?.startsWith(approvalMethodId)
        if (isApprovalWcMethod != true) return null
        val simulation = result.simulation as? SimulationResult.Success
            ?: return null
        val approves = (simulation.data as? SimulationData.Approve)?.approvedAmounts
            ?: return null
        if (approves.size != 1) return null
        val amount = approves.first()
        return amount
    }
}

sealed interface WcEthTxAction {

    data class UpdateFee(val fee: Fee) : WcEthTxAction
    data class UpdateApprovalAmount(val amount: WcApprovedAmount?) : WcEthTxAction
}