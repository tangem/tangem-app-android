package com.tangem.data.walletconnect.network.solana

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import com.squareup.moshi.Moshi
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.decodeBase58
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.common.extensions.toHexString
import com.tangem.data.walletconnect.model.CAIP2
import com.tangem.data.walletconnect.model.NamespaceKey
import com.tangem.data.walletconnect.request.WcRequestToUseCaseConverter
import com.tangem.data.walletconnect.request.WcRequestToUseCaseConverter.Companion.fromJson
import com.tangem.data.walletconnect.sign.WcMethodUseCaseContext
import com.tangem.data.walletconnect.utils.WcNamespaceConverter
import com.tangem.data.walletconnect.utils.WcNetworksConverter
import com.tangem.domain.models.network.Network
import com.tangem.domain.walletconnect.model.WcRequestError
import com.tangem.domain.walletconnect.model.WcSolanaMethod
import com.tangem.domain.walletconnect.model.WcSolanaMethodName
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import com.tangem.domain.walletconnect.repository.WcSessionsManager
import com.tangem.domain.walletconnect.usecase.method.WcMethodUseCase
import com.tangem.domain.walletmanager.WalletManagersFacade
import jakarta.inject.Inject

internal class WcSolanaNetwork(
    private val moshi: Moshi,
    private val sessionsManager: WcSessionsManager,
    private val factories: Factories,
    private val networksConverter: WcNetworksConverter,
    private val walletManagersFacade: WalletManagersFacade,
) : WcRequestToUseCaseConverter {

    override fun toWcMethodName(request: WcSdkSessionRequest): WcSolanaMethodName? {
        val methodKey = request.request.method
        val name = WcSolanaMethodName.entries.find { it.raw == methodKey } ?: return null
        return name
    }

    @Suppress("CyclomaticComplexMethod")
    override suspend fun toUseCase(
        request: WcSdkSessionRequest,
    ): Either<WcRequestError.HandleMethodError, WcMethodUseCase> {
        fun error(message: String) = WcRequestError.HandleMethodError(message).left()
        val name = toWcMethodName(request) ?: return error("Unknown method name")
        val method: WcSolanaMethod = name.toMethod(request)
            .getOrElse { return error(it.message.orEmpty()) }
            ?: return error("Failed to parse $name")
        val session = sessionsManager.findSessionByTopic(request.topic)
            ?: return error("Failed to find session for topic ${request.topic}")
        val wallet = session.wallet
        val chainId = request.chainId.orEmpty()
        suspend fun anyExistNetwork() = networksConverter.mainOrAnyWalletNetworkForRequest(chainId, wallet)
        suspend fun anyAddress() = anyExistNetwork()
            ?.let { network -> walletManagersFacade.getDefaultAddress(wallet.walletId, network).orEmpty() }
            .orEmpty()

        val accountAddress = when (method) {
            is WcSolanaMethod.SignAllTransaction -> anyAddress()
            is WcSolanaMethod.SignMessage -> anyAddress()
            is WcSolanaMethod.SignTransaction -> method.address ?: anyAddress()
        }
        val walletNetwork = networksConverter
            .findWalletNetworkForRequest(request, session, accountAddress)
            ?: anyExistNetwork()
            ?: return error("Failed to find walletNetwork for accountAddress $accountAddress")

        val context = WcMethodUseCaseContext(
            session = session,
            rawSdkRequest = request,
            network = walletNetwork,
            accountAddress = accountAddress,
        )
        return when (method) {
            is WcSolanaMethod.SignMessage -> factories.messageSign.create(context, method)
            is WcSolanaMethod.SignTransaction -> factories.signTransaction.create(context, method)
            is WcSolanaMethod.SignAllTransaction -> factories.signAllTransaction.create(context, method)
        }.right()
    }

    internal class NamespaceConverter @Inject constructor(
        override val excludedBlockchains: ExcludedBlockchains,
    ) : WcNamespaceConverter {

        override val namespaceKey: NamespaceKey = NamespaceKey("solana")

        override fun toBlockchain(chainId: CAIP2): Blockchain? {
            if (chainId.namespace != namespaceKey.key) return null
            return when (chainId.reference) {
                MAINNET_CHAIN_ID -> Blockchain.Solana
                TESTNET_CHAIN_ID -> Blockchain.SolanaTestnet
                else -> null
            }
        }

        override fun toCAIP2(network: Network): CAIP2? {
            val blockchain = network.toBlockchain()
            val chainId = when (blockchain) {
                Blockchain.Solana -> MAINNET_CHAIN_ID
                Blockchain.SolanaTestnet -> TESTNET_CHAIN_ID
                else -> null
            }
            chainId ?: return null
            return CAIP2(
                namespace = namespaceKey.key,
                reference = chainId,
            )
        }
    }

    private fun WcSolanaMethodName.toMethod(request: WcSdkSessionRequest): Either<Throwable, WcSolanaMethod?> {
        val rawParams = request.request.params
        return when (this) {
            WcSolanaMethodName.SignMessage -> moshi.fromJson<WcSolanaSignMessageRequest>(rawParams)
                .getOrElse { return it.left() }
                ?.let { request ->
                    val humanMsg = request.message.decodeBase58()?.toHexString().orEmpty()
                    WcSolanaMethod.SignMessage(
                        pubKey = request.publicKey,
                        rawMessage = request.message,
                        humanMsg = humanMsg,
                    )
                }
            WcSolanaMethodName.SignTransaction -> moshi.fromJson<WcSolanaSignTransactionRequest>(rawParams)
                .getOrElse { return it.left() }
                ?.let { request -> WcSolanaMethod.SignTransaction(request.transaction, request.feePayer) }
            WcSolanaMethodName.SendAllTransaction -> moshi.fromJson<List<String>>(rawParams)
                .getOrElse { return it.left() }
                ?.let { list -> WcSolanaMethod.SignAllTransaction(list) }
        }.right()
    }

    internal class Factories @Inject constructor(
        val messageSign: WcSolanaMessageSignUseCase.Factory,
        val signTransaction: WcSolanaSignTransactionUseCase.Factory,
        val signAllTransaction: WcSolanaSignAllTransactionUseCase.Factory,
    )

    companion object {
        private const val MAINNET_CHAIN_ID = "5eykt4UsFv8P8NJdTREpY1vzqKqZKvdp"
        private const val TESTNET_CHAIN_ID = "4uhcVJyU9pJkvQyS88uRDiswHXSCkY3z"
    }
}