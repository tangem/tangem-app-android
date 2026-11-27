package com.tangem.features.onramp.main.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.onramp.main.entity.OnrampAmountButtonUM
import com.tangem.utils.annotations.RemoveWithToggle

@Composable
@RemoveWithToggle(toggleName = "AND_19143_ONRAMP_DESIGN_SYSTEM_ENABLED", description = "Rename over the old file")
internal fun OnrampAmountButtonDs(button: OnrampAmountButtonUM, modifier: Modifier = Modifier) {
    val focusManager = LocalFocusManager.current
    TangemButton(
        modifier = modifier,
        variant = TangemButton.Variant.Secondary,
        size = TangemButton.Size.X8,
        text = stringReference("${button.value}${button.currency}"),
        onClick = {
            focusManager.clearFocus()
            button.onClick()
        },
    )
}