package com.tangem.data.polymarket.derivation

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.common.card.EllipticCurve
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.data.wallets.cold.UserWalletIdPreflightReadFilter
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncStrict
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.derivation.OWNER_DERIVATION_PATH
import com.tangem.domain.polymarket.derivation.PolymarketEoaDeriver
import com.tangem.domain.polymarket.model.PolymarketDerivationError
import com.tangem.domain.wallets.derivations.DerivationsRepository
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class DefaultPolymarketEoaDeriver @Inject constructor(
    private val userWalletsListRepository: UserWalletsListRepository,
    private val derivationsRepository: DerivationsRepository,
    private val tangemSdkManager: TangemSdkManager,
    private val addressFactory: PolymarketAddressFactory,
    private val dispatchers: CoroutineDispatcherProvider,
) : PolymarketEoaDeriver {

    override suspend fun deriveOwnerEoa(userWalletId: UserWalletId): Either<PolymarketDerivationError, String> =
        withContext(dispatchers.io) {
            val userWallet = userWalletsListRepository.getSyncStrict(userWalletId)

            if (userWallet is UserWallet.Cold &&
                userWallet.scanResponse.card.firmwareVersion < FirmwareVersion.HDWalletAvailable
            ) {
                return@withContext PolymarketDerivationError.DerivationUnsupported.left()
            }

            val seedKey = userWallet.secp256k1SeedKey()
                ?: return@withContext PolymarketDerivationError.MissingWallet.left()

            val path = DerivationPath(OWNER_DERIVATION_PATH)

            cachedAddress(userWalletId, seedKey, path)?.let { return@withContext it.right() }

            when (userWallet) {
                is UserWallet.Cold -> deriveCold(userWalletId)
                is UserWallet.Hot -> deriveHot(userWalletId, seedKey, path)
            }
        }

    private suspend fun cachedAddress(userWalletId: UserWalletId, seedKey: ByteArray, path: DerivationPath): String? {
        val existing = derivationsRepository.getExistingDerivedKeys(userWalletId, ByteArrayKey(seedKey))
        val extendedPublicKey = existing[path] ?: return null
        return addressFactory.createAddress(extendedPublicKey)
    }

    private suspend fun deriveCold(userWalletId: UserWalletId): Either<PolymarketDerivationError, String> {
        return tangemSdkManager
            .polymarketProduceOwnerKeyData(UserWalletIdPreflightReadFilter(userWalletId))
            .fold(
                ifLeft = { it.toDerivationError().left() },
                ifRight = { keyData ->
                    derivationsRepository.storeDerivedKeys(userWalletId, keyData.derivedKeys)
                    keyData.address.right()
                },
            )
    }

    private suspend fun deriveHot(
        userWalletId: UserWalletId,
        seedKey: ByteArray,
        path: DerivationPath,
    ): Either<PolymarketDerivationError, String> = Either
        .catch {
            val derived = derivationsRepository.derivePublicKeys(
                userWalletId,
                mapOf(ByteArrayKey(seedKey) to listOf(path)),
            )
            val extendedPublicKey = derived.getValue(ByteArrayKey(seedKey))[path]
                ?: error("Failed to derive Polymarket owner key")
            addressFactory.createAddress(extendedPublicKey)
        }
        .mapLeft { it.toDerivationError() }

    private fun UserWallet.secp256k1SeedKey(): ByteArray? = when (this) {
        is UserWallet.Cold -> scanResponse.card.wallets
            .firstOrNull { it.curve == EllipticCurve.Secp256k1 }
            ?.publicKey
        is UserWallet.Hot -> wallets
            ?.firstOrNull { it.curve == EllipticCurve.Secp256k1 }
            ?.publicKey
    }

    private fun Throwable.toDerivationError(): PolymarketDerivationError = when (this) {
        is TangemSdkError.UserCancelled -> PolymarketDerivationError.UserCancelled
        is TangemSdkError.WalletNotFound -> PolymarketDerivationError.MissingWallet
        is TangemSdkError -> PolymarketDerivationError.CardError
        else -> PolymarketDerivationError.Unknown
    }
}