package com.tangem.domain.features.addCustomToken.redux

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.DerivationStyle
import com.tangem.domain.common.form.*
import com.tangem.domain.features.addCustomToken.*
import com.tangem.domain.features.addCustomToken.CustomTokenFieldId.*
import com.tangem.network.api.tangemTech.TangemTechService
import org.rekotlin.StateType

data class AddCustomTokenState(
    val form: Form = Form(createFormFields()),
    val formValidators: Map<CustomTokenFieldId, CustomTokenValidator<*>> = createFormValidators(),
    val formErrors: Map<CustomTokenFieldId, AddCustomTokenError> = emptyMap(),
    val warnings: Set<AddCustomTokenWarning> = emptySet(),
    val screenState: ScreenState = createInitialScreenState(),
    val addCustomTokenManager: AddCustomTokenManager = AddCustomTokenManager(TangemTechService()),
    val derivationStyle: DerivationStyle? = null
) : StateType {

    val completeDataType: CompleteDataType
        get() = calculateDataType()

    inline fun <reified T> visitDataConverter(converter: FieldDataConverter<T>): T {
        form.visitDataConverter(converter)
        return converter.getConvertedData()
    }

    fun getValidator(id: FieldId): CustomTokenValidator<*> = formValidators[id]!!

    fun hasError(id: FieldId): Boolean = formErrors[id] != null

    fun getError(id: FieldId): AddCustomTokenError? {
        return formErrors[id]
    }

    fun convertBlockchainName(blockchain: Blockchain, unknown: String): String = when (blockchain) {
        Blockchain.Unknown -> unknown
        else -> blockchain.fullName
    }

    fun convertDerivationPathLabel(blockchain: Blockchain, unknown: String): String {
        return blockchain.derivationPath(derivationStyle)?.rawPath ?: unknown
    }

    private fun calculateDataType(): CompleteDataType {
        val idsToCheck = listOf(ContractAddress, Name, Symbol, Decimals)
        val fieldsToCheck = form.fieldList.filter { idsToCheck.contains(it.id) }

        val isEmptyValidator = StringIsEmptyValidator()
        fieldsToCheck.map { data -> data.toString() }.forEach {
            // if one of the fields has error -> then it
            val error = isEmptyValidator.validate(it)
            if (error != null) return CompleteDataType.Token
        }

        return CompleteDataType.Blockchain
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

        private fun createFormValidators(): Map<CustomTokenFieldId, CustomTokenValidator<*>> {
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