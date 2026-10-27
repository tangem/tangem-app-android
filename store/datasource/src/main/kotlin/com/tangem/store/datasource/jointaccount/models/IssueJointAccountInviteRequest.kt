package com.tangem.store.datasource.jointaccount.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** @property slot `1..membersCount - 1`; slot `0` is the creator's */
@JsonClass(generateAdapter = true)
data class IssueJointAccountInviteRequest(
    @Json(name = "cryptoAccountId") val cryptoAccountId: String,
    @Json(name = "slot") val slot: Int,
)