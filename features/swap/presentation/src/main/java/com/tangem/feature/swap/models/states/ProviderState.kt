package com.tangem.feature.swap.models.states

sealed class ProviderState {

    abstract val onProviderClick: ((String) -> Unit)?
    abstract val id: String

    data class Loading(
        override val id: String = "",
        override val onProviderClick: ((String) -> Unit)? = null,
    ) : ProviderState()

    data class Content(
        override val id: String,
        val name: String,
        val type: String,
        val iconUrl: String,
        val rate: String,
        val selectionType: SelectionType,
        val additionalBadge: AdditionalBadge,
        val percentLowerThenBest: Float?,
        override val onProviderClick: (String) -> Unit,
    ) : ProviderState()

    data class Unavailable(
        override val id: String,
        val name: String,
        val type: String,
        val iconUrl: String,
        val alertText: String,
        override val onProviderClick: ((String) -> Unit)? = null,
    ) : ProviderState()

    sealed class AdditionalBadge {
        object BestTrade : AdditionalBadge()
        object Empty : AdditionalBadge()
        object PermissionRequired : AdditionalBadge()
    }

    enum class SelectionType {
        NONE, CLICK, SELECT
    }
}
