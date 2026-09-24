package com.tangem.data.collectibles.account.derivation

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import com.tangem.blockchain.extensions.encodeBase58
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.domain.collectibles.account.derivation.COLLECTIBLES_OWNER_DERIVATION_PATH
import com.tangem.domain.collectibles.CollectiblesAccountError
import com.tangem.domain.collectibles.account.derivation.CollectiblesOwnerDeriver
import com.tangem.data.collectibles.vault.asFailure
import com.tangem.domain.collectibles.vault.GachaVaultError
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncOrNull
import com.tangem.domain.common.wallets.getSyncStrict
import com.tangem.domain.core.utils.catchOn
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.derivations.DerivationsRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject

/** Reads the owner key from the wallet's stored derivations and, on a miss, derives it in a session of its own. */
internal class DefaultCollectiblesOwnerDeriver @Inject constructor(
    private val userWalletsListRepository: UserWalletsListRepository,
    private val derivationsRepository: DerivationsRepository,
    private val dispatchers: CoroutineDispatcherProvider,
) : CollectiblesOwnerDeriver {

    override suspend fun deriveOwner(userWalletId: UserWalletId): Either<CollectiblesAccountError, String> = Either
        .catchOn(dispatchers.io) {
            val userWallet = userWalletsListRepository.getSyncStrict(userWalletId)

            if (userWallet is UserWallet.Cold &&
                userWallet.scanResponse.card.firmwareVersion < FirmwareVersion.HDWalletAvailable
            ) {
                TangemLogger.d("ownerDeriver: firmware has no HD wallet support")
                return@catchOn GachaVaultError.WalletUnavailable.asFailure().left()
            }

            val seedKey = userWallet.solanaSeedKey() ?: run {
                TangemLogger.d("ownerDeriver: wallet has no Solana seed key")
                return@catchOn GachaVaultError.WalletUnavailable.asFailure().left()
            }

            val owner = ownerKey(userWalletId = userWalletId, seedKey = ByteArrayKey(seedKey)) ?: run {
                TangemLogger.d("ownerDeriver: derivation returned no key for the path")
                return@catchOn GachaVaultError.Unexpected(
                    cause = IllegalStateException("No key derived for $COLLECTIBLES_OWNER_DERIVATION_PATH"),
                ).asFailure().left()
            }

            owner.publicKey
                .encodeBase58()
                .also { TangemLogger.d("ownerDeriver: Solana address was derived") }
                .right()
        }
        .getOrElse { throwable ->
            TangemLogger.d("ownerDeriver: failed, error=$throwable")
            throwable.toAccountError().left()
        }

    override suspend fun storedOwner(userWalletId: UserWalletId): String? = Either
        .catchOn(dispatchers.io) {
            val userWallet = userWalletsListRepository.getSyncOrNull(userWalletId) ?: return@catchOn null
            val seedKey = userWallet.solanaSeedKey() ?: return@catchOn null

            storedOwnerKey(userWalletId = userWalletId, seedKey = ByteArrayKey(seedKey))?.publicKey?.encodeBase58()
        }
        .getOrNull()

    private suspend fun ownerKey(userWalletId: UserWalletId, seedKey: ByteArrayKey): ExtendedPublicKey? {
        return storedOwnerKey(userWalletId = userWalletId, seedKey = seedKey)
            ?: derivationsRepository
                .derivePublicKeys(userWalletId = userWalletId, derivations = mapOf(seedKey to listOf(path)))[seedKey]
                ?.get(path)
    }

    private suspend fun storedOwnerKey(userWalletId: UserWalletId, seedKey: ByteArrayKey): ExtendedPublicKey? {
        return derivationsRepository.getExistingDerivedKeys(userWalletId = userWalletId, seedKey = seedKey)[path]
    }

    /** A `TangemSdkError` is the cold card session's own refusal; the NFC sheet has shown it. */
    private fun Throwable.toAccountError(): CollectiblesAccountError = when (this) {
        is TangemSdkError.UserCancelled -> CollectiblesAccountError.Cancelled
        is TangemSdkError -> CollectiblesAccountError.CardError(code = code, message = customMessage)
        else -> GachaVaultError.Unexpected(cause = this).asFailure()
    }

    private companion object {

        val path = DerivationPath(rawPath = COLLECTIBLES_OWNER_DERIVATION_PATH)
    }
}