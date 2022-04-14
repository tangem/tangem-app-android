package com.tangem.domain.features.addCustomToken.redux

import android.webkit.ValueCallback
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.guard
import com.tangem.common.extensions.toHexString
import com.tangem.common.services.Result
import com.tangem.domain.DomainDialog
import com.tangem.domain.DomainException
import com.tangem.domain.common.TapWorkarounds.derivationStyle
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
                hubState.addedCurrencies.guard {
                    return throwUnAppropriateInitialization("addedTokens")
                }
            }
            is OnDestroy -> hubScope.cancel()
            is OnTokenContractAddressChanged -> {
                dispatchOnMain(
                    Screen.UpdateAddButton(
                        ViewStates.AddButton(!hubState.allFieldsIsEmpty())
                    )
                )
                val contractAddress = action.contractAddress.value
                val validator: TokenContractAddressValidator = hubState.getValidator(ContractAddress)
                val error = validator.validate(contractAddress)
                addOrRemoveError(ContractAddress, error)

                if (error != null || contractAddress.isEmpty()) {
                    dispatchOnMain(unlockTokenFields())
                    return
                }
                if (!action.contractAddress.isUserInput) return

                manageTokenChanges(requestInfoAboutContractAddress(contractAddress))
            }
            is OnTokenNetworkChanged -> {
                if (!action.blockchainNetwork.isUserInput) return

                val contractAddress = hubState.getField<TokenField>(ContractAddress).data.value
                manageTokenChanges(requestInfoAboutContractAddress(contractAddress))
            }
            is OnTokenNameChanged -> {
                val validator: TokenNameValidator = hubState.getValidator(Name)
                addOrRemoveError(Name, validator.validate(action.tokenName.value))
            }
            is OnTokenSymbolChanged -> {
                val validator: TokenSymbolValidator = hubState.getValidator(Symbol)
                addOrRemoveError(Symbol, validator.validate(action.tokenSymbol.value))
            }
            is OnTokenDecimalsChanged -> {
                val validator: TokenDecimalsValidator = hubState.getValidator(Decimals)
                addOrRemoveError(Decimals, validator.validate(action.tokenDecimals.value))
            }
//            is OnTokenDerivationPathChanged -> {
//                val validator: TokenDerivationPathValidator = getValidator(DerivationPath, hubState)
//                addOrRemoveError(DerivationPath, validator.validate(action.value.value))
//            }
            is ClearTokenFields -> {
                val nameField = hubState.getField<TokenField>(Name)
                val symbolField = hubState.getField<TokenField>(Symbol)
                val decimalsField = hubState.getField<TokenField>(Decimals)

                nameField.data = Field.Data("", false)
                symbolField.data = Field.Data("", false)
                decimalsField.data = Field.Data("", false)

                dispatchOnMain(UpdateForm(hubState))
            }
            is FillTokenFields -> {
                val networkField = hubState.getField<TokenBlockchainField>(Network)
                val nameField = hubState.getField<TokenField>(Name)
                val symbolField = hubState.getField<TokenField>(Symbol)
                val decimalsField = hubState.getField<TokenField>(Decimals)

                val token = action.token
                val contract = action.contract
                val blockchain = Blockchain.fromNetworkId(contract.networkId) ?: Blockchain.Unknown
                networkField.data = Field.Data(blockchain, false)
                nameField.data = Field.Data(token.name, false)
                symbolField.data = Field.Data(token.symbol, false)
                decimalsField.data = Field.Data(contract.decimalCount.toString(), false)

                dispatchOnMain(UpdateForm(hubState))
            }
            is OnAddCustomTokenClicked -> {
//                if (hubState.allFieldsIsEmpty()) {
                dispatchOnMain(
                    DomainGlobalAction.ShowDialog(DomainDialog.DialogError(
                        AddCustomTokenError.FieldIsEmpty
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
                dispatchOnMain(unlockTokenFields())
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
                            dispatchOnMain(lockTokenFields())
                        } else {
                            toRemoveWarnings.add(AddCustomTokenWarning.TokenAlreadyAdded)
                            dispatchOnMain(Screen.UpdateAddButton(ViewStates.AddButton(true)))

                            val isStandardDerivation = true
                            val tokenContract = token.contracts[0]
                            if (tokenContract.active && isStandardDerivation) {
                                toRemoveWarnings.add(AddCustomTokenWarning.PotentialScamToken)
                                dispatchOnMain(FillTokenFields(token, contract))
                                dispatchOnMain(lockTokenFields())
                            } else {
                                toAddWarnings.add(AddCustomTokenWarning.PotentialScamToken)
                                dispatchOnMain(ClearTokenFields)
                                dispatchOnMain(unlockTokenFields())
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
                                    dispatchOnMain(lockTokenFields())
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

    private suspend fun addOrRemoveError(id: CustomTokenFieldId, error: AddCustomTokenError?) {
        if (error == null) {
            dispatchOnMain(FieldError.Remove(id))
        } else {
            dispatchOnMain(FieldError.Add(id, error))
        }
    }

    private fun lockTokenFields(): Action {
        val state = hubState
        return Screen.UpdateTokenFields(listOf(
            Network to state.screenState.network.copy(isEnabled = false),
            Name to state.screenState.name.copy(isEnabled = false),
            Symbol to state.screenState.symbol.copy(isEnabled = false),
            Decimals to state.screenState.decimals.copy(isEnabled = false),
        ))
    }

    private fun unlockTokenFields(): Action {
        val state = hubState
        return Screen.UpdateTokenFields(listOf(
            Network to state.screenState.network.copy(isEnabled = true),
            Name to state.screenState.name.copy(isEnabled = true),
            Symbol to state.screenState.symbol.copy(isEnabled = true),
            Decimals to state.screenState.decimals.copy(isEnabled = true),
        ))
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
                val card = requireNotNull(globalState.scanResponse?.card)
                val tangemTechServiceManager = TangemTechServiceManager(TangemTechService())
                tangemTechServiceManager.attachAuthKey(card.cardPublicKey.toHexString())
                state.copy(
                    derivationStyle = card.derivationStyle,
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
}