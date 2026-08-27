package com.tangem.data.polymarket.derivation

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.data.polymarket.secp256k1SeedKey
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncStrict
import com.tangem.domain.core.utils.catchOn
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.derivation.POLYMARKET_OWNER_DERIVATION_PATH
import com.tangem.domain.polymarket.derivation.PolymarketEoaDeriver
import com.tangem.domain.polymarket.model.PolymarketDerivationError
import com.tangem.domain.wallets.derivations.DerivationsRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import javax.inject.Inject

internal class DefaultPolymarketEoaDeriver @Inject constructor(
    private val userWalletsListRepository: UserWalletsListRepository,
    private val derivationsRepository: DerivationsRepository,
    private val addressFactory: PolymarketAddressFactory,
    private val dispatchers: CoroutineDispatcherProvider,
) : PolymarketEoaDeriver {

    override suspend fun deriveOwnerEoa(userWalletId: UserWalletId): Either<PolymarketDerivationError, String> = Either
        .catchOn(dispatchers.io) {
            val userWallet = userWalletsListRepository.getSyncStrict(userWalletId)

            if (userWallet is UserWallet.Cold &&
                userWallet.scanResponse.card.firmwareVersion < FirmwareVersion.HDWalletAvailable
            ) {
                return@catchOn PolymarketDerivationError.DerivationUnsupported.left()
            }

            val seedKey = userWallet.secp256k1SeedKey()
                ?: return@catchOn PolymarketDerivationError.MissingWallet.left()

            val extendedPublicKey = extendedPublicKey(userWalletId, ByteArrayKey(seedKey))
                ?: return@catchOn PolymarketDerivationError.Unknown.left()

            addressFactory.createAddress(extendedPublicKey).right()
        }
        .getOrElse { it.toDerivationError().left() }

    private suspend fun extendedPublicKey(userWalletId: UserWalletId, seedKey: ByteArrayKey): ExtendedPublicKey? {
        val path = DerivationPath(POLYMARKET_OWNER_DERIVATION_PATH)

        return derivationsRepository.getExistingDerivedKeys(userWalletId, seedKey)[path]
            ?: derivationsRepository
                .derivePublicKeys(userWalletId, mapOf(seedKey to listOf(path)))[seedKey]
                ?.get(path)
    }

    private fun Throwable.toDerivationError(): PolymarketDerivationError = when (this) {
        is TangemSdkError.UserCancelled -> PolymarketDerivationError.UserCancelled
        is TangemSdkError.WalletNotFound -> PolymarketDerivationError.MissingWallet
        is TangemSdkError -> PolymarketDerivationError.CardError
        else -> PolymarketDerivationError.Unknown
    }
}