package com.tangem.data.tangempay.datasource

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.recover
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toMapKey
import com.tangem.core.error.ext.tangemError
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.domain.card.common.tangempay.TangemPayUtilities
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.tangempay.WithdrawalSignatureResult
import com.tangem.domain.tangempay.datasource.TangemPayRemoteDataSource
import com.tangem.domain.tangempay.error.TangemPayActivationError
import com.tangem.domain.tangempay.error.TangemPayCardScanError
import com.tangem.domain.tangempay.model.TangemPayInitialCredentials
import com.tangem.domain.tangempay.model.TangemPayInitialData
import com.tangem.domain.tangempay.model.MoneyAccountActivationData
import com.tangem.domain.wallets.hot.HotWalletAccessor
import com.tangem.hot.sdk.TangemHotSdk
import com.tangem.hot.sdk.model.DataToSign
import com.tangem.hot.sdk.model.DeriveWalletRequest
import com.tangem.hot.sdk.model.UnlockHotWallet
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import javax.inject.Inject

internal class TangemPayHotSdkManager @Inject constructor(
    private val hotWalletAccessor: HotWalletAccessor,
    private val tangemHotSdk: TangemHotSdk,
    private val tangemPayAuthRemoteDataSource: TangemPayRemoteDataSource,
) {

    suspend fun produceInitialData(hotWallet: UserWallet.Hot): Either<Throwable, TangemPayInitialData> =
        withUnlockedHotWallet(hotWallet) { unlockHotWallet ->
            val initialCredentials = authorize(
                hotWallet = hotWallet,
                unlockHotWallet = unlockHotWallet,
                extendedPublicKey = getExtendedPublicKey(unlockHotWallet = unlockHotWallet),
            )
            val (seedKey, moneyAccountKey) = deriveMoneyAccountKey(unlockHotWallet = unlockHotWallet)

            TangemPayInitialData(
                initialCredentials = initialCredentials,
                moneyAccountDerivedKeys = moneyAccountDerivedKeys(
                    seedKey = seedKey,
                    extendedPublicKey = moneyAccountKey,
                ),
            )
        }

    suspend fun produceMoneyAccountData(hotWallet: UserWallet.Hot): Either<Throwable, MoneyAccountActivationData> =
        withUnlockedHotWallet(hotWallet) { unlockHotWallet ->
            val (seedKey, extendedPublicKey) = deriveMoneyAccountKey(unlockHotWallet = unlockHotWallet)

            val customerWalletKey = getExtendedPublicKey(unlockHotWallet = unlockHotWallet)
            val initialCredentials = authorize(
                hotWallet = hotWallet,
                unlockHotWallet = unlockHotWallet,
                extendedPublicKey = customerWalletKey,
            )

            val address = TangemPayUtilities.generateAddressFromExtendedKey(extendedPublicKey)
            val content = TangemPayUtilities.moneyAccountDepositAddressMessage(depositAddress = address)
            val signature = getSignature(
                unlockHotWallet = unlockHotWallet,
                hash = TangemPayUtilities.hashPersonalMessage(content.toByteArray(Charsets.UTF_8)),
                extendedPublicKey = customerWalletKey,
            )

            MoneyAccountActivationData(
                address = address,
                signature = signature,
                derivedKeys = moneyAccountDerivedKeys(seedKey = seedKey, extendedPublicKey = extendedPublicKey),
                initialCredentials = initialCredentials,
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
    }.recover { error ->
        // Dismissing the password/access-code prompt is an expected outcome, not an error —
        // mirrors the cold-wallet path in DefaultTangemSdkManager.getWithdrawalSignature
        if (error is TangemSdkError.UserCancelled) WithdrawalSignatureResult.Cancelled else raise(error)
    }

    private suspend fun Raise<Throwable>.authorize(
        hotWallet: UserWallet.Hot,
        unlockHotWallet: UnlockHotWallet,
        extendedPublicKey: ExtendedPublicKey,
    ): TangemPayInitialCredentials {
        val address = TangemPayUtilities.generateAddressFromExtendedKey(extendedPublicKey)
        val challenge = tangemPayAuthRemoteDataSource.getCustomerWalletAuthChallenge(
            customerWalletAddress = address,
            customerWalletId = hotWallet.walletId.stringValue,
        ).getOrElse { raise(it.tangemError) }

        val content = TangemPayUtilities.signWithNonceMessage(challenge.challenge)
        val hash = TangemPayUtilities.hashPersonalMessage(content.toByteArray(Charsets.UTF_8))
        val signature = getSignature(
            unlockHotWallet = unlockHotWallet,
            hash = hash,
            extendedPublicKey = extendedPublicKey,
        )

        val authTokens = tangemPayAuthRemoteDataSource.getTokenWithCustomerWallet(
            sessionId = challenge.session.sessionId,
            signature = signature,
            nonce = challenge.challenge,
        ).getOrElse { raise(TangemPayActivationError.FailedRemoteState.tangemError) }

        return TangemPayInitialCredentials(
            customerWalletAddress = address,
            authTokens = authTokens,
        )
    }

    private suspend fun Raise<Throwable>.deriveMoneyAccountKey(
        unlockHotWallet: UnlockHotWallet,
    ): Pair<ByteArrayKey, ExtendedPublicKey> {
        val response = tangemHotSdk.derivePublicKey(
            unlockHotWallet = unlockHotWallet,
            request = DeriveWalletRequest(
                requests = listOf(
                    DeriveWalletRequest.Request(
                        curve = TangemPayUtilities.curve,
                        paths = listOf(TangemPayUtilities.moneyAccountDerivationPath),
                    ),
                ),
            ),
        )
        val curveResponse = response.responses.firstOrNull { it.curve == TangemPayUtilities.curve }
            ?: raise(TangemPayActivationError.MissingWallet.tangemError)
        val extendedPublicKey = curveResponse.publicKeys[TangemPayUtilities.moneyAccountDerivationPath]
            ?: raise(TangemPayActivationError.MissingWallet.tangemError)

        return curveResponse.seedKey.publicKey.toMapKey() to extendedPublicKey
    }

    private fun moneyAccountDerivedKeys(
        seedKey: ByteArrayKey,
        extendedPublicKey: ExtendedPublicKey,
    ): Map<ByteArrayKey, ExtendedPublicKeysMap> {
        return mapOf(
            seedKey to ExtendedPublicKeysMap(mapOf(TangemPayUtilities.moneyAccountDerivationPath to extendedPublicKey)),
        )
    }

    private suspend fun Raise<Throwable>.getExtendedPublicKey(unlockHotWallet: UnlockHotWallet): ExtendedPublicKey {
        val publicKeyResponse = tangemHotSdk.derivePublicKey(
            unlockHotWallet = unlockHotWallet,
            request = DeriveWalletRequest(
                requests = listOf(
                    DeriveWalletRequest.Request(
                        curve = TangemPayUtilities.curve,
                        paths = listOf(TangemPayUtilities.customDerivationPath),
                    ),
                ),
            ),
        )
        return publicKeyResponse.responses
            .firstOrNull { it.curve == TangemPayUtilities.curve }
            ?.publicKeys[TangemPayUtilities.customDerivationPath]
            ?: raise(TangemPayActivationError.MissingWallet.tangemError)
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
                    curve = TangemPayUtilities.curve,
                    derivationPath = TangemPayUtilities.customDerivationPath,
                    hashes = listOf(hash),
                ),
            ),
        )
        val signature = signedHashes
            .firstOrNull { it.curve == TangemPayUtilities.curve }
            ?.signatures
            ?.firstOrNull()
            ?: raise(TangemPayCardScanError.FailedToSignChallenge.tangemError)

        return TangemPayUtilities.unmarshallSignature(
            signature = signature,
            hash = hash,
            extendedPublicKey = extendedPublicKey,
        )
    }

    /**
     * Runs [block] with a contextually unlocked hot wallet.
     *
     * Unlocking may show the password/access-code prompt, and [HotWalletAccessor] signals its dismissal
     * by throwing [TangemSdkError.UserCancelled]. [catch] converts thrown errors into [Either.Left]
     * so the returned [Either] actually holds them (fatal errors and coroutine cancellation still propagate).
     */
    private suspend inline fun <T> withUnlockedHotWallet(
        hotWallet: UserWallet.Hot,
        block: Raise<Throwable>.(UnlockHotWallet) -> T,
    ): Either<Throwable, T> = either {
        catch(
            block = {
                try {
                    val unlockHotWallet = hotWalletAccessor.getContextualUnlock(hotWallet.hotWalletId)
                        ?: hotWalletAccessor.unlockContextual(hotWallet.hotWalletId)
                    block(unlockHotWallet)
                } finally {
                    hotWalletAccessor.clearContextualUnlock(hotWallet.hotWalletId)
                }
            },
            catch = { raise(it) },
        )
    }
}