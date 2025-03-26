package com.tangem.domain.walletconnect.usecase.ethereum

import com.squareup.moshi.Moshi
import com.tangem.domain.walletconnect.model.WcRequest
import com.tangem.domain.walletconnect.request.WcRequestHandler
import com.tangem.domain.walletconnect.respond.WcRespondService
import com.tangem.domain.walletconnect.usecase.WcUseCase
import com.tangem.domain.walletconnect.usecase.WcUseCasesFlowProvider
import com.tangem.domain.walletconnect.usecase.ethereum.WcEthMethod.SignMessage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

@Suppress("UnusedPrivateClass") // todo(wc) remove later
private class WcEthChain(
    val moshi: Moshi,
    private val respondService: WcRespondService,
) : WcRequestHandler<WcEthMethod>, WcUseCasesFlowProvider {

    private val _flow: Channel<WcUseCase> = Channel(Channel.BUFFERED)
    override val flow = _flow.receiveAsFlow()

    override fun canHandle(methodName: String): Boolean {
        return Name.entries.find { it.raw == methodName } != null
    }

    override fun deserialize(methodName: String, params: String): WcEthMethod? {
        val name = Name.entries.find { it.raw == methodName } ?: return null
        return when (name) {
            Name.Sign -> TODO()
            Name.PersonalSign -> WcRequestHandler.fromJson<SignMessage>(params, moshi)
            Name.SignTypeData -> TODO()
            Name.SignTypeDataV4 -> TODO()
            Name.SignTransaction -> TODO()
            Name.SendTransaction -> TODO()
        }
    }

    override fun handle(wcRequest: WcRequest<WcEthMethod>) {
        val useCase = when (wcRequest.method) {
            is SignMessage -> EthPersonalSignUseCase(wcRequest as WcRequest<SignMessage>, respondService)
        }
        _flow.trySend(useCase)
    }

    enum class Name(val raw: String) {
        Sign("eth_sign"),
        PersonalSign("personal_sign"),
        SignTypeData("eth_signTypedData"),
        SignTypeDataV4("eth_signTypedData_v4"),
        SignTransaction("eth_signTransaction"),
        SendTransaction("eth_sendTransaction"),
    }
}