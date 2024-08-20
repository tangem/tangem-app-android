package com.tangem.features.managetokens.ui

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.core.ui.components.block.information.InformationBlock
import com.tangem.core.ui.components.fields.SimpleTextField
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.managetokens.component.preview.PreviewCustomTokenFormComponent
import com.tangem.features.managetokens.entity.ClickableFieldUM
import com.tangem.features.managetokens.entity.CustomTokenFormUM
import com.tangem.features.managetokens.entity.TextInputFieldUM

internal fun LazyListScope.customTokenFormContent(model: CustomTokenFormUM) {
    item {
        ClickableField(
            modifier = Modifier.padding(bottom = TangemTheme.dimens.spacing12),
            model = model.networkName,
        )
    }

    item {
        Column(
            modifier = Modifier
                .padding(bottom = TangemTheme.dimens.spacing12)
                .background(
                    color = TangemTheme.colors.background.action,
                    shape = TangemTheme.shapes.roundedCornersXMedium,
                ),
        ) {
            TextField(
                model = model.contractAddress,
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Next,
                ),
            )
            TextField(
                model = model.tokenName,
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Next,
                ),
            )
            TextField(
                model = model.tokenSymbol,
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Next,
                ),
            )
            TextField(
                model = model.tokenDecimals,
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next,
                ),
            )
        }
    }

    item {
        ClickableField(
            modifier = Modifier.padding(bottom = TangemTheme.dimens.spacing12),
            model = model.derivationPath,
        )
    }

    items(
        items = model.notifications,
        key = { it.id },
    ) { notification ->
        Notification(
            modifier = Modifier.padding(bottom = TangemTheme.dimens.spacing12),
            config = notification.config,
            containerColor = TangemTheme.colors.button.disabled,
        )
    }
}

@Composable
private fun TextField(
    model: TextInputFieldUM,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    InformationBlock(
        modifier = modifier,
        title = {
            val color by animateColorAsState(
                targetValue = if (model.error != null) {
                    TangemTheme.colors.text.warning
                } else {
                    TangemTheme.colors.text.tertiary
                },
                label = "Field label color",
            )

            Text(
                text = (model.error ?: model.label).resolveReference(),
                style = TangemTheme.typography.subtitle2,
                color = color,
            )
        },
        content = {
            SimpleTextField(
                modifier = Modifier.padding(bottom = TangemTheme.dimens.spacing12),
                value = model.value,
                onValueChange = model.onValueChange,
                readOnly = false,
                placeholder = model.placeholder,
                singleLine = true,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
            )
        },
    )
}

@Composable
private fun ClickableField(model: ClickableFieldUM, modifier: Modifier = Modifier) {
    InformationBlock(
        modifier = modifier
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .clickable(onClick = model.onClick),
        title = {
            Text(
                text = model.label.resolveReference(),
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.tertiary,
            )
        },
        content = {
            Text(
                modifier = Modifier.padding(bottom = TangemTheme.dimens.spacing12),
                text = model.value.resolveReference(),
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.primary1,
            )
        },
    )
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Preview_CustomTokenFormContent(
    @PreviewParameter(PreviewCustomTokenFormComponentProvider::class)
    component: PreviewCustomTokenFormComponent,
) {
    TangemThemePreview {
        LazyColumn(
            modifier = Modifier.background(color = TangemTheme.colors.background.secondary),
        ) { component.content(scope = this) }
    }
}

private class PreviewCustomTokenFormComponentProvider :
    PreviewParameterProvider<PreviewCustomTokenFormComponent> {

    override val values: Sequence<PreviewCustomTokenFormComponent>
        get() = sequenceOf(
            PreviewCustomTokenFormComponent(),
            PreviewCustomTokenFormComponent(
                contractAddress = TextInputFieldUM(
                    label = stringReference("Contract address"),
                    value = "0x1234567890",
                    error = stringReference("Contract address is invalid"),
                    placeholder = stringReference("0x1234567890"),
                    onValueChange = {},
                ),
            ),
        )
}
// endregion Preview