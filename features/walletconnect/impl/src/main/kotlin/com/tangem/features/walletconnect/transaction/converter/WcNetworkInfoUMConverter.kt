package com.tangem.features.walletconnect.transaction.converter

import com.tangem.core.ui.extensions.getActiveIconRes
import com.tangem.domain.models.network.Network
import com.tangem.features.walletconnect.transaction.entity.common.WcNetworkInfoUM
import com.tangem.utils.converter.Converter
import javax.inject.Inject

internal class WcNetworkInfoUMConverter @Inject constructor() : Converter<Network, WcNetworkInfoUM> {

    override fun convert(value: Network) = WcNetworkInfoUM(
        name = value.name,
        iconRes = getActiveIconRes(value.rawId),
    )
}