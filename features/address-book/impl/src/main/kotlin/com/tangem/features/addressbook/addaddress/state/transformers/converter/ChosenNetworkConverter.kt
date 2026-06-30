package com.tangem.features.addressbook.addaddress.state.transformers.converter

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.ui.extensions.getActiveIconRes
import com.tangem.features.addressbook.addaddress.ui.state.AddAddressUM.ChosenNetworkStateUM.Result.NetworkUM
import com.tangem.utils.converter.Converter

internal class ChosenNetworkConverter : Converter<Blockchain, NetworkUM> {

    override fun convert(value: Blockchain): NetworkUM = NetworkUM(
        networkName = value.fullName,
        iconResId = getActiveIconRes(value),
    )
}