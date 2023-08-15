package com.tangem.tap.features.customtoken.impl.presentation.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.components.PrimaryButtonIconStart
import com.tangem.core.ui.res.TangemTheme
import com.tangem.tap.features.customtoken.impl.presentation.models.AddCustomTokenFloatingButton
import com.tangem.wallet.R

/**
 * Add custom token floating button. Attached above the keyboard.
 *
 * @param model    button model
 * @param modifier modifier
 *
[REDACTED_AUTHOR]
 */
@Composable
internal fun AddCustomTokenFloatingButton(model: AddCustomTokenFloatingButton, modifier: Modifier = Modifier) {
    PrimaryButtonIconStart(
        modifier = modifier
            .imePadding()
            .padding(horizontal = TangemTheme.dimens.spacing16)
            .fillMaxWidth(),
        text = stringResource(id = R.string.custom_token_add_token),
        iconResId = R.drawable.ic_plus_24,
        enabled = model.isEnabled,
        onClick = model.onClick,
    )
}

@Preview
@Composable
private fun Preview_AddCustomTokenFloatingButton(
    @PreviewParameter(AddCustomTokenFloatingButtonProvider::class) model: AddCustomTokenFloatingButton,
) {
    TangemTheme {
        AddCustomTokenFloatingButton(model)
    }
}

private class AddCustomTokenFloatingButtonProvider : CollectionPreviewParameterProvider<AddCustomTokenFloatingButton>(
    listOf(
        AddCustomTokenFloatingButton(isEnabled = true, onClick = {}),
        AddCustomTokenFloatingButton(isEnabled = false, onClick = {}),
    ),
)