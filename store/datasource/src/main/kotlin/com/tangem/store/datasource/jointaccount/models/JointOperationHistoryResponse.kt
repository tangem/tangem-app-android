package com.tangem.store.datasource.jointaccount.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**

 *
 * @property pagination [Pagination.startDeltaCursor] comes only on the first page that has items
 */
@JsonClass(generateAdapter = true)
data class JointOperationHistoryResponse(
    @Json(name = "items") val items: List<JointOperationHistoryItemDto>,
    @Json(name = "pagination") val pagination: Pagination,
) {

    @JsonClass(generateAdapter = true)
    data class Pagination(
        @Json(name = "endCursor") val endCursor: String?,
        @Json(name = "startDeltaCursor") val startDeltaCursor: String?,
        @Json(name = "hasMore") val hasMore: Boolean,
    )
}

/**
 * The operations that changed after the cursor, oldest first. The same operation comes again after each change.
 *
 * @property pagination [Pagination.startCursor] of the last page may repeat its items on the next request
 */
@JsonClass(generateAdapter = true)
data class JointOperationHistoryDeltaResponse(
    @Json(name = "items") val items: List<JointOperationHistoryItemDto>,
    @Json(name = "pagination") val pagination: Pagination,
) {

    @JsonClass(generateAdapter = true)
    data class Pagination(
        @Json(name = "startCursor") val startCursor: String?,
        @Json(name = "hasMore") val hasMore: Boolean,
    )
}

/**
 * [JointOperationDto] in any status, plus where it lives and how it ended.
 *
 * @property status          `awaiting_signatures` | `pending` | `executed` | `failed` | `cancelled`. Raw string,
 * same reasoning as [JointOperationDto.status]
 * @property cryptoAccountId the requesting wallet's own account row of this joint account
 * @property safeAddress     the Safe address, lowercase
 * @property txHash          the relayer transaction; `null` while in flight, for `cancelled`, and for `failed`
 * before the chain
 * @property finishedAt      when the operation got its result or was cancelled, ISO-8601; `null` while in flight
 */
@JsonClass(generateAdapter = true)
data class JointOperationHistoryItemDto(
    @Json(name = "operationId") val operationId: String,
    @Json(name = "chainId") val chainId: Long,
    @Json(name = "safeTx") val safeTx: JointOperationDto.SafeTxDto,
    @Json(name = "status") val status: String,
    @Json(name = "initiator") val initiator: String? = null,
    @Json(name = "createdAt") val createdAt: String,
    @Json(name = "votes") val votes: List<JointOperationDto.VoteDto> = emptyList(),
    @Json(name = "cryptoAccountId") val cryptoAccountId: String,
    @Json(name = "safeAddress") val safeAddress: String,
    @Json(name = "txHash") val txHash: String? = null,
    @Json(name = "finishedAt") val finishedAt: String? = null,
)