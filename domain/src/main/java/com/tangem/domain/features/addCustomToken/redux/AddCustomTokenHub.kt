package com.tangem.domain.features.addCustomToken.redux

import android.webkit.ValueCallback
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.DerivationStyle
import com.tangem.common.services.Result
import com.tangem.domain.DomainDialog
import com.tangem.domain.DomainException
import com.tangem.domain.common.extensions.fromNetworkId
import com.tangem.domain.common.extensions.toNetworkId
import com.tangem.domain.common.form.*
import com.tangem.domain.features.addCustomToken.*
import com.tangem.domain.features.addCustomToken.CustomTokenFieldId.*
import com.tangem.domain.features.addCustomToken.redux.AddCustomTokenAction.*
import com.tangem.domain.redux.BaseStoreHub
import com.tangem.domain.redux.DomainState
import com.tangem.domain.redux.dispatchOnMain
import com.tangem.domain.redux.domainStore
import com.tangem.domain.redux.global.DomainGlobalAction
import com.tangem.network.api.tangemTech.Coins
import com.tangem.network.api.tangemTech.TangemTechService
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.rekotlin.Action
import timber.log.Timber

/**
[REDACTED_AUTHOR]
 */
internal class AddCustomTokenHub : BaseStoreHub<AddCustomTokenState>("AddCustomTokenHub") {

    private val hubState: AddCustomTokenState
        get() = domainStore.state.addCustomTokensState

    override fun getHubState(storeState: DomainState): AddCustomTokenState = hubState

    override fun updateStoreState(storeState: DomainState, newHubState: AddCustomTokenState): DomainState {
        return storeState.copy(addCustomTokensState = newHubState)
    }

    private val contractAddressValidator: TokenContractAddressValidator
        get() = hubState.getValidator(ContractAddress)
    private val nameValidator: TokenNameValidator
        get() = hubState.getValidator(Name)
    private val symbolValidator: TokenSymbolValidator
        get() = hubState.getValidator(Symbol)
    private val decimalsValidator: TokenDecimalsValidator
        get() = hubState.getValidator(Decimals)
    val networkValidator: TokenNetworkValidator
        get() = hubState.getValidator(Network)

    override suspend fun handleAction(
        action: Action,
        storeState: DomainState,
        cancel: ValueCallback<Action>
    ) {
        if (action !is AddCustomTokenAction) return

        when (action) {
            is Init.SetAddedCurrencies -> {}
            is Init.SetOnAddTokenCallback -> {}
            is OnCreate -> {
//                hubState.addedCurrencies.guard {
//                    return throwUnAppropriateInitialization("addedTokens")
//                }
            }
            is OnDestroy -> hubScope.cancel()
            is OnTokenContractAddressChanged -> {
                val address = action.contractAddress.value
                when (val error = ContractAddress.validate(address)) {
                    null -> {
                        ContractAddress.removeError()
                        dispatchOnMain(unlockTokenFieldsAction())
                    }
                    AddCustomTokenError.FieldIsEmpty -> {
                        ContractAddress.removeError()
                        dispatchOnMain(lockTokenFieldsAction())
                        return
                    }
                    AddCustomTokenError.InvalidContractAddress -> {
                        ContractAddress.addError(error)
                        dispatchOnMain(unlockTokenFieldsAction())
                        return
                    }
                    else -> {}
                }

                if (!action.contractAddress.isUserInput) return

                manageTokenChanges(requestInfoAboutContractAddress(address))
            }
            is OnTokenNetworkChanged -> {
                if (!action.blockchainNetwork.isUserInput) return

                val contractAddress = ContractAddress.getFieldValue<String>()
                val error = ContractAddress.validate(contractAddress)
                if (error == null) {
                    manageTokenChanges(requestInfoAboutContractAddress(contractAddress))
                } else {

                }
            }
            is OnTokenNameChanged -> {
                Name.addOrRemoveError(Name.validate(action.tokenName.value))
            }
            is OnTokenSymbolChanged -> {
                Symbol.addOrRemoveError(Symbol.validate(action.tokenSymbol.value))
            }
            is OnTokenDecimalsChanged -> {
                Decimals.addOrRemoveError(Decimals.validate(action.tokenDecimals.value))
            }
            is ClearTokenFields -> {
                Name.setFieldValue(Field.Data("", false))
                Symbol.setFieldValue(Field.Data("", false))
                Decimals.setFieldValue(Field.Data("", false))
                dispatchOnMain(UpdateForm(hubState))
            }
            is FillTokenFields -> {
                val token = action.token
                val contract = action.contract
                val blockchain = Blockchain.fromNetworkId(contract.networkId) ?: Blockchain.Unknown

                Network.setFieldValue(Field.Data(blockchain, false))
                Name.setFieldValue(Field.Data(token.name, false))
                Symbol.setFieldValue(Field.Data(token.symbol, false))
                Decimals.setFieldValue(Field.Data(contract.decimalCount.toString(), false))

                dispatchOnMain(UpdateForm(hubState))
            }
            is OnAddCustomTokenClicked -> {
//                if (hubState.allFieldsIsEmpty()) {
                dispatchOnMain(
                    DomainGlobalAction.ShowDialog(DomainDialog.DialogError(
                        AddCustomTokenError.InvalidDerivationPath
                    )))
                return
//                }
                when {
                    !hubState.customTokensFieldsIsEmpty() && !hubState.networkIsEmpty() -> {
                        hubState.getCompleteData(CompleteDataType.Token)
                    }
//                    !hubState.customTokensFieldsIsEmpty() && -> {
//                    }
                }
//                if (true) {
//                    dispatchOnMain(NavigationAction.PopBackTo())
//                    hubState.onTokenAddCallback?.invoke()
//                }
            }
            else -> {}
        }
    }

    private suspend fun requestInfoAboutContractAddress(
        contractAddress: String,
    ): List<Coins.CheckAddressResponse.Token> {
        val tangemTechServiceManager = requireNotNull(hubState.tangemTechServiceManager)
        dispatchOnMain(Screen.UpdateTokenFields(listOf(ContractAddress to ViewStates.TokenField(isLoading = true))))
        val field = hubState.getField<TokenBlockchainField>(Network)
        val selectedNetworkId: String? = field.data.value.let {
            if (it == Blockchain.Unknown) null else it
        }?.toNetworkId()

        // simulate loading effect. It would be better if the delay would only run if tokenManager.checkAddress()
        // got the result faster than 500ms and the delay would only be the difference between them.
        delay(500)

        val foundTokensResult = tangemTechServiceManager.checkAddress(contractAddress, selectedNetworkId)
        val result = when (foundTokensResult) {
            is Result.Success -> foundTokensResult.data
            is Result.Failure -> {
//                val warning = AddCustomTokenWarning.Network.CheckAddressRequestError
//                dispatchOnMain(Warning.Add(setOf(warning)))
                emptyList()
            }
        }
        dispatchOnMain(Screen.UpdateTokenFields(listOf(ContractAddress to ViewStates.TokenField(isLoading = false))))
        return result
    }

    private suspend fun manageTokenChanges(foundTokens: List<Coins.CheckAddressResponse.Token>) {
        val toAddWarnings = mutableSetOf<AddCustomTokenWarning>()
        val toRemoveWarnings = mutableSetOf<AddCustomTokenWarning>()

        when {
            foundTokens.isEmpty() -> {
                toAddWarnings.add(AddCustomTokenWarning.PotentialScamToken)
                toRemoveWarnings.add(AddCustomTokenWarning.TokenAlreadyAdded)
                dispatchOnMain(ClearTokenFields)
                dispatchOnMain(unlockTokenFieldsAction())
            }
            else -> {
                val token = foundTokens[0]
                val contracts = token.contracts
                when {
                    contracts.isEmpty() -> {
                        // TODO: refactoring:
                        Timber.e("Unexpected state -> throw to FB")
                    }
                    contracts.size == 1 -> {
                        val contract = contracts[0]
                        val isPersistIntoTheAppAddedTokenList = isPersistIntoTheAppAddedTokenList(token, contract)

                        if (isPersistIntoTheAppAddedTokenList) {
                            toAddWarnings.add(AddCustomTokenWarning.TokenAlreadyAdded)
                            toRemoveWarnings.add(AddCustomTokenWarning.PotentialScamToken)

                            dispatchOnMain(Screen.UpdateAddButton(ViewStates.AddButton(false)))
                            dispatchOnMain(lockTokenFieldsAction())
                        } else {
                            toRemoveWarnings.add(AddCustomTokenWarning.TokenAlreadyAdded)
                            dispatchOnMain(Screen.UpdateAddButton(ViewStates.AddButton(true)))

                            val isStandardDerivation = true
                            val tokenContract = token.contracts[0]
                            if (tokenContract.active && isStandardDerivation) {
                                toRemoveWarnings.add(AddCustomTokenWarning.PotentialScamToken)
                                dispatchOnMain(FillTokenFields(token, contract))
                                dispatchOnMain(lockTokenFieldsAction())
                            } else {
                                toAddWarnings.add(AddCustomTokenWarning.PotentialScamToken)
                                dispatchOnMain(ClearTokenFields)
                                dispatchOnMain(unlockTokenFieldsAction())
                            }
                        }
                    }
                    else -> {
                        val dialog = DomainDialog.SelectTokenDialog(
                            items = contracts,
                            networkIdConverter = { networkId ->
                                val blockchain = Blockchain.fromNetworkId(networkId)
                                if (blockchain == null || blockchain == Blockchain.Unknown) {
                                    throw DomainException.SelectTokeNetworkException(networkId)
                                }
                                hubState.convertBlockchainName(blockchain, "")
                            },
                            onSelect = { selectedContract ->
                                hubScope.launch {
                                    // find how to connect to the upper coroutineContext and dispatch through them
                                    dispatchOnMain(FillTokenFields(token, selectedContract))
                                    dispatchOnMain(lockTokenFieldsAction())
                                }
                            },
                        )
                        dispatchOnMain(DomainGlobalAction.ShowDialog(dialog))
                    }
                }
            }
        }

        if (toAddWarnings.isNotEmpty() || toRemoveWarnings.isNotEmpty()) {
            dispatchOnMain(Warning.Replace(toRemoveWarnings.toSet(), toAddWarnings.toSet()))
        }
    }

    private fun isPersistIntoTheAppAddedTokenList(
        token: Coins.CheckAddressResponse.Token,
        contract: Coins.CheckAddressResponse.Token.Contract
    ): Boolean = false

    private suspend fun CustomTokenFieldId.addError(error: AddCustomTokenError) {
        dispatchOnMain(FieldError.Add(this, error))
    }

    private suspend fun CustomTokenFieldId.removeError() {
        dispatchOnMain(FieldError.Remove(this))
    }

    private suspend fun CustomTokenFieldId.addOrRemoveError(error: AddCustomTokenError?) {
        when (error) {
            null -> removeError()
            else -> addError(error)
        }
    }

    private inline fun <reified T> CustomTokenFieldId.getField(): T {
        val state = hubState
        val value = when (this) {
            ContractAddress -> state.getField<TokenField>(this)
            Network -> state.getField<TokenBlockchainField>(this)
            Name -> state.getField<TokenField>(this)
            Symbol -> state.getField<TokenField>(this)
            Decimals -> state.getField<TokenField>(this)
            DerivationPath -> state.getField<TokenDerivationPathField>(this)
        }
        return value as T
    }

    private inline fun <reified T> CustomTokenFieldId.getFieldValue(): T {
        val value = when (this) {
            ContractAddress -> getField<TokenField>().data.value
            Network -> getField<TokenBlockchainField>().data.value
            Name -> getField<TokenField>().data.value
            Symbol -> getField<TokenField>().data.value
            Decimals -> getField<TokenField>().data.value
            DerivationPath -> getField<TokenDerivationPathField>().data.value
        }
        return value as T
    }

    private fun CustomTokenFieldId.setFieldValue(fieldData: Field.Data<*>) {
        when (this) {
            ContractAddress -> getField<TokenField>().data = fieldData as Field.Data<String>
            Network -> getField<TokenBlockchainField>().data = fieldData as Field.Data<Blockchain>
            Name -> getField<TokenField>().data = fieldData as Field.Data<String>
            Symbol -> getField<TokenField>().data = fieldData as Field.Data<String>
            Decimals -> getField<TokenField>().data = fieldData as Field.Data<String>
            DerivationPath -> getField<TokenDerivationPathField>().data = fieldData as Field.Data<Blockchain>
        }
    }

    private fun CustomTokenFieldId.validate(value: Any): AddCustomTokenError? {
        return when (this) {
            ContractAddress -> contractAddressValidator.validate(value as String)
            Network -> networkValidator.validate(value as Blockchain)
            Name -> nameValidator.validate(value as String)
            Symbol -> symbolValidator.validate(value as String)
            Decimals -> decimalsValidator.validate(value as String)
            DerivationPath -> networkValidator.validate(value as Blockchain)
        }
    }

    private fun lockTokenFieldsAction(): Action {
        val state = hubState
        return Screen.UpdateTokenFields(listOf(
            Network to state.screenState.network.copy(isEnabled = false),
            Name to state.screenState.name.copy(isEnabled = false),
            Symbol to state.screenState.symbol.copy(isEnabled = false),
            Decimals to state.screenState.decimals.copy(isEnabled = false),
        ))
    }

    private fun unlockTokenFieldsAction(): Action {
        val state = hubState
        return Screen.UpdateTokenFields(listOf(
            Network to state.screenState.network.copy(isEnabled = true),
            Name to state.screenState.name.copy(isEnabled = true),
            Symbol to state.screenState.symbol.copy(isEnabled = true),
            Decimals to state.screenState.decimals.copy(isEnabled = true),
        ))
    }

    private suspend fun toggleAddButtonAction(enable: Boolean) = when (enable) {
        true -> unlockAddButtonAction()
        else -> lockAddButtonAction()
    }

    private suspend fun lockAddButtonAction() {
        dispatchOnMain(Screen.UpdateAddButton(ViewStates.AddButton(false)))
    }

    private suspend fun unlockAddButtonAction() {
        dispatchOnMain(Screen.UpdateAddButton(ViewStates.AddButton(true)))
    }

    override fun reduceAction(action: Action, state: AddCustomTokenState): AddCustomTokenState {
        return when (action) {
            is Init.SetAddedCurrencies -> {
                state.copy(addedCurrencies = action.addedCurrencies)
            }
            is Init.SetOnAddTokenCallback -> {
                state.copy(onTokenAddCallback = action.callback)
            }
            is OnCreate -> {
//                val card = requireNotNull(globalState.scanResponse?.card)
                val tangemTechServiceManager = TangemTechServiceManager(TangemTechService())
//                tangemTechServiceManager.attachAuthKey(card.cardPublicKey.toHexString())
                state.copy(
//                    derivationStyle = card.derivationStyle,
                    derivationStyle = DerivationStyle.LEGACY,
                    tangemTechServiceManager = tangemTechServiceManager
                )
            }
            is OnDestroy -> state.reset()
            is UpdateForm -> {
                updateFormState(action.state)
            }
            is OnTokenContractAddressChanged -> {
                val field: TokenField = state.getField(ContractAddress)
                field.data = action.contractAddress
                updateFormState(state)
            }
            is OnTokenNetworkChanged -> {
                val field: TokenBlockchainField = state.getField(Network)
                field.data = action.blockchainNetwork
                updateFormState(state)
            }
            is OnTokenNameChanged -> {
                val field: TokenField = state.getField(Name)
                field.data = action.tokenName
                updateFormState(state)
            }
            is OnTokenSymbolChanged -> {
                val field: TokenField = state.getField(Symbol)
                field.data = action.tokenSymbol
                updateFormState(state)
            }
            is OnTokenDecimalsChanged -> {
                val field: TokenField = state.getField(Decimals)
                field.data = action.tokenDecimals
                updateFormState(state)
            }
            is OnTokenDerivationPathChanged -> {
                val field: TokenDerivationPathField = state.getField(DerivationPath)
                field.data = action.blockchainDerivationPath
                updateFormState(state)
            }
            is FieldError.Add -> {
                val newMap = state.formErrors.toMutableMap().apply { this[action.id] = action.error }
                state.copy(formErrors = newMap)
            }
            is FieldError.Remove -> {
                val newMap = state.formErrors.toMutableMap().apply { remove(action.id) }
                state.copy(formErrors = newMap)
            }
            is SetTokenId -> {
                state.copy(tokenId = action.id)
            }
            is Warning.Add -> {
                val newList = state.warnings.toMutableSet().apply { addAll(action.warnings) }
                state.copy(warnings = newList.toSet())
            }
            is Warning.Remove -> {
                val newList = state.warnings.toMutableSet().apply { removeAll(action.warnings) }
                state.copy(warnings = newList.toSet())
            }
            is Warning.Replace -> {
                val newList = state.warnings.toMutableSet().apply {
                    removeAll(action.remove)
                    addAll(action.add)
                }
                state.copy(warnings = newList.toSet())
            }
            is Screen.UpdateTokenFields -> {
                var newScreenState = state.screenState
                action.pairs.forEach {
                    newScreenState = when (it.first) {
                        ContractAddress -> {
                            if (state.screenState.contractAddressField == it.second) {
                                newScreenState
                            } else {
                                newScreenState.copy(contractAddressField = it.second)
                            }
                        }
                        Network -> {
                            if (state.screenState.network == it.second) {
                                newScreenState
                            } else {
                                newScreenState.copy(network = it.second)
                            }
                        }
                        Name -> {
                            if (state.screenState.name == it.second) {
                                newScreenState
                            } else {
                                newScreenState.copy(name = it.second)
                            }
                        }
                        Symbol -> {
                            if (state.screenState.symbol == it.second) {
                                newScreenState
                            } else {
                                newScreenState.copy(symbol = it.second)
                            }
                        }
                        Decimals -> {
                            if (state.screenState.decimals == it.second) {
                                newScreenState
                            } else {
                                newScreenState.copy(decimals = it.second)
                            }
                        }
                        else -> newScreenState
                    }
                }
                if (state.screenState == newScreenState) {
                    state
                } else {
                    state.copy(screenState = newScreenState)
                }
            }
            is Screen.UpdateAddButton -> {
                val newScreenState = if (state.screenState.addButton == action.addButton) {
                    state.screenState
                } else {
                    state.screenState.copy(addButton = action.addButton)
                }
                if (newScreenState == state.screenState) {
                    state
                } else {
                    state.copy(screenState = newScreenState)
                }
            }
            else -> state
        }
    }

    private fun updateFormState(state: AddCustomTokenState): AddCustomTokenState {
        return state.copy(form = Form(state.form.fieldList))
    }

    @Throws
    private fun throwUnAppropriateInitialization(objName: String) {
        throw DomainException.UnAppropriateInitializationException(
            "AddCustomTokenHub", "$objName must be not NULL"
        )
    }

    companion object {

    }
}