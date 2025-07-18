package com.tangem.data.walletconnect.network.ethereum

import com.squareup.moshi.Moshi
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.data.walletconnect.model.CAIP2
import com.tangem.data.walletconnect.model.NamespaceKey
import com.tangem.data.walletconnect.request.WcRequestToUseCaseConverter
import com.tangem.data.walletconnect.request.WcRequestToUseCaseConverter.Companion.fromJson
import com.tangem.data.walletconnect.sign.WcMethodUseCaseContext
import com.tangem.data.walletconnect.utils.WcNamespaceConverter
import com.tangem.data.walletconnect.utils.WcNetworksConverter
import com.tangem.domain.models.network.Network
import com.tangem.domain.walletconnect.model.*
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import com.tangem.domain.walletconnect.repository.WcSessionsManager
import com.tangem.domain.walletconnect.usecase.method.WcMethodUseCase
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWallet
import jakarta.inject.Inject

internal class WcEthNetwork(
    private val moshi: Moshi,
    private val sessionsManager: WcSessionsManager,
    private val factories: Factories,
    private val networksConverter: WcNetworksConverter,
    private val walletManagersFacade: WalletManagersFacade,
) : WcRequestToUseCaseConverter {

    override fun toWcMethodName(request: WcSdkSessionRequest): WcEthMethodName? {
        val methodKey = request.request.method
        val name = WcEthMethodName.entries.find { it.raw == methodKey } ?: return null
        return name
    }

    @Suppress("CyclomaticComplexMethod")
    override suspend fun toUseCase(request: WcSdkSessionRequest): WcMethodUseCase? {
        val name = toWcMethodName(request) ?: return null
        val session = sessionsManager.findSessionByTopic(request.topic) ?: return null
        val wallet = session.wallet
        val chainId = request.chainId.orEmpty()
        val method: WcEthMethod = name.toMethod(request, wallet) ?: return null
        suspend fun anyExistNetwork() = networksConverter.mainOrAnyWalletNetworkForRequest(chainId, wallet)

        val accountAddress = when (method) {
            is WcEthMethod.MessageSign -> method.account
            is WcEthMethod.SendTransaction -> method.transaction.from
            is WcEthMethod.SignTransaction -> method.transaction.from
            is WcEthMethod.SignTypedData -> method.account
            is WcEthMethod.AddEthereumChain ->
                anyExistNetwork()
                    ?.let { network -> walletManagersFacade.getDefaultAddress(wallet.walletId, network).orEmpty() }
                    .orEmpty()
        }
        val walletNetwork = when (method) {
            is WcEthMethod.SignTypedData,
            is WcEthMethod.MessageSign,
            is WcEthMethod.SendTransaction,
            is WcEthMethod.SignTransaction,
            -> networksConverter.findWalletNetworkForRequest(request, session, accountAddress)
            is WcEthMethod.AddEthereumChain -> anyExistNetwork()
        } ?: return null

        val context = WcMethodUseCaseContext(
            session = session,
            rawSdkRequest = request,
            network = walletNetwork,
            accountAddress = accountAddress,
        )
        return when (method) {
            is WcEthMethod.MessageSign -> factories.messageSign.create(context, method)
            is WcEthMethod.SendTransaction -> factories.sendTransaction.create(context, method)
            is WcEthMethod.SignTransaction -> factories.signTransaction.create(context, method)
            is WcEthMethod.SignTypedData -> factories.signTypedData.create(context, method)
            is WcEthMethod.AddEthereumChain -> factories.addNetwork.create(context, method)
        }
    }

    private suspend fun WcEthMethodName.toMethod(request: WcSdkSessionRequest, wallet: UserWallet): WcEthMethod? {
        val rawParams = request.request.params
        return when (this) {
            WcEthMethodName.EthSign,
            WcEthMethodName.PersonalSign,
            -> parseMessageSign(rawParams)
            WcEthMethodName.SignTypeData,
            WcEthMethodName.SignTypeDataV4,
            -> parseTypeData(rawParams)
            WcEthMethodName.SignTransaction,
            WcEthMethodName.SendTransaction,
            -> moshi.fromJson<List<WcEthTransactionParams>>(rawParams)
                ?.firstOrNull()
                ?.let {
                    if (this == WcEthMethodName.SignTransaction) {
                        WcEthMethod.SignTransaction(transaction = it)
                    } else {
                        WcEthMethod.SendTransaction(transaction = it)
                    }
                }
            WcEthMethodName.AddEthereumChain -> moshi.fromJson<List<WcEthAddChain>>(rawParams)
                ?.firstOrNull()
                ?.let {
                    val newNetwork = networksConverter.mainOrAnyWalletNetworkForRequest(it.chainId, wallet)
                        ?: return null
                    WcEthMethod.AddEthereumChain(rawChain = it, network = newNetwork)
                }
        }
    }

    private fun WcEthMethodName.parseMessageSign(rawParams: String): WcEthMethod.MessageSign? {
        val list = moshi.fromJson<List<String>>(rawParams) ?: return null
        val accountIndex = if (this == WcEthMethodName.EthSign) 0 else 1
        val messageIndex = if (this == WcEthMethodName.EthSign) 1 else 0
        val account = list.getOrNull(accountIndex) ?: return null
        val message = list.getOrNull(messageIndex) ?: return null
        val humanMsg = LegacySdkHelper.hexToAscii(message).orEmpty()
        return WcEthMethod.MessageSign(account = account, rawMessage = message, humanMsg = humanMsg)
    }

    private fun parseTypeData(params: String): WcEthMethod.SignTypedData? {
        val account = params.substring(params.indexOf("\"") + 1, params.indexOf("\"", startIndex = 2))
        val data = params.substring(params.indexOfFirst { it == '{' }, params.indexOfLast { it == '}' } + 1)
        val parsedParams = moshi.fromJson<WcEthSignTypedDataParams>(data) ?: return null
        return WcEthMethod.SignTypedData(params = parsedParams, account = account, dataForSign = data)
    }

    internal class NamespaceConverter(
        override val excludedBlockchains: ExcludedBlockchains,
    ) : WcNamespaceConverter {

        override val namespaceKey: NamespaceKey = NamespaceKey("eip155")

        override fun toBlockchain(chainId: CAIP2): Blockchain? {
            if (chainId.namespace != namespaceKey.key) return null
            val ethChainId = chainId.reference.toIntOrNull() ?: return null
            return Blockchain.fromChainId(ethChainId)
        }

        override fun toCAIP2(network: Network): CAIP2? {
            val blockchain = network.toBlockchain()
            if (!blockchain.isEvm()) return null
            val chainId = blockchain.getChainId() ?: return null
            return CAIP2(
                namespace = namespaceKey.key,
                reference = chainId.toString(),
            )
        }
    }

    internal class Factories @Inject constructor(
        val messageSign: WcEthMessageSignUseCase.Factory,
        val signTypedData: WcEthSignTypedDataUseCase.Factory,
        val sendTransaction: WcEthSendTransactionUseCase.Factory,
        val signTransaction: WcEthSignTransactionUseCase.Factory,
        val addNetwork: WcEthAddNetworkUseCase.Factory,
    )
}