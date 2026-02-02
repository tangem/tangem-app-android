package com.tangem.domain.staking.model

sealed interface StakingTargetImage {
    data class Url(val url: String) : StakingTargetImage
    data class Local(val type: StakingLocalImageType) : StakingTargetImage
}

enum class StakingLocalImageType {
    P2P_VAULT,
}