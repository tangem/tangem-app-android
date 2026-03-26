package com.tangem.data.payment.datasource

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.Raise
import arrow.core.raise.either
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.toKeccak
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.UnmarshalHelper
import com.tangem.common.extensions.toDecompressedPublicKey
import com.tangem.common.extensions.toHexString
import com.tangem.core.error.ext.tangemError
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.payment.auth.PaymentRemoteDataSource
import com.tangem.domain.payment.models.auth.PaymentAuthApiError
import com.tangem.domain.payment.models.auth.PaymentAuthConfig
import com.tangem.domain.payment.models.auth.PaymentInitialCredentials
import com.tangem.domain.wallets.hot.HotWalletAccessor
import com.tangem.hot.sdk.TangemHotSdk
import com.tangem.hot.sdk.model.DataToSign
import com.tangem.hot.sdk.model.DeriveWalletRequest
import com.tangem.hot.sdk.model.UnlockHotWallet

internal class PaymentHotWalletSdkManager(
    private val remoteDataSource: PaymentRemoteDataSource,
    private val tangemHotSdk: TangemHotSdk,
    private val hotWalletAccessor: HotWalletAccessor,
) {

    suspend fun produceInitialCredentials(
        hotWallet: UserWallet.Hot,
        config: PaymentAuthConfig,
    ): Either<Throwable, PaymentInitialCredentials> = withUnlockedHotWallet(hotWallet) { unlockHotWallet ->
        val extendedPublicKey = getExtendedPublicKey(unlockHotWallet = unlockHotWallet, config = config)
        val address = generateAddressFromExtendedKey(
            extendedPublicKey = extendedPublicKey,
            blockchain = Blockchain.fromId(config.blockchainId),
        )
        val challenge = remoteDataSource.getCustomerWalletAuthChallenge(
            customerWalletAddress = address,
            customerWalletId = hotWallet.walletId.stringValue,
        ).getOrElse { raise(it.tangemError) }

        val content = config.singMessage(challenge.challenge)
        val hash = hashPersonalMessage(content.toByteArray(Charsets.UTF_8))
        val signature = getSignature(
            unlockHotWallet = unlockHotWallet,
            hash = hash,
            extendedPublicKey = extendedPublicKey,
            config = config,
        )

        val authTokens = remoteDataSource.getTokenWithCustomerWallet(
            sessionId = challenge.session.sessionId,
            signature = signature,
            nonce = challenge.challenge,
        ).getOrElse { raise(PaymentAuthApiError.FailedRemoteState.tangemError) }

        PaymentInitialCredentials(
            customerWalletAddress = address,
            authTokens = authTokens,
        )
    }

    private suspend fun Raise<Throwable>.getExtendedPublicKey(
        unlockHotWallet: UnlockHotWallet,
        config: PaymentAuthConfig,
    ): ExtendedPublicKey {
        val publicKeyResponse = tangemHotSdk.derivePublicKey(
            unlockHotWallet = unlockHotWallet,
            request = DeriveWalletRequest(
                requests = listOf(
                    DeriveWalletRequest.Request(
                        curve = config.curve,
                        paths = listOf(config.customDerivationPath),
                    ),
                ),
            ),
        )
        return publicKeyResponse.responses
            .firstOrNull { it.curve == config.curve }
            ?.publicKeys[config.customDerivationPath]
            ?: raise(PaymentAuthApiError.MissingWallet.tangemError)
    }

    private suspend fun Raise<Throwable>.getSignature(
        unlockHotWallet: UnlockHotWallet,
        hash: ByteArray,
        extendedPublicKey: ExtendedPublicKey,
        config: PaymentAuthConfig,
    ): String {
        val signedHashes = tangemHotSdk.signHashes(
            unlockHotWallet = unlockHotWallet,
            dataToSign = listOf(
                DataToSign(
                    curve = config.curve,
                    derivationPath = config.customDerivationPath,
                    hashes = listOf(hash),
                ),
            ),
        )
        val signature = signedHashes
            .firstOrNull { it.curve == config.curve }
            ?.signatures
            ?.firstOrNull()
            ?: raise(PaymentAuthApiError.FailedToSignChallenge.tangemError)

        return unmarshallSignature(
            signature = signature,
            hash = hash,
            extendedPublicKey = extendedPublicKey,
        )
    }

    private suspend inline fun <Error, T> withUnlockedHotWallet(
        hotWallet: UserWallet.Hot,
        block: Raise<Error>.(UnlockHotWallet) -> T,
    ): Either<Error, T> = either {
        try {
            val unlockHotWallet = hotWalletAccessor.getContextualUnlock(hotWallet.hotWalletId)
                ?: hotWalletAccessor.unlockContextual(hotWallet.hotWalletId)
            block(unlockHotWallet)
        } finally {
            hotWalletAccessor.clearContextualUnlock(hotWallet.hotWalletId)
        }
    }

    private fun generateAddressFromExtendedKey(extendedPublicKey: ExtendedPublicKey, blockchain: Blockchain): String {
        val derivationData = blockchain.makeAddressesFromExtendedPublicKey(
            extendedPublicKey = extendedPublicKey,
            cachedIndex = null,
        )
        return derivationData.address
    }

    private fun unmarshallSignature(
        signature: ByteArray,
        hash: ByteArray,
        extendedPublicKey: ExtendedPublicKey,
    ): String {
        return UnmarshalHelper.unmarshalSignatureExtended(
            signature = signature,
            hash = hash,
            publicKey = extendedPublicKey.publicKey.toDecompressedPublicKey(),
        ).asRSVLegacyEVM().toHexString().lowercase()
    }

    private fun hashPersonalMessage(message: ByteArray): ByteArray {
        val prefix = "\u0019Ethereum Signed Message:\n${message.size}".toByteArray()
        return (prefix + message).toKeccak()
    }

    interface Factory {
        fun create(remoteDataSource: PaymentRemoteDataSource): PaymentHotWalletSdkManager
    }
}