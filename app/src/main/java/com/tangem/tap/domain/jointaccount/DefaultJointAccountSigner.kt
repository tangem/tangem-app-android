package com.tangem.tap.domain.jointaccount

import arrow.core.Either
import arrow.core.left
import arrow.core.flatMap
import arrow.core.raise.either
import arrow.core.right
import com.tangem.blockchain.common.Wallet
import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.extensions.toMapKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import com.tangem.data.wallets.hot.TangemHotWalletSigner
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncStrict
import com.tangem.domain.jointaccount.derivation.jointAccountOwnerDerivationPath
import com.tangem.domain.jointaccount.model.JointAccountSignInput
import com.tangem.domain.jointaccount.model.JointAccountSignResult
import com.tangem.domain.jointaccount.signing.CanonicalJson
import com.tangem.domain.jointaccount.signing.JointAccountSigner
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.usecase.primarySecp256k1PublicKey
import com.tangem.domain.wallets.derivations.DerivationsRepository
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.tap.domain.tasks.UserWalletIdPreflightReadFilter
import com.tangem.tap.domain.tasks.jointaccount.JointAccountSignTask
import com.tangem.tap.domain.tasks.jointaccount.JointAccountSigning
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

/**
 * Signs joint account payloads with either wallet kind.
 *
 * The two kinds cannot share one path. A cold wallet must derive the owner key and sign in a **single** card
 * session — deriving and signing separately would cost the user two taps — so it runs [JointAccountSignTask].
 * A hot wallet has its keys on the device: it derives without any user interaction and then signs with the
 * derived key, so it needs no task at all.
 *
 * What both paths must share is the result: the same address, digest and signature encoding — see
 * [JointAccountSigning].
 */
internal class DefaultJointAccountSigner @Inject constructor(
    private val userWalletsListRepository: UserWalletsListRepository,
    private val tangemSdkManager: TangemSdkManager,
    private val signTaskFactory: JointAccountSignTask.Factory,
    private val derivationsRepository: DerivationsRepository,
    private val hotSignerFactory: TangemHotWalletSigner.Factory,
) : JointAccountSigner {

    override suspend fun sign(
        userWalletId: UserWalletId,
        input: JointAccountSignInput,
    ): Either<Throwable, JointAccountSignResult> {
        return when (val userWallet = userWalletsListRepository.getSyncStrict(id = userWalletId)) {
            is UserWallet.Cold -> signWithCard(userWalletId = userWalletId, input = input)
            is UserWallet.Hot -> signWithHotWallet(userWallet = userWallet, input = input)
        }
    }

    private suspend fun signWithCard(
        userWalletId: UserWalletId,
        input: JointAccountSignInput,
    ): Either<Throwable, JointAccountSignResult> = coroutineScope {
        val result = tangemSdkManager.runTaskAsync(
            runnable = signTaskFactory.create(coroutineScope = this, input = input),
            preflightReadFilter = UserWalletIdPreflightReadFilter(expectedUserWalletId = userWalletId),
        )

        when (result) {
            is CompletionResult.Failure<*> -> result.error.left()
            is CompletionResult.Success<JointAccountSignResult> -> Either
                .catch {
                    // The session's in-memory copy of the derived key dies with the session
                    derivationsRepository.storeDerivedKeys(
                        userWalletId = userWalletId,
                        derivedKeys = result.data.derivedKeys,
                    )

                    result.data
                }
        }
    }

    private suspend fun signWithHotWallet(
        userWallet: UserWallet.Hot,
        input: JointAccountSignInput,
    ): Either<Throwable, JointAccountSignResult> = either {
        val seedKey = userWallet.primarySecp256k1PublicKey()
            ?: raise(TangemSdkError.WalletNotFound())
        val derivationPath = jointAccountOwnerDerivationPath(index = input.derivationIndex.value)
        val ownerKey = deriveOwnerKey(
            userWalletId = userWallet.walletId,
            seedKey = seedKey,
            derivationPath = derivationPath,
        ).bind()

        val ownerAddress = JointAccountSigning.evmAddress(ownerKey = ownerKey)
        val canonicalPayload = CanonicalJson.canonicalize(input.makePayload(ownerAddress).toCanonicalMap())
        val digest = JointAccountSigning.eip191Digest(canonicalPayload = canonicalPayload)

        val signature = signDigest(
            userWallet = userWallet,
            digest = digest,
            seedKey = seedKey,
            ownerKey = ownerKey,
            derivationPath = derivationPath,
        ).bind()

        JointAccountSignResult(
            ownerAddress = ownerAddress,
            canonicalPayload = canonicalPayload,
            signature = JointAccountSigning.toRsvHex(
                signature = signature,
                digest = digest,
                ownerKey = ownerKey,
            ),
            // Derived and persisted by the repository below, so nothing is left for the caller to store
            derivedKeys = mapOf(
                seedKey.toMapKey() to ExtendedPublicKeysMap(mapOf(derivationPath to ownerKey)),
            ),
        )
    }

    /** Local derivation: no user interaction, and the repository persists the key on the way out. */
    private suspend fun deriveOwnerKey(
        userWalletId: UserWalletId,
        seedKey: ByteArray,
        derivationPath: DerivationPath,
    ): Either<Throwable, ExtendedPublicKey> = Either
        .catch {
            derivationsRepository.derivePublicKeys(
                userWalletId = userWalletId,
                derivations = mapOf(ByteArrayKey(seedKey) to listOf(derivationPath)),
            )
        }
        .flatMap { derived ->
            derived[ByteArrayKey(seedKey)]?.get(derivationPath)?.right()
                ?: TangemSdkError.WalletNotFound().left()
        }

    private suspend fun signDigest(
        userWallet: UserWallet.Hot,
        digest: ByteArray,
        seedKey: ByteArray,
        ownerKey: ExtendedPublicKey,
        derivationPath: DerivationPath,
    ): Either<Throwable, ByteArray> {
        val publicKey = Wallet.PublicKey(
            seedKey = seedKey,
            derivationType = Wallet.PublicKey.DerivationType.Plain(
                Wallet.HDKey(extendedPublicKey = ownerKey, path = derivationPath),
            ),
        )

        return when (val result = hotSignerFactory.create(userWallet).sign(digest, publicKey)) {
            is CompletionResult.Failure -> result.error.left()
            is CompletionResult.Success -> result.data.right()
        }
    }
}