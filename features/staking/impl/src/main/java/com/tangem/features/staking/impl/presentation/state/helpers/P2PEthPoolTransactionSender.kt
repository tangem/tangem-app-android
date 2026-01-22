package com.tangem.features.staking.impl.presentation.state.helpers

import arrow.core.getOrElse
import com.squareup.moshi.Moshi
import com.tangem.blockchain.blockchains.ethereum.models.EthereumCompiledTransaction
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.extensions.formatHex
import com.tangem.common.extensions.toHexString
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.staking.model.P2PEthPoolIntegration
import com.tangem.domain.staking.model.ethpool.P2PEthPoolStakingConfig
import com.tangem.domain.staking.model.ethpool.P2PEthPoolUnsignedTx
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.repositories.P2PEthPoolRepository
import com.tangem.domain.transaction.usecase.PrepareForSendUseCase
import com.tangem.domain.txhistory.usecase.GetExplorerTransactionUrlUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import java.math.BigInteger

@Suppress("LongParameterList")
internal class P2PEthPoolTransactionSender @AssistedInject constructor(
    private val transactionCreator: P2PEthPoolTransactionCreator,
    private val stakingBalanceUpdater: StakingBalanceUpdater.Factory,
    private val prepareForSendUseCase: PrepareForSendUseCase,
    private val getExplorerTransactionUrlUseCase: GetExplorerTransactionUrlUseCase,
    private val p2pEthPoolRepository: P2PEthPoolRepository,
    @Assisted private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    @Assisted private val userWallet: UserWallet,
    @Assisted private val integration: P2PEthPoolIntegration,
) : StakingTransactionSender {

    private val balanceUpdater: StakingBalanceUpdater
        get() = stakingBalanceUpdater.create(cryptoCurrencyStatus, userWallet, integration)

    override suspend fun send(callbacks: StakingTransactionSender.Callbacks) {
        val params = transactionCreator.extractParams(cryptoCurrencyStatus)
            ?: run {
                callbacks.onConstructError(
                    StakingError.DomainError("Invalid state for transaction"),
                )
                return
            }

        val unsignedTx = transactionCreator.createTransaction(cryptoCurrencyStatus).getOrElse { error ->
            callbacks.onConstructError(error)
            return
        }

        val compiledTxJson = createCompiledTransactionJson(unsignedTx, params.sourceAddress)

        val transactionData = TransactionData.Compiled(
            value = TransactionData.Compiled.Data.RawString(compiledTxJson),
        )

        val signedTxBytes = prepareForSendUseCase(
            transactionData = transactionData,
            userWallet = userWallet,
            network = cryptoCurrencyStatus.currency.network,
        ).getOrElse { error ->
            callbacks.onSendError(error)
            return
        }

        val signedTxHex = signedTxBytes.toHexString().lowercase().formatHex()

        p2pEthPoolRepository.broadcastTransaction(
            network = P2PEthPoolStakingConfig.activeNetwork,
            signedTransaction = signedTxHex,
        ).fold(
            ifLeft = { error ->
                callbacks.onConstructError(error)
            },
            ifRight = { broadcastResult ->
                val txUrl = getExplorerTransactionUrlUseCase(
                    txHash = broadcastResult.hash,
                    networkId = cryptoCurrencyStatus.currency.network.id,
                ).getOrNull().orEmpty()

                balanceUpdater.updateAfterTransaction()
                callbacks.onSendSuccess(txUrl)
            },
        )
    }

    private fun createCompiledTransactionJson(unsignedTx: P2PEthPoolUnsignedTx, fromAddress: String): String {
        val compiledTx = EthereumCompiledTransaction(
            from = fromAddress,
            to = unsignedTx.to,
            data = unsignedTx.data,
            value = unsignedTx.value.toBigInteger().toHexString(),
            nonce = unsignedTx.nonce,
            chainId = unsignedTx.chainId,
            gasLimit = unsignedTx.gasLimit.toBigInteger().toHexString(),
            gasPrice = null, // EIP-1559: gasPrice is null
            maxFeePerGas = unsignedTx.maxFeePerGas.toBigInteger().toHexString(),
            maxPriorityFeePerGas = unsignedTx.maxPriorityFeePerGas.toBigInteger().toHexString(),
            type = EIP_1559_TX_TYPE,
        )
        return ethereumCompiledTxAdapter.toJson(compiledTx)
    }

    private fun BigInteger.toHexString(): String {
        val hex = toString(HEX_RADIX)
        val paddedHex = if (hex.length % 2 != 0) "0$hex" else hex
        return HEX_PREFIX + paddedHex
    }

    @AssistedFactory
    interface Factory {
        fun create(
            cryptoCurrencyStatus: CryptoCurrencyStatus,
            userWallet: UserWallet,
            integration: P2PEthPoolIntegration,
        ): P2PEthPoolTransactionSender
    }

    private companion object {
        const val HEX_PREFIX = "0x"
        const val HEX_RADIX = 16
        const val EIP_1559_TX_TYPE = 2

        val ethereumCompiledTxAdapter: com.squareup.moshi.JsonAdapter<EthereumCompiledTransaction> =
            Moshi.Builder().build().adapter(EthereumCompiledTransaction::class.java)
    }
}