package com.tangem.datasource.api.jointaccount.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * A joint account as the backend returns it, both in the `GET` list and in the `POST` create response.
 *
 * @property cryptoAccountId the requesting wallet's own row in `accounts` (64 hex chars, upper case) — the merge key
 * against `accounts[].id` from `GET /v1/wallets/{walletId}/accounts`, which carries the account's personal name,
 * icon, color, ordering and tokens
 * @property membersCount    slots including the creator, fixed at creation
 * @property threshold       signatures required to execute an operation, fixed at creation
 * @property address         the Safe address, one and the same in every supported EVM network; `null` while `pending`
 * @property status          `pending` → `confirming` → `active`, forward only. Kept as a raw string: mapping to a
 * domain type (including a value the backend adds later) is the data layer's job, a parse must never fail on it
 * @property members         taken slots only, ordered by join time, the creator first; free slots are
 * `membersCount - members.size`
 * @property invites         one per free slot; present **only** in the `POST` create response — this is the only
 * place the backend ever returns them, so they must be persisted right away
 */
@JsonClass(generateAdapter = true)
data class JointAccountDto(
    @Json(name = "cryptoAccountId") val cryptoAccountId: String,
    @Json(name = "membersCount") val membersCount: Int,
    @Json(name = "threshold") val threshold: Int,
    @Json(name = "address") val address: String?,
    @Json(name = "status") val status: String,
    @Json(name = "members") val members: List<Member>,
    @Json(name = "invites") val invites: List<Invite>? = null,
) {

    /**
     * @property name    display name, 1..25 chars
     * @property address the member's owner address in the Safe, EIP-55 checksummed. The app finds itself in the list
     * by comparing this against the address it derives from the card (lowercased), never by trusting the backend
     * @property role    `creator` | `member` — only the creator sends invites and activates the account. Raw string,
     * same reasoning as [JointAccountDto.status]
     */
    @JsonClass(generateAdapter = true)
    data class Member(
        @Json(name = "name") val name: String,
        @Json(name = "address") val address: String,
        @Json(name = "role") val role: String,
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