package com.tangem.data.walletconnect.pay

import com.reown.walletkit.client.Wallet
import com.reown.walletkit.client.WalletKit
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.UnmarshalHelper
import com.tangem.blockchain.extensions.formatHex
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.common.extensions.toDecompressedPublicKey
import com.tangem.common.extensions.toHexString
import com.tangem.data.common.network.NetworkFactory
import com.tangem.data.walletconnect.network.ethereum.LegacySdkHelper
import com.tangem.data.walletconnect.pay.WcPayModelConverter.toDomain
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.transaction.usecase.SignUseCase
import com.tangem.domain.walletconnect.model.pay.WcPayConfirmResult
import com.tangem.domain.walletconnect.model.pay.WcPayRequiredAction
import com.tangem.domain.walletconnect.model.pay.WcPaymentOptionsResponse
import com.tangem.domain.walletconnect.usecase.pay.WcPayUseCase
import com.tangem.domain.walletmanager.WalletManagersFacade
import javax.inject.Inject

internal class DefaultWcPayUseCase @Inject constructor(
    private val signUseCase: SignUseCase,
    private val walletManagersFacade: WalletManagersFacade,
    excludedBlockchains: ExcludedBlockchains,
) : WcPayUseCase {

    private val networkFactory = NetworkFactory(excludedBlockchains)

    override fun isPaymentLink(uri: String): Boolean {
        return WalletKit.Pay.isPaymentLink(uri)
    }

    override suspend fun getPaymentOptions(
        paymentLink: String,
        accounts: List<String>,
    ): Result<WcPaymentOptionsResponse> {
        return WalletKit.Pay.getPaymentOptions(paymentLink, accounts)
            .map { it.toDomain() }
    }

    override suspend fun getRequiredActions(paymentId: String, optionId: String): Result<List<WcPayRequiredAction>> {
        val params = Wallet.Params.RequiredPaymentActions(
            paymentId = paymentId,
            optionId = optionId,
        )
        return WalletKit.Pay.getRequiredPaymentActions(params)
            .map { actions ->
                actions.filterIsInstance<Wallet.Model.RequiredAction.WalletRpc>()
                    .map { it.toDomain() }
            }
    }

    override suspend fun confirmPayment(
        paymentId: String,
        optionId: String,
        signatures: List<String>,
    ): Result<WcPayConfirmResult> {
        val params = Wallet.Params.ConfirmPayment(
            paymentId = paymentId,
            optionId = optionId,
            signatures = signatures,
        )
        return WalletKit.Pay.confirmPayment(params)
            .map { it.toDomain() }
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun signPayAction(action: WcPayRequiredAction, userWallet: UserWallet): Result<String> {
        return try {
            val network = resolveNetwork(action.chainId, userWallet)
                ?: error("Unsupported chain: ${action.chainId}")

            val hashToSign = when (action.method) {
                METHOD_SIGN_TYPED_DATA_V4 -> {
                    val params = org.json.JSONArray(action.params)
                    val typedData = params.getString(1)
                    EthereumUtils.makeTypedDataHash(typedData)
                }
                METHOD_PERSONAL_SIGN -> {
                    val params = org.json.JSONArray(action.params)
                    val message = params.getString(0)
                    LegacySdkHelper.createMessageData(message)
                }
                else -> error("Unsupported signing method: ${action.method}")
            }

            val signedHash = signUseCase(hashToSign, userWallet, network)
                .fold(
                    ifLeft = { error("Signing failed: ${it.customMessage}") },
                    ifRight = { it },
                )

            val walletManager = walletManagersFacade.getOrCreateWalletManager(userWallet.walletId, network)
                ?: error("WalletManager not found for network: ${network.rawId}")

            val signature = UnmarshalHelper.unmarshalSignatureExtended(
                signature = signedHash,
                hash = hashToSign,
                publicKey = walletManager.wallet.publicKey.blockchainKey.toDecompressedPublicKey(),
            ).asRSVLegacyEVM().toHexString().formatHex().lowercase()

            Result.success(signature)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result.failure(e)
        }
    }

    override suspend fun buildPayAccounts(userWallet: UserWallet): List<String> {
        return PAY_BLOCKCHAINS.mapNotNull { blockchain ->
            val chainId = blockchain.getChainId() ?: return@mapNotNull null
            val network = networkFactory.create(
                blockchain = blockchain,
                extraDerivationPath = null,
                userWallet = userWallet,
            ) ?: return@mapNotNull null

            val address = walletManagersFacade.getDefaultAddress(userWallet.walletId, network)
                ?: return@mapNotNull null

            "eip155:$chainId:$address"
        }
    }

    private fun resolveNetwork(caip2ChainId: String, userWallet: UserWallet): Network? {
        val parts = caip2ChainId.split(":")
        if (parts.size != 2 || parts[0] != "eip155") return null
        val ethChainId = parts[1].toIntOrNull() ?: return null
        val blockchain = Blockchain.fromChainId(ethChainId) ?: return null
        return networkFactory.create(
            blockchain = blockchain,
            extraDerivationPath = null,
            userWallet = userWallet,
        )
    }

    companion object {
        private const val METHOD_SIGN_TYPED_DATA_V4 = "eth_signTypedData_v4"
        private const val METHOD_PERSONAL_SIGN = "personal_sign"

        /** Blockchains supported by WalletConnect Pay */
        private val PAY_BLOCKCHAINS = listOf(
            Blockchain.Ethereum,
            Blockchain.Base,
            Blockchain.Polygon,
            Blockchain.Optimism,
            Blockchain.Arbitrum,
        )
    }
}