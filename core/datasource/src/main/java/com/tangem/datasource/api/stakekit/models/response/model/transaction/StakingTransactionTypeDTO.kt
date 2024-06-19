package com.tangem.datasource.api.stakekit.models.response.model.transaction

import com.squareup.moshi.Json

enum class StakingTransactionTypeDTO {
    @Json(name = "SWAP")
    SWAP,

    @Json(name = "DEPOSIT")
    DEPOSIT,

    @Json(name = "APPROVAL")
    APPROVAL,

    @Json(name = "STAKE")
    STAKE,

    @Json(name = "CLAIM_UNSTAKED")
    CLAIM_UNSTAKED,

    @Json(name = "CLAIM_REWARDS")
    CLAIM_REWARDS,

    @Json(name = "RESTAKE_REWARDS")
    RESTAKE_REWARDS,

    @Json(name = "UNSTAKE")
    UNSTAKE,

    @Json(name = "SPLIT")
    SPLIT,

    @Json(name = "MERGE")
    MERGE,

    @Json(name = "LOCK")
    LOCK,

    @Json(name = "UNLOCK")
    UNLOCK,

    @Json(name = "SUPPLY")
    SUPPLY,

    @Json(name = "BRIDGE")
    BRIDGE,

    @Json(name = "VOTE")
    VOTE,

    @Json(name = "REVOKE")
    REVOKE,

    @Json(name = "RESTAKE")
    RESTAKE,

    @Json(name = "REBOND")
    REBOND,

    @Json(name = "WITHDRAW")
    WITHDRAW,

    @Json(name = "CREATE_ACCOUNT")
    CREATE_ACCOUNT,

    @Json(name = "REVEAL")
    REVEAL,

    @Json(name = "MIGRATE")
    MIGRATE,

    @Json(name = "UTXO_P_TO_C_IMPORT")
    UTXO_P_TO_C_IMPORT,

    @Json(name = "UTXO_C_TO_P_IMPORT")
    UTXO_C_TO_P_IMPORT,

    @Json(name = "UNFREEZE_LEGACY")
    UNFREEZE_LEGACY,

    @Json(name = "UNFREEZE_LEGACY_BANDWIDTH")
    UNFREEZE_LEGACY_BANDWIDTH,

    @Json(name = "UNFREEZE_LEGACY_ENERGY")
    UNFREEZE_LEGACY_ENERGY,

    @Json(name = "UNFREEZE_BANDWIDTH")
    UNFREEZE_BANDWIDTH,

    @Json(name = "UNFREEZE_ENERGY")
    UNFREEZE_ENERGY,

    @Json(name = "FREEZE_BANDWIDTH")
    FREEZE_BANDWIDTH,

    @Json(name = "FREEZE_ENERGY")
    FREEZE_ENERGY,

    @Json(name = "UNDELEGATE_BANDWIDTH")
    UNDELEGATE_BANDWIDTH,

    @Json(name = "UNDELEGATE_ENERGY")
    UNDELEGATE_ENERGY,

    @Json(name = "P2P_NODE_REQUEST")
    P2P_NODE_REQUEST,

    @Json(name = "LUGANODES_PROVISION")
    LUGANODES_PROVISION,

    @Json(name = "LUGANODES_EXIT_REQUEST")
    LUGANODES_EXIT_REQUEST,

    @Json(name = "INFSTONES_PROVISION")
    INFSTONES_PROVISION,

    @Json(name = "INFSTONES_EXIT_REQUEST")
    INFSTONES_EXIT_REQUEST,

    @Json(name = "INFSTONES_CLAIM_REQUEST")
    INFSTONES_CLAIM_REQUEST,

    UNKNOWN,
}
