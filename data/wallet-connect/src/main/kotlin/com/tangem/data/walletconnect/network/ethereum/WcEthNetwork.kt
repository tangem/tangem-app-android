package com.tangem.data.walletconnect.network.ethereum

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import com.squareup.moshi.Moshi
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.core.analytics.api.AnalyticsExceptionHandler
import com.tangem.core.analytics.models.ExceptionAnalyticsEvent
import com.tangem.data.walletconnect.model.CAIP2
import com.tangem.data.walletconnect.model.NamespaceKey
import com.tangem.data.walletconnect.request.WcRequestToUseCaseConverter
import com.tangem.data.walletconnect.request.WcRequestToUseCaseConverter.Companion.fromJson
import com.tangem.data.walletconnect.sign.WcMethodUseCaseContext
import com.tangem.data.walletconnect.utils.WcNamespaceConverter
import com.tangem.data.walletconnect.utils.WcNetworksConverter
import com.tangem.domain.walletconnect.model.*
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import com.tangem.domain.walletconnect.repository.WcSessionsManager
import com.tangem.domain.walletconnect.usecase.method.WcMethodUseCase
import jakarta.inject.Inject
import org.json.JSONArray
import org.json.JSONException

internal class WcEthNetwork(
    private val moshi: Moshi,
    private val sessionsManager: WcSessionsManager,
    private val factories: Factories,
    private val networksConverter: WcNetworksConverter,
    private val analyticsExceptionHandler: AnalyticsExceptionHandler,
) : WcRequestToUseCaseConverter {

    override fun toWcMethodName(request: WcSdkSessionRequest): WcEthMethodName? {
        val methodKey = request.request.method
        val name = WcEthMethodName.entries.find { it.raw == methodKey } ?: return null
        return name
    }

    @Suppress("CyclomaticComplexMethod")
    override suspend fun toUseCase(request: WcSdkSessionRequest): Either<HandleMethodError, WcMethodUseCase> {
        fun error(message: String) = HandleMethodError.UnknownError(message).left()
        val name = toWcMethodName(request) ?: return error("Unknown method name")
        val session = sessionsManager.findSessionByTopic(request.topic)
            ?: return HandleMethodError.UnknownSession.left()
        val account = session.account
        val wallet = session.wallet
        val chainId = request.chainId.orEmpty()
        val method: WcEthMethod = name.toMethod(request)
            .getOrElse { return error(it.message.orEmpty()) }
            ?: return error("Failed to parse $name")
        suspend fun anyExistNetwork() = networksConverter.mainOrAnyWalletNetworkForRequest(
            rawChainId = chainId,
            account = account,
        )

        val accountAddress = when (method) {
            is WcEthMethod.MessageSign -> method.account
            is WcEthMethod.SendTransaction -> method.transaction.from
            is WcEthMethod.SignTransaction -> method.transaction.from
            is WcEthMethod.SignTypedData -> method.account
            is WcEthMethod.AddEthereumChain,
            is WcEthMethod.SwitchEthereumChain,
            ->
                anyExistNetwork()
                    ?.let { network -> networksConverter.getAddressForWC(wallet.walletId, network).orEmpty() }
                    .orEmpty()
        }
        val walletNetwork = when (method) {
            is WcEthMethod.SignTypedData,
            is WcEthMethod.MessageSign,
            is WcEthMethod.SendTransaction,
            is WcEthMethod.SignTransaction,
            -> networksConverter.findWalletNetworkForRequest(request, session, accountAddress)
            is WcEthMethod.AddEthereumChain,
            is WcEthMethod.SwitchEthereumChain,
            -> anyExistNetwork()
        } ?: return error("Failed to find walletNetwork for accountAddress $accountAddress")

        val networkDerivationsCount = networksConverter.filterWalletNetworkForRequest(chainId, account).size
        val context = WcMethodUseCaseContext(
            session = session,
            rawSdkRequest = request,
            network = walletNetwork,
            accountAddress = accountAddress,
            networkDerivationsCount = networkDerivationsCount,
        )
        return when (method) {
            is WcEthMethod.MessageSign -> factories.messageSign.create(context, method)
            is WcEthMethod.SendTransaction -> factories.sendTransaction.create(context, method)
            is WcEthMethod.SignTransaction -> factories.signTransaction.create(context, method)
            is WcEthMethod.SignTypedData -> factories.signTypedData.create(context, method)
            is WcEthMethod.AddEthereumChain -> factories.addNetwork.create(context, method)
            is WcEthMethod.SwitchEthereumChain -> factories.switchNetwork.create(context, method)
        }.right()
    }

    private fun WcEthMethodName.toMethod(request: WcSdkSessionRequest): Either<Throwable, WcEthMethod?> {
        val rawParams = request.request.params
        return when (this) {
            WcEthMethodName.EthSign,
            WcEthMethodName.PersonalSign,
            -> parseMessageSign(rawParams)
            WcEthMethodName.SignTypeData,
            WcEthMethodName.SignTypeDataV4,
            -> parseTypeData(request)
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
            WcEthMethodName.AddEthereumChain,
            WcEthMethodName.SwitchEthereumChain,
            -> moshi.fromJson<List<WcEthAddChain>>(rawParams)
                .getOrElse { return it.left() }
                ?.firstOrNull()
                ?.let {
                    if (this == WcEthMethodName.AddEthereumChain) {
                        WcEthMethod.AddEthereumChain(rawChain = it).right()
                    } else {
                        WcEthMethod.SwitchEthereumChain(rawChain = it).right()
                    }
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

    private fun parseTypeData(request: WcSdkSessionRequest): Either<Throwable, WcEthMethod.SignTypedData?> {
        // Params come as a JSON array [account, typedData]. `typedData` may be either a stringified JSON
        // or a raw JSON object, so we read it as a generic element and serialize it back to a JSON string.
        val account: String
        val data: String
        try {
            val paramsArray = JSONArray(request.request.params)
            account = paramsArray.getString(ACCOUNT_INDEX)
            data = paramsArray.get(TYPED_DATA_INDEX).toString()
        } catch (e: JSONException) {
            sendTypedDataParseException(request, e)
            return IllegalArgumentException("Failed to parse typed data params", e).left()
        }
        val parsedParams = moshi.fromJson<WcEthSignTypedDataParams>(data)
            .getOrElse { return it.left() }
            ?: return null.right()
        return WcEthMethod.SignTypedData(params = parsedParams, account = account, dataForSign = data).right()
    }

    /** Reports a malformed `eth_signTypedData` payload to Crashlytics for future analysis. */
    private fun sendTypedDataParseException(request: WcSdkSessionRequest, error: Throwable) {
        analyticsExceptionHandler.sendException(
            ExceptionAnalyticsEvent(
                exception = error,
                params = mapOf(
                    "method" to request.request.method,
                    "dApp_name" to request.dAppMetaData.name,
                    "dApp_url" to request.dAppMetaData.url,
                    "params" to request.request.params.take(PARAMS_LOG_LIMIT),
                ),
            ),
        )
    }

    internal class NamespaceConverter(
        override val excludedBlockchains: ExcludedBlockchains,
    ) : WcNamespaceConverter {

        override val namespaceKey: NamespaceKey = NamespaceKey(ETH_NAMESPACE_KEY)

        override fun toBlockchain(chainId: CAIP2): Blockchain? {
            if (chainId.namespace != namespaceKey.key) return null
            val ethChainId = chainId.reference.toIntOrNull() ?: return null
            return Blockchain.fromChainId(ethChainId)
        }

        companion object {
            const val ETH_NAMESPACE_KEY = "eip155"
        }
    }

    internal class Factories @Inject constructor(
        val messageSign: WcEthMessageSignUseCase.Factory,
        val signTypedData: WcEthSignTypedDataUseCase.Factory,
        val sendTransaction: WcEthSendTransactionUseCase.Factory,
        val signTransaction: WcEthSignTransactionUseCase.Factory,
        val addNetwork: WcEthAddNetworkUseCase.Factory,
        val switchNetwork: WcEthSwitchNetworkUseCase.Factory,
    )

    private companion object {
        const val ACCOUNT_INDEX = 0
        const val TYPED_DATA_INDEX = 1
        const val PARAMS_LOG_LIMIT = 1000
    }
}