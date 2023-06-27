package com.tangem.tap.features.wallet.ui

import android.view.View
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.address.AddressType
import com.tangem.wallet.R

object MultipleAddressUiHelper {

    fun typeToId(type: AddressType, blockchain: Blockchain): Int {
        return when (blockchain) {
            in blockchainsSupportingSplit -> {
                if (type == AddressType.Legacy) {
                    R.id.chip_legacy
                } else {
                    R.id.chip_default
                }
            }
            else -> {
                View.NO_ID
            }
        }
    }

    fun idToType(id: Int, blockchain: Blockchain?): AddressType? {
        return when (id) {
            R.id.chip_default -> {
                when (blockchain) {
                    in blockchainsSupportingSplit -> AddressType.Default
                    else -> null
                }
            }

            R.id.chip_legacy -> {
                when (blockchain) {
                    in blockchainsSupportingSplit -> AddressType.Legacy
                    else -> null
                }
            }

            else -> null
        }
    }

    private val blockchainsSupportingSplit = listOf(
        Blockchain.Bitcoin,
        Blockchain.BitcoinTestnet,
        Blockchain.Litecoin,
        Blockchain.BitcoinCash,
        Blockchain.CardanoShelley
    )
}