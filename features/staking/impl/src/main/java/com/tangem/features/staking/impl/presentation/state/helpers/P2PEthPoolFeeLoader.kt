package com.tangem.features.staking.impl.presentation.state.helpers

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.staking.model.P2PEthPoolIntegration
import com.tangem.domain.staking.model.ethpool.P2PEthPoolUnsignedTx
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.transaction.error.GetFeeError
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class P2PEthPoolFeeLoader @AssistedInject constructor(
    private val transactionCreator: P2PEthPoolTransactionCreator,
    @Assisted private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    @Assisted private val integration: P2PEthPoolIntegration,
) : StakingFeeLoader {

    override suspend fun getFee(
        onStakingFee: (Fee, Boolean) -> Unit,
        onStakingFeeError: (StakingError) -> Unit,
        onApprovalFee: (TransactionFee) -> Unit,
        onFeeError: (GetFeeError) -> Unit,
    ) {
        transactionCreator.createTransaction(cryptoCurrencyStatus).fold(
            ifLeft = onStakingFeeError,
            ifRight = { unsignedTx ->
                val fee = convertUnsignedTxToFee(unsignedTx)
                onStakingFee(fee, false)
            },
        )
    }

    private fun convertUnsignedTxToFee(unsignedTx: P2PEthPoolUnsignedTx): Fee {
        val blockchain = integration.integrationId.blockchain
        val decimals = blockchain.decimals()
        val feeInWei = unsignedTx.gasLimit * unsignedTx.maxFeePerGas
        val feeValue = feeInWei.movePointLeft(decimals)
        return Fee.Common(
            Amount(
                currencySymbol = blockchain.currency,
                value = feeValue,
                decimals = decimals,
            ),
        )
    }

    @AssistedFactory
    interface Factory {

        fun create(cryptoCurrencyStatus: CryptoCurrencyStatus, integration: P2PEthPoolIntegration): P2PEthPoolFeeLoader
    }
}