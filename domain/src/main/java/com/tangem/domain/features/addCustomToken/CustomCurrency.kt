package com.tangem.domain.features.addCustomToken

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.DerivationStyle
import com.tangem.blockchain.common.Token
import com.tangem.common.hdWallet.DerivationPath
import com.tangem.domain.common.form.BaseFieldDataConverter
import com.tangem.domain.common.form.FieldId
import com.tangem.domain.features.addCustomToken.redux.AddCustomTokenState

/**
 * Created by Anton Zhilenkov on 07/04/2022.
 */
sealed class CustomCurrency(
    val network: Blockchain,
    val derivationPath: DerivationPath?,
) {

    class CustomBlockchain(
        network: Blockchain,
        derivationPath: DerivationPath?,
    ) : CustomCurrency(network, derivationPath) {

        class Converter(
            private val derivationStyle: DerivationStyle?,
        ) : BaseFieldDataConverter<CustomBlockchain>() {
            override fun getConvertedData(): CustomBlockchain {
                val mainNetwork = collectedData[CustomTokenFieldId.Network] as Blockchain
                val derivationPathNetwork = collectedData[CustomTokenFieldId.DerivationPath] as Blockchain
                val derivationPath = AddCustomTokenState.getDerivationPath(
                    mainNetwork,
                    derivationPathNetwork,
                    derivationStyle,
                )
                return CustomBlockchain(mainNetwork, derivationPath)
            }

            override fun getIdToCollect(): List<FieldId> =
                listOf(CustomTokenFieldId.Network, CustomTokenFieldId.DerivationPath)
        }
    }

    class CustomToken(
        val token: Token,
        network: Blockchain,
        derivationPath: DerivationPath?,
    ) : CustomCurrency(network, derivationPath) {

        class Converter(
            private val tokenId: String?,
            private val derivationStyle: DerivationStyle?,
        ) : BaseFieldDataConverter<CustomToken>() {

            override fun getConvertedData(): CustomToken {
                val mainNetwork = collectedData[CustomTokenFieldId.Network] as Blockchain
                val derivationPathNetwork = collectedData[CustomTokenFieldId.DerivationPath] as Blockchain
                val derivationPath = AddCustomTokenState.getDerivationPath(
                    mainNetwork,
                    derivationPathNetwork,
                    derivationStyle,
                )

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
                    derivationPath,
                )
            }

            override fun getIdToCollect(): List<FieldId> = CustomTokenFieldId.values().toList()
        }
    }
}
