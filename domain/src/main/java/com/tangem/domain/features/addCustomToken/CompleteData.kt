package com.tangem.domain.features.addCustomToken

import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.common.form.BaseFieldDataConverter
import com.tangem.domain.common.form.FieldDataConverter
import com.tangem.domain.common.form.FieldId

/**
[REDACTED_AUTHOR]
 */
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
                collectedData[CustomTokenFieldId.Network] as Blockchain,
                collectedData[CustomTokenFieldId.DerivationPath] as? String,
            )

            override fun getIdToCollect(): List<FieldId> = listOf(CustomTokenFieldId.Network, CustomTokenFieldId.DerivationPath)
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
                collectedData[CustomTokenFieldId.ContractAddress] as String,
                collectedData[CustomTokenFieldId.Network] as Blockchain,
                collectedData[CustomTokenFieldId.Name] as String,
                collectedData[CustomTokenFieldId.Symbol] as String,
                collectedData[CustomTokenFieldId.Decimals] as Int,
                collectedData[CustomTokenFieldId.DerivationPath] as? String,
            )

            override fun getIdToCollect(): List<FieldId> = CustomTokenFieldId.values().toList()
        }
    }
}