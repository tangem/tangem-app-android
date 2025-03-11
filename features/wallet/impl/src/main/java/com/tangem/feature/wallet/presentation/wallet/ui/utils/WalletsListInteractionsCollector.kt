package com.tangem.feature.wallet.presentation.wallet.ui.utils

import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
import kotlinx.coroutines.flow.FlowCollector

internal class WalletsListInteractionsCollector(
    private val onDragStart: () -> Unit,
) : FlowCollector<Interaction?> {

    override suspend fun emit(value: Interaction?) {
        if (value is DragInteraction.Start) onDragStart()
    }
}