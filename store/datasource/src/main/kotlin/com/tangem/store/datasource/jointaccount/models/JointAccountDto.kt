package com.tangem.store.datasource.jointaccount.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.core.remote.moshi.SkipMalformedElements


@JsonClass(generateAdapter = true)
data class JointAccountDto(
    @Json(name = "cryptoAccountId") val cryptoAccountId: String,
    @Json(name = "membersCount") val membersCount: Int,
    @Json(name = "threshold") val threshold: Int,
    @Json(name = "safeAddress") val safeAddress: String?,
    @Json(name = "status") val status: String,
    @Json(name = "members") val members: List<Member>,
    @Json(name = "invites") val invites: List<Invite>? = null,
    @SkipMalformedElements
    @Json(name = "operations") val operations: List<JointOperationDto>? = null,
) {

    /**
     * @property name     display name, 1..25 chars
     * @property address  the member's owner address in the Safe, lower-case hex (the backend does not checksum).
     * The app finds itself in the list by comparing this against the address it derives from the card
     * (case-insensitively), never by trusting the backend
     * @property role     `creator` | `member` — only the creator sends invites. Raw string, same reasoning as
     * [JointAccountDto.status]
     * @property slot     `0` for the creator; only the creator of a `pending` account sees it in the `GET` list,
     * every other response carries `null` (v1.6)
     * @property signature the member's `/confirm` signature; `null` until the member confirms, absent in pre-v1.5
     * responses
     */
    @JsonClass(generateAdapter = true)
    data class Member(
        @Json(name = "name") val name: String,
        @Json(name = "address") val address: String,
        @Json(name = "role") val role: String,
        @Json(name = "slot") val slot: Int? = null,
        @Json(name = "signature") val signature: String? = null,
    )

    /**
     * @property expiresAt ISO 8601 by the server clock; an expired invite stays listed until the slot is reissued
     */
    @JsonClass(generateAdapter = true)
    data class Invite(
        @Json(name = "slot") val slot: Int,
        @Json(name = "expiresAt") val expiresAt: String,
    )
}