package com.tangem.feature.swap.models.states

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

@Immutable
sealed class ProviderState {

    abstract val onProviderClick: ((String) -> Unit)?
    abstract val id: String

    data class Empty(
        override val id: String = "",
        override val onProviderClick: ((String) -> Unit)? = null,
    ) : ProviderState()

    data class Loading(
        override val id: String = "",
        override val onProviderClick: ((String) -> Unit)? = null,
    ) : ProviderState()

    data class Content(
        override val id: String,
        val name: String,
        val type: String,
        val iconUrl: String,
        val subtitle: TextReference,
        val selectionType: SelectionType,
        val additionalBadge: AdditionalBadge,
        val percentLowerThenBest: PercentDifference = PercentDifference.Empty,
        val namePrefix: PrefixType,
        override val onProviderClick: (String) -> Unit,
    ) : ProviderState()

    data class Unavailable(
        override val id: String,
        val name: String,
        val type: String,
        val iconUrl: String,
        val alertText: TextReference,
        val selectionType: SelectionType,
        override val onProviderClick: ((String) -> Unit)? = null,
    ) : ProviderState()

    @Immutable
    sealed class AdditionalBadge {
        data object FCAWarningList : AdditionalBadge()
        data object BestTrade : AdditionalBadge()
        data object Empty : AdditionalBadge()
        data object PermissionRequired : AdditionalBadge()
        data object Recommended : AdditionalBadge()
    }

    @Immutable
    enum class SelectionType {
        NONE, CLICK, SELECT
    }

    // Prefix will be disabled in 5.12 but mechanism is still implemented
    @Immutable
    enum class PrefixType {
        NONE, PROVIDED_BY
    }
}

@Immutable
sealed class PercentDifference {
    data class Value(val value: Float) : PercentDifference()
    object Empty : PercentDifference()
}

object ProviderPercentDiffComparator : Comparator<ProviderState> {
    override fun compare(o1: ProviderState, o2: ProviderState): Int {
        if (o1 is ProviderState.Content && o2 !is ProviderState.Content) {
            return -1
        }
        if (o1 !is ProviderState.Content && o2 is ProviderState.Content) {
            return 1
        }
        if (o1 is ProviderState.Content && o2 is ProviderState.Content) {
            val o1Percent = o1.percentLowerThenBest
            val o2Percent = o2.percentLowerThenBest
            if (o1Percent is PercentDifference.Value && o2Percent !is PercentDifference.Value) {
                return -1
            }
            if (o1Percent !is PercentDifference.Value && o2Percent is PercentDifference.Value) {
                return 1
            }
            return if (o1Percent is PercentDifference.Value && o2Percent is PercentDifference.Value) {
                o2Percent.value.compareTo(o1Percent.value)
            } else {
                0
            }
        } else {
            return 0
        }
    }
}