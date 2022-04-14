package com.tangem.domain.features.addCustomToken

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.domain.common.form.BaseFieldDataConverter
import com.tangem.domain.common.form.FieldId

/**
[REDACTED_AUTHOR]
 */
enum class CompleteDataType {
    Blockchain, Token
}

sealed class CompleteData() {

    class CustomBlockchain(
        val network: Blockchain,
        val derivationPath: String?
    ) : CompleteData() {

        class Converter : BaseFieldDataConverter<CustomBlockchain>() {
            override fun getConvertedData(): CustomBlockchain {
                val network = collectedData[CustomTokenFieldId.Network] as Blockchain
                val derivationPath = collectedData[CustomTokenFieldId.DerivationPath] as? String
                return CustomBlockchain(network, derivationPath)
            }

            override fun getIdToCollect(): List<FieldId> = listOf(CustomTokenFieldId.Network, CustomTokenFieldId.DerivationPath)
        }
    }

    class CustomToken(
        val token: Token,
        val network: Blockchain,
        val derivationPath: String?,
    ) : CompleteData() {

        class Converter(val tokenId: String?) : BaseFieldDataConverter<CustomToken>() {

            override fun getConvertedData(): CustomToken {
                val token = Token(
                    name = collectedData[CustomTokenFieldId.Name] as String,
                    symbol = collectedData[CustomTokenFieldId.Symbol] as String,
                    contractAddress = collectedData[CustomTokenFieldId.ContractAddress] as String,
                    decimals = (collectedData[CustomTokenFieldId.Decimals] as String).toInt(),
                    id = tokenId,
                )
                return CustomToken(
                    token,
                    collectedData[CustomTokenFieldId.Network] as Blockchain,
                    collectedData[CustomTokenFieldId.DerivationPath] as? String,
                )
            }

            override fun getIdToCollect(): List<FieldId> = CustomTokenFieldId.values().toList()
        }
    }
}