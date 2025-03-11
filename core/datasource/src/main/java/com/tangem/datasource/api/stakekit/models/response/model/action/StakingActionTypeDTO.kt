package com.tangem.datasource.api.stakekit.models.response.model.action

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class StakingActionTypeDTO {
    @Json(name = "STAKE")
    STAKE,

    @Json(name = "UNSTAKE")
    UNSTAKE,

    @Json(name = "CLAIM_REWARDS")
    CLAIM_REWARDS,

    @Json(name = "RESTAKE_REWARDS")
    RESTAKE_REWARDS,

    @Json(name = "WITHDRAW")
    WITHDRAW,

    @Json(name = "RESTAKE")
    RESTAKE,

    @Json(name = "CLAIM_UNSTAKED")
    CLAIM_UNSTAKED,

    @Json(name = "UNLOCK_LOCKED")
    UNLOCK_LOCKED,

    @Json(name = "STAKE_LOCKED")
    STAKE_LOCKED,

    @Json(name = "VOTE")
    VOTE,

    @Json(name = "REVOKE")
    REVOKE,

    @Json(name = "VOTE_LOCKED")
    VOTE_LOCKED,

    @Json(name = "REVOTE")
    REVOTE,

    @Json(name = "REBOND")
    REBOND,

    @Json(name = "MIGRATE")
    MIGRATE,

    UNKNOWN,
}