package com.tangem.data.polymarket.signing

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import com.tangem.blockchain.common.Wallet
import com.tangem.common.card.FirmwareVersion
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.data.polymarket.derivation.PolymarketAddressFactory
import com.tangem.data.polymarket.secp256k1SeedKey
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncStrict
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.derivation.POLYMARKET_OWNER_DERIVATION_PATH
import com.tangem.domain.polymarket.model.PolymarketSigningError
import com.tangem.domain.polymarket.signing.PolymarketSignRequest
import com.tangem.domain.polymarket.signing.PolymarketSigned
import com.tangem.domain.polymarket.signing.PolymarketSigningSession
import com.tangem.domain.wallets.signing.DeriveAndSignSession
import com.tangem.domain.wallets.signing.SignRequest
import com.tangem.domain.wallets.signing.Signed
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class DefaultPolymarketSigningSession @Inject constructor(
    private val userWalletsListRepository: UserWalletsListRepository,
    private val deriveAndSignSession: DeriveAndSignSession,
    private val digestFactory: PolymarketDigestFactory,
    private val addressFactory: PolymarketAddressFactory,
    private val formatter: PolymarketSignatureFormatter,
    private val dispatchers: CoroutineDispatcherProvider,
) : PolymarketSigningSession {

    override suspend fun <T> run(
        userWalletId: UserWalletId,
        prepare: suspend (ownerAddress: String) -> PolymarketSignRequest<T>,
    ): Either<PolymarketSigningError, PolymarketSigned<T>> = withContext(dispatchers.io) {
        val userWallet = runSuspendCatching { userWalletsListRepository.getSyncStrict(userWalletId) }
            .getOrElse { return@withContext PolymarketSigningError.MissingWallet.left() }

        if (userWallet is UserWallet.Cold &&
            userWallet.scanResponse.card.firmwareVersion < FirmwareVersion.HDWalletAvailable
        ) {
            return@withContext PolymarketSigningError.DerivationUnsupported.left()
        }

        val seedKey = userWallet.secp256k1SeedKey()
            ?: return@withContext PolymarketSigningError.MissingWallet.left()

        deriveAndSignSession
            .run(userWallet = userWallet, seedKey = seedKey, path = ownerPath) { ownerKey ->
                val ownerAddress = addressFactory.createAddress(ownerKey)
                val request = prepare(ownerAddress)
                val digests = request.payloads.map { payload -> digestFactory.digest(payload, ownerAddress) }
                SignRequest(value = Prepared(request.value, ownerKey, digests), hashes = digests)
            }
            .mapLeft { it.toPolymarketSigningError() }
            .flatMap { signed -> signed.formatted(seedKey) }
    }

    private fun <T> Signed<Prepared<T>>.formatted(
        seedKey: ByteArray,
    ): Either<PolymarketSigningError, PolymarketSigned<T>> {
        val digests = value.digests
        if (signatures.size != digests.size) return PolymarketSigningError.Unknown.left()

        val publicKey = Wallet.PublicKey(
            seedKey = seedKey,
            derivationType = Wallet.PublicKey.DerivationType.Plain(
                Wallet.HDKey(extendedPublicKey = value.ownerKey, path = ownerPath),
            ),
        )
        return Either
            .catch {
                signatures.zip(digests) { signature, digest -> formatter.format(signature, digest, publicKey) }
            }
            .mapLeft { PolymarketSigningError.Unknown }
            .map { formatted -> PolymarketSigned(value = value.value, signatures = formatted) }
    }

    private class Prepared<T>(val value: T, val ownerKey: ExtendedPublicKey, val digests: List<ByteArray>)

    private companion object {
        val ownerPath = DerivationPath(POLYMARKET_OWNER_DERIVATION_PATH)
    }
}