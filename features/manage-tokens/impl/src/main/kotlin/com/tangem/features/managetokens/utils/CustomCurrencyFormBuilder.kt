package com.tangem.features.managetokens.utils

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.managetokens.component.CustomTokenFormComponent
import com.tangem.features.managetokens.entity.customtoken.CustomTokenFormUM
import com.tangem.features.managetokens.entity.customtoken.CustomTokenFormUM.TokenFormUM.Field
import com.tangem.features.managetokens.entity.customtoken.TextInputFieldUM
import com.tangem.features.managetokens.impl.R
import kotlinx.collections.immutable.persistentMapOf
import javax.inject.Inject

@ComponentScoped
internal class CustomCurrencyFormBuilder @Inject constructor(
    paramsContainer: ParamsContainer,
) {

    private val params: CustomTokenFormComponent.Params = paramsContainer.require()

    fun buildForm(
        updateFormFieldValue: (Field, String) -> Unit,
        updateFormFieldFocus: (Field, Boolean) -> Unit,
    ): CustomTokenFormUM.TokenFormUM {
        val formValues = params.formValues
        val fields = persistentMapOf(
            Field.CONTRACT_ADDRESS to TextInputFieldUM(
                label = resourceReference(R.string.custom_token_contract_address_input_title),
                placeholder = stringReference(CONTRACT_ADDRESS_PLACEHOLDER),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    keyboardType = KeyboardType.Text,
                ),
                onValueChange = { value ->
                    updateFormFieldValue(Field.CONTRACT_ADDRESS, value)
                },
                onFocusChange = { isFocused ->
                    updateFormFieldFocus(Field.CONTRACT_ADDRESS, isFocused)
                },
            ),
            Field.NAME to TextInputFieldUM(
                label = resourceReference(R.string.custom_token_name_input_title),
                placeholder = resourceReference(R.string.custom_token_name_input_placeholder),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    keyboardType = KeyboardType.Text,
                ),
                onValueChange = { value ->
                    updateFormFieldValue(Field.NAME, value)
                },
                onFocusChange = { isFocused ->
                    updateFormFieldFocus(Field.NAME, isFocused)
                },
            ),
            Field.SYMBOL to TextInputFieldUM(
                label = resourceReference(R.string.custom_token_token_symbol_input_title),
                placeholder = resourceReference(R.string.custom_token_token_symbol_input_placeholder),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    keyboardType = KeyboardType.Text,
                ),
                onValueChange = { value ->
                    updateFormFieldValue(Field.SYMBOL, value)
                },
                onFocusChange = { isFocused ->
                    updateFormFieldFocus(Field.SYMBOL, isFocused)
                },
            ),
            Field.DECIMALS to TextInputFieldUM(
                label = resourceReference(R.string.custom_token_decimals_input_title),
                placeholder = stringReference(DECIMALS_PLACEHOLDER),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                onValueChange = { value ->
                    updateFormFieldValue(Field.DECIMALS, value)
                },
                onFocusChange = { isFocused ->
                    updateFormFieldFocus(Field.DECIMALS, isFocused)
                },
            ),
        )
        val form = CustomTokenFormUM.TokenFormUM(fields)

        return formValues.fillValues(form)
    }

    private companion object {
        const val CONTRACT_ADDRESS_PLACEHOLDER = "0x000000000000000000000000000..."
        const val DECIMALS_PLACEHOLDER = "0"
    }
}