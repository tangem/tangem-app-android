package com.tangem.data.walletconnect.network.ethereum

import com.squareup.moshi.Moshi
import com.tangem.blockchain.common.Blockchain
import com.tangem.data.walletconnect.model.CAIP2
import com.tangem.data.walletconnect.model.NamespaceKey
import com.tangem.data.walletconnect.request.WcMethodHandler
import com.tangem.data.walletconnect.utils.WcNamespaceConverter
import com.tangem.domain.walletconnect.model.WcMethod
import com.tangem.domain.walletconnect.model.WcRequest
import com.tangem.domain.walletconnect.respond.WcRespondService
import com.tangem.domain.walletconnect.usecase.WcUseCase
import com.tangem.domain.walletconnect.usecase.WcUseCasesFlowProvider
import com.tangem.domain.walletconnect.usecase.ethereum.EthPersonalSignUseCase
import com.tangem.domain.walletconnect.usecase.ethereum.WcEthMethod
import com.tangem.domain.walletconnect.usecase.ethereum.WcEthMethod.SignMessage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

internal class WcEthNetwork(
    private val moshi: Moshi,
    private val respondService: WcRespondService,
) : WcMethodHandler<WcEthMethod>, WcUseCasesFlowProvider, WcNamespaceConverter {

    private val _useCases: Channel<WcUseCase> = Channel(Channel.BUFFERED)
    override val useCases = _useCases.receiveAsFlow()

    override val namespaceKey: NamespaceKey = NamespaceKey("eip155")

    override fun canHandle(methodName: String): Boolean {
        return Name.entries.find { it.raw == methodName } != null
    }

    override fun deserialize(methodName: String, params: String): WcEthMethod? {
        val name = Name.entries.find { it.raw == methodName } ?: return null
        return when (name) {
            Name.Sign -> TODO()
            Name.PersonalSign -> WcMethodHandler.fromJson<SignMessage>(params, moshi)
            Name.SignTypeData -> TODO()
            Name.SignTypeDataV4 -> TODO()
            Name.SignTransaction -> TODO()
            Name.SendTransaction -> TODO()
        }
    }

    override fun handle(wcRequest: WcRequest<WcMethod>) {
        wcRequest as WcRequest<WcEthMethod>
        val useCase = when (wcRequest.method) {
            is SignMessage -> EthPersonalSignUseCase(wcRequest as WcRequest<SignMessage>, respondService)
        }
        _useCases.trySend(useCase)
    }

    enum class Name(val raw: String) {
        Sign("eth_sign"),
        PersonalSign("personal_sign"),
        SignTypeData("eth_signTypedData"),
        SignTypeDataV4("eth_signTypedData_v4"),
        SignTransaction("eth_signTransaction"),
        SendTransaction("eth_sendTransaction"),
    }

    override fun toBlockchain(chainId: CAIP2): Blockchain? {
        if (chainId.namespace != namespaceKey.key) return null
        val ethChainId = chainId.reference.toIntOrNull() ?: return null
        return Blockchain.fromChainId(ethChainId)
    }

    override fun toCAIP2(blockchain: Blockchain): CAIP2? {
        if (!blockchain.isEvm()) return null
        val chainId = blockchain.getChainId() ?: return null
        return CAIP2(
            namespace = namespaceKey.key,
            reference = chainId.toString(),
        )
    }
}