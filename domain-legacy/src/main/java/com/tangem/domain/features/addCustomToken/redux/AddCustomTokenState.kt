package com.tangem.domain.features.addCustomToken.redux

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.DerivationStyle
import com.tangem.common.json.MoshiJsonConverter
import com.tangem.datasource.api.tangemTech.models.CoinsResponse
import com.tangem.domain.AddCustomTokenError
import com.tangem.domain.DomainWrapped
import com.tangem.domain.common.CardDTO
import com.tangem.domain.common.TapWorkarounds.isTestCard
import com.tangem.domain.common.extensions.isSupportedInApp
import com.tangem.domain.common.extensions.supportedBlockchains
import com.tangem.domain.common.extensions.supportedTokens
import com.tangem.domain.common.form.CustomTokenValidator
import com.tangem.domain.common.form.DataField
import com.tangem.domain.common.form.FieldDataConverter
import com.tangem.domain.common.form.FieldId
import com.tangem.domain.common.form.FieldToJsonConverter
import com.tangem.domain.common.form.Form
import com.tangem.domain.common.form.StringIsEmptyValidator
import com.tangem.domain.common.form.StringIsNotEmptyValidator
import com.tangem.domain.common.form.TokenContractAddressValidator
import com.tangem.domain.common.form.TokenDecimalsValidator
import com.tangem.domain.common.form.TokenNameValidator
import com.tangem.domain.common.form.TokenNetworkValidator
import com.tangem.domain.common.form.TokenSymbolValidator
import com.tangem.domain.features.addCustomToken.AddCustomTokenService
import com.tangem.domain.features.addCustomToken.CustomCurrency
import com.tangem.domain.features.addCustomToken.CustomTokenFieldId
import com.tangem.domain.features.addCustomToken.CustomTokenFieldId.ContractAddress
import com.tangem.domain.features.addCustomToken.CustomTokenFieldId.Decimals
import com.tangem.domain.features.addCustomToken.CustomTokenFieldId.DerivationPath
import com.tangem.domain.features.addCustomToken.CustomTokenFieldId.Name
import com.tangem.domain.features.addCustomToken.CustomTokenFieldId.Network
import com.tangem.domain.features.addCustomToken.CustomTokenFieldId.Symbol
import com.tangem.domain.features.addCustomToken.TokenBlockchainField
import com.tangem.domain.features.addCustomToken.TokenDerivationPathField
import com.tangem.domain.features.addCustomToken.TokenField
import com.tangem.domain.redux.DomainState
import com.tangem.domain.redux.state.StringActionStateConverter
import org.rekotlin.Action
import org.rekotlin.StateType

data class AddCustomTokenState(
    val appSavedCurrencies: List<DomainWrapped.Currency>? = null,
    val onTokenAddCallback: ((CustomCurrency) -> Unit)? = null,
    val cardDerivationStyle: DerivationStyle? = null,
    val form: Form = Form(listOf()),
    val formValidators: Map<CustomTokenFieldId, CustomTokenValidator<out Any>> = createFormValidators(),
    val formErrors: Map<CustomTokenFieldId, AddCustomTokenError> = emptyMap(),
    val foundToken: CoinsResponse.Coin? = null,
    val warnings: Set<AddCustomTokenError.Warning> = emptySet(),
    val screenState: ScreenState = createInitialScreenState(),
    val tangemTechServiceManager: AddCustomTokenService? = null,
) : StateType {

    inline fun <reified T> getField(id: FieldId): T = form.getField(id) as T

    fun setField(field: DataField<*>) {
        form.setField(field)
    }

    inline fun <reified T> getValidator(id: FieldId): T = formValidators[id] as T

    fun getError(id: FieldId): AddCustomTokenError? = formErrors[id]

    fun hasError(id: FieldId): Boolean = formErrors[id] != null

    inline fun <reified T> visitDataConverter(converter: FieldDataConverter<T>): T {
        form.visitDataConverter(converter)
        return converter.getConvertedData()
    }

    fun blockchainToName(blockchain: Blockchain, isDerivationPath: Boolean = false): String? {
        return when {
            isDerivationPath -> blockchain.derivationPath(DerivationStyle.LEGACY)?.rawPath
            else -> {
                when (blockchain) {
                    Blockchain.Unknown -> null
                    else -> blockchain.fullName
                }
            }
        }
    }

    // except network
    fun tokensFieldsIsFilled(): Boolean {
        val idsToCheck = listOf(ContractAddress, Name, Symbol, Decimals)
        val fieldsToCheck = form.fieldList.filter { idsToCheck.contains(it.id) }
        val validator = StringIsNotEmptyValidator()
        fieldsToCheck.forEach { field ->
            val error = validator.validate(field.data.value?.toString())
            if (error != null) return false
        }
        return true
    }

    // except network
    fun tokensAnyFieldsIsFilled(): Boolean {
        val idsToCheck = listOf(ContractAddress, Name, Symbol, Decimals)
        val fieldsToCheck = form.fieldList.filter { idsToCheck.contains(it.id) }
        val validator = StringIsEmptyValidator()
        val errorsList = fieldsToCheck.mapNotNull { field ->
            validator.validate(field.data.value?.toString())
        }
        return errorsList.isNotEmpty()
    }

    fun networkIsSelected(): Boolean {
        val network = getField<TokenBlockchainField>(Network)
        return network.data.value != Blockchain.Unknown
    }

    fun derivationPathIsSelected(): Boolean {
        val network = getField<TokenDerivationPathField>(DerivationPath)
        return network.data.value != Blockchain.Unknown
    }

    fun getCustomTokenType(): CustomTokenType = when {
        tokensAnyFieldsIsFilled() || tokensFieldsIsFilled() -> CustomTokenType.Token
        else -> CustomTokenType.Blockchain
    }

    fun gatherUserToken(): CustomCurrency.CustomToken? = try {
        getToken()
    } catch (ex: Exception) {
        null
    }

    fun gatherBlockchain(): CustomCurrency.CustomBlockchain? = try {
        getBlockchain()
    } catch (ex: Exception) {
        null
    }

    fun reset(card: CardDTO): AddCustomTokenState {
        return this.copy(
            appSavedCurrencies = null,
            onTokenAddCallback = null,
            cardDerivationStyle = null,
            form = Form(createFormFields(card, CustomTokenType.Blockchain)),
            formErrors = emptyMap(),
            foundToken = null,
            warnings = emptySet(),
            screenState = createInitialScreenState(card.settings.isHDWalletAllowed),
            tangemTechServiceManager = null,
        )
    }

    private fun getToken(): CustomCurrency.CustomToken {
        return CustomCurrency.CustomToken.Converter(foundToken?.id, cardDerivationStyle)
            .apply { visitDataConverter(this) }
            .getConvertedData()
    }

    private fun getBlockchain(): CustomCurrency.CustomBlockchain {
        return CustomCurrency.CustomBlockchain.Converter(cardDerivationStyle)
            .apply { visitDataConverter(this) }
            .getConvertedData()
    }

    fun getNetworks(card: CardDTO, type: CustomTokenType): List<Blockchain> {
        return getNetworksList(card, type)
    }

    companion object {

        /**
         * If an user select derivation path (derivationNetwork) as Blockchain.Unknown,
         * then we should use a blockchain from the mainNetwork to determine a DerivationPath
         */
        internal fun getDerivationPath(
            mainNetwork: Blockchain,
            derivationNetwork: Blockchain,
            derivationStyle: DerivationStyle?,
        ): com.tangem.common.hdWallet.DerivationPath? {
            // If we allow user to select derivations, we need to provide different derivations
            // (Legacy style derivations).
            // But the mainNetwork derivation depends on whether a user has a card
            // with legacy derivations or new style derivations.
            val derivationStyleToUse = if (derivationNetwork == Blockchain.Unknown) {
                derivationStyle
            } else {
                DerivationStyle.LEGACY
            }
            return when (derivationNetwork) {
                Blockchain.Unknown -> mainNetwork
                else -> derivationNetwork
            }.derivationPath(derivationStyleToUse)
        }

        internal fun createFormFields(card: CardDTO, type: CustomTokenType): List<DataField<*>> {
            return listOf(
                TokenField(ContractAddress),
                TokenBlockchainField(Network, getNetworksList(card, type)),
                TokenField(Name),
                TokenField(Symbol),
                TokenField(Decimals),
                TokenDerivationPathField(DerivationPath, getSupportedDerivations(card)),
            )
        }

        /**
         * Serves to determine the networks (blockchains & tokens) that can be selected by Form.Networks.
         * Blockchain.Unknown - is the default selection
         */
        private fun getNetworksList(card: CardDTO, type: CustomTokenType): List<Blockchain> {
            val evmBlockchains = Blockchain.values()
                .filter { it.isEvm() }
                .filter { card.isTestCard == it.isTestnet() }

            val additionalBlockchains = listOf(
                Blockchain.Binance,
                Blockchain.BinanceTestnet,
                Blockchain.Solana,
                Blockchain.SolanaTestnet,
                Blockchain.Tron,
                Blockchain.TronTestnet,
            )

            val supportedByCard = when (type) {
                CustomTokenType.Blockchain -> card.supportedBlockchains()
                CustomTokenType.Token -> card.supportedTokens()
            }
            val typedNetworksList = (evmBlockchains + additionalBlockchains)
                .filter { supportedByCard.contains(it) }
                .toMutableList()

            val default = Blockchain.Unknown
            typedNetworksList.add(0, default)

            return typedNetworksList.sortByName()
        }

        private fun createFormValidators(): Map<CustomTokenFieldId, CustomTokenValidator<out Any>> {
            return mapOf(
                ContractAddress to TokenContractAddressValidator(),
                Network to TokenNetworkValidator(),
                Name to TokenNameValidator(),
                Symbol to TokenSymbolValidator(),
                Decimals to TokenDecimalsValidator(),
            )
        }

        private fun getSupportedDerivations(card: CardDTO): List<Blockchain> {
            val evmBlockchains = Blockchain.values()
                .filter { card.isTestCard == it.isTestnet() && it.isEvm() }
                .filter { it.isSupportedInApp() }

            return (listOf(Blockchain.Unknown) + evmBlockchains).sortByName()
        }

        internal fun createInitialScreenState(showDerivationPathField: Boolean = false): ScreenState {
            return ScreenState(
                contractAddressField = ViewStates.TokenField(),
                network = ViewStates.TokenField(),
                name = ViewStates.TokenField(isEnabled = false),
                symbol = ViewStates.TokenField(isEnabled = false),
                decimals = ViewStates.TokenField(isEnabled = false),
                derivationPath = ViewStates.TokenField(isVisible = showDerivationPathField),
                addButton = ViewStates.AddButton(isEnabled = false),
            )
        }
    }

    class Converter : StringActionStateConverter<DomainState> {
        private val jsonConverter: MoshiJsonConverter = MoshiJsonConverter.INSTANCE
        private var builder: StringBuilder = StringBuilder()

        override fun convert(action: Action, stateHolder: DomainState): String? {
            val action = action as? AddCustomTokenAction ?: return null

            val state = stateHolder.addCustomTokensState
            val fieldConverter =
                FieldToJsonConverter(
                    listOf(
                        ContractAddress,
                        Network,
                        Name,
                        Symbol,
                        Decimals,
                        DerivationPath,
                    ),
                    jsonConverter,
                )
            state.visitDataConverter(fieldConverter)
            val errors = state.formErrors.map {
                "${it.key}: ${it.value::class.java.simpleName}"
            }
            val warnings = state.warnings.map { it::class.java.simpleName }

            printAction(action, state)
            printStateValue("fields", fieldConverter.getConvertedData())
            printStateValue("fieldErrors", toJson(errors))
            printStateValue("warnings", toJson(warnings))
            printStateValue("screenState", toJson(state.screenState))
            printMessage("------------------------------------------------------")

            val printed = builder.toString()
            builder = StringBuilder()

            return printed
        }

        private fun printStateValue(name: String, value: String) {
            printMessage("$name: $value")
        }

        private fun printAction(action: AddCustomTokenAction, state: AddCustomTokenState) {
            printMessage("action: $action, state: ${state::class.java.simpleName}")
        }

        private fun toJson(value: Any): String {
            return jsonConverter.prettyPrint(value)
        }

        private fun printMessage(message: String) {
            builder.append("$message\n")
        }
    }
}

private fun List<Blockchain>.sortByName(): List<Blockchain> = this.sortedBy { it.fullName }

enum class CustomTokenType {
    Token, Blockchain
}
