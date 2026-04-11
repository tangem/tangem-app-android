package com.tangem.data.walletconnect.network.ton

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import com.squareup.moshi.Moshi
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.data.walletconnect.model.CAIP2
import com.tangem.data.walletconnect.model.NamespaceKey
import com.tangem.data.walletconnect.request.WcRequestToUseCaseConverter
import com.tangem.data.walletconnect.request.WcRequestToUseCaseConverter.Companion.fromJson
import com.tangem.data.walletconnect.sign.WcMethodUseCaseContext
import com.tangem.data.walletconnect.utils.WcNamespaceConverter
import com.tangem.data.walletconnect.utils.WcNetworksConverter
import com.tangem.domain.walletconnect.model.HandleMethodError
import com.tangem.domain.walletconnect.model.WcTonMethod
import com.tangem.domain.walletconnect.model.WcTonMethodName
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import com.tangem.domain.walletconnect.repository.WcSessionsManager
import com.tangem.domain.walletconnect.usecase.method.WcMethodUseCase
import jakarta.inject.Inject

internal class WcTonNetwork(
    private val moshi: Moshi,
    private val sessionsManager: WcSessionsManager,
    private val factories: Factories,
    private val networksConverter: WcNetworksConverter,
) : WcRequestToUseCaseConverter {

    override fun toWcMethodName(request: WcSdkSessionRequest): WcTonMethodName? {
        val methodKey = request.request.method
        return WcTonMethodName.entries.find { it.raw == methodKey }
    }

    @Suppress("CyclomaticComplexMethod")
    override suspend fun toUseCase(request: WcSdkSessionRequest): Either<HandleMethodError, WcMethodUseCase> {
        fun error(message: String) = HandleMethodError.UnknownError(message).left()
        val name = toWcMethodName(request) ?: return error("Unknown method name")
        val method: WcTonMethod = name.toMethod(request)
            .getOrElse { return error(it.message.orEmpty()) }
            ?: return error("Failed to parse $name")
        val session = sessionsManager.findSessionByTopic(request.topic)
            ?: return HandleMethodError.UnknownSession.left()
        val wallet = session.wallet
        val account = session.account
        val chainId = request.chainId.orEmpty()
        suspend fun anyExistNetwork() = networksConverter.mainOrAnyWalletNetworkForRequest(chainId, account)
        suspend fun anyAddress() = anyExistNetwork()
            ?.let { network -> networksConverter.getAddressForWC(wallet.walletId, network).orEmpty() }
            .orEmpty()

        val accountAddress = when (method) {
            is WcTonMethod.SendMessage -> method.from ?: anyAddress()
            is WcTonMethod.SignData -> method.from ?: anyAddress()
        }
        val walletNetwork = networksConverter
            .findWalletNetworkForRequest(request, session, accountAddress)
            ?: anyExistNetwork()
            ?: return error("Failed to find walletNetwork for accountAddress $accountAddress")

        val networkDerivationsCount = networksConverter.filterWalletNetworkForRequest(chainId, account).size
        val context = WcMethodUseCaseContext(
            session = session,
            rawSdkRequest = request,
            network = walletNetwork,
            accountAddress = accountAddress,
            networkDerivationsCount = networkDerivationsCount,
        )
        return when (method) {
            is WcTonMethod.SendMessage -> factories.sendMessage.create(context, method)
            is WcTonMethod.SignData -> factories.signData.create(context, method)
        }.right()
    }

    internal class NamespaceConverter @Inject constructor(
        override val excludedBlockchains: ExcludedBlockchains,
    ) : WcNamespaceConverter {

        override val namespaceKey: NamespaceKey = NamespaceKey("ton")

        override fun toBlockchain(chainId: CAIP2): Blockchain? {
            if (chainId.namespace != namespaceKey.key) return null
            return when (chainId.reference) {
                MAINNET_CHAIN_ID -> Blockchain.TON
                TESTNET_CHAIN_ID -> Blockchain.TONTestnet
                else -> null
            }
        }
    }

    private fun WcTonMethodName.toMethod(request: WcSdkSessionRequest): Either<Throwable, WcTonMethod?> {
        val rawParams = request.request.params
        return when (this) {
            WcTonMethodName.SendMessage -> moshi.fromJson<WcTonSendMessageRequest>(rawParams)
                .getOrElse { return it.left() }
                ?.let { req ->
                    WcTonMethod.SendMessage(
                        validUntil = req.validUntil,
                        from = req.from,
                        messages = req.messages.map { msg ->
                            WcTonMethod.SendMessage.Message(
                                address = msg.address,
                                amount = msg.amount,
                                payload = msg.payload,
                                stateInit = msg.stateInit,
                            )
                        },
                    )
                }
            WcTonMethodName.SignData -> moshi.fromJson<WcTonSignDataRequest>(rawParams)
                .getOrElse { return it.left() }
                ?.let { req ->
                    val type = when (req.type) {
                        "text" -> WcTonMethod.SignData.Type.Text(req.text.orEmpty())
                        "binary" -> WcTonMethod.SignData.Type.Binary(req.bytes.orEmpty())
                        "cell" -> WcTonMethod.SignData.Type.Cell(
                            schema = req.schema.orEmpty(),
                            cell = req.cell.orEmpty(),
                        )
                        else -> return IllegalArgumentException("Unknown ton_signData type: ${req.type}").left()
                    }
                    WcTonMethod.SignData(type = type, from = req.from)
                }
        }.right()
    }

    internal class Factories @Inject constructor(
        val sendMessage: WcTonSendMessageUseCase.Factory,
        val signData: WcTonSignDataUseCase.Factory,
    )

    companion object {
        private const val MAINNET_CHAIN_ID = "-239"
        private const val TESTNET_CHAIN_ID = "-3"
    }
}