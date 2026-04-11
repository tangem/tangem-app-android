package com.tangem.data.walletconnect.network.ton

import arrow.core.left
import com.domain.blockaid.models.transaction.CheckTransactionResult
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.data.walletconnect.respond.WcRespondService
import com.tangem.data.walletconnect.sign.BaseWcSignUseCase
import com.tangem.data.walletconnect.sign.SignCollector
import com.tangem.data.walletconnect.sign.SignStateConverter.toResult
import com.tangem.data.walletconnect.sign.WcMethodUseCaseContext
import com.tangem.domain.core.lce.LceFlow
import com.tangem.domain.transaction.usecase.SignUseCase
import com.tangem.domain.walletconnect.error.parseTangemSdkError
import com.tangem.domain.walletconnect.model.WcTonMethod
import com.tangem.domain.walletconnect.usecase.method.WcMessageSignUseCase
import com.tangem.domain.walletconnect.usecase.method.WcSignState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import okio.ByteString.Companion.decodeBase64
import okio.ByteString.Companion.toByteString

internal class WcTonSignDataUseCase @AssistedInject constructor(
    @Assisted override val context: WcMethodUseCaseContext,
    @Assisted override val method: WcTonMethod.SignData,
    private val signUseCase: SignUseCase,
    override val respondService: WcRespondService,
    override val analytics: AnalyticsEventHandler,
) : BaseWcSignUseCase<Nothing, WcMessageSignUseCase.SignModel>(),
    WcMessageSignUseCase {

    override val securityStatus: LceFlow<Throwable, CheckTransactionResult> = emptyFlow()

    override suspend fun SignCollector<WcMessageSignUseCase.SignModel>.onSign(
        state: WcSignState<WcMessageSignUseCase.SignModel>,
    ) {
        val bytesToSign = when (val type = method.type) {
            is WcTonMethod.SignData.Type.Text -> type.text.toByteArray(Charsets.UTF_8)
            is WcTonMethod.SignData.Type.Binary -> type.bytes.decodeBase64()?.toByteArray() ?: byteArrayOf()
            is WcTonMethod.SignData.Type.Cell -> type.cell.decodeBase64()?.toByteArray() ?: byteArrayOf()
        }

        val signedHash = signUseCase(bytesToSign, wallet, network)
            .onLeft { emit(state.toResult(parseTangemSdkError(it).left())) }
            .getOrNull() ?: return

        val respond = signedHash.toByteString().base64()
        val respondResult = respondService.respond(rawSdkRequest, respond)
        emit(state.toResult(respondResult))
    }

    override fun invoke(): Flow<WcSignState<WcMessageSignUseCase.SignModel>> {
        val humanMsg = when (val type = method.type) {
            is WcTonMethod.SignData.Type.Text -> type.text
            is WcTonMethod.SignData.Type.Binary -> "[Binary data]"
            is WcTonMethod.SignData.Type.Cell -> "[Cell data]"
        }
        return delegate.invoke(initModel = WcMessageSignUseCase.SignModel(humanMsg))
    }

    @AssistedFactory
    interface Factory {
        fun create(context: WcMethodUseCaseContext, method: WcTonMethod.SignData): WcTonSignDataUseCase
    }
}