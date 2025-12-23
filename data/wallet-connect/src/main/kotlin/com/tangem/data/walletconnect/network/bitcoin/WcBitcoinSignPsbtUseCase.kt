package com.tangem.data.walletconnect.network.bitcoin

import arrow.core.left
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.tangem.blockchain.blockchains.bitcoin.walletconnect.models.SignInput
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.extensions.Result as SdkResult
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.data.walletconnect.respond.WcRespondService
import com.tangem.data.walletconnect.sign.BaseWcSignUseCase
import com.tangem.data.walletconnect.sign.SignCollector
import com.tangem.data.walletconnect.sign.SignStateConverter.toResult
import com.tangem.data.walletconnect.sign.WcMethodUseCaseContext
import com.tangem.data.walletconnect.utils.BlockAidVerificationDelegate
import com.tangem.datasource.di.SdkMoshi
import com.tangem.domain.core.lce.LceFlow
import com.tangem.domain.walletconnect.WcTransactionSignerProvider
import com.tangem.domain.walletconnect.model.HandleMethodError
import com.tangem.domain.walletconnect.model.WcBitcoinMethod
import com.tangem.domain.walletconnect.usecase.method.BlockAidTransactionCheck
import com.tangem.domain.walletconnect.usecase.method.WcSignState
import com.tangem.domain.walletconnect.usecase.method.WcTransactionUseCase
import com.tangem.domain.walletmanager.WalletManagersFacade
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Use case for Bitcoin signPsbt WalletConnect method.
 *
 * Signs a Partially Signed Bitcoin Transaction (BIP-174 PSBT) with optional broadcast.
 */
@JsonClass(generateAdapter = true)
internal data class SignPsbtResponse(
    @Json(name = "psbt") val psbt: String,
    @Json(name = "txid") val txid: String? = null,
)

@Suppress("LongParameterList")
internal class WcBitcoinSignPsbtUseCase @AssistedInject constructor(
    @Assisted override val context: WcMethodUseCaseContext,
    @Assisted override val method: WcBitcoinMethod.SignPsbt,
    private val walletManagersFacade: WalletManagersFacade,
    private val signerProvider: WcTransactionSignerProvider,
    override val respondService: WcRespondService,
    override val analytics: AnalyticsEventHandler,
    blockAidDelegate: BlockAidVerificationDelegate,
    @SdkMoshi private val moshi: Moshi,
) : BaseWcSignUseCase<Nothing, TransactionData>(),
    WcTransactionUseCase {

    override val wallet get() = context.session.wallet

    override val securityStatus: LceFlow<Throwable, BlockAidTransactionCheck.Result> =
        blockAidDelegate.getSecurityStatus(
            network = network,
            method = method,
            rawSdkRequest = rawSdkRequest,
            session = session,
            accountAddress = context.accountAddress,
        ).map { lce ->
            lce.map { result -> BlockAidTransactionCheck.Result.Plain(result) }
        }

    override suspend fun SignCollector<TransactionData>.onSign(state: WcSignState<TransactionData>) {
        // Update wallet manager to refresh UTXO data before processing Bitcoin transaction
        walletManagersFacade.update(
            userWalletId = wallet.walletId,
            network = network,
            extraTokens = emptySet(),
        )

        val walletManager = walletManagersFacade.getOrCreateWalletManager(wallet.walletId, network)
            ?: run {
                emit(state.toResult(HandleMethodError.UnknownError("Failed to create wallet manager").left()))
                return
            }

        val signer = signerProvider.createSigner(wallet)
        val signInputs = method.signInputs.map { input ->
            SignInput(
                address = input.address,
                index = input.index,
                sighashTypes = input.sighashTypes,
            )
        }
        val signedPsbtResult = walletManager.signPsbt(
            psbtBase64 = method.psbt,
            signInputs = signInputs,
            signer = signer,
        )

        when (signedPsbtResult) {
            is SdkResult.Success -> {
                val signedPsbt = signedPsbtResult.data
                val txid = if (method.broadcast == true) {
                    when (val broadcastResult = walletManager.broadcastPsbt(signedPsbt)) {
                        is SdkResult.Success -> broadcastResult.data
                        is SdkResult.Failure -> {
                            val error = HandleMethodError.UnknownError(broadcastResult.error.customMessage).left()
                            emit(state.toResult(error))
                            return
                        }
                    }
                } else {
                    null
                }

                val response = buildJsonResponse(signedPsbt, txid)
                val wcRespondResult = respondService.respond(rawSdkRequest, response)
                emit(state.toResult(wcRespondResult))
            }
            is SdkResult.Failure -> {
                emit(state.toResult(HandleMethodError.UnknownError(signedPsbtResult.error.customMessage).left()))
            }
        }
    }

    override fun invoke(): Flow<WcSignState<TransactionData>> {
        val transactionData = TransactionData.Compiled(
            value = TransactionData.Compiled.Data.RawString(method.psbt),
        )
        return delegate.invoke(transactionData)
    }

    private fun buildJsonResponse(signedPsbt: String, txid: String?): String {
        val response = SignPsbtResponse(
            psbt = signedPsbt,
            txid = txid,
        )
        return moshi.adapter(SignPsbtResponse::class.java).toJson(response)
    }

    @AssistedFactory
    interface Factory {
        fun create(context: WcMethodUseCaseContext, method: WcBitcoinMethod.SignPsbt): WcBitcoinSignPsbtUseCase
    }
}