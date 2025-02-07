package com.tangem.features.managetokens.ui

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.components.block.information.InformationBlock
import com.tangem.core.ui.components.bottomFade
import com.tangem.core.ui.components.buttons.common.TangemButton
import com.tangem.core.ui.components.buttons.common.TangemButtonIconPosition
import com.tangem.core.ui.components.buttons.common.TangemButtonsDefaults
import com.tangem.core.ui.components.fields.SimpleTextField
import com.tangem.core.ui.components.isOpened
import com.tangem.core.ui.components.keyboardAsState
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.managetokens.component.preview.PreviewCustomTokenFormComponent
import com.tangem.features.managetokens.entity.customtoken.ClickableFieldUM
import com.tangem.features.managetokens.entity.customtoken.CustomTokenFormUM
import com.tangem.features.managetokens.entity.customtoken.CustomTokenFormUM.TokenFormUM.Field
import com.tangem.features.managetokens.entity.customtoken.TextInputFieldUM
import com.tangem.features.managetokens.impl.R
import com.tangem.features.managetokens.ui.component.AddCustomTokenDescription
import kotlinx.collections.immutable.mutate

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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .bottomFade(),
        ) {
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .fillMaxSize()
                    .padding(bottom = 128.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing16),
            ) {
                AddCustomTokenDescription()
                FormContent(model)
            }
        }

        TangemButton(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = TangemTheme.dimens.spacing16 + bottomBarHeight)
                .fillMaxWidth(),
            text = stringResourceSafe(id = R.string.common_add_token),
            colors = TangemButtonsDefaults.primaryButtonColors,
            enabled = model.canAddToken,
            showProgress = model.isValidating,
            animateContentChange = true,
            icon = if (model.needToAddDerivation) {
                TangemButtonIconPosition.End(R.drawable.ic_tangem_24)
            } else {
                TangemButtonIconPosition.None
            },
            textStyle = TangemTheme.typography.subtitle1,
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
            TokenForm(tokenForm = tokenForm)
        }

        val derivationPath = model.derivationPath
        if (derivationPath != null) {
            ClickableField(model = derivationPath)
        }

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
        tokenForm.fields.values.forEach { field ->
            TextField(model = field)
        }
    }
}

@Composable
private fun TextField(model: TextInputFieldUM, modifier: Modifier = Modifier) {
    InformationBlock(
        modifier = modifier,
        title = {
            val color by animateColorAsState(
                targetValue = when {
                    !model.isEnabled -> {
                        TangemTheme.colors.text.disabled
                    }
                    model.error != null -> {
                        TangemTheme.colors.text.warning
                    }
                    model.isEnabled || model.value.isNotBlank() || model.isFocused -> {
                        TangemTheme.colors.text.tertiary
                    }
                    else -> {
                        TangemTheme.colors.text.disabled
                    }
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
            val color by animateColorAsState(
                targetValue = if (model.isEnabled) {
                    TangemTheme.colors.text.primary1
                } else {
                    TangemTheme.colors.text.disabled
                },
                label = "Field value color",
            )

            SimpleTextField(
                modifier = Modifier
                    .padding(bottom = TangemTheme.dimens.spacing12)
                    .fillMaxWidth()
                    .onFocusChanged {
                        model.onFocusChange(it.isFocused)
                    },
                value = model.value,
                color = color,
                onValueChange = model.onValueChange,
                placeholder = model.placeholder,
                readOnly = !model.isEnabled && !model.isFocused,
                singleLine = true,
                keyboardOptions = model.keyboardOptions,
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
            PreviewCustomTokenFormComponent(
                tokenForm = PreviewCustomTokenFormComponent.tokenForm.let { form ->
                    form.copy(
                        fields = form.fields.mutate {
                            it[Field.CONTRACT_ADDRESS] = it[Field.CONTRACT_ADDRESS]!!.copy(
                                label = stringReference("Contract address"),
                                value = "0x1234567890",
                                placeholder = stringReference("0x1234567890"),
                            )
                        },
                    )
                },
            ),
            PreviewCustomTokenFormComponent(
                tokenForm = PreviewCustomTokenFormComponent.tokenForm.let { form ->
                    form.copy(
                        fields = form.fields.mutate {
                            it[Field.CONTRACT_ADDRESS] = it[Field.CONTRACT_ADDRESS]!!.copy(
                                label = stringReference("Contract address"),
                                value = "0x1234567890",
                                error = stringReference("Contract address is invalid"),
                                placeholder = stringReference("0x1234567890"),
                            )
                        },
                    )
                },
            ),
            PreviewCustomTokenFormComponent(
                tokenForm = null,
            ),
            PreviewCustomTokenFormComponent(
                derivationPath = null,
            ),
            PreviewCustomTokenFormComponent(
                tokenForm = null,
                derivationPath = null,
            ),
        )
}
// endregion Preview