package com.tangem.features.managetokens.ui

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.block.information.InformationBlock
import com.tangem.core.ui.components.fields.SimpleTextField
import com.tangem.core.ui.components.isOpened
import com.tangem.core.ui.components.keyboardAsState
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.managetokens.component.preview.PreviewCustomTokenFormComponent
import com.tangem.features.managetokens.entity.customtoken.ClickableFieldUM
import com.tangem.features.managetokens.entity.customtoken.CustomTokenFormUM
import com.tangem.features.managetokens.entity.customtoken.TextInputFieldUM
import com.tangem.features.managetokens.impl.R
import com.tangem.features.managetokens.ui.component.AddCustomTokenDescription

@Composable
internal fun CustomTokenFormContent(model: CustomTokenFormUM, modifier: Modifier = Modifier) {
    val keyboard by keyboardAsState()
    val bottomBarHeight by animateDpAsState(
        label = "Bottom bar height",
        targetValue = if (keyboard.isOpened) {
            TangemTheme.dimens.spacing0
        } else {
            with(LocalDensity.current) {
                WindowInsets.systemBars.getBottom(density = this).toDp()
            }
        },
    )

    Box(
        modifier = modifier
            .imePadding()
            .fillMaxSize()
            .background(color = TangemTheme.colors.background.secondary),
    ) {
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .fillMaxSize()
                .padding(bottom = TangemTheme.dimens.spacing76),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing16),
        ) {
            AddCustomTokenDescription()
            FormContent(model)
        }

        PrimaryButton(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = TangemTheme.dimens.spacing16 + bottomBarHeight)
                .fillMaxWidth(),
            text = stringResource(id = R.string.custom_token_add_token),
            enabled = model.canAddToken,
            onClick = model.saveToken,
        )
    }
}

@Composable
private fun FormContent(model: CustomTokenFormUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
    ) {
        ClickableField(
            model = model.networkName,
        )

        val tokenForm = model.tokenForm
        if (tokenForm != null) {
            TokenForm(tokenForm)
        }

        ClickableField(
            model = model.derivationPath,
        )

        model.notifications.fastForEach { notification ->
            Notification(
                config = notification.config,
                containerColor = TangemTheme.colors.button.disabled,
            )
        }
    }
}

@Composable
private fun TokenForm(tokenForm: CustomTokenFormUM.TokenFormUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(
                color = TangemTheme.colors.background.action,
                shape = TangemTheme.shapes.roundedCornersXMedium,
            ),
    ) {
        TextField(
            model = tokenForm.contractAddress,
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Next,
            ),
        )
        TextField(
            model = tokenForm.name,
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Next,
            ),
        )
        TextField(
            model = tokenForm.symbol,
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Next,
            ),
        )
        TextField(
            model = tokenForm.decimals,
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next,
            ),
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
                modifier = Modifier
                    .padding(bottom = TangemTheme.dimens.spacing12)
                    .fillMaxWidth(),
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
        component.Content(modifier = Modifier)
    }
}

private class PreviewCustomTokenFormComponentProvider :
    PreviewParameterProvider<PreviewCustomTokenFormComponent> {

    override val values: Sequence<PreviewCustomTokenFormComponent>
        get() = sequenceOf(
            PreviewCustomTokenFormComponent(),
            PreviewCustomTokenFormComponent(
                tokenForm = PreviewCustomTokenFormComponent.tokenForm.copy(
                    contractAddress = TextInputFieldUM(
                        label = stringReference("Contract address"),
                        value = "0x1234567890",
                        error = stringReference("Contract address is invalid"),
                        placeholder = stringReference("0x1234567890"),
                        onValueChange = {},
                    ),
                ),
            ),
            PreviewCustomTokenFormComponent(
                tokenForm = null,
            ),
        )
}
// endregion Preview
