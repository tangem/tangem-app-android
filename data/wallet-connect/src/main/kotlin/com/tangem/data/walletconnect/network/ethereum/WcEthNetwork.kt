package com.tangem.data.walletconnect.network.ethereum

import com.squareup.moshi.Moshi
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.data.walletconnect.model.CAIP2
import com.tangem.data.walletconnect.model.NamespaceKey
import com.tangem.data.walletconnect.request.WcRequestToUseCaseConverter
import com.tangem.data.walletconnect.request.WcRequestToUseCaseConverter.Companion.fromJson
import com.tangem.data.walletconnect.sign.WcMethodUseCaseContext
import com.tangem.data.walletconnect.utils.WcNamespaceConverter
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.walletconnect.model.WcEthMethod
import com.tangem.domain.walletconnect.model.WcEthTransactionParams
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import com.tangem.domain.walletconnect.repository.WcSessionsManager
import com.tangem.domain.walletconnect.usecase.WcMethodUseCase
import com.tangem.domain.wallets.models.UserWallet
import jakarta.inject.Inject

internal class WcEthNetwork(
    private val moshi: Moshi,
    private val excludedBlockchains: ExcludedBlockchains,
    private val sessionsManager: WcSessionsManager,
    private val factories: Factories,
) : WcRequestToUseCaseConverter, WcNamespaceConverter {

    override val namespaceKey: NamespaceKey = NamespaceKey("eip155")

    override suspend fun toUseCase(request: WcSdkSessionRequest): WcMethodUseCase? {
        val methodKey = request.request.method
        val name = Name.entries.find { it.raw == methodKey } ?: return null
        val method: WcEthMethod = name.toMethod(request) ?: return null
        val session = sessionsManager.findSessionByTopic(request.topic) ?: return null
        val network = toNetwork(request.chainId.orEmpty(), session.wallet) ?: return null
        val context = WcMethodUseCaseContext(session = session, rawSdkRequest = request, network = network)
        return when (method) {
            is WcEthMethod.MessageSign -> factories.messageSign.create(context, method)
            is WcEthMethod.SendTransaction -> factories.sendTransaction.create(context, method)
            is WcEthMethod.SignTransaction -> factories.signTransaction.create(context, method)
        }
    }

    private fun Name.toMethod(request: WcSdkSessionRequest): WcEthMethod? {
        val rawParams = request.request.params
        return when (this) {
            Name.EthSign,
            Name.PersonalSign,
            -> moshi.fromJson<List<String>>(rawParams)?.let { list ->
                val accountIndex = if (this == Name.EthSign) 0 else 1
                val messageIndex = if (this == Name.EthSign) 1 else 0
                val account = list.getOrNull(accountIndex) ?: return@let null
                val message = list.getOrNull(messageIndex) ?: return@let null
                WcEthMethod.MessageSign(account = account, message = message)
            }
            Name.SignTypeData -> TODO()
            Name.SignTypeDataV4 -> TODO()
            Name.SignTransaction,
            Name.SendTransaction,
            -> moshi.fromJson<List<WcEthTransactionParams>>(rawParams)
                ?.firstOrNull()
                ?.let {
                    if (this == Name.SignTransaction) {
                        WcEthMethod.SignTransaction(transaction = it)
                    } else {
                        WcEthMethod.SendTransaction(transaction = it)
                    }
                }
        }
    }

    enum class Name(val raw: String) {
        EthSign("eth_sign"),
        PersonalSign("personal_sign"),
        SignTypeData("eth_signTypedData"),
        SignTypeDataV4("eth_signTypedData_v4"),
        SignTransaction("eth_signTransaction"),
        SendTransaction("eth_sendTransaction"),
    }

    override fun toNetwork(chainId: String, wallet: UserWallet): Network? {
        return toNetwork(chainId, wallet, excludedBlockchains)
    }

    override fun toBlockchain(chainId: CAIP2): Blockchain? {
        if (chainId.namespace != namespaceKey.key) return null
        val ethChainId = chainId.reference.toIntOrNull() ?: return null
        return Blockchain.fromChainId(ethChainId)
    }

    override fun toCAIP2(network: Network): CAIP2? {
        val blockchain = Blockchain.fromId(network.id.value)
        if (!blockchain.isEvm()) return null
        val chainId = blockchain.getChainId() ?: return null
        return CAIP2(
            namespace = namespaceKey.key,
            reference = chainId.toString(),
        )
    }

    internal class Factories @Inject constructor(
        val messageSign: DefaultWcEthMessageSignUseCase.Factory,
        val sendTransaction: DefaultWcEthSendTransactionUseCase.Factory,
        val signTransaction: DefaultWcEthSignTransactionUseCase.Factory,
    )
}