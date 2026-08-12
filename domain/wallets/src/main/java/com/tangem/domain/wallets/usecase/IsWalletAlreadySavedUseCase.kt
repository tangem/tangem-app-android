package com.tangem.domain.wallets.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.common.card.EllipticCurve
import com.tangem.crypto.bip39.Mnemonic
import com.tangem.crypto.hdWallet.masterkey.AnyMasterKeyFactory
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.error.SaveWalletError
import com.tangem.domain.wallets.builder.UserWalletIdBuilder
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

class IsWalletAlreadySavedUseCase(
    private val userWalletsListRepository: UserWalletsListRepository,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    suspend operator fun invoke(mnemonic: Mnemonic, passphrase: String?): Either<SaveWalletError, Boolean> = either {
        val masterPublicKey = Either.catch {
            withContext(dispatchers.default) {
                deriveMasterPublicKey(mnemonic = mnemonic, passphrase = passphrase)
            }
        }
            .mapLeft { SaveWalletError.DataError(messageId = null) }
            .bind()

        val userWalletId = UserWalletIdBuilder.walletPublicKey(publicKey = masterPublicKey)

        userWalletsListRepository.userWalletsSync()
            .any { it.walletId == userWalletId }
    }

    private fun deriveMasterPublicKey(mnemonic: Mnemonic, passphrase: String?): ByteArray {
        val curve = EllipticCurve.Secp256k1
        val extendedPrivateKey = AnyMasterKeyFactory(
            mnemonic = mnemonic,
            passphrase = passphrase.orEmpty(),
        ).makeMasterKey(curve)

        return extendedPrivateKey.makePublicKey(curve).publicKey
    }
}