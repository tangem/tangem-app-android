package com.tangem.data.cloudbackup

import kotlinx.serialization.json.Json

/**
 * Custom JSON for the backup file format — intentionally NOT the shared
 * `KotlinxDataStoreSerializer.DefaultJson`, which encodes defaults. Here `encodeDefaults = false` is
 * required so that the backup file keeps the Web3/Ethereum keystore shape (no extra default fields).
 *
 * `ignoreUnknownKeys = true` tolerates keys added by other platforms.
 */
internal val CloudBackupJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
}