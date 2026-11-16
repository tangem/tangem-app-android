package com.tangem.features.earn.impl.getfunds

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class EarnGetTokenSheetUM(
    val title: TextReference,
    val rows: ImmutableList<Row>,
    val onDismiss: () -> Unit,
) {

    /** @property isEnabled the look only: a tap on a disabled row still reaches [onClick] to explain the reason. */
    @Immutable
    data class Row(
        val icon: ImageVector,
        val title: TextReference,
        val subtitle: TextReference,
        val isEnabled: Boolean,
        val onClick: () -> Unit,
    )
}