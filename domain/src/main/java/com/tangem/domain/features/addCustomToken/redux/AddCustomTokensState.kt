package com.tangem.domain.features.addCustomToken.redux

import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.common.form.*
import com.tangem.domain.features.addCustomToken.*
import com.tangem.domain.features.addCustomToken.CustomTokenFieldId.*
import com.tangem.network.api.tangemTech.TangemTechService
import org.rekotlin.StateType

data class AddCustomTokensState(
    val form: Form = Form(createFormFields()),
    val formValidators: Map<CustomTokenFieldId, CustomTokenValidator<*>> = createFormValidators(),
    val formErrors: Map<CustomTokenFieldId, AddCustomTokenError> = emptyMap(),
    val warnings: List<AddCustomTokenWarning> = emptyList(),
    val addCustomTokenManager: AddCustomTokenManager = AddCustomTokenManager(TangemTechService())
) : StateType {

    val completeDataType: CompleteDataType
        get() = calculateDataType()

    fun getData(
        converter: FieldDataConverter<out CompleteData> = CompleteData.createDataConverter(completeDataType)
    ): CompleteData {
        form.getData(converter)
        return converter.getConvertedData()
    }

    fun getLockedFieldsForKnownToken(): List<CustomTokenFieldId> {
        return listOf(
            Name, Symbol, Decimals
        )
    }

    fun getValidator(id: FieldId): CustomTokenValidator<*> = formValidators[id]!!

    fun hasError(id: FieldId): Boolean = formErrors[id] != null

    fun getError(id: FieldId): AddCustomTokenError? {
        return formErrors[id]
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


    companion object Utils {
        private fun createFormFields(): List<DataField<*>> {
            return listOf(
                TokenField(ContractAddress),
                TokenNetworkField(Network, getSupportedBlockchains()),
                TokenField(Name),
                TokenField(Symbol),
                TokenField(Decimals),
                TokenDerivationPathField(DerivationPath),
            )
        }

        private fun createFormValidators(): Map<CustomTokenFieldId, CustomTokenValidator<*>> {
            return mapOf(
                ContractAddress to TokenContractAddressValidator(),
                Network to TokenNetworkValidator(),
                Name to StringIsNotEmptyValidator(),
                Symbol to StringIsNotEmptyValidator(),
                Decimals to StringIsNotEmptyValidator(),
                DerivationPath to DerivationPathValidator(),
            )
        }

        private fun getSupportedBlockchains(): List<Blockchain> {
            return Blockchain.values().filter { !it.isTestnet() }.toList()
        }
    }
}

enum class CompleteDataType {
    Blockchain, Token
}

sealed class CompleteData() {

    companion object {
        fun createDataConverter(completeDataType: CompleteDataType): FieldDataConverter<out CompleteData> =
            when (completeDataType) {
                CompleteDataType.Blockchain -> CustomBlockchain.Converter()
                CompleteDataType.Token -> CustomToken.Converter()
            }
    }

    class CustomBlockchain(
        val selectedNetwork: Blockchain,
        val derivationPath: String?
    ) : CompleteData() {

        class Converter : BaseFieldDataConverter<CustomBlockchain>() {
            override fun getConvertedData(): CustomBlockchain = CustomBlockchain(
                collectedData[Network] as Blockchain,
                collectedData[DerivationPath] as? String,
            )

            override fun getIdToCollect(): List<FieldId> = listOf(Network, DerivationPath)
        }
    }

    class CustomToken(
        val contractAddress: String,
        val selectedNetwork: Blockchain,
        val name: String,
        val tokenSymbol: String,
        val decimals: Int,
        val derivationPath: String?,
    ) : CompleteData() {

        class Converter : BaseFieldDataConverter<CustomToken>() {
            override fun getConvertedData(): CustomToken = CustomToken(
                collectedData[ContractAddress] as String,
                collectedData[Network] as Blockchain,
                collectedData[Name] as String,
                collectedData[Symbol] as String,
                collectedData[Decimals] as Int,
                collectedData[DerivationPath] as? String,
            )

            override fun getIdToCollect(): List<FieldId> = CustomTokenFieldId.values().toList()
        }
    }
}