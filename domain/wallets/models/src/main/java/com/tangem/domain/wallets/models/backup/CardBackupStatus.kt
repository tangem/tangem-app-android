package com.tangem.domain.wallets.models.backup

/**
 * State of a card's backup as it is reported to and stored by the backend.
 *
 * A flattened counterpart of `CardDTO.BackupStatus`: the card count carried by the sealed variants is not
 * reported, the backend derives it from the size of the cards array.
 */
enum class CardBackupStatus {

    /** The card has not performed `LINK_PRIMARY_CARD` / `LINK_SECONDARY_CARDS` */
    NO_BACKUP,

    /** `LINK_PRIMARY_CARD` / `LINK_SECONDARY_CARDS` are done, `READ_BACKUP_DATA` / `WRITE_BACKUP_DATA` are not */
    CARD_LINKED,

    /** `READ_BACKUP_DATA` / `WRITE_BACKUP_DATA` completed successfully */
    ACTIVE,
}