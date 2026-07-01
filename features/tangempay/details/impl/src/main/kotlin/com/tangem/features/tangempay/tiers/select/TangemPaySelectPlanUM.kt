package com.tangem.features.tangempay.tiers.select

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class TangemPaySelectPlanUM(
    val topBarTitle: TextReference,
    val plans: ImmutableList<PlanUM>,
    val selectedIndex: Int,
    val onPlanSelected: (Int) -> Unit,
    val onBackClick: () -> Unit,
    val onCloseClick: () -> Unit,
    val content: Content,
) {

    @Immutable
    data class PlanUM(
        val name: TextReference,
        val imageUrl: String?,
        val points: ImmutableList<PointUM>,
    )

    @Immutable
    data class PointUM(
        val title: TextReference,
        val body: TextReference?,
    )

    @Immutable
    sealed interface Content {

        data class Select(
            val onComparePlansClick: () -> Unit,
            val onSelectClick: () -> Unit,
        ) : Content

        data class Confirm(
            val title: TextReference,
            val points: ImmutableList<PointUM>,
            val confirmButtonText: TextReference,
            val onCancelClick: () -> Unit,
            val onConfirmClick: () -> Unit,
        ) : Content
    }
}