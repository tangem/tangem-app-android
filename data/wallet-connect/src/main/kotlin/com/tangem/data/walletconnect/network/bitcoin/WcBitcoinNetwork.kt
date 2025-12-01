package com.tangem.data.walletconnect.network.bitcoin

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
import com.tangem.data.walletconnect.utils.WC_TAG
import com.tangem.data.walletconnect.utils.WcNamespaceConverter
import com.tangem.data.walletconnect.utils.WcNetworksConverter
import com.tangem.domain.walletconnect.model.HandleMethodError
import com.tangem.domain.walletconnect.model.WcBitcoinMethod
import com.tangem.domain.walletconnect.model.WcBitcoinMethodName
import com.tangem.domain.walletconnect.model.sdkcopy.WcSdkSessionRequest
import com.tangem.domain.walletconnect.repository.WcSessionsManager
import com.tangem.domain.walletconnect.usecase.method.WcMethodUseCase
import com.tangem.domain.walletmanager.WalletManagersFacade
import jakarta.inject.Inject
import timber.log.Timber

/**
 * WalletConnect request handler for Bitcoin blockchain.
 *
 * Handles Bitcoin-specific RPC methods: sendTransfer, getAccountAddresses, signPsbt, signMessage.
 *
 * @see <a href="https://docs.reown.com/advanced/multichain/rpc-reference/bitcoin-rpc">Bitcoin RPC Reference</a>
 */
internal class WcBitcoinNetwork(
    private val moshi: Moshi,
    private val sessionsManager: WcSessionsManager,
    private val factories: Factories,
    private val networksConverter: WcNetworksConverter,
    private val walletManagersFacade: WalletManagersFacade,
) : WcRequestToUseCaseConverter {

    override fun toWcMethodName(request: WcSdkSessionRequest): WcBitcoinMethodName? {
        val methodKey = request.request.method
        return WcBitcoinMethodName.entries.find { it.raw == methodKey }
    }

    @Suppress("CyclomaticComplexMethod")
    override suspend fun toUseCase(request: WcSdkSessionRequest): Either<HandleMethodError, WcMethodUseCase> {
        fun error(message: String) = HandleMethodError.UnknownError(message).left()
        val name = toWcMethodName(request) ?: return error("Unknown method name").also {
            Timber.tag(WC_TAG).e("ERROR: Unknown BTC method name ══")
        }
        Timber.tag(WC_TAG).i("Method name: $name")

        val method: WcBitcoinMethod = name.toMethod(request)
            .getOrElse {
                Timber.tag(WC_TAG).e("ERROR: Failed to parse method: ${it.message} ══")
                return error(it.message.orEmpty())
            }
            ?: return error("Failed to parse $name").also {
                Timber.tag(WC_TAG).e("ERROR: Method parsing returned null ══")
            }

        Timber.tag(WC_TAG).i("Parsed method: ${method.javaClass.simpleName}")
        when (method) {
            is WcBitcoinMethod.SignMessage -> {
                Timber.tag(WC_TAG).i("SignMessage detected")
                Timber.tag(WC_TAG).i("  Account: ${method.account}")
                Timber.tag(WC_TAG).i("  Address: ${method.address}")
                Timber.tag(WC_TAG).i("  Protocol: ${method.protocol}")
                Timber.tag(WC_TAG).d("  Message: ${method.message.take(50)}...")
            }
            is WcBitcoinMethod.SignPsbt -> {
                Timber.tag(WC_TAG).i("→ SignPsbt detected")
                Timber.tag(WC_TAG).i("  Sign inputs count: ${method.signInputs.size}")
            }
            is WcBitcoinMethod.SendTransfer -> {
                Timber.tag(WC_TAG).i("→ SendTransfer detected")
                Timber.tag(WC_TAG).i("  Amount: ${method.amount}")
            }
            is WcBitcoinMethod.GetAccountAddresses -> {
                Timber.tag(WC_TAG).i("GetAccountAddresses detected")
            }
        }

        val session = sessionsManager.findSessionByTopic(request.topic)
            ?: return HandleMethodError.UnknownSession.left().also {
                Timber.tag(WC_TAG).e("ERROR: Session not found for topic ══")
            }

        Timber.tag(WC_TAG).i("Session found: ${session.sdkModel.appMetaData?.name}")
        val wallet = session.wallet
        Timber.tag(WC_TAG).i("Wallet: ${wallet.name}")
        val chainId = request.chainId.orEmpty()
        Timber.tag(WC_TAG).i("Chain ID: $chainId")

        suspend fun anyExistNetwork() = networksConverter.mainOrAnyWalletNetworkForRequest(chainId, wallet)
        suspend fun anyAddress() = anyExistNetwork()
            ?.let { network -> walletManagersFacade.getDefaultAddress(wallet.walletId, network).orEmpty() }
            .orEmpty()

        val accountAddress = when (method) {
            is WcBitcoinMethod.SendTransfer -> method.account
            is WcBitcoinMethod.GetAccountAddresses -> method.account
            is WcBitcoinMethod.SignPsbt -> method.signInputs.firstOrNull()?.address ?: anyAddress()
            is WcBitcoinMethod.SignMessage -> method.address ?: method.account
        }
        Timber.tag(WC_TAG).i("Account address: $accountAddress")

        val walletNetwork = networksConverter
            .findWalletNetworkForRequest(request, session, accountAddress)
            ?: anyExistNetwork()
            ?: return error("Failed to find walletNetwork for accountAddress $accountAddress").also {
                Timber.tag(WC_TAG).e("ERROR: Wallet network not found")
            }

        Timber.tag(WC_TAG).i("Wallet network: ${walletNetwork.name}")

        val context = WcMethodUseCaseContext(
            session = session,
            rawSdkRequest = request,
            network = walletNetwork,
            accountAddress = accountAddress,
            networkDerivationsCount = networksConverter.filterWalletNetworkForRequest(chainId, wallet).size,
        )

        Timber.tag(WC_TAG).i("Creating use case...")
        val useCase = when (method) {
            is WcBitcoinMethod.SendTransfer -> factories.sendTransfer.create(context, method)
            is WcBitcoinMethod.GetAccountAddresses -> factories.getAccountAddresses.create(context, method)
            is WcBitcoinMethod.SignPsbt -> factories.signPsbt.create(context, method)
            is WcBitcoinMethod.SignMessage -> factories.signMessage.create(context, method)
        }
        Timber.tag(WC_TAG).i("Use case created: ${useCase.javaClass.simpleName}")
        return useCase.right()
    }

    private fun WcBitcoinMethodName.toMethod(request: WcSdkSessionRequest): Either<Throwable, WcBitcoinMethod?> {
        val rawParams = request.request.params
        return when (this) {
            WcBitcoinMethodName.SendTransfer -> moshi.fromJson<WcBitcoinSendTransferRequest>(rawParams)
                .getOrElse { return it.left() }
                ?.let { req ->
                    WcBitcoinMethod.SendTransfer(
                        account = req.account,
                        recipientAddress = req.recipientAddress,
                        amount = req.amount,
                        memo = req.memo,
                        changeAddress = req.changeAddress,
                    )
                }
            WcBitcoinMethodName.GetAccountAddresses -> moshi.fromJson<WcBitcoinGetAccountAddressesRequest>(rawParams)
                .getOrElse { return it.left() }
                ?.let { req ->
                    WcBitcoinMethod.GetAccountAddresses(
                        account = req.account.orEmpty(),
                        intentions = req.intentions,
                    )
                }
            WcBitcoinMethodName.SignPsbt -> moshi.fromJson<WcBitcoinSignPsbtRequest>(rawParams)
                .getOrElse { return it.left() }
                ?.let { req ->
                    WcBitcoinMethod.SignPsbt(
                        psbt = req.psbt,
                        signInputs = req.signInputs.map { input ->
                            WcBitcoinMethod.SignInput(
                                address = input.address,
                                index = input.index,
                                sighashTypes = input.sighashTypes,
                            )
                        },
                        broadcast = req.broadcast ?: false,
                    )
                }
            WcBitcoinMethodName.SignMessage -> moshi.fromJson<WcBitcoinSignMessageRequest>(rawParams)
                .getOrElse { return it.left() }
                ?.let { req ->
                    WcBitcoinMethod.SignMessage(
                        account = req.account,
                        message = req.message,
                        address = req.address,
                        protocol = req.protocol ?: "ecdsa",
                    )
                }
        }.right()
    }

    /**
     * Bitcoin namespace converter for CAIP-2 chain IDs.
     *
     * Bitcoin uses BIP-122 namespace with genesis block hash as reference.
     * Example: bip122:000000000019d6689c085ae165831e934ff763ae46a2a6c172b3f1b60a8ce26f
     */
    internal class NamespaceConverter @Inject constructor(
        override val excludedBlockchains: ExcludedBlockchains,
    ) : WcNamespaceConverter {

        override val namespaceKey: NamespaceKey = NamespaceKey(NAMESPACE)

        override fun toBlockchain(chainId: CAIP2): Blockchain? {
            if (chainId.namespace != namespaceKey.key) return null
            return when {
                isMainnetReference(chainId.reference) -> Blockchain.Bitcoin
                isTestnetReference(chainId.reference) -> Blockchain.BitcoinTestnet
                else -> null
            }
        }

        private fun isMainnetReference(reference: String): Boolean {
            return MAINNET_GENESIS_PREFIX.any { reference.startsWith(it, ignoreCase = true) } ||
                reference.equals("mainnet", ignoreCase = true)
        }

        private fun isTestnetReference(reference: String): Boolean {
            return TESTNET_GENESIS_PREFIX.any { reference.startsWith(it, ignoreCase = true) } ||
                reference.equals("testnet", ignoreCase = true)
        }

        companion object {
            private const val NAMESPACE = "bip122"

            // Bitcoin mainnet genesis hash prefixes (supports any truncated version)
            private val MAINNET_GENESIS_PREFIX = listOf(
                "000000000019d6689c085ae165831e93", // Mainnet genesis hash prefix (min 32 chars for uniqueness)
            )

            // Bitcoin testnet genesis hash prefixes (supports any truncated version)
            private val TESTNET_GENESIS_PREFIX = listOf(
                "000000000933ea01ad0ee984209779ba", // Standard testnet genesis hash prefix (9 leading zeros)
                "0000000000933ea01ad0ee984209779ba", // Alternative testnet prefix (10 leading zeros, as seen in some dApps)
            )
        }
    }

    /**
     * Factory classes for creating Bitcoin WalletConnect use cases.
     */
    internal class Factories @Inject constructor(
        val sendTransfer: WcBitcoinSendTransferUseCase.Factory,
        val getAccountAddresses: WcBitcoinGetAccountAddressesUseCase.Factory,
        val signPsbt: WcBitcoinSignPsbtUseCase.Factory,
        val signMessage: WcBitcoinSignMessageUseCase.Factory,
    )

    companion object {
        private const val NAMESPACE = "bip122"
    }
}