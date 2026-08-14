package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Card of a wallet with its backup state.
 *
 * @property cardId         card identifier
 * @property cardPublicKey  public key of the card
 * @property role           role of the card within the wallet
 * @property backupStatus   backup state of the card

 *                          e.g. `["secp256k1", "ed25519"]`. May be empty
 * @property errorCode      code of the error received while performing a card command, if any
 * @property errorMessage   text of the error received while performing a card command, if any
 */
@JsonClass(generateAdapter = true)
data class WalletCardDTO(
    @Json(name = "cardId") val cardId: String,
    @Json(name = "cardPublicKey") val cardPublicKey: String,
    @Json(name = "role") val role: Role,
    @Json(name = "backupStatus") val backupStatus: BackupStatus,
    @Json(name = "curves") val curves: List<String>,
    @Json(name = "errorCode") val errorCode: String? = null,
    @Json(name = "errorMessage") val errorMessage: String? = null,
) {

    /** Role of a card within the wallet */
    @JsonClass(generateAdapter = false)
    enum class Role {
        @Json(name = "primary")
        PRIMARY,

        @Json(name = "backup1")
        BACKUP_1,

        @Json(name = "backup2")
        BACKUP_2,
    }

    /** Backup state of a card */
    @JsonClass(generateAdapter = false)
    enum class BackupStatus {

        /** The card has not performed `LINK_PRIMARY_CARD` / `LINK_SECONDARY_CARDS` */
        @Json(name = "noBackup")
        NO_BACKUP,

        /** `LINK_PRIMARY_CARD` / `LINK_SECONDARY_CARDS` are done, `READ_BACKUP_DATA` / `WRITE_BACKUP_DATA` are not */
        @Json(name = "cardLinked")
        CARD_LINKED,

        /** `READ_BACKUP_DATA` / `WRITE_BACKUP_DATA` completed successfully */
        @Json(name = "active")
        ACTIVE,
    }
}