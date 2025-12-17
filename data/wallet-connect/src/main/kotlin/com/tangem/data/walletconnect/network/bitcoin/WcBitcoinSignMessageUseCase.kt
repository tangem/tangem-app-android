package com.tangem.data.walletconnect.network.bitcoin

import arrow.core.left
import com.squareup.moshi.Moshi
import com.tangem.blockchain.blockchains.bitcoin.BitcoinWalletManager
import com.tangem.blockchain.extensions.Result as SdkResult
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.data.walletconnect.respond.WcRespondService
import com.tangem.data.walletconnect.sign.BaseWcSignUseCase
import com.tangem.data.walletconnect.sign.SignCollector
import com.tangem.data.walletconnect.sign.SignStateConverter.toResult
import com.tangem.data.walletconnect.sign.WcMethodUseCaseContext
import com.tangem.datasource.di.SdkMoshi
import com.domain.blockaid.models.transaction.CheckTransactionResult
import com.domain.blockaid.models.transaction.SimulationResult
import com.domain.blockaid.models.transaction.ValidationResult
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.core.lce.LceFlow
import com.tangem.domain.walletconnect.WcTransactionSignerProvider
import com.tangem.domain.walletconnect.model.HandleMethodError
import com.tangem.domain.walletconnect.model.WcBitcoinMethod
import com.tangem.domain.walletconnect.usecase.method.WcMessageSignUseCase
import com.tangem.domain.walletconnect.usecase.method.WcSignState
import com.tangem.domain.walletmanager.WalletManagersFacade
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Use case for Bitcoin signMessage WalletConnect method.
 *
 * Signs an arbitrary message using Bitcoin message signing format (BIP-137 ECDSA).
 */
@Suppress("LongParameterList")
internal class WcBitcoinSignMessageUseCase @AssistedInject constructor(
    @Assisted override val context: WcMethodUseCaseContext,
    @Assisted override val method: WcBitcoinMethod.SignMessage,
    private val walletManagersFacade: WalletManagersFacade,
    private val signerProvider: WcTransactionSignerProvider,
    override val respondService: WcRespondService,
    override val analytics: AnalyticsEventHandler,
    @SdkMoshi private val moshi: Moshi,
) : BaseWcSignUseCase<Nothing, WcMessageSignUseCase.SignModel>(),
    WcMessageSignUseCase {

    override val securityStatus: LceFlow<Throwable, CheckTransactionResult> = flowOf(
        Lce.Content(
            CheckTransactionResult(
                validation = ValidationResult.FAILED_TO_VALIDATE,
                simulation = SimulationResult.FailedToSimulate,
            ),
        ),
    )

    override suspend fun SignCollector<WcMessageSignUseCase.SignModel>.onSign(
        state: WcSignState<WcMessageSignUseCase.SignModel>,
    ) {
        val walletManager = walletManagersFacade.getOrCreateWalletManager(wallet.walletId, network)
        if (walletManager !is BitcoinWalletManager) {
            emit(state.toResult(HandleMethodError.UnknownError("Invalid wallet manager type").left()))
            return
        }

        val signer = signerProvider.createSigner(wallet)
        val signingAddress = method.address ?: method.account

        val signatureResult = signMessage(walletManager, signingAddress, signer, state) ?: return

        respondWithSignature(signatureResult, state)
    }

    private suspend fun SignCollector<WcMessageSignUseCase.SignModel>.signMessage(
        walletManager: BitcoinWalletManager,
        signingAddress: String,
        signer: com.tangem.blockchain.common.TransactionSigner,
        state: WcSignState<WcMessageSignUseCase.SignModel>,
    ): com.tangem.blockchain.common.messagesigning.MessageSignatureResult? {
        return when (val result = walletManager.signMessage(method.message, signingAddress, method.protocol, signer)) {
            is SdkResult.Success -> result.data
            is SdkResult.Failure -> {
                emit(state.toResult(HandleMethodError.UnknownError(result.error.customMessage).left()))
                null
            }
        }
    }

    private suspend fun SignCollector<WcMessageSignUseCase.SignModel>.respondWithSignature(
        signatureResult: com.tangem.blockchain.common.messagesigning.MessageSignatureResult,
        state: WcSignState<WcMessageSignUseCase.SignModel>,
    ) {
        val signMessageResponse = com.tangem.blockchain.blockchains.bitcoin.walletconnect.models.SignMessageResponse(
            address = signatureResult.address,
            signature = signatureResult.signature,
            messageHash = signatureResult.messageHash,
        )
        val response = buildJsonResponse(signMessageResponse)
        val wcRespondResult = respondService.respond(rawSdkRequest, response)
        emit(state.toResult(wcRespondResult))
    }

    override fun invoke(): Flow<WcSignState<WcMessageSignUseCase.SignModel>> {
        return delegate.invoke(initModel = WcMessageSignUseCase.SignModel(method.message))
    }

    private fun buildJsonResponse(
        data: com.tangem.blockchain.blockchains.bitcoin.walletconnect.models.SignMessageResponse,
    ): String {
        return moshi.adapter(
            com.tangem.blockchain.blockchains.bitcoin.walletconnect.models.SignMessageResponse::class.java,
        ).toJson(data)
    }

    @AssistedFactory
    interface Factory {
        fun create(context: WcMethodUseCaseContext, method: WcBitcoinMethod.SignMessage): WcBitcoinSignMessageUseCase
    }
}