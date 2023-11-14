package com.tangem.feature.swap.models.states

sealed class ProviderState {

    object Loading : ProviderState()

    data class Content(
        val id: String,
        val name: String,
        val type: String,
        val iconUrl: String,
        val isBestTrade: Boolean,
        val rate: String,
        val onProviderClick: () -> Unit,
    ) : ProviderState()
}