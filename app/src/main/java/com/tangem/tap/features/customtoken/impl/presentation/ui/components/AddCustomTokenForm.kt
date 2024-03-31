package com.tangem.tap.features.customtoken.impl.presentation.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
* [REDACTED_AUTHOR]
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
            if (model.showTokenFields) InputField(model = model.contractAddressInputField)
            SelectorField(model = model.networkSelectorField)
            if (model.showTokenFields) InputField(model = model.tokenNameInputField)
            if (model.showTokenFields) InputField(model = model.tokenSymbolInputField)
            if (model.showTokenFields) InputField(model = model.decimalsInputField)
            model.derivationPathSelectorField?.let { SelectorField(model = it) }
            if (model.derivationPathInputField?.showField == true) InputField(model = model.derivationPathInputField)
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
            is AddCustomTokenInputField.DerivationPath -> true
        }

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = model.value,
            onValueChange = model.onValueChange,
            keyboardOptions = model.keyboardOptions,
            label = {
                Text(
                    text = model.label.resolveReference(),
                    style = TangemTheme.typography.caption2,
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

/**
 * Be careful with this function – ExposedDropdownMenuBox can crash the app if it is open and user clicks system back
 * button. It was fixed in compose-material 1.6.4.
 */
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
                                style = TangemTheme.typography.caption2,
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
