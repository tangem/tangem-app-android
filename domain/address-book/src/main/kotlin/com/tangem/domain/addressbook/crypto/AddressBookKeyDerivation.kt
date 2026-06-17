package com.tangem.domain.addressbook.crypto

import com.tangem.domain.models.wallet.UserWallet
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Derives the AES-256 symmetric key used to encrypt a wallet's address book.
 *
 * The scheme mirrors `UserWalletEncyptionKeyCalculator`, differing only in the domain-separation
 * message: `HMAC-SHA256(SHA-256(walletPublicKey), "TokensSymmetricKey")`. HMAC-SHA256 yields 32
 * bytes, exactly an AES-256 key.
 */
internal object AddressBookKeyDerivation {

    private const val SYMMETRIC_KEY_MESSAGE = "TokensSymmetricKey"
    private const val SHA_256 = "SHA-256"
    private const val HMAC_SHA_256 = "HmacSHA256"

    /** Primary wallet public key, or `null` if the wallet is locked / has no wallets. */
    fun walletPublicKey(userWallet: UserWallet): ByteArray? = when (userWallet) {
        is UserWallet.Cold -> {
            val card = userWallet.scanResponse.card
            card.wallets
                .firstOrNull()
                ?.publicKey
        }
        is UserWallet.Hot -> userWallet.wallets?.firstOrNull()?.publicKey
    }

    /** `HMAC-SHA256(key = SHA-256(publicKey), msg = "TokensSymmetricKey")` — a 32-byte AES-256 key. */
    fun deriveAesKey(publicKey: ByteArray): ByteArray {
        val keyHash = MessageDigest.getInstance(SHA_256).digest(publicKey)
        val mac = Mac.getInstance(HMAC_SHA_256)
        mac.init(SecretKeySpec(keyHash, HMAC_SHA_256))
        return mac.doFinal(SYMMETRIC_KEY_MESSAGE.toByteArray(Charsets.UTF_8))
    }
}