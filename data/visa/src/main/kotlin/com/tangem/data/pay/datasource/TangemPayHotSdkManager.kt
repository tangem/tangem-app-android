package com.tangem.data.pay.datasource

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.Raise
import arrow.core.raise.either
import com.tangem.common.extensions.hexToBytes
import com.tangem.core.error.ext.tangemError
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.domain.card.common.visa.VisaUtilities
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.pay.WithdrawalSignatureResult
import com.tangem.domain.visa.datasource.TangemPayRemoteDataSource
import com.tangem.domain.visa.error.VisaActivationError
import com.tangem.domain.visa.error.VisaCardScanError
import com.tangem.domain.visa.model.TangemPayInitialCredentials
import com.tangem.domain.wallets.hot.HotWalletAccessor
import com.tangem.hot.sdk.TangemHotSdk
import com.tangem.hot.sdk.model.DataToSign
import com.tangem.hot.sdk.model.DeriveWalletRequest
import com.tangem.hot.sdk.model.UnlockHotWallet
import javax.inject.Inject

internal class TangemPayHotSdkManager @Inject constructor(
    private val hotWalletAccessor: HotWalletAccessor,
    private val tangemHotSdk: TangemHotSdk,
    private val tangemPayAuthRemoteDataSource: TangemPayRemoteDataSource,
) {

    suspend fun produceInitialCredentials(hotWallet: UserWallet.Hot): Either<Throwable, TangemPayInitialCredentials> =
        withUnlockedHotWallet(hotWallet) { unlockHotWallet ->
            val extendedPublicKey = getExtendedPublicKey(unlockHotWallet = unlockHotWallet)
            val address = VisaUtilities.generateAddressFromExtendedKey(extendedPublicKey)
            val challenge = tangemPayAuthRemoteDataSource.getCustomerWalletAuthChallenge(
                customerWalletAddress = address,
                customerWalletId = hotWallet.walletId.stringValue,
            ).getOrElse { raise(it.tangemError) }

            val content = VisaUtilities.signWithNonceMessage(challenge.challenge)
            val hash = VisaUtilities.hashPersonalMessage(content.toByteArray(Charsets.UTF_8))
            val signature = getSignature(
                unlockHotWallet = unlockHotWallet,
                hash = hash,
                extendedPublicKey = extendedPublicKey,
            )

            val authTokens = tangemPayAuthRemoteDataSource.getTokenWithCustomerWallet(
                sessionId = challenge.session.sessionId,
                signature = signature,
                nonce = challenge.challenge,
            ).getOrElse { raise(VisaActivationError.FailedRemoteState.tangemError) }

            TangemPayInitialCredentials(
                customerWalletAddress = address,
                authTokens = authTokens,
            )
        }

    suspend fun getWithdrawalSignature(
        hotWallet: UserWallet.Hot,
        hash: String,
    ): Either<Throwable, WithdrawalSignatureResult> = withUnlockedHotWallet(hotWallet) { unlockHotWallet ->
        val signature = getSignature(
            unlockHotWallet = unlockHotWallet,
            hash = hash.hexToBytes(),
            extendedPublicKey = getExtendedPublicKey(unlockHotWallet = unlockHotWallet),
        )

        WithdrawalSignatureResult.Success(signature)
    }

    private suspend fun Raise<Throwable>.getExtendedPublicKey(unlockHotWallet: UnlockHotWallet): ExtendedPublicKey {
        val publicKeyResponse = tangemHotSdk.derivePublicKey(
            unlockHotWallet = unlockHotWallet,
            request = DeriveWalletRequest(
                requests = listOf(
                    DeriveWalletRequest.Request(
                        curve = VisaUtilities.curve,
                        paths = listOf(VisaUtilities.customDerivationPath),
                    ),
                ),
            ),
        )
        return publicKeyResponse.responses
            .firstOrNull { it.curve == VisaUtilities.curve }
            ?.publicKeys[VisaUtilities.customDerivationPath]
            ?: raise(VisaActivationError.MissingWallet.tangemError)
    }

    private suspend fun Raise<Throwable>.getSignature(
        unlockHotWallet: UnlockHotWallet,
        hash: ByteArray,
        extendedPublicKey: ExtendedPublicKey,
    ): String {
        val signedHashes = tangemHotSdk.signHashes(
            unlockHotWallet = unlockHotWallet,
            dataToSign = listOf(
                DataToSign(
                    curve = VisaUtilities.curve,
                    derivationPath = VisaUtilities.customDerivationPath,
                    hashes = listOf(hash),
                ),
            ),
        )
        val signature = signedHashes
            .firstOrNull { it.curve == VisaUtilities.curve }
            ?.signatures
            ?.firstOrNull()
            ?: raise(VisaCardScanError.FailedToSignChallenge.tangemError)

        return VisaUtilities.unmarshallSignature(
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
}