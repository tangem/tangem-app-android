package com.tangem.domain.features.addCustomToken.redux

import android.webkit.ValueCallback
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.card.Card
import com.tangem.common.services.Result
import com.tangem.domain.DomainDialog
import com.tangem.domain.DomainException
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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.rekotlin.Action
import timber.log.Timber

/**
[REDACTED_AUTHOR]
 */
internal class AddCustomTokenHub : BaseStoreHub<AddCustomTokenState>("AddCustomTokenHub") {

    private val hubState: AddCustomTokenState
        get() = domainStore.state.addCustomTokensState

    override fun getHubState(storeState: DomainState): AddCustomTokenState {
        return storeState.addCustomTokensState
    }

    override fun updateStoreState(storeState: DomainState, newHubState: AddCustomTokenState): DomainState {
        return storeState.copy(addCustomTokensState = newHubState)
    }

    override suspend fun handleAction(
        action: Action,
        storeState: DomainState,
        cancel: ValueCallback<Action>
    ) {
        if (action !is AddCustomTokenAction) return
//        val card = storeState.globalState.scanResponse?.card
//            ?: throw IllegalStateException("ScanResponse must be set before showing the AddCustomToken screen")

        when (action) {
            is OnCreate -> {
//                hubState.addCustomTokenManager.attachAuthKey(card.cardPublicKey.toHexString())
            }
            is OnDestroy -> hubScope.cancel()
            is OnTokenContractAddressChanged -> {
                val contractAddress = action.contractAddress.value
                val validator: TokenContractAddressValidator = getValidator(ContractAddress, hubState)
                val error = validator.validate(contractAddress)
                addOrRemoveError(ContractAddress, error)

                if (error != null || contractAddress.isEmpty()) {
                    dispatchOnMain(actionsUnlockTokenFields())
                    return
                }
                if (!action.contractAddress.isUserInput) return

                val foundTokens = requestInfoAboutContractAddress(contractAddress, hubState)
                manageTokenChanges(null, foundTokens)
            }
            is OnTokenNetworkChanged -> {
                if (!action.blockchainNetwork.isUserInput) return

                val contractAddress = getField<TokenField>(ContractAddress, hubState).data.value
                val foundTokens = requestInfoAboutContractAddress(contractAddress, hubState)
                manageTokenChanges(null, foundTokens)
            }
            is OnTokenNameChanged -> {
                val validator: TokenNameValidator = getValidator(Name, hubState)
                addOrRemoveError(Name, validator.validate(action.tokenName.value))
            }
            is OnTokenSymbolChanged -> {
                val validator: TokenSymbolValidator = getValidator(Symbol, hubState)
                addOrRemoveError(Symbol, validator.validate(action.tokenSymbol.value))
            }
            is OnTokenDecimalsChanged -> {
                val validator: TokenDecimalsValidator = getValidator(Decimals, hubState)
                addOrRemoveError(Decimals, validator.validate(action.tokenDecimals.value))
            }
//            is OnTokenDerivationPathChanged -> {
//                val validator: TokenDerivationPathValidator = getValidator(DerivationPath, hubState)
//                addOrRemoveError(DerivationPath, validator.validate(action.value.value))
//            }
            is ClearTokenFields -> {
                val nameField = getField<TokenField>(Name, hubState)
                val symbolField = getField<TokenField>(Symbol, hubState)
                val decimalsField = getField<TokenField>(Decimals, hubState)

                nameField.data = Field.Data("", false)
                symbolField.data = Field.Data("", false)
                decimalsField.data = Field.Data("", false)

                dispatchOnMain(UpdateForm(hubState))
            }
            is FillTokenFields -> {
                val networkField = getField<TokenBlockchainField>(Network, hubState)
                val nameField = getField<TokenField>(Name, hubState)
                val symbolField = getField<TokenField>(Symbol, hubState)
                val decimalsField = getField<TokenField>(Decimals, hubState)

                val token = action.token
                val contract = action.contract
                val blockchain = Blockchain.fromNetworkId(contract.networkId)
                networkField.data = Field.Data(blockchain, false)
                nameField.data = Field.Data(token.name, false)
                symbolField.data = Field.Data(token.symbol, false)
                decimalsField.data = Field.Data(contract.decimalCount.toString(), false)

                dispatchOnMain(UpdateForm(hubState))
            }
            else -> {}
        }
    }

    private suspend fun requestInfoAboutContractAddress(
        contractAddress: String,
        hubState: AddCustomTokenState
    ): List<Coins.CheckAddressResponse.Token> {
        dispatchOnMain(Screen.UpdateTokenFields(listOf(ContractAddress to ViewStates.TokenField(isLoading = true))))
        val tokenManager = hubState.addCustomTokenManager
        val field = getField<TokenBlockchainField>(Network, hubState)
        val selectedNetworkId: String? = field.data.value.let {
            if (it == Blockchain.Unknown) null else it
        }?.toNetworkId()

//        delay(1000)
        val result = when (val foundTokensResult = tokenManager.checkAddress(contractAddress, selectedNetworkId)) {
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

    private suspend fun manageTokenChanges(
        card: Card?,
        foundTokens: List<Coins.CheckAddressResponse.Token>,
    ) {
        val toAddWarnings = mutableSetOf<AddCustomTokenWarning>()
        val toRemoveWarnings = mutableSetOf<AddCustomTokenWarning>()

        when {
            foundTokens.isEmpty() -> {
                toAddWarnings.add(AddCustomTokenWarning.PotentialScamToken)
                toRemoveWarnings.add(AddCustomTokenWarning.TokenAlreadyAdded)
                dispatchOnMain(ClearTokenFields)
                dispatchOnMain(actionsUnlockTokenFields())
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
                            dispatchOnMain(actionsLockTokenFields())
                        } else {
                            toRemoveWarnings.add(AddCustomTokenWarning.TokenAlreadyAdded)
                            dispatchOnMain(Screen.UpdateAddButton(ViewStates.AddButton(true)))

                            val isStandardDerivation = true
                            val tokenContract = token.contracts[0]
                            if (tokenContract.active && isStandardDerivation) {
                                toRemoveWarnings.add(AddCustomTokenWarning.PotentialScamToken)
                                dispatchOnMain(FillTokenFields(token, contract))
                                dispatchOnMain(actionsLockTokenFields())
                            } else {
                                toAddWarnings.add(AddCustomTokenWarning.PotentialScamToken)
                                dispatchOnMain(ClearTokenFields)
                                dispatchOnMain(actionsUnlockTokenFields())
                            }
                        }
                    }
                    else -> {
                        val dialog = DomainDialog.SelectTokenDialog(
                            items = contracts,
                            networkIdConverter = { networkId ->
                                val blockchain = Blockchain.fromNetworkId(networkId)
                                if (blockchain == Blockchain.Unknown) {
                                    throw DomainException.SelectTokeNetworkException(networkId)
                                }
                                AddCustomTokenState.convertBlockchainName(blockchain, "")
                            },
                            onSelect = { selectedContract ->
                                hubScope.launch {
                                    // find how to connect to the upper coroutineContext and dispatch through them
                                    dispatchOnMain(FillTokenFields(token, selectedContract))
                                    dispatchOnMain(actionsLockTokenFields())
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

    private fun actionsLockTokenFields(): Action {
        val state = hubState
        return Screen.UpdateTokenFields(listOf(
            Network to state.screenState.network.copy(isEnabled = false),
            Name to state.screenState.name.copy(isEnabled = false),
            Symbol to state.screenState.symbol.copy(isEnabled = false),
            Decimals to state.screenState.decimals.copy(isEnabled = false),
        ))
    }

    private fun actionsUnlockTokenFields(): Action {
        val state = hubState
        return Screen.UpdateTokenFields(listOf(
            Network to state.screenState.network.copy(isEnabled = true),
            Name to state.screenState.name.copy(isEnabled = true),
            Symbol to state.screenState.symbol.copy(isEnabled = true),
            Decimals to state.screenState.decimals.copy(isEnabled = true),
        ))
    }

    private inline fun <reified T> getField(id: FieldId, state: AddCustomTokenState): T {
        return state.form.getField(id) as T
    }

    private inline fun <reified T> getValidator(id: FieldId, state: AddCustomTokenState): T {
        return state.getValidator(id) as T
    }

    override fun reduceAction(action: Action, state: AddCustomTokenState): AddCustomTokenState {
        return when (action) {
            is UpdateForm -> {
                updateFormState(action.state)
            }
            is OnTokenContractAddressChanged -> {
                val field: TokenField = getField(ContractAddress, state)
                field.data = action.contractAddress
                updateFormState(state)
            }
            is OnTokenNetworkChanged -> {
                val field: TokenBlockchainField = getField(Network, state)
                field.data = action.blockchainNetwork
                updateFormState(state)
            }
            is OnTokenNameChanged -> {
                val field: TokenField = getField(Name, state)
                field.data = action.tokenName
                updateFormState(state)
            }
            is OnTokenSymbolChanged -> {
                val field: TokenField = getField(Symbol, state)
                field.data = action.tokenSymbol
                updateFormState(state)
            }
            is OnTokenDecimalsChanged -> {
                val field: TokenField = getField(Decimals, state)
                field.data = action.tokenDecimals
                updateFormState(state)
            }
            is OnTokenDerivationPathChanged -> {
                val field: TokenDerivationPathField = getField(DerivationPath, state)
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

//TODO: refactoring: replace by Blockchain.Companion.fromNetworkId
fun Blockchain.Companion.fromNetworkId(networkId: String): Blockchain {
    return when (networkId) {
        "avalanche" -> Blockchain.Avalanche
        "binancecoin" -> Blockchain.Binance
        "binance-smart-chain" -> Blockchain.BSC
        "ethereum" -> Blockchain.Ethereum
        "polygon-pos" -> Blockchain.Polygon
        "solana" -> Blockchain.Solana
        "fantom" -> Blockchain.Fantom
        "bitcoin" -> Blockchain.Bitcoin
        "bitcoin-cash" -> Blockchain.BitcoinCash
        "cardano" -> Blockchain.CardanoShelley
        "dogecoin" -> Blockchain.Dogecoin
        "ducatus" -> Blockchain.Ducatus
        "litecoin" -> Blockchain.Litecoin
        "rsk" -> Blockchain.RSK
        "stellar" -> Blockchain.Stellar
        "tezos" -> Blockchain.Tezos
        "ripple" -> Blockchain.XRP
        else -> Blockchain.Unknown
    }
}

fun Blockchain.toNetworkId(): String? {
    return when (this) {
        Blockchain.Unknown -> null
        Blockchain.Avalanche -> "avalanche"
        Blockchain.AvalancheTestnet -> "avalanche"
        Blockchain.Binance -> "binancecoin"
        Blockchain.BinanceTestnet -> "binancecoin"
        Blockchain.BSC -> "binance-smart-chain"
        Blockchain.BSCTestnet -> "binance-smart-chain"
        Blockchain.Bitcoin -> "bitcoin"
        Blockchain.BitcoinTestnet -> "bitcoin"
        Blockchain.BitcoinCash -> "bitcoin-cash"
        Blockchain.BitcoinCashTestnet -> "bitcoin-cash"
        Blockchain.Cardano -> "cardano"
        Blockchain.CardanoShelley -> "cardano"
        Blockchain.Dogecoin -> "dogecoin"
        Blockchain.Ducatus -> "ducatus"
        Blockchain.Ethereum -> "ethereum"
        Blockchain.EthereumTestnet -> "ethereum"
        Blockchain.Fantom -> "fantom"
        Blockchain.FantomTestnet -> "fantom"
        Blockchain.Litecoin -> "litecoin"
        Blockchain.Polygon -> "matic-network"
        Blockchain.PolygonTestnet -> "matic-networks"
        Blockchain.RSK -> "rootstock"
        Blockchain.Stellar -> "stellar"
        Blockchain.StellarTestnet -> "stellar"
        Blockchain.Solana -> "solana"
        Blockchain.SolanaTestnet -> "solana"
        Blockchain.Tezos -> "tezos"
        Blockchain.XRP -> "ripple"
    }
}