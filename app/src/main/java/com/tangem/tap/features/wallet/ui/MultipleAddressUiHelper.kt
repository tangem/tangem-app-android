package com.tangem.tap.features.wallet.ui

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.address.AddressType
import com.tangem.wallet.R

object MultipleAddressUiHelper {

    fun typeToId(type: AddressType): Int {
        return when (type) {
            AddressType.Legacy -> R.id.chip_legacy
            AddressType.Default -> R.id.chip_default
        }
    }

    fun idToType(id: Int, blockchain: Blockchain?): AddressType? {
        return when (id) {
            R.id.chip_default -> AddressType.Default
            R.id.chip_legacy -> AddressType.Legacy
            else -> null
        }
    }

}