package com.tangem.domain.addressbook.error

/**
 * Failure of a backend address-book operation (`PUT /address-books/{walletId}` or
 * `POST /address-books/sync`). The backend is the source of truth, so when one of these is raised the
 * local blob is left untouched.
 */
sealed interface AddressBookSyncError {

    /** Etag mismatch on update (HTTP 412) — the book was changed elsewhere. */
    data object Conflict : AddressBookSyncError

    /** The wallet does not exist on the backend (HTTP 404). */
    data object NotFound : AddressBookSyncError

    /** Invalid API key (HTTP 401). */
    data object Unauthorized : AddressBookSyncError

    /** Malformed request or exceeded the wallet limit (HTTP 400). */
    data object BadRequest : AddressBookSyncError

    /** No network or the request could not be completed. */
    data object Network : AddressBookSyncError

    /**
     * The wallet already has a stored book that could not be decrypted. The write is refused so the broken
     * (but non-empty) backend book is not overwritten by a fresh one built on the device — see
     * [com.tangem.domain.addressbook.repository.AddressBookRepository.saveContact].
     */
    data object DecryptionFailed : AddressBookSyncError

    /** Any other unexpected failure (encryption, missing data, unmapped HTTP code). */
    data object Unknown : AddressBookSyncError

    /**
     * The stored book uses a contract version newer than this build supports
     * ([com.tangem.domain.addressbook.model.AddressBookBlob.isVersionCompatible]). Determined locally (no
     * network), the write is refused so a book the app cannot fully understand is not downgraded/overwritten.
     */
    data object VersionMismatch : AddressBookSyncError
}