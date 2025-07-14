package com.tangem.data.walletconnect.utils

import com.domain.blockaid.models.transaction.*
import com.tangem.data.walletconnect.sign.BlockAidChainNameConverter
import com.tangem.domain.blockaid.BlockAidVerifier
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.core.lce.LceFlow
import com.tangem.domain.models.network.Network
import com.tangem.domain.walletconnect.model.WcEthMethod
import com.tangem.domain.walletconnect.model.WcMethod
import com.tangem.domain.walletconnect.model.WcSession
import com.tangem.domain.walletconnect.model.WcSolanaMethod
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import javax.inject.Inject

internal class BlockAidVerificationDelegate @Inject constructor(
    private val blockAidVerifier: BlockAidVerifier,
    private val blockAidChainNameConverter: BlockAidChainNameConverter,
) {

    fun getSecurityStatus(
        network: Network,
        method: WcMethod,
        rawSdkRequest: WcSdkSessionRequest,
        session: WcSession,
        accountAddress: String?,
    ): LceFlow<Throwable, CheckTransactionResult> = flow {
        emit(Lce.Loading(partialContent = null))
        val failedResult = CheckTransactionResult(
            validation = ValidationResult.FAILED_TO_VALIDATE,
            simulation = SimulationResult.FailedToSimulate,
        )
        if (accountAddress.isNullOrEmpty()) {
            emit(Lce.Content(failedResult))
            return@flow
        }
        when (method) {
            is WcEthMethod -> TransactionParams.Evm(rawSdkRequest.request.params)
            is WcSolanaMethod.SignAllTransaction -> TransactionParams.Solana(method.transaction)
            is WcSolanaMethod.SignTransaction -> TransactionParams.Solana(listOf(method.transaction))
            is WcSolanaMethod.SignMessage -> {
                // BlockAid doesn't support solana_signMessage
                emit(Lce.Content(failedResult))
                return@flow
            }
            else -> null
        }?.let { params ->
            blockAidVerifier.verifyTransaction(
                TransactionData(
                    chain = blockAidChainNameConverter.convert(network),
                    accountAddress = accountAddress,
                    method = rawSdkRequest.request.method,
                    domainUrl = session.sdkModel.appMetaData.url,
                    params = params,
                ),
            ).fold(
                ifLeft = {
                    Timber.e("Failed to verify transaction: ${it.localizedMessage}")
                    emit(Lce.Error(it))
                },
                ifRight = {
                    emit(Lce.Content(it))
                },
            )
        } ?: emit(Lce.Content(failedResult))
    }
}