package com.tangem.tap.features.customtoken.impl.presentation.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.PrimaryButtonIconLeft
import com.tangem.core.ui.res.TangemTheme
import com.tangem.tap.features.customtoken.impl.presentation.models.AddCustomTokenFloatingButton
import com.tangem.wallet.R

/**
 * Add custom token floating button. Attached above the keyboard.
 *
 * @param model button model
 *
* [REDACTED_AUTHOR]
 */
@Composable
internal fun AddCustomTokenFloatingButton(model: AddCustomTokenFloatingButton) {
    PrimaryButtonIconLeft(
        modifier = Modifier
            .imePadding()
            .padding(horizontal = TangemTheme.dimens.spacing16)
            .fillMaxWidth(),
        text = stringResource(id = R.string.common_add),
        icon = painterResource(id = R.drawable.ic_plus_24),
        enabled = model.isEnabled,
        onClick = model.onClick,
    )
}

@Preview
@Composable
private fun Preview_AddCustomTokenFloatingButton_Enabled() {
    TangemTheme {
        AddCustomTokenFloatingButton(model = AddCustomTokenFloatingButton(isEnabled = true, onClick = {}))
    }
}

@Preview
@Composable
private fun Preview_AddCustomTokenFloatingButton_Disabled() {
    TangemTheme {
        AddCustomTokenFloatingButton(model = AddCustomTokenFloatingButton(isEnabled = false, onClick = {}))
    }
}
