package com.tangem.tap.features.customtoken.impl.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import com.tangem.blockchain.common.Blockchain
import com.tangem.core.ui.res.TangemTheme
import com.tangem.tap.common.compose.TangemTextFieldsDefault
import com.tangem.tap.features.customtoken.impl.presentation.models.AddCustomTokenForm
import com.tangem.tap.features.customtoken.impl.presentation.models.AddCustomTokenInputField
import com.tangem.tap.features.customtoken.impl.presentation.models.AddCustomTokenSelectorField
import com.tangem.tap.features.details.ui.cardsettings.TextReference
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
    Box {
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
                        enabled = model.isEnabled,
                        error = model.isError,
                        interactionSource = remember { MutableInteractionSource() },
                    ).value,
                )
            },
            placeholder = {
                Text(
                    text = model.placeholder.resolveReference(),
                    style = TangemTheme.typography.body1,
                    color = TangemTextFieldsDefault.defaultTextFieldColors
                        .placeholderColor(enabled = model.isEnabled)
                        .value,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            singleLine = true,
            enabled = model.isEnabled,
            isError = model.isError,
            colors = TangemTextFieldsDefault.defaultTextFieldColors,
        )

        AnimatedVisibility(
            visible = model.isLoading,
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
        OutlinedTextField(
            value = when (val item = model.selectedItem) {
                is AddCustomTokenSelectorField.SelectorItem.Title -> item.title
                is AddCustomTokenSelectorField.SelectorItem.TitleWithSubtitle -> item.subtitle
            }.resolveReference(),
            modifier = Modifier.fillMaxWidth(),
            onValueChange = {},
            readOnly = true,
            enabled = model.isEnabled,
            label = { Text(text = model.label.resolveReference()) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
            colors = TangemTextFieldsDefault.defaultTextFieldColors,
        )

        ExposedDropdownMenu(
            expanded = isExpanded && model.isEnabled,
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
private fun Preview_AddCustomTokenForm() {
    TangemTheme {
        AddCustomTokenForm(
            AddCustomTokenForm(
                contractAddressInputField = AddCustomTokenInputField.ContactAddress(
                    value = "",
                    onValueChange = {},
                    isError = false,
                    isLoading = false,
                ),
                networkSelectorField = AddCustomTokenSelectorField.Network(
                    selectedItem = AddCustomTokenSelectorField.SelectorItem.Title(
                        title = TextReference.Str(value = "Avalanche"),
                        blockchain = Blockchain.Avalanche,
                    ),
                    items = listOf(),
                    onMenuItemClick = {},
                ),
                tokenNameInputField = AddCustomTokenInputField.TokenName(
                    value = "",
                    onValueChange = {},
                    isEnabled = false,
                    isError = false,
                ),
                tokenSymbolInputField = AddCustomTokenInputField.TokenSymbol(
                    value = "",
                    onValueChange = {},
                    isEnabled = false,
                    isError = false,
                ),
                decimalsInputField = AddCustomTokenInputField.Decimals(
                    value = "",
                    onValueChange = {},
                    isEnabled = false,
                    isError = false,
                ),
                derivationPathSelectorField = null,
            ),
        )
    }
}
