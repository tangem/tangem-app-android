package com.tangem.data.collectibles.derivation

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.extensions.encodeBase58
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.domain.collectibles.derivation.COLLECTIBLES_OWNER_DERIVATION_PATH
import com.tangem.domain.collectibles.derivation.CollectiblesDerivationError
import com.tangem.domain.collectibles.derivation.CollectiblesOwnerDeriver
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncOrNull
import com.tangem.domain.common.wallets.getSyncStrict
import com.tangem.domain.core.utils.catchOn
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.config.curvesConfig
import com.tangem.domain.wallets.derivations.DerivationsRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject

/**
 * Reads the owner key from the wallet's stored derivations and, on a miss, asks the card for it in a session of

 */
internal class DefaultCollectiblesOwnerDeriver @Inject constructor(
    private val userWalletsListRepository: UserWalletsListRepository,
    private val derivationsRepository: DerivationsRepository,
    private val dispatchers: CoroutineDispatcherProvider,
) : CollectiblesOwnerDeriver {

    override suspend fun deriveOwner(userWalletId: UserWalletId): Either<CollectiblesDerivationError, String> = Either
        .catchOn(dispatchers.io) {
            log("deriveOwner: start, path=$COLLECTIBLES_OWNER_DERIVATION_PATH")

            val userWallet = userWalletsListRepository.getSyncStrict(userWalletId)

            if (userWallet is UserWallet.Cold &&
                userWallet.scanResponse.card.firmwareVersion < FirmwareVersion.HDWalletAvailable
            ) {
                log("deriveOwner: firmware has no HD wallet support")
                return@catchOn CollectiblesDerivationError.DerivationUnsupported.left()
            }

            val seedKey = userWallet.solanaSeedKey() ?: run {
                log("deriveOwner: wallet has no Solana seed key")
                return@catchOn CollectiblesDerivationError.MissingWallet.left()
            }

            val owner = ownerKey(userWalletId = userWalletId, seedKey = ByteArrayKey(seedKey)) ?: run {
                log("deriveOwner: derivation returned no key for the path")
                return@catchOn CollectiblesDerivationError.Unknown(
                    cause = IllegalStateException("No key derived for $COLLECTIBLES_OWNER_DERIVATION_PATH"),
                ).left()
            }

            owner.publicKey.encodeBase58()
                .also { log("deriveOwner: done") }
                .right()
        }
        .getOrElse { throwable ->
            log("deriveOwner: failed, error=$throwable")
            throwable.toDerivationError().left()
        }

    override suspend fun storedOwner(userWalletId: UserWalletId): String? = Either
        .catchOn(dispatchers.io) {
            val userWallet = userWalletsListRepository.getSyncOrNull(userWalletId) ?: return@catchOn null
            val seedKey = userWallet.solanaSeedKey() ?: return@catchOn null

            storedOwnerKey(userWalletId = userWalletId, seedKey = ByteArrayKey(seedKey))?.publicKey?.encodeBase58()
        }
        .getOrNull()

    /** Derives on the card only when the key is not stored yet; the repository persists whatever it derived. */
    private suspend fun ownerKey(userWalletId: UserWalletId, seedKey: ByteArrayKey): ExtendedPublicKey? {
        return storedOwnerKey(userWalletId = userWalletId, seedKey = seedKey)
            // TODO([REDACTED_TASK_KEY]): this call opens a card session of its own, so onboarding through it costs two taps.
            //  `initVault` must not go this way: derive and sign in one session instead — a `CardSessionRunnable`
            //  shaped like `tap/domain/tasks/visa/TangemPayGenerateAddressAndSignChallengeTask` (derive with
            //  `DeriveWalletPublicKeyTask` in the given session, then sign), with the blockhash fetched before the
            //  session opens and the derived key handed back for `DerivationsRepository.storeDerivedKeys`.
            ?: derivationsRepository
                .derivePublicKeys(userWalletId = userWalletId, derivations = mapOf(seedKey to listOf(path)))[seedKey]
                ?.get(path)
    }

    private suspend fun storedOwnerKey(userWalletId: UserWalletId, seedKey: ByteArrayKey): ExtendedPublicKey? {
        return derivationsRepository.getExistingDerivedKeys(userWalletId = userWalletId, seedKey = seedKey)[path]
    }

    /**
     * The seed public key the wallet derives its Solana keys from, or `null` if it has none.
     *
     * The curve comes from the wallet's own config rather than being hardcoded: Wallet 2 cards keep Solana on
     * `Ed25519Slip0010`, older ones on plain `Ed25519`. `Blockchain.Solana` stands in for the devnet too — both map
     * to the same curve.
     */
    private fun UserWallet.solanaSeedKey(): ByteArray? {
        val curve = curvesConfig.primaryCurve(Blockchain.Solana) ?: return null

        return when (this) {
            is UserWallet.Cold -> scanResponse.card.wallets.firstOrNull { it.curve == curve }?.publicKey
            is UserWallet.Hot -> wallets?.firstOrNull { it.curve == curve }?.publicKey
        }
    }

    private fun Throwable.toDerivationError(): CollectiblesDerivationError = when (this) {
        is TangemSdkError.UserCancelled -> CollectiblesDerivationError.UserCancelled
        is TangemSdkError.WalletNotFound -> CollectiblesDerivationError.MissingWallet
        is TangemSdkError -> CollectiblesDerivationError.CardError(cause = this)
        else -> CollectiblesDerivationError.Unknown(cause = this)
    }

    private fun log(message: String) {
        TangemLogger.d(messageString = "CollectiblesOwnerDeriver: $message")
    }

    private companion object {

        val path = DerivationPath(rawPath = COLLECTIBLES_OWNER_DERIVATION_PATH)
    }
}