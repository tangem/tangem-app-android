package com.tangem.tap.features.wallet.ui

import android.view.View
import com.tangem.blockchain.blockchains.bitcoin.BitcoinAddressType
import com.tangem.blockchain.blockchains.cardano.CardanoAddressType
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.address.AddressType
import com.tangem.tap.common.redux.global.CryptoCurrencyName
import com.tangem.wallet.R

class MultipleAddressUiHelper {
    companion object {
        fun typeToId(type: AddressType): Int {
            return when (type) {
                is BitcoinAddressType.Legacy -> R.id.chip_legacy
                is BitcoinAddressType.Segwit -> R.id.chip_default
                is CardanoAddressType.Byron -> R.id.chip_legacy
                is CardanoAddressType.Shelley -> R.id.chip_default
                else -> View.NO_ID
            }
        }

        fun idToType(id: Int, currencyName: CryptoCurrencyName?): AddressType? {
            val blockchain = currencyName?.let { currency ->
                Blockchain.fromCurrency(currency)
            }
            return idToType(id, blockchain)
        }

        fun idToType(id: Int, blockchain: Blockchain?): AddressType? {
            return when (id) {
                R.id.chip_default -> {
                    when (blockchain) {
                        Blockchain.Bitcoin, Blockchain.BitcoinTestnet -> BitcoinAddressType.Segwit
                        Blockchain.CardanoShelley -> CardanoAddressType.Shelley
                        else -> null
                    }
                }
                R.id.chip_legacy -> {
                    when (blockchain) {
                        Blockchain.Bitcoin, Blockchain.BitcoinTestnet -> BitcoinAddressType.Legacy
                        Blockchain.CardanoShelley -> CardanoAddressType.Byron
                        else -> null
                    }
                }
                else -> null
            }
        }
    }
}