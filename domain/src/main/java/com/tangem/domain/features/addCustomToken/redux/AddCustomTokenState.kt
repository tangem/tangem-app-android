package com.tangem.domain.features.addCustomToken.redux

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.DerivationStyle
import com.tangem.domain.AddCustomTokenError
import com.tangem.domain.DomainWrapped
import com.tangem.domain.common.form.*
import com.tangem.domain.features.addCustomToken.*
import com.tangem.domain.features.addCustomToken.CustomTokenFieldId.*
import org.rekotlin.StateType

data class AddCustomTokenState(
    val appSavedCurrencies: List<DomainWrapped.Currency>? = null,
    val onTokenAddCallback: ((CustomCurrency) -> Unit)? = null,
    val cardDerivationStyle: DerivationStyle? = null,
    val form: Form = Form(createFormFields(CustomTokenType.Blockchain)),
    val formValidators: Map<CustomTokenFieldId, CustomTokenValidator<out Any>> = createFormValidators(),
    val formErrors: Map<CustomTokenFieldId, AddCustomTokenError> = emptyMap(),
    val tokenId: String? = null,
    val warnings: Set<AddCustomTokenError.Warning> = emptySet(),
    val screenState: ScreenState = createInitialScreenState(),
    val tangemTechServiceManager: AddCustomTokenService? = null
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
            isDerivationPath -> blockchain.derivationPath(cardDerivationStyle)?.rawPath
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

    fun reset(): AddCustomTokenState {
        return this.copy(
            appSavedCurrencies = null,
            onTokenAddCallback = null,
            cardDerivationStyle = null,
            form = Form(createFormFields(CustomTokenType.Blockchain)),
            formErrors = emptyMap(),
            tokenId = null,
            warnings = emptySet(),
            screenState = createInitialScreenState(),
            tangemTechServiceManager = null,
        )
    }

    private fun getToken(): CustomCurrency.CustomToken {
        return CustomCurrency.CustomToken.Converter(tokenId, cardDerivationStyle)
            .apply { visitDataConverter(this) }
            .getConvertedData()
    }

    private fun getBlockchain(): CustomCurrency.CustomBlockchain {
        return CustomCurrency.CustomBlockchain.Converter(cardDerivationStyle)
            .apply { visitDataConverter(this) }
            .getConvertedData()
    }

    fun getNetworks(type: CustomTokenType): List<Blockchain> {
        return getSupportedNetworks(type)
    }

    companion object {

        /**
         * If an user select derivation path (derivationNetwork) as Blockchain.Unknown,
         * then we should use a blockchain from the mainNetwork to determine a DerivationPath
         */
        fun getDerivationPath(
            mainNetwork: Blockchain,
            derivationNetwork: Blockchain,
            derivationStyle: DerivationStyle?
        ): com.tangem.common.hdWallet.DerivationPath? = when (derivationNetwork) {
            Blockchain.Unknown -> mainNetwork
            else -> derivationNetwork
        }.derivationPath(derivationStyle)

        private fun createFormFields(type: CustomTokenType): List<DataField<*>> {
            return listOf(
                TokenField(ContractAddress),
                TokenBlockchainField(Network, getSupportedNetworks(type)),
                TokenField(Name),
                TokenField(Symbol),
                TokenField(Decimals),
                TokenDerivationPathField(DerivationPath, getSupportedDerivations()),
            )
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

        private fun getSupportedNetworks(type: CustomTokenType): List<Blockchain> {
            val networks = mutableListOf(
                Blockchain.Unknown,
                Blockchain.Ethereum,
                Blockchain.BSC,
                Blockchain.Binance,     // not evm
                Blockchain.Polygon,
                Blockchain.Avalanche,
                Blockchain.Fantom,
                Blockchain.Solana,      // not evm. Should be unsupported for coins until they are added to the Blockchain SDK
            )
            if (type == CustomTokenType.Token) networks.remove(Blockchain.Solana)

            return networks
        }

        private fun getSupportedDerivations(): List<Blockchain> {
            val evmBlockchains = Blockchain.values().filter {
                !it.isTestnet() && it.getChainId() != null
            }
            return listOf(Blockchain.Unknown) + evmBlockchains
        }

        private fun createInitialScreenState(): ScreenState {
            return ScreenState(
                contractAddressField = ViewStates.TokenField(),
                network = ViewStates.TokenField(),
                name = ViewStates.TokenField(isEnabled = false),
                symbol = ViewStates.TokenField(isEnabled = false),
                decimals = ViewStates.TokenField(isEnabled = false),
                derivationPath = ViewStates.TokenField(),
                addButton = ViewStates.AddButton(isEnabled = false)
            )
        }
    }
}

enum class CustomTokenType {
    Token, Blockchain
}