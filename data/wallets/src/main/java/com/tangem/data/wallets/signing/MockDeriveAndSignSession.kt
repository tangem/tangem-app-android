package com.tangem.data.wallets.signing

import arrow.core.Either
import arrow.core.right
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.extensions.hexToBytes
import com.tangem.crypto.Secp256k1
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.derivations.DerivationsRepository
import com.tangem.domain.wallets.signing.DeriveAndSignError
import com.tangem.domain.wallets.signing.DeriveAndSignSession
import com.tangem.domain.wallets.signing.SignRequest
import com.tangem.domain.wallets.signing.Signed
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import javax.inject.Inject

internal class MockDeriveAndSignSession @Inject constructor(
    private val derivationsRepository: DerivationsRepository,
) : DeriveAndSignSession {

    override suspend fun <T> run(
        userWallet: UserWallet,
        seedKey: ByteArray,
        path: DerivationPath,
        prepare: suspend (derivedKey: ExtendedPublicKey) -> SignRequest<T>,
    ): Either<DeriveAndSignError, Signed<T>> {
        if (derivationsRepository.getExistingDerivedKeys(userWallet, ByteArrayKey(seedKey))[path] == null) {
            derivationsRepository.storeDerivedKeys(
                userWalletId = userWallet.walletId,
                derivedKeys = mapOf(ByteArrayKey(seedKey) to ExtendedPublicKeysMap(mapOf(path to MOCK_KEY))),
            )
        }
        val request = prepare(MOCK_KEY)
        val signatures = request.hashes.map { hash -> Secp256k1.ecdsaSignDigest(hash, MOCK_PRIVATE_KEY) }
        return Signed(request.value, signatures).right()
    }

    private companion object {
        val MOCK_KEY = ExtendedPublicKey(
            publicKey = "0279BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798".hexToBytes(),
            chainCode = ByteArray(size = 32),
        )
        val MOCK_PRIVATE_KEY = ByteArray(size = 32).apply { this[lastIndex] = 1 }
    }
}