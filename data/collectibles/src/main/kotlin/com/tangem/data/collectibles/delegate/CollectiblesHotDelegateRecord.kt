package com.tangem.data.collectibles.delegate

import com.squareup.moshi.JsonClass

/**
 * The secret stays in the hot SDK keystore — [hotWalletId] plus [authType] is what addresses it there.
 * [authType] is stored by name so a reordered `HotWalletId.AuthType` cannot silently remap it.
 */
@JsonClass(generateAdapter = true)
internal data class CollectiblesHotDelegateRecord(
    val hotWalletId: String,
    val authType: String,
    val publicKeyBase58: String,
)