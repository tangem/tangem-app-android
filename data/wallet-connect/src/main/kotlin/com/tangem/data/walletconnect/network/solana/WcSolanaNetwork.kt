package com.tangem.data.walletconnect.network.solana

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
import com.tangem.domain.walletconnect.model.WcSolanaMethod
import com.tangem.domain.walletconnect.model.WcSolanaMethodName
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import com.tangem.domain.walletconnect.repository.WcSessionsManager
import com.tangem.domain.walletconnect.usecase.WcMethodUseCase
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.lib.crypto.UserWalletManager
import jakarta.inject.Inject
import timber.log.Timber

internal class WcSolanaNetwork(
    private val moshi: Moshi,
    private val sessionsManager: WcSessionsManager,
    private val factories: Factories,
    private val excludedBlockchains: ExcludedBlockchains,
    private val walletManager: UserWalletManager,
) : WcNamespaceConverter, WcRequestToUseCaseConverter {

    override val namespaceKey: NamespaceKey = NamespaceKey("solana")

    override fun toWcMethodName(request: WcSdkSessionRequest): WcSolanaMethodName? {
        val methodKey = request.request.method
        val name = WcSolanaMethodName.entries.find { it.raw == methodKey } ?: return null
        return name
    }

    override suspend fun toUseCase(request: WcSdkSessionRequest): WcMethodUseCase? {
        val name = toWcMethodName(request) ?: return null
        val method: WcSolanaMethod = name.toMethod(request) ?: return null
        val session = sessionsManager.findSessionByTopic(request.topic) ?: return null
        val network = toNetwork(request.chainId.orEmpty(), session.wallet) ?: return null
        val accountAddress = getAccountAddress(network)
        val context = WcMethodUseCaseContext(
            session = session,
            rawSdkRequest = request,
            network = network,
            accountAddress = accountAddress,
        )
        return when (method) {
            is WcSolanaMethod.SignMessage -> TODO()
            is WcSolanaMethod.SignTransaction -> factories.signTransaction.create(context, method)
            is WcSolanaMethod.SignAllTransaction -> factories.signAllTransaction.create(context, method)
        }
    }

    private suspend fun getAccountAddress(network: Network): String? {
        return try {
            walletManager.getWalletAddress(network.id.value, network.derivationPath.value)
        } catch (exception: Exception) {
            Timber.e(exception)
            null
        }
    }

    override fun toBlockchain(chainId: CAIP2): Blockchain? {
        if (chainId.namespace != namespaceKey.key) return null
        return when (chainId.reference) {
            MAINNET_CHAIN_ID -> Blockchain.Solana
            TESTNET_CHAIN_ID -> Blockchain.SolanaTestnet
            else -> null
        }
    }

    override fun toNetwork(chainId: String, wallet: UserWallet): Network? {
        return toNetwork(chainId, wallet, excludedBlockchains)
    }

    override fun toCAIP2(network: Network): CAIP2? {
        val blockchain = Blockchain.fromId(network.id.value)
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

    private fun WcSolanaMethodName.toMethod(request: WcSdkSessionRequest): WcSolanaMethod? {
        val rawParams = request.request.params
        return when (this) {
            WcSolanaMethodName.SignMessage -> moshi.fromJson<WcSolanaSignMessageRequest>(rawParams)?.let { request ->
                WcSolanaMethod.SignMessage(pubKey = request.publicKey, message = request.message)
            }
            WcSolanaMethodName.SignTransaction -> moshi.fromJson<WcSolanaSignTransactionRequest>(rawParams)
                ?.let { request -> WcSolanaMethod.SignTransaction(request.transaction) }
            WcSolanaMethodName.SendAllTransaction -> moshi.fromJson<List<String>>(rawParams)?.let { list ->
                WcSolanaMethod.SignAllTransaction(list)
            }
        }
    }

    internal class Factories @Inject constructor(
        val signTransaction: DefaultWcSolanaSignTransactionUseCase.Factory,
        val signAllTransaction: DefaultWcSolanaSignAllTransactionUseCase.Factory,
    )

    companion object {
        private const val MAINNET_CHAIN_ID = "5eykt4UsFv8P8NJdTREpY1vzqKqZKvdp"
        private const val TESTNET_CHAIN_ID = "4uhcVJyU9pJkvQyS88uRDiswHXSCkY3z"
    }
}