package com.tangem.domain.tokens.model.details

import kotlinx.serialization.Serializable

@Serializable
sealed class NavigationAction {
    data object Staking : NavigationAction()
    data class YieldSupply(val isActive: Boolean) : NavigationAction()
}