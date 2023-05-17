package com.tangem.tap.features.customtoken.impl.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.res.TangemTheme
import com.tangem.tap.common.compose.TangemTextFieldsDefault
import com.tangem.tap.features.customtoken.impl.presentation.models.AddCustomTokenForm
import com.tangem.tap.features.customtoken.impl.presentation.models.AddCustomTokenInputField
import com.tangem.tap.features.customtoken.impl.presentation.models.AddCustomTokenSelectorField
import com.tangem.tap.features.customtoken.impl.presentation.ui.AddCustomTokenPreviewData
import com.tangem.tap.features.details.ui.cardsettings.resolveReference

/**
 * Add custom token form
 *
 * @param model component model
 *
[REDACTED_AUTHOR]
 */
@Composable
internal fun AddCustomTokenForm(model: AddCustomTokenForm) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(TangemTheme.dimens.spacing16),
        shape = RoundedCornerShape(TangemTheme.dimens.radius8),
        backgroundColor = TangemTheme.colors.background.primary,
        elevation = TangemTheme.dimens.elevation4,
    ) {
        Column(
            modifier = Modifier.padding(TangemTheme.dimens.spacing16),
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8),
        ) {
            InputField(model = model.contractAddressInputField)
            SelectorField(model = model.networkSelectorField)
            InputField(model = model.tokenNameInputField)
            InputField(model = model.tokenSymbolInputField)
            InputField(model = model.decimalsInputField)
            model.derivationPathSelectorField?.let { SelectorField(model = it) }
        }
    }
}

@Composable
private fun InputField(model: AddCustomTokenInputField) {
    Column {
        val isError = (model as? AddCustomTokenInputField.ContactAddress)?.isError ?: false

        TextField(model, isError)

        (model as? AddCustomTokenInputField.ContactAddress)?.error?.resolveReference()?.let {
            AnimatedVisibility(
                visible = isError,
                enter = fadeIn() + slideInVertically(),
                exit = slideOutVertically() + fadeOut(),
            ) {
                Text(
                    text = it,
                    color = MaterialTheme.colors.error,
                    style = TangemTheme.typography.body2,
                )
            }
        }
    }
}

@Composable
private fun TextField(model: AddCustomTokenInputField, isError: Boolean) {
    Box {
        val isEnabled = when (model) {
            is AddCustomTokenInputField.ContactAddress -> true
            is AddCustomTokenInputField.Decimals -> model.isEnabled
            is AddCustomTokenInputField.TokenName -> model.isEnabled
            is AddCustomTokenInputField.TokenSymbol -> model.isEnabled
        }

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = model.value,
            onValueChange = model.onValueChange,
            keyboardOptions = model.keyboardOptions,
            label = {
                Text(
                    text = model.label.resolveReference(),
                    style = TangemTheme.typography.caption,
                    color = TangemTextFieldsDefault.defaultTextFieldColors.labelColor(
                        enabled = isEnabled,
                        error = isError,
                        interactionSource = remember { MutableInteractionSource() },
                    ).value,
                )
            },
            placeholder = {
                Text(
                    text = model.placeholder.resolveReference(),
                    style = TangemTheme.typography.body1,
                    color = TangemTextFieldsDefault.defaultTextFieldColors
                        .placeholderColor(enabled = isEnabled)
                        .value,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            singleLine = true,
            enabled = isEnabled,
            isError = isError,
            colors = TangemTextFieldsDefault.defaultTextFieldColors,
        )

        AnimatedVisibility(
            visible = (model as? AddCustomTokenInputField.ContactAddress)?.isLoading ?: false,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = TangemTheme.dimens.spacing6)
                .padding(bottom = TangemTheme.dimens.spacing6),
        ) {
            LinearProgressIndicator(color = TangemTheme.colors.icon.primary1)
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun SelectorField(model: AddCustomTokenSelectorField) {
    var isExpanded by remember { mutableStateOf(value = false) }

    ExposedDropdownMenuBox(
        expanded = isExpanded,
        onExpandedChange = { isExpanded = !isExpanded },
    ) {
        val isEnabled = (model as? AddCustomTokenSelectorField.DerivationPath)?.isEnabled ?: true
        OutlinedTextField(
            value = when (val item = model.selectedItem) {
                is AddCustomTokenSelectorField.SelectorItem.Title -> item.title
                is AddCustomTokenSelectorField.SelectorItem.TitleWithSubtitle -> item.subtitle
            }.resolveReference(),
            modifier = Modifier.fillMaxWidth(),
            onValueChange = {},
            readOnly = true,
            enabled = isEnabled,
            label = { Text(text = model.label.resolveReference()) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
            colors = TangemTextFieldsDefault.defaultTextFieldColors,
        )

        ExposedDropdownMenu(
            expanded = isExpanded && isEnabled,
            onDismissRequest = { isExpanded = false },
        ) {
            FocusRequester
            model.items.forEachIndexed { index, item ->
                DropdownMenuItem(
                    onClick = {
                        model.onMenuItemClick(index)
                        isExpanded = false
                    },
                ) {
                    Column {
                        Text(text = item.title.resolveReference())

                        val subtitle = (item as? AddCustomTokenSelectorField.SelectorItem.TitleWithSubtitle)
                            ?.subtitle?.resolveReference()

                        if (!subtitle.isNullOrBlank()) {
                            Text(
                                text = subtitle,
                                color = TangemTheme.colors.text.secondary,
                                maxLines = 1,
                                style = TangemTheme.typography.caption,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun Preview_AddCustomTokenForm(@PreviewParameter(AddCustomTokenFormProvider::class) model: AddCustomTokenForm) {
    TangemTheme {
        AddCustomTokenForm(model)
    }
}

private class AddCustomTokenFormProvider : CollectionPreviewParameterProvider<AddCustomTokenForm>(
    collection = listOf(
        AddCustomTokenPreviewData.createDefaultForm(),
        AddCustomTokenPreviewData.createDefaultForm().copy(derivationPathSelectorField = null),
        AddCustomTokenPreviewData.createDefaultForm().let { form ->
            form.copy(contractAddressInputField = form.contractAddressInputField.copy(isLoading = true))
        },
    ),
)