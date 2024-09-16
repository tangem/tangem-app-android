package com.tangem.common.ui.alerts.models

import com.tangem.common.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference

data class AlertDemoModeUM(
    override val onConfirmClick: () -> Unit,
) : AlertUM {
    override val confirmButtonText: TextReference = resourceReference(id = R.string.common_ok)
    override val title: TextReference = resourceReference(id = R.string.warning_demo_mode_title)
    override val message: TextReference = resourceReference(id = R.string.warning_demo_mode_message)
}