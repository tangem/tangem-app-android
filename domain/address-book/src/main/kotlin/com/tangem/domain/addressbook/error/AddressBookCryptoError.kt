package com.tangem.domain.addressbook.error

import com.tangem.domain.addressbook.model.AddressBook
import com.tangem.domain.addressbook.model.AddressBookBlob

/** Failure modes of [com.tangem.domain.addressbook.crypto.AddressBookCipher]. */
sealed interface AddressBookCryptoError {

    /** The wallet exposes no public key, so the symmetric key cannot be derived. */
    data object NoWalletPublicKey : AddressBookCryptoError

    /** [AddressBookBlob.walletId] does not belong to the wallet passed to decrypt. */
    data object WalletMismatch : AddressBookCryptoError

    /** Authentication tag check failed, or the nonce/ciphertext/tag were not valid hex. */
    data object DecryptionFailed : AddressBookCryptoError

    /** Decryption succeeded but the plaintext could not be parsed into an [AddressBook]. */
    data object MalformedBlob : AddressBookCryptoError
}