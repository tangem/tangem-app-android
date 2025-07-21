package com.tangem.data.walletconnect.network.ethereum

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
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
    override suspend fun toUseCase(
        request: WcSdkSessionRequest,
    ): Either<WcRequestError.HandleMethodError, WcMethodUseCase> {
        fun error(message: String) = WcRequestError.HandleMethodError(message).left()
        val name = toWcMethodName(request) ?: return error("Unknown method name")
        val session = sessionsManager.findSessionByTopic(request.topic)
            ?: return error("Failed to find session for topic ${request.topic}")
        val wallet = session.wallet
        val chainId = request.chainId.orEmpty()
        val method: WcEthMethod = name.toMethod(request, wallet)
            .getOrElse { return error(it.message.orEmpty()) }
            ?: return error("Failed to parse $name")
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
        } ?: return error("Failed to find walletNetwork for accountAddress $accountAddress")

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
        }.right()
    }

    private suspend fun WcEthMethodName.toMethod(
        request: WcSdkSessionRequest,
        wallet: UserWallet,
    ): Either<Throwable, WcEthMethod?> {
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
                .getOrElse { return it.left() }
                ?.firstOrNull()
                ?.let {
                    if (this == WcEthMethodName.SignTransaction) {
                        WcEthMethod.SignTransaction(transaction = it).right()
                    } else {
                        WcEthMethod.SendTransaction(transaction = it).right()
                    }
                }
                ?: return null.right()
            WcEthMethodName.AddEthereumChain -> moshi.fromJson<List<WcEthAddChain>>(rawParams)
                .getOrElse { return it.left() }
                ?.firstOrNull()
                ?.let {
                    val newNetwork = networksConverter
                        .mainOrAnyWalletNetworkForRequest(it.chainId, wallet)
                        ?: return null.right()
                    WcEthMethod.AddEthereumChain(rawChain = it, network = newNetwork).right()
                }
                ?: null.right()
        }
    }

    private fun WcEthMethodName.parseMessageSign(rawParams: String): Either<Throwable, WcEthMethod.MessageSign?> {
        val list = moshi.fromJson<List<String>>(rawParams)
            .getOrElse { return it.left() }
            ?: return null.right()
        val accountIndex = if (this == WcEthMethodName.EthSign) 0 else 1
        val messageIndex = if (this == WcEthMethodName.EthSign) 1 else 0
        val account = list.getOrNull(accountIndex) ?: return null.right()
        val message = list.getOrNull(messageIndex) ?: return null.right()
        val humanMsg = LegacySdkHelper.hexToAscii(message).orEmpty()
        return WcEthMethod.MessageSign(account = account, rawMessage = message, humanMsg = humanMsg).right()
    }

    private fun parseTypeData(params: String): Either<Throwable, WcEthMethod.SignTypedData?> {
        val account = params.substring(params.indexOf("\"") + 1, params.indexOf("\"", startIndex = 2))
        val data = params.substring(params.indexOfFirst { it == '{' }, params.indexOfLast { it == '}' } + 1)
        val parsedParams = moshi.fromJson<WcEthSignTypedDataParams>(data)
            .getOrElse { return it.left() }
            ?: return null.right()
        return WcEthMethod.SignTypedData(params = parsedParams, account = account, dataForSign = data).right()
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