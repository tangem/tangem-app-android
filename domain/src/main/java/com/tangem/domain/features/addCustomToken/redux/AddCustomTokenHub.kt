package com.tangem.domain.features.addCustomToken.redux

import android.webkit.ValueCallback
import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.common.form.*
import com.tangem.domain.features.addCustomToken.*
import com.tangem.domain.features.addCustomToken.CustomTokenFieldId.*
import com.tangem.domain.features.addCustomToken.redux.AddCustomTokenAction.*
import com.tangem.domain.store.BaseStoreHub
import com.tangem.domain.store.DomainState
import com.tangem.domain.store.dispatchOnMain
import kotlinx.coroutines.cancel
import org.rekotlin.Action
import org.rekotlin.DispatchFunction

/**
[REDACTED_AUTHOR]
 */
internal object AddCustomTokenHub : BaseStoreHub<AddCustomTokensState>("AddCustomTokenHub") {

    override val initialState: AddCustomTokensState = AddCustomTokensState()

    override fun handle(state: () -> DomainState?, action: Action, dispatch: DispatchFunction) = when (action) {
        is OnBackPressed -> hubScope.cancel()
        else -> super.handle(state, action, dispatch)
    }

    override suspend fun handleAction(
        state: DomainState,
        action: Action,
        dispatch: DispatchFunction,
        cancel: ValueCallback<Action>
    ) {
        if (action !is AddCustomTokenAction) return
        val state = state.addCustomTokensState

        when (action) {
            is OnTokenContractAddressChanged -> {
                val contractAddress = action.value
                val validator: TokenContractAddressValidator = getValidator(ContractAddress, state)
                val error = validator.validate(contractAddress)
                addOrRemoveError(ContractAddress, error)
                if (error != null) return

                val manager = state.addCustomTokenManager
                val selectedNetwork: Blockchain? = getField<TokenNetworkField>(Network, state).value.let {
                    if (it == Blockchain.Unknown) null else it
                }
                val foundTokens = manager.findContractAddress(contractAddress, selectedNetwork?.id)
                when {
                    foundTokens.isEmpty() -> {}
                    foundTokens.size == 1 -> {
                        // fill and disable other fields by token info
                        val token = foundTokens[0]
                        dispatchOnMain(FillTokenFields(token, token.contracts[0]))
                    }
                    else -> {
                        // show tokens list for selection

                    }

                }
            }
            is OnTokenNetworkChanged -> {
                val validator: TokenNetworkValidator = getValidator(Network, state)
                addOrRemoveError(Network, validator.validate(action.value))
            }
            is OnTokenFieldChanged -> {
                val validator: StringIsNotEmptyValidator = getValidator(action.id, state)
                addOrRemoveError(action.id as CustomTokenFieldId, validator.validate(action.value))
            }
            is OnTokenDerivationPathChanged -> {
                val validator: DerivationPathValidator = getValidator(DerivationPath, state)
                addOrRemoveError(DerivationPath, validator.validate(action.value))
            }
            is FillTokenFields -> {
                val networkField = getField<TokenNetworkField>(Network, state)
                val nameField = getField<TokenField>(Name, state)
                val symbolField = getField<TokenField>(Symbol, state)
                val decimalsField = getField<TokenField>(Decimals, state)

                val token = action.token
                val contract = action.contract
                networkField.value = Blockchain.fromId(contract.networkId)
                nameField.value = token.name
                symbolField.value = token.symbol
                decimalsField.value = contract.decimalCount.toString()

                dispatchOnMain(UpdateForm)
            }
        }
    }

    private suspend fun addOrRemoveError(id: CustomTokenFieldId, error: AddCustomTokenError?) {
        if (error == null) {
            dispatchOnMain(Error.Remove(id))
        } else {
            dispatchOnMain(Error.Add(id, error))
        }
    }

    private inline fun <reified T> getField(id: FieldId, state: AddCustomTokensState): T {
        return state.form.getField(id) as T
    }

    private inline fun <reified T> getValidator(id: FieldId, state: AddCustomTokensState): T {
        return state.getValidator(id) as T
    }

    override fun reduceAction(action: Action, state: AddCustomTokensState): AddCustomTokensState {
        return when (action) {
            is SetTangemTechAuthHeader -> {
                state.apply { addCustomTokenManager.attachAuthKey(action.cardPublicKeyHex) }
            }
            is UpdateForm -> updateFormState(state)
            is OnTokenNetworkChanged -> {
                val field: TokenNetworkField = getField(Network, state)
                field.value = action.value
                updateFormState(state)
            }
            is OnTokenContractAddressChanged -> {
                val field: TokenField = getField(ContractAddress, state)
                field.value = action.value
                updateFormState(state)
            }
            is OnTokenDerivationPathChanged -> {
                val field: TokenDerivationPathField = getField(Network, state)
                field.value = action.value
                updateFormState(state)
            }
            is OnTokenFieldChanged -> {
                val field: TokenField = getField(action.id, state)
                field.value = action.value
                updateFormState(state)
            }
            is Error.Add -> {
                val newMap = state.formErrors.toMutableMap().apply { this[action.id] = action.error }
                state.copy(formErrors = newMap)
            }
            is Error.Remove -> {
                val newMap = state.formErrors.toMutableMap().apply { remove(action.id) }
                state.copy(formErrors = newMap)
            }
            is Warning.Add -> {
                val newList = state.warnings.toMutableList().apply { add(action.warning) }
                state.copy(warnings = newList)
            }
            is Warning.Remove -> {
                val newList = state.warnings.toMutableList().apply { remove(action.warning) }
                state.copy(warnings = newList)
            }
            else -> state
        }
    }

    private fun updateFormState(state: AddCustomTokensState): AddCustomTokensState {
        return state.copy(form = Form(state.form.fieldList))
    }
}