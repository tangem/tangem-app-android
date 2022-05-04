package com.tangem.domain.features.addCustomToken.redux

import android.webkit.ValueCallback
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.DerivationStyle
import com.tangem.common.extensions.guard
import com.tangem.common.services.Result
import com.tangem.domain.AddCustomTokenError
import com.tangem.domain.AddCustomTokenError.Warning.PotentialScamToken
import com.tangem.domain.AddCustomTokenError.Warning.TokenAlreadyAdded
import com.tangem.domain.AddCustomTokenException
import com.tangem.domain.DomainDialog
import com.tangem.domain.DomainWrapped
import com.tangem.domain.common.TapWorkarounds.derivationStyle
import com.tangem.domain.common.extensions.fromNetworkId
import com.tangem.domain.common.extensions.toNetworkId
import com.tangem.domain.common.form.*
import com.tangem.domain.features.addCustomToken.*
import com.tangem.domain.features.addCustomToken.CustomTokenFieldId.*
import com.tangem.domain.features.addCustomToken.redux.AddCustomTokenAction.*
import com.tangem.domain.redux.*
import com.tangem.domain.redux.global.DomainGlobalAction
import com.tangem.domain.redux.global.DomainGlobalState
import com.tangem.network.api.tangemTech.CoinsResponse
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

    override fun getReducer(): ReStoreReducer<AddCustomTokenState> = AddCustomTokenReducer(globalState)

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
                validateContractAddressAndNotify(action.contractAddress.value)
            }
            is OnTokenNetworkChanged -> {
                if (!action.blockchainNetwork.isUserInput) return

                validateContractAddressAndNotify(ContractAddress.getFieldValue())
            }
            is OnTokenDerivationPathChanged -> {
                checkAndUpdateAddButton()
            }
            is OnTokenNameChanged, is OnTokenSymbolChanged, is OnTokenDecimalsChanged -> {
                checkAndUpdateAddButton()
            }
            is OnAddCustomTokenClicked -> {
                val state = hubState
                val completeData = when {
                    state.getCustomTokenType() == CustomTokenType.Token && state.networkIsSelected() -> {
                        state.gatherUserToken()
                    }
                    state.getCustomTokenType() == CustomTokenType.Blockchain && state.networkIsSelected() -> {
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

    private suspend fun validateContractAddressAndNotify(contractAddress: String) {
        val error = ContractAddress.validateValue(contractAddress)
        if (Network.isFilled()) {
            when (error) {
                null -> {
                    // valid contract address
                    ContractAddress.removeError()
                    changeBlockchainNetworkList()
                    findTokenAndUpdateFields(contractAddress)
                }
                AddCustomTokenError.InvalidContractAddress -> {
                    // invalid contract address
                    ContractAddress.addError(error)
                    updateTokenDetailFields(hubState.tokensAnyFieldsIsFilled())
                    changeBlockchainNetworkList()
                    checkAndUpdateAddButton()
                }
                AddCustomTokenError.FieldIsEmpty -> {
                    // empty contract address
                    ContractAddress.removeError()
                    clearTokenDetailsFields()
                    updateTokenDetailFields(false)
                    changeBlockchainNetworkList()
                    checkAndUpdateAddButton()
                }
            }
        } else {
            when (error) {
                null -> {
                    // valid contract address
                    ContractAddress.removeError()
                    changeBlockchainNetworkList()
                    findTokenAndUpdateFields(contractAddress)
                }
                else -> {
                    ContractAddress.removeError()
                    clearTokenDetailsFields()
                    updateTokenDetailFields(false)
                    changeBlockchainNetworkList()
                    checkAndUpdateAddButton()
                }
            }
        }
    }

    //TODO: Solana token
    /**
     * This feature is only needed until Solana coins are added.
     * While they are not there - this function excludes the Solana blockchain if the user has
     * filled in at least one field of the token.
     */
    private suspend fun changeBlockchainNetworkList() {
        val state = hubState
        val networkBlockchainList = Network.getField<TokenBlockchainField>().itemList
        val newNetworkBlockchainList: List<Blockchain> = state.getNetworks(state.getCustomTokenType())

        val listsIdentical = newNetworkBlockchainList.toSet() == networkBlockchainList.toSet()
        if (listsIdentical) return

        val networkFieldIsFilled = Network.isFilled()
        val newNetworkField = Network.getField<TokenBlockchainField>().copy(itemList = newNetworkBlockchainList)
        newNetworkField.data = Field.Data(Network.getFieldValue(), newNetworkField.data.isUserInput)
        if (networkFieldIsFilled) {
            val listSameSize = networkBlockchainList.size == newNetworkBlockchainList.size
            val newListLessThanOld = networkBlockchainList.size > newNetworkBlockchainList.size
            if (listSameSize || newListLessThanOld) {
//                 check selected blockchain
                val selectedBlockchain = Network.getFieldValue<Blockchain>()
                val defaultSelection = Blockchain.Unknown
                if (!newNetworkBlockchainList.contains(selectedBlockchain)
                    && newNetworkBlockchainList.contains(defaultSelection)
                ) {
//                     selectedBlockchain not present in the new list. Change selection to default
                    newNetworkField.data = Field.Data(defaultSelection, false)
                }
            }
        }

        hubState.setField(newNetworkField)
        dispatchOnMain(UpdateForm(hubState))
    }

    private suspend fun requestInfoAboutToken(
        contractAddress: String,
    ): List<CoinsResponse.Coin> {
        val tangemTechServiceManager = requireNotNull(hubState.tangemTechServiceManager)
        dispatchOnMain(Screen.UpdateTokenFields(listOf(ContractAddress to ViewStates.TokenField(isLoading = true))))

        val field = hubState.getField<TokenBlockchainField>(Network)
        val selectedNetworkId: String? = field.data.value.let {
            if (it == Blockchain.Unknown) null else it
        }?.toNetworkId()

        // simulate loading effect. It would be better if the delay would only run if tokenManager.checkAddress()
        // got the result faster than 500ms and the delay would only be the difference between them.
        delay(500)

        val foundTokensResult = tangemTechServiceManager.findToken(contractAddress, selectedNetworkId)
        val result = when (foundTokensResult) {
            is Result.Success -> foundTokensResult.data
            is Result.Failure -> {
//                val warning = Warning.Network.CheckAddressRequestError
//                dispatchOnMain(Warning.Add(setOf(warning)))
                emptyList()
            }
        }
        dispatchOnMain(Screen.UpdateTokenFields(listOf(ContractAddress to ViewStates.TokenField(isLoading = false))))
        return result
    }

    private suspend fun findTokenAndUpdateFields(contractAddress: String) {
        val foundTokens = requestInfoAboutToken(contractAddress)
        if (foundTokens.isEmpty()) {
            // token not found - it's completely custom
            PotentialScamToken.add()

            dispatchOnMain(SetFoundTokenId(null))
            updateTokenDetailFields(true)
            checkAndUpdateAddButton()
            return
        }

        // foundToken - contains all info about the token
        val foundToken = foundTokens[0]
        dispatchOnMain(SetFoundTokenId(foundToken.id))
        when {
            foundToken.networks.isEmpty() -> {
                Timber.e("Unexpected state -> throw to FB")
            }
            foundToken.networks.size == 1 -> {
                // token with single contract address
                val singleTokenContract = foundToken.networks[0]
                fillTokenFields(foundToken, singleTokenContract)

                val isInAppSavedTokens = isTokenPersistIntoAppSavedTokensList()
                if (isInAppSavedTokens) {
                    updateTokenDetailFields(false)
                    updateAddButton(false)
                    PotentialScamToken.replaceBy(TokenAlreadyAdded)
                } else {
                    // not in the saved coins list
                    if (foundToken.active) {
                        updateTokenDetailFields(false)
                        updateAddButton(true)
                        if (hubState.derivationPathIsSelected()) {
                            PotentialScamToken.add()
                        } else {
                            TokenAlreadyAdded.remove()
                            PotentialScamToken.remove()
                        }
                    } else {
                        updateAddButton(true)
                        PotentialScamToken.add()
                    }
                }
            }
            else -> {
                val dialog = DomainDialog.SelectTokenDialog(
                    items = foundToken.networks,
                    networkIdConverter = { networkId ->
                        val blockchain = Blockchain.fromNetworkId(networkId)
                        if (blockchain == null || blockchain == Blockchain.Unknown) {
                            throw AddCustomTokenException.SelectTokeNetworkException(networkId)
                        }
                        hubState.blockchainToName(blockchain) ?: ""
                    },
                    onSelect = { selectedContract ->
                        hubScope.launch {
                            // find how to connect to the upper coroutineContext and dispatch through them
                            PotentialScamToken.remove()
                            fillTokenFields(foundToken, selectedContract)
                            updateTokenDetailFields(false)
                            checkAndUpdateAddButton()
                        }
                    },
                )
                dispatchOnMain(DomainGlobalAction.ShowDialog(dialog))
            }
        }
    }

    private suspend fun checkAndUpdateAddButton() {
        val alreadyAddedToAppTokensList = if (hubState.tokensAnyFieldsIsFilled()) {
            isTokenPersistIntoAppSavedTokensList()
        } else {
            isBlockchainPersistIntoAppSavedTokensList()
        }
        if (alreadyAddedToAppTokensList) {
            TokenAlreadyAdded.add()
            updateAddButton(false)
            return
        } else {
            TokenAlreadyAdded.remove()
        }

        val state = hubState
        when {
            // token
            state.tokensFieldsIsFilled() && state.networkIsSelected() -> {
                val error = ContractAddress.validateValue(ContractAddress.getFieldValue<String>())
                updateAddButton(error == null)
            }
            // token
            state.tokensAnyFieldsIsFilled() -> {
                updateAddButton(false)
            }
            // blockchain
            else -> {
                if (state.networkIsSelected()) {
                    val alreadyAdded = isBlockchainPersistIntoAppSavedTokensList()
                    if (alreadyAdded) {
                        updateAddButton(false)
                    } else {
                        updateAddButton(true)
                    }
                } else {
                    updateAddButton(false)
                }
            }
        }
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
            ContractAddress -> {
                contractAddressValidator.nextValidationFor(Network.getFieldValue())
                contractAddressValidator.validate(value as String)
            }
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
        token: CoinsResponse.Coin,
        coinNetwork: CoinsResponse.Coin.Network,
    ) {
        val blockchain = Blockchain.fromNetworkId(coinNetwork.networkId) ?: Blockchain.Unknown
        Network.setFieldValue(Field.Data(blockchain, false))
        Name.setFieldValue(Field.Data(token.name, false))
        Symbol.setFieldValue(Field.Data(token.symbol, false))
        Decimals.setFieldValue(Field.Data(coinNetwork.decimalCount.toString(), false))
        dispatchOnMain(UpdateForm(hubState))
    }

    private suspend fun clearTokenDetailsFields() {
        Name.setFieldValue(Field.Data("", false))
        Symbol.setFieldValue(Field.Data("", false))
        Decimals.setFieldValue(Field.Data("", false))
        dispatchOnMain(UpdateForm(hubState))
    }

    private suspend fun updateTokenDetailFields(isEnabled: Boolean = true) {
        val state = hubState
        val action = Screen.UpdateTokenFields(listOf(
            Name to state.screenState.name.copy(isEnabled = isEnabled),
            Symbol to state.screenState.symbol.copy(isEnabled = isEnabled),
            Decimals to state.screenState.decimals.copy(isEnabled = isEnabled),
        ))
        dispatchOnMain(action)
    }

    private suspend fun updateBlockchainNetworkField(isEnabled: Boolean) {
        val action = Screen.UpdateTokenFields(listOf(
            Name to hubState.screenState.name.copy(isEnabled = isEnabled),
        ))
        dispatchOnMain(action)
    }

    private suspend fun updateAddButton(isEnabled: Boolean) {
        dispatchOnMain(Screen.UpdateAddButton(ViewStates.AddButton(isEnabled)))
    }

    private suspend fun AddCustomTokenError.Warning.add() {
        dispatchOnMain(Warning.Add(setOf(this)))
    }

    private suspend fun AddCustomTokenError.Warning.remove() {
        dispatchOnMain(Warning.Remove(setOf(this)))
    }

    private suspend fun AddCustomTokenError.Warning.replaceBy(to: AddCustomTokenError.Warning) {
        dispatchOnMain(Warning.Replace(setOf(this), setOf(to)))
    }

    @Throws
    private fun throwUnAppropriateInitialization(objName: String) {
        throw AddCustomTokenException.UnAppropriateInitializationException(
            "AddCustomTokenHub", "$objName must be not NULL"
        )
    }
}

private class AddCustomTokenReducer(
    private val globalState: DomainGlobalState,
) : ReStoreReducer<AddCustomTokenState> {

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
                val supportedTokenNetworkIds = AddCustomTokenState.getSupportedTokensBlockchain().map {
                    it.toNetworkId()
                }
                val tangemTechServiceManager = AddCustomTokenService(
                    tangemTechService = globalState.networkServices.tangemTechService,
                    supportedTokenNetworkIds = supportedTokenNetworkIds
                )

                var derivationPathState = state.screenState.derivationPath
                derivationPathState = when (card.derivationStyle) {
                    DerivationStyle.LEGACY -> derivationPathState.copy(isVisible = true)
                    null, DerivationStyle.NEW -> derivationPathState.copy(isVisible = false)
                }
                val form = Form(AddCustomTokenState.createFormFields(CustomTokenType.Blockchain))
                state.copy(
                    cardDerivationStyle = card.derivationStyle,
                    form = form,
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

}