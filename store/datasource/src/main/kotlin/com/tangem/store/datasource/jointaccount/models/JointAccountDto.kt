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
     * @property role     `creator` | `member` — only the creator sends invites and activates the account. Raw string,
     * same reasoning as [JointAccountDto.status]
     * @property inviteId the invite this member redeemed to take the slot; `null` for the creator, who never had
     * one, and absent in pre-v1.3 responses
     */
    @JsonClass(generateAdapter = true)
    data class Member(
        @Json(name = "name") val name: String,
        @Json(name = "address") val address: String,
        @Json(name = "role") val role: String,
        @Json(name = "inviteId") val inviteId: String? = null,
    )

    /**
     * @property id 32 random bytes as upper-case hex — the only secret guarding a pending account: whoever holds it
     * can take the slot
     */
    @JsonClass(generateAdapter = true)
    data class Invite(
        @Json(name = "id") val id: String,
    )
}