package com.tangem.features.tangempay.orderCard.impl.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

@Immutable
internal data class TangemPayOrderCardSuccessScreenUM(
    val deliveryEtaMaxBusinessDays: Int,
    val email: String,
    val buttonText: TextReference,
    val onFinishClick: () -> Unit,
)