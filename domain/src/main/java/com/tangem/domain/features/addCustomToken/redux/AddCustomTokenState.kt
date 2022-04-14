package com.tangem.domain.features.addCustomToken.redux

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.DerivationStyle
import com.tangem.domain.common.form.*
import com.tangem.domain.features.addCustomToken.*
import com.tangem.domain.features.addCustomToken.CustomTokenFieldId.*
import org.rekotlin.StateType

data class AddCustomTokenState(
    val addedCurrencies: AddedCurrencies? = null,
    val onTokenAddCallback: ((CompleteData) -> Unit)? = null,
    val derivationStyle: DerivationStyle? = null,
    val form: Form = Form(createFormFields()),
    val formValidators: Map<CustomTokenFieldId, CustomTokenValidator<out Any>> = createFormValidators(),
    val formErrors: Map<CustomTokenFieldId, AddCustomTokenError> = emptyMap(),
    val tokenId: String? = null,
    val warnings: Set<AddCustomTokenWarning> = emptySet(),
    val screenState: ScreenState = createInitialScreenState(),
    val tangemTechServiceManager: TangemTechServiceManager? = null
) : StateType {

    inline fun <reified T> getField(id: FieldId): T = form.getField(id) as T

    inline fun <reified T> getValidator(id: FieldId): T = formValidators[id] as T

    fun getError(id: FieldId): AddCustomTokenError? = formErrors[id]

    fun hasError(id: FieldId): Boolean = formErrors[id] != null

    fun getCompleteData(type: CompleteDataType): CompleteData = when (type) {
        CompleteDataType.Token -> getToken()
        CompleteDataType.Blockchain -> getBlockchain()
    }

    inline fun <reified T> visitDataConverter(converter: FieldDataConverter<T>): T {
        form.visitDataConverter(converter)
        return converter.getConvertedData()
    }

    fun convertBlockchainName(blockchain: Blockchain, unknown: String): String = when (blockchain) {
        Blockchain.Unknown -> unknown
        else -> blockchain.fullName
    }

    fun convertDerivationPathLabel(blockchain: Blockchain, unknown: String): String {
        return blockchain.derivationPath(derivationStyle)?.rawPath ?: unknown
    }

    fun reset(): AddCustomTokenState {
        return this.copy(
            addedCurrencies = null,
            onTokenAddCallback = null,
            derivationStyle = null,
            form = Form(createFormFields()),
            formErrors = emptyMap(),
            tokenId = null,
            warnings = emptySet(),
            screenState = createInitialScreenState(),
            tangemTechServiceManager = null,
        )
    }

    fun networkIsEmpty(): Boolean {
        val network = getField<TokenBlockchainField>(Network)
        return network.data.value != Blockchain.Unknown
    }

    fun customTokensFieldsIsEmpty(): Boolean {
        val idsToCheck = listOf(ContractAddress, Name, Symbol, Decimals)
        val fieldsToCheck = form.fieldList.filter { idsToCheck.contains(it.id) }
        val validator = StringIsEmptyValidator()
//        val errors = mutableMapOf<>()
        fieldsToCheck.forEach { field ->
            val error = validator.validate(field.data.value?.toString())
            if (error != null) return true
        }
        return false
    }

    fun allFieldsIsEmpty(): Boolean = networkIsEmpty() && customTokensFieldsIsEmpty()

    private fun getToken(): CompleteData.CustomToken {
        return CompleteData.CustomToken.Converter(tokenId)
            .apply { visitDataConverter(this) }
            .getConvertedData()
    }

    private fun getBlockchain(): CompleteData.CustomBlockchain {
        return CompleteData.CustomBlockchain.Converter()
            .apply { visitDataConverter(this) }
            .getConvertedData()
    }

    companion object {
        private fun createFormFields(): List<DataField<*>> {
            return listOf(
                TokenField(ContractAddress),
                TokenBlockchainField(Network, getSupportedNetworks()),
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

        private fun getSupportedNetworks(): List<Blockchain> {
            return listOf(
                Blockchain.Unknown,
                Blockchain.Ethereum,
                Blockchain.BSC,
                Blockchain.Binance,
                Blockchain.Polygon,
                Blockchain.Avalanche,
//                Blockchain.Solana, // not supported until tokens added to the Blockchain SDK
                Blockchain.Fantom,
            )
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
                name = ViewStates.TokenField(),
                symbol = ViewStates.TokenField(),
                decimals = ViewStates.TokenField(),
                derivationPath = ViewStates.TokenField(),
                addButton = ViewStates.AddButton()
            )
        }
    }
}