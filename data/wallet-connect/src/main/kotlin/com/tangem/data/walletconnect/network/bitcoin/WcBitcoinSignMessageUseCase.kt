package com.tangem.data.walletconnect.network.bitcoin

import arrow.core.left
import com.tangem.blockchain.blockchains.bitcoin.BitcoinWalletManager
import com.tangem.blockchain.blockchains.bitcoin.walletconnect.models.SignMessageRequest
import com.tangem.blockchain.extensions.Result as SdkResult
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.data.walletconnect.respond.WcRespondService
import com.tangem.data.walletconnect.sign.BaseWcSignUseCase
import com.tangem.data.walletconnect.sign.SignCollector
import com.tangem.data.walletconnect.sign.SignStateConverter.toResult
import com.tangem.data.walletconnect.sign.WcMethodUseCaseContext
import com.tangem.data.walletconnect.utils.WC_TAG
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
import kotlinx.coroutines.flow.emptyFlow
import timber.log.Timber

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
) : BaseWcSignUseCase<Nothing, WcMessageSignUseCase.SignModel>(),
    WcMessageSignUseCase {

    // BlockAid doesn't support Bitcoin message signing
    override val securityStatus: LceFlow<Throwable, CheckTransactionResult> = kotlinx.coroutines.flow.flowOf(
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
        Timber.tag(WC_TAG).i("Wallet manager type: ${walletManager?.javaClass?.simpleName}")

        if (walletManager !is BitcoinWalletManager) {
            Timber.tag(WC_TAG).e("ERROR: Invalid wallet manager type, expected BitcoinWalletManager")
            emit(state.toResult(HandleMethodError.UnknownError("Invalid wallet manager type").left()))
            return
        }

        Timber.tag(WC_TAG).i("Creating transaction signer...")
        val signer = createTransactionSigner()
        val request = SignMessageRequest(
            account = method.account,
            message = method.message,
            address = method.address,
            protocol = method.protocol,
        )
        Timber.tag(WC_TAG).i("Calling walletManager.walletConnectHandler.signMessage()...")

        when (val result = walletManager.walletConnectHandler.signMessage(request, signer)) {
            is SdkResult.Success -> {
                Timber.tag(WC_TAG).i("Response address: ${result.data.address}")
                Timber.tag(WC_TAG).i("Signature length: ${result.data.signature.length}")
                Timber.tag(WC_TAG).d("Signature: ${result.data.signature}")
                result.data.messageHash?.let {
                    Timber.tag(WC_TAG).i("Message hash: $it")
                }

                val response = buildJsonResponse(result.data)
                Timber.tag(WC_TAG).i("Sending response to WalletConnect...")

                val wcRespondResult = respondService.respond(rawSdkRequest, response)
                Timber.tag(WC_TAG).i("WalletConnect respond result: ${if (wcRespondResult.isRight()) "SUCCESS" else "FAILED"}")
                emit(state.toResult(wcRespondResult))
            }
            is SdkResult.Failure -> {
                Timber.tag(WC_TAG).e("BTC SignMessage FAILED")
                Timber.tag(WC_TAG).e("Error: ${result.error.customMessage}")
                emit(state.toResult(HandleMethodError.UnknownError(result.error.customMessage).left()))
            }
        }
    }

    override fun invoke(): Flow<WcSignState<WcMessageSignUseCase.SignModel>> {
        return delegate.invoke(initModel = WcMessageSignUseCase.SignModel(method.message))
    }

    private fun createTransactionSigner() = signerProvider.createSigner(wallet)

    private fun buildJsonResponse(
        data: com.tangem.blockchain.blockchains.bitcoin.walletconnect.models.SignMessageResponse,
    ): String {
        return buildString {
            append("{")
            append("\"address\":\"${data.address}\",")
            append("\"signature\":\"${data.signature}\"")
            data.messageHash?.let { append(",\"messageHash\":\"$it\"") }
            append("}")
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            context: WcMethodUseCaseContext,
            method: WcBitcoinMethod.SignMessage,
        ): WcBitcoinSignMessageUseCase
    }
}