package com.tangem.tap.features.wallet.ui

import android.view.View
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.address.AddressType
import com.tangem.wallet.R

object MultipleAddressUiHelper {

    private val blockchainsSupportingSplit = listOf(
        Blockchain.Bitcoin,
        Blockchain.BitcoinTestnet,
        Blockchain.Litecoin,
        Blockchain.BitcoinCash,
        Blockchain.CardanoShelley,
    )

    fun typeToId(type: AddressType, blockchain: Blockchain): Int {
        return if (blockchain in blockchainsSupportingSplit) {
            if (type == AddressType.Legacy) {
                R.id.chip_legacy
            } else {
                R.id.chip_default
            }
        } else {
            View.NO_ID
        }
    }

    fun idToType(id: Int, blockchain: Blockchain): AddressType? {
        return when (blockchain) {
            in blockchainsSupportingSplit -> {
                when (id) {
                    R.id.chip_default -> AddressType.Default
                    R.id.chip_legacy -> AddressType.Legacy
                    else -> null
                }
            }
            else -> null
        }
    }
}