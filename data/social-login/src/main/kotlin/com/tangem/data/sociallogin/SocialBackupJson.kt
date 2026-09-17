package com.tangem.data.sociallogin

import kotlinx.serialization.json.Json

/**
 * `encodeDefaults = false` keeps the file to exactly the documented fields, `ignoreUnknownKeys`
 * tolerates keys a future version or another platform adds.
 */
internal val SocialBackupJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
}