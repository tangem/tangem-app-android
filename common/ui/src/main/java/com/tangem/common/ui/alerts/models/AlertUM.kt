package com.tangem.common.ui.alerts.models

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

@Immutable
interface AlertUM {
    val title: TextReference?
    val message: TextReference
    val confirmButtonText: TextReference
    val onConfirmClick: (() -> Unit)?
}