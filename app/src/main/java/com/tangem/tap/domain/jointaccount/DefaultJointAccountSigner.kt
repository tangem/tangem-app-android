package com.tangem.tap.domain.jointaccount

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import com.tangem.common.CompletionResult
import com.tangem.common.card.EllipticCurve
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.toMapKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.data.wallets.cold.UserWalletIdPreflightReadFilter
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncStrict
import com.tangem.domain.jointaccount.derivation.jointAccountOwnerDerivationPath
import com.tangem.domain.jointaccount.model.JointAccountSignInput
import com.tangem.domain.jointaccount.model.JointAccountSignResult
import com.tangem.domain.jointaccount.signing.CanonicalJson
import com.tangem.domain.jointaccount.signing.JointAccountSigner
import com.tangem.domain.models.MobileWallet
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.derivations.DerivationsRepository
import com.tangem.domain.wallets.hot.HotWalletAccessor
import com.tangem.hot.sdk.TangemHotSdk
import com.tangem.hot.sdk.model.DataToSign
import com.tangem.hot.sdk.model.DeriveWalletRequest
import com.tangem.hot.sdk.model.UnlockHotWallet
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.tap.domain.tasks.jointaccount.JointAccountSignTask
import com.tangem.tap.domain.tasks.jointaccount.JointAccountSigning
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

/**
 * Signs joint account payloads with either wallet kind, asking the user to authorize exactly once.
 *
 * The two kinds cannot share one path. A cold wallet must derive the owner key and sign in a **single** card
 * session — deriving and signing separately would cost the user two taps — so it runs [JointAccountSignTask].
 * A hot wallet has its keys on the device, but every [HotWalletAccessor] operation asks for the password on its
 * own, so deriving and signing through it would cost two prompts — the hot counterpart of two taps. It therefore
 * takes one contextual unlock and spends it on both SDK calls.
 *
 * What both paths must share is the result: the same address, digest and signature encoding — see
 * [JointAccountSigning].
 */
@Suppress("LongParameterList")
internal class DefaultJointAccountSigner @Inject constructor(
    private val userWalletsListRepository: UserWalletsListRepository,
    private val tangemSdkManager: TangemSdkManager,
    private val signTaskFactory: JointAccountSignTask.Factory,
    private val derivationsRepository: DerivationsRepository,
    private val hotWalletAccessor: HotWalletAccessor,
    private val tangemHotSdk: TangemHotSdk,
) : JointAccountSigner {

    override suspend fun sign(
        userWalletId: UserWalletId,
        input: JointAccountSignInput,
    ): Either<Throwable, JointAccountSignResult> = either {
        val result = when (val userWallet = userWalletsListRepository.getSyncStrict(id = userWalletId)) {
            is UserWallet.Cold -> signWithCard(userWallet = userWallet, input = input)
            is UserWallet.Hot -> signWithHotWallet(userWallet = userWallet, input = input)
        }.bind()
        Either
            .catch {
                derivationsRepository.storeDerivedKeys(userWalletId = userWalletId, derivedKeys = result.derivedKeys)
            }
            .bind()

        result
    }

    private suspend fun signWithCard(
        userWallet: UserWallet.Cold,
        input: JointAccountSignInput,
    ): Either<Throwable, JointAccountSignResult> = coroutineScope {
        val result = tangemSdkManager.runTaskAsync(
            runnable = signTaskFactory.create(coroutineScope = this, input = input),
            preflightReadFilter = UserWalletIdPreflightReadFilter(expectedUserWallet = userWallet),
            registerColdWallet = false,
        )

        when (result) {
            is CompletionResult.Failure<*> -> result.error.left()
            is CompletionResult.Success<JointAccountSignResult> -> result.data.right()
        }
    }

    private suspend fun signWithHotWallet(
        userWallet: UserWallet.Hot,
        input: JointAccountSignInput,
    ): Either<Throwable, JointAccountSignResult> {
        val seedWallet = userWallet.wallets.orEmpty().firstOrNull { it.curve == EllipticCurve.Secp256k1 }
            ?: return TangemSdkError.WalletNotFound().left()
        val derivationPath = jointAccountOwnerDerivationPath(index = input.derivationIndex.value)

        return withUnlockedHotWallet(userWallet) { unlock ->
            val ownerKey = deriveOwnerKey(unlock = unlock, seedWallet = seedWallet, derivationPath = derivationPath)

            val ownerAddress = JointAccountSigning.evmAddress(ownerKey = ownerKey)
            val canonicalPayload = CanonicalJson.canonicalize(input.makePayload(ownerAddress).toCanonicalMap())
            val digest = JointAccountSigning.eip191Digest(canonicalPayload = canonicalPayload)

            val signature = tangemHotSdk.signHashes(
                unlockHotWallet = unlock,
                dataToSign = listOf(
                    DataToSign(curve = seedWallet.curve, hashes = listOf(digest), derivationPath = derivationPath),
                ),
            ).first().signatures.first()

            JointAccountSignResult(
                ownerAddress = ownerAddress,
                canonicalPayload = canonicalPayload,
                signature = JointAccountSigning.toRsvHex(signature = signature, digest = digest, ownerKey = ownerKey),
                derivedKeys = mapOf(
                    seedWallet.publicKey.toMapKey() to ExtendedPublicKeysMap(mapOf(derivationPath to ownerKey)),
                ),
            )
        }
    }

    private suspend fun deriveOwnerKey(
        unlock: UnlockHotWallet,
        seedWallet: MobileWallet,
        derivationPath: DerivationPath,
    ): ExtendedPublicKey {
        val request = DeriveWalletRequest(
            requests = listOf(DeriveWalletRequest.Request(curve = seedWallet.curve, paths = listOf(derivationPath))),
        )

        return tangemHotSdk.derivePublicKey(unlockHotWallet = unlock, request = request)
            .responses
            .firstOrNull { it.seedKey.publicKey.contentEquals(seedWallet.publicKey) }
            ?.publicKeys
            ?.get(derivationPath)
            ?: throw TangemSdkError.WalletNotFound()
    }

    private suspend fun <T> withUnlockedHotWallet(
        userWallet: UserWallet.Hot,
        block: suspend (unlock: UnlockHotWallet) -> T,
    ): Either<Throwable, T> = Either.catch {
        try {
            block(hotWalletAccessor.unlockContextual(userWallet.hotWalletId))
        } finally {
            hotWalletAccessor.clearContextualUnlock(userWallet.hotWalletId)
        }
    }
}