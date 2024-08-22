package com.tangem.features.managetokens.component.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.managetokens.component.CustomTokenFormComponent
import com.tangem.features.managetokens.entity.customtoken.ClickableFieldUM
import com.tangem.features.managetokens.entity.customtoken.CustomTokenFormUM
import com.tangem.features.managetokens.entity.customtoken.CustomTokenFormValues
import com.tangem.features.managetokens.entity.customtoken.TextInputFieldUM
import com.tangem.features.managetokens.impl.R
import com.tangem.features.managetokens.ui.CustomTokenFormContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class DefaultCustomTokenFormComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Assisted private val params: CustomTokenFormComponent.Params,
) : CustomTokenFormComponent, AppComponentContext by context {

    private val state: MutableStateFlow<CustomTokenFormUM> = MutableStateFlow(
        value = getInitialState(),
    )

    @Composable
    override fun Content(modifier: Modifier) {
        val state by state.collectAsStateWithLifecycle()

        CustomTokenFormContent(
            modifier = modifier,
            model = state,
        )
    }

    private fun getInitialState(): CustomTokenFormUM {
        return CustomTokenFormUM(
            networkName = ClickableFieldUM(
                label = resourceReference(R.string.custom_token_network_input_title),
                value = params.network.name,
                onClick = ::selectNetwork,
            ),
            tokenForm = getInitialTokenForm(),
            derivationPath = ClickableFieldUM(
                label = resourceReference(R.string.custom_token_derivation_path),
                value = params.derivationPath.name,
                onClick = ::selectDerivationPath,
            ),
            saveToken = {
                // TODO: Save token: [REDACTED_JIRA]
            },
        )
    }

    private fun getInitialTokenForm(): CustomTokenFormUM.TokenFormUM {
        val formValues = params.formValues

        val form = CustomTokenFormUM.TokenFormUM(
            contractAddress = TextInputFieldUM(
                label = resourceReference(R.string.custom_token_contract_address_input_title),
                placeholder = stringReference(value = "0x000000000000000000000000000..."),
                onValueChange = ::updateContractAddress,
            ),
            name = TextInputFieldUM(
                label = resourceReference(R.string.custom_token_name_input_title),
                placeholder = resourceReference(R.string.custom_token_name_input_placeholder),
                onValueChange = ::updateTokenName,
            ),
            symbol = TextInputFieldUM(
                label = resourceReference(R.string.custom_token_token_symbol_input_title),
                placeholder = resourceReference(R.string.custom_token_token_symbol_input_placeholder),
                onValueChange = ::updateTokenSymbol,
            ),
            decimals = TextInputFieldUM(
                label = resourceReference(R.string.custom_token_decimals_input_title),
                placeholder = stringReference(value = "0"),
                onValueChange = ::updateDecimals,
            ),
        )

        return formValues.fillValues(form)
    }

    private fun updateContractAddress(value: String) {
        // TODO: Add field validation: [REDACTED_JIRA]
        state.update { state ->
            val tokenForm = state.tokenForm ?: return@update state

            state.copy(
                tokenForm = tokenForm.copy(
                    contractAddress = tokenForm.contractAddress.copy(value = value),
                ),
            )
        }
    }

    private fun updateTokenName(value: String) {
        // TODO: Add field validation: [REDACTED_JIRA]
        state.update { state ->
            val tokenForm = state.tokenForm ?: return@update state

            state.copy(
                tokenForm = tokenForm.copy(
                    name = tokenForm.name.copy(value = value),
                ),
            )
        }
    }

    private fun updateTokenSymbol(value: String) {
        // TODO: Add field validation: [REDACTED_JIRA]
        state.update { state ->
            val tokenForm = state.tokenForm ?: return@update state

            state.copy(
                tokenForm = tokenForm.copy(
                    symbol = tokenForm.symbol.copy(value = value),
                ),
            )
        }
    }

    private fun updateDecimals(value: String) {
        // TODO: Add field validation: [REDACTED_JIRA]
        state.update { state ->
            val tokenForm = state.tokenForm ?: return@update state

            state.copy(
                tokenForm = tokenForm.copy(
                    decimals = tokenForm.decimals.copy(value = value),
                ),
            )
        }
    }

    private fun selectNetwork() {
        params.onSelectNetworkClick(CustomTokenFormValues(state.value.tokenForm))
    }

    private fun selectDerivationPath() {
        params.onSelectDerivationPathClick(CustomTokenFormValues(state.value.tokenForm))
    }

    @AssistedFactory
    interface Factory : CustomTokenFormComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: CustomTokenFormComponent.Params,
        ): DefaultCustomTokenFormComponent
    }

    private companion object {
        const val FORM_VALUES_KEY = "form_values"
    }
}