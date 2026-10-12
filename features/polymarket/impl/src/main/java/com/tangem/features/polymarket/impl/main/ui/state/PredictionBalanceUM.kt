package com.tangem.features.polymarket.impl.main.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.extensions.TextReference

@Immutable
internal data class PredictionBalanceUM(
    val balance: TextReference,
    val label: TextReference,
    val isSwitchEnabled: Boolean,
    val onSwitchClick: () -> Unit,
    val addFunds: TangemButtonUM,
    val withdraw: TangemButtonUM,
)