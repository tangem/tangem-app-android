package com.tangem.domain.features.addCustomToken.redux

import android.webkit.ValueCallback
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.DerivationStyle
import com.tangem.common.extensions.guard
import com.tangem.common.extensions.toHexString
import com.tangem.common.services.Result
import com.tangem.domain.DomainDialog
import com.tangem.domain.DomainException
import com.tangem.domain.DomainWrapped
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
import kotlinx.coroutines.Dispatchers
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
            is OnCreate -> {
                hubState.appSavedCurrencies.guard {
                    return throwUnAppropriateInitialization("addedTokens")
                }
            }
            is OnDestroy -> cancelAll()
            is OnTokenContractAddressChanged -> {
                val address = action.contractAddress.value

                when (val error = ContractAddress.validateValue(address)) {
                    null -> {
                        ContractAddress.removeError()
                        unlockTokenFields()
                    }
                    AddCustomTokenError.FieldIsEmpty -> {
                        ContractAddress.removeError()
                        return
                    }
                    AddCustomTokenError.InvalidContractAddress -> {
                        ContractAddress.addError(error)
                        unlockTokenFields()
                        return
                    }
                    else -> {}
                }

                if (!action.contractAddress.isUserInput) return
                manageFoundTokenChanges(requestInfoAboutToken(address))
            }
            is OnTokenNameChanged -> {
                updateAddButton()
            }
            is OnTokenSymbolChanged -> {
                updateAddButton()
            }
            is OnTokenDecimalsChanged -> {
                updateAddButton()
            }
            is OnTokenNetworkChanged -> {
                if (!action.blockchainNetwork.isUserInput) return

                val contractAddress = ContractAddress.getFieldValue<String>()
                val error = ContractAddress.validateValue(contractAddress)
                if (error == null && contractAddress.isNotEmpty()) {
                    // token branch
                    manageFoundTokenChanges(requestInfoAboutToken(contractAddress))
                } else {
                    // blockchain branch
                    val isAlreadyAdded = isBlockchainPersistIntoAppSavedTokensList(
                        selectedNetwork = action.blockchainNetwork.value
                    )
                    updateWarningAlreadyAdded(isAlreadyAdded)
                    updateAddButton()
                }
            }
            is OnTokenDerivationPathChanged -> {
                val isAlreadyAdded = if (ContractAddress.isFilled()) {
                    // token branch
                    isTokenPersistIntoAppSavedTokensList(
                        selectedDerivation = action.blockchainDerivationPath.value
                    )
                } else {
                    // blockchain branch
                    isBlockchainPersistIntoAppSavedTokensList(
                        selectedDerivation = action.blockchainDerivationPath.value
                    )
                }
                updateWarningAlreadyAdded(isAlreadyAdded)
                updateAddButton()
            }
            is OnAddCustomTokenClicked -> {
                val state = hubState
                val completeData = when {
                    state.tokensFieldsIsFilled() && state.networkIsSelected() -> {
                        state.gatherUserToken()
                    }
                    !state.tokensFieldsIsFilled() && state.networkIsSelected() -> {
                        state.gatherBlockchain()
                    }
                    else -> null
                }

                if (completeData == null) {
                    // normally it can't be, because the AddButton must be blocked
                } else {
                    hubScope.launch(Dispatchers.Main) {
                        state.onTokenAddCallback?.invoke(completeData)
                    }
                }
            }
            else -> {}
        }
    }

    private suspend fun updateWarningAlreadyAdded(isInAppSavedList: Boolean) {
        if (isInAppSavedList) {
            AddCustomTokenWarning.TokenAlreadyAdded.add()
        } else {
            AddCustomTokenWarning.TokenAlreadyAdded.remove()
        }
    }

    private suspend fun requestInfoAboutToken(
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

    private suspend fun manageFoundTokenChanges(foundTokens: List<Coins.CheckAddressResponse.Token>) {
        if (foundTokens.isEmpty()) {
            // token not found - it's completely custom
            AddCustomTokenWarning.TokenAlreadyAdded.remove()
            AddCustomTokenWarning.PotentialScamToken.add()

            dispatchOnMain(SetFoundTokenId(null))
            clearTokenFields()
            unlockTokenFields()
            updateAddButton()
            return
        }

        // foundToken - contains all info about the token
        val foundToken = foundTokens[0]
        dispatchOnMain(SetFoundTokenId(foundToken.id))
        when {
            foundToken.contracts.isEmpty() -> {
                Timber.e("Unexpected state -> throw to FB")
            }
            foundToken.contracts.size == 1 -> {
                // token with single contract address
                val singleTokenContract = foundToken.contracts[0]
                fillTokenFields(foundToken, singleTokenContract)

                val isInAppSavedTokens = isTokenPersistIntoAppSavedTokensList()
                if (isInAppSavedTokens) {
                    lockTokenFields()
                    lockAddButton()
                    AddCustomTokenWarning.PotentialScamToken.replace(AddCustomTokenWarning.TokenAlreadyAdded)
                } else {
                    // not in the saved tokens list
                    if (singleTokenContract.active) {
                        lockTokenFields()
                        unlockAddButton()
                        if (hubState.derivationPathIsSelected()) {
                            AddCustomTokenWarning.PotentialScamToken.add()
                        } else {
                            AddCustomTokenWarning.TokenAlreadyAdded.remove()
                            AddCustomTokenWarning.PotentialScamToken.remove()
                        }
                    } else {
                        unlockAddButton()
                        AddCustomTokenWarning.PotentialScamToken.add()
                    }
                }
            }
            else -> {
                AddCustomTokenWarning.PotentialScamToken.replace(AddCustomTokenWarning.TokenAlreadyAdded)

                val dialog = DomainDialog.SelectTokenDialog(
                    items = foundToken.contracts,
                    networkIdConverter = { networkId ->
                        val blockchain = Blockchain.fromNetworkId(networkId)
                        if (blockchain == null || blockchain == Blockchain.Unknown) {
                            throw DomainException.SelectTokeNetworkException(networkId)
                        }
                        hubState.blockchainToName(blockchain) ?: ""
                    },
                    onSelect = { selectedContract ->
                        hubScope.launch {
                            // find how to connect to the upper coroutineContext and dispatch through them
                            fillTokenFields(foundToken, selectedContract)
                            lockTokenFields()
                            unlockAddButton()
                        }
                    },
                )
                dispatchOnMain(DomainGlobalAction.ShowDialog(dialog))
            }
        }
    }

    private suspend fun replaceWarnings(
        warningsAdd: MutableSet<AddCustomTokenWarning> = mutableSetOf(),
        warningsRemove: MutableSet<AddCustomTokenWarning> = mutableSetOf(),
    ) {
        if (warningsAdd.isNotEmpty() || warningsRemove.isNotEmpty()) {
            dispatchOnMain(Warning.Replace(warningsRemove.toSet(), warningsAdd.toSet()))
        }
    }

    private suspend fun updateAddButton() {
        val state = hubState
        if (state.warnings.contains(AddCustomTokenWarning.TokenAlreadyAdded)) {
            lockAddButton()
            return
        }
        when {
            // token
            state.tokensOneFieldsIsFilled() -> {
                lockAddButton()
            }
            // token
            state.tokensFieldsIsFilled() && state.networkIsSelected() -> {
                unlockAddButton()
            }
            // blockchain
            else -> {
                if (state.networkIsSelected()) {
                    val alreadyAdded = isBlockchainPersistIntoAppSavedTokensList()
                    if (alreadyAdded) {
                        lockAddButton()
                    } else {
                        unlockAddButton()
                    }
                } else {
                    lockAddButton()
                }
            }
        }
    }

    private suspend fun lockAddButton() {
        dispatchOnMain(Screen.UpdateAddButton(ViewStates.AddButton(false)))
    }

    private suspend fun unlockAddButton() {
        dispatchOnMain(Screen.UpdateAddButton(ViewStates.AddButton(true)))
    }

    /**
     * These are helper functions.
     */
    private fun isTokenPersistIntoAppSavedTokensList(
        tokenId: String? = hubState.tokenId,
        tokenContractAddress: String = ContractAddress.getFieldValue(),
        tokenNetworkId: String = Network.getFieldValue<Blockchain>().toNetworkId(),
        selectedDerivation: Blockchain = DerivationPath.getFieldValue()
    ): Boolean {
        val savedCurrencies = hubState.appSavedCurrencies ?: return false

        val derivationPath = getDerivationPathFromSelectedBlockchain(selectedDerivation)
        savedCurrencies.forEach { wrappedCurrency ->
            when (wrappedCurrency) {
                is DomainWrapped.Currency.Blockchain -> {}
                is DomainWrapped.Currency.Token -> {
                    val sameId = tokenId == wrappedCurrency.token.id
                    val sameAddress = tokenContractAddress == wrappedCurrency.token.contractAddress
                    val sameBlockchain = Blockchain.fromNetworkId(tokenNetworkId) == wrappedCurrency.blockchain
                    val sameDerivationPath = derivationPath?.rawPath == wrappedCurrency.derivationPath
                    if (sameId && sameAddress && sameBlockchain && sameDerivationPath) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun isBlockchainPersistIntoAppSavedTokensList(
        selectedNetwork: Blockchain = Network.getFieldValue(),
        selectedDerivation: Blockchain = DerivationPath.getFieldValue()
    ): Boolean {
        val state = hubState
        val savedCurrencies = state.appSavedCurrencies ?: return false

        val derivationPath = getDerivationPathFromSelectedBlockchain(selectedDerivation)
        savedCurrencies.forEach { wrappedCurrency ->
            when (wrappedCurrency) {
                is DomainWrapped.Currency.Blockchain -> {
                    val isSameBlockchain = selectedNetwork == wrappedCurrency.blockchain
                    val isSameDerivationPath = derivationPath?.rawPath == wrappedCurrency.derivationPath
                    if (isSameBlockchain && isSameDerivationPath) return true
                }
                is DomainWrapped.Currency.Token -> {}
            }
        }
        return false
    }

    private fun getDerivationPathFromSelectedBlockchain(
        selectedDerivationBlockchain: Blockchain
    ): com.tangem.common.hdWallet.DerivationPath? = AddCustomTokenState.getDerivationPath(
        mainNetwork = Network.getFieldValue(),
        derivationNetwork = selectedDerivationBlockchain,
        derivationStyle = hubState.cardDerivationStyle
    )

    private suspend fun CustomTokenFieldId.addError(error: AddCustomTokenError) {
        dispatchOnMain(FieldError.Add(this, error))
    }

    private suspend fun CustomTokenFieldId.removeError() {
        dispatchOnMain(FieldError.Remove(this))
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

    private fun CustomTokenFieldId.validateValue(value: Any): AddCustomTokenError? {
        val state = hubState
        val contractAddressValidator: TokenContractAddressValidator = state.getValidator(ContractAddress)
        val nameValidator: TokenNameValidator = state.getValidator(Name)
        val symbolValidator: TokenSymbolValidator = state.getValidator(Symbol)
        val decimalsValidator: TokenDecimalsValidator = state.getValidator(Decimals)
        val networkValidator: TokenNetworkValidator = state.getValidator(Network)
        return when (this) {
            ContractAddress -> contractAddressValidator.validate(value as String)
            Network, DerivationPath -> networkValidator.validate(value as Blockchain)
            Name -> nameValidator.validate(value as String)
            Symbol -> symbolValidator.validate(value as String)
            Decimals -> decimalsValidator.validate(value as String)
        }
    }

    private fun CustomTokenFieldId.isFilled(): Boolean {
        return when (this) {
            ContractAddress -> getFieldValue<String>().isNotEmpty()
            Network -> getFieldValue<Blockchain>() != Blockchain.Unknown
            Name -> getFieldValue<String>().isNotEmpty()
            Symbol -> getFieldValue<String>().isNotEmpty()
            Decimals -> getFieldValue<String>().isNotEmpty()
            DerivationPath -> getFieldValue<Blockchain>() != Blockchain.Unknown
        }
    }

    /**
     * The field is being validated.
     * If there is an error, then it adds it to the field.
     */
    private suspend fun CustomTokenFieldId.validateAndUpdateError(value: Any): AddCustomTokenError? {
        val error = this.validateValue(value)
        when (error) {
            null -> this.removeError()
            else -> this.addError(error)
        }
        return error
    }

    private suspend fun fillTokenFields(
        token: Coins.CheckAddressResponse.Token,
        contract: Coins.CheckAddressResponse.Token.Contract,
    ) {
        val blockchain = Blockchain.fromNetworkId(contract.networkId) ?: Blockchain.Unknown
        Network.setFieldValue(Field.Data(blockchain, false))
        Name.setFieldValue(Field.Data(token.name, false))
        Symbol.setFieldValue(Field.Data(token.symbol, false))
        Decimals.setFieldValue(Field.Data(contract.decimalCount.toString(), false))
        dispatchOnMain(UpdateForm(hubState))
    }

    private suspend fun clearTokenFields() {
        Name.setFieldValue(Field.Data("", false))
        Symbol.setFieldValue(Field.Data("", false))
        Decimals.setFieldValue(Field.Data("", false))
        dispatchOnMain(UpdateForm(hubState))
    }

    private suspend fun lockTokenFields() {
        val state = hubState
        val action = Screen.UpdateTokenFields(listOf(
            Network to state.screenState.network.copy(isEnabled = false),
            Name to state.screenState.name.copy(isEnabled = false),
            Symbol to state.screenState.symbol.copy(isEnabled = false),
            Decimals to state.screenState.decimals.copy(isEnabled = false),
        ))
        dispatchOnMain(action)
    }

    private suspend fun unlockTokenFields() {
        val state = hubState
        val action = Screen.UpdateTokenFields(listOf(
            Network to state.screenState.network.copy(isEnabled = true),
            Name to state.screenState.name.copy(isEnabled = true),
            Symbol to state.screenState.symbol.copy(isEnabled = true),
            Decimals to state.screenState.decimals.copy(isEnabled = true),
        ))
        dispatchOnMain(action)
    }

    private suspend fun AddCustomTokenWarning.add() {
        dispatchOnMain(Warning.Add(setOf(this)))
    }

    private suspend fun AddCustomTokenWarning.remove() {
        dispatchOnMain(Warning.Remove(setOf(this)))
    }

    private suspend fun AddCustomTokenWarning.replace(to: AddCustomTokenWarning) {
        dispatchOnMain(Warning.Replace(setOf(this), setOf(to)))
    }

//    private suspend fun AddCustomTokenWarning.replace(replace: Boolean, to: AddCustomTokenWarning) {
//        if (replace) dispatchOnMain(Warning.Replace(setOf(this), setOf(to)))
//    }


    override fun reduceAction(action: Action, state: AddCustomTokenState): AddCustomTokenState {
        return when (action) {
            is Init.SetAddedCurrencies -> {
                state.copy(appSavedCurrencies = action.addedCurrencies)
            }
            is Init.SetOnAddTokenCallback -> {
                state.copy(onTokenAddCallback = action.callback)
            }
            is OnCreate -> {
                val card = requireNotNull(globalState.scanResponse?.card)
                val tangemTechServiceManager = TangemTechServiceManager(TangemTechService())
                tangemTechServiceManager.attachAuthKey(card.cardPublicKey.toHexString())

                var derivationPathState = state.screenState.derivationPath
                derivationPathState = when (card.derivationStyle) {
                    DerivationStyle.LEGACY -> derivationPathState.copy(isVisible = true)
                    null, DerivationStyle.NEW -> derivationPathState.copy(isVisible = false)
                }
                state.copy(
                    cardDerivationStyle = card.derivationStyle,
                    tangemTechServiceManager = tangemTechServiceManager,
                    screenState = state.screenState.copy(derivationPath = derivationPathState)
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
            is SetFoundTokenId -> {
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