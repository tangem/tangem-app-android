package com.tangem.feature.wallet.child.organizetokens.model.converter.items

import com.tangem.core.ui.ds.row.header.TangemHeaderRowUM
import com.tangem.core.ui.ds.row.internal.TangemRowTailUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.network.Network
import com.tangem.feature.wallet.child.organizetokens.entity.OrganizeRowItemUM
import com.tangem.feature.wallet.impl.R
import com.tangem.utils.converter.Converter

internal object OrganizeNetworkItemConverter : Converter<Pair<AccountId, Network>, OrganizeRowItemUM.Network> {

    override fun convert(value: Pair<AccountId, Network>): OrganizeRowItemUM.Network {
        val (accountId, groupNetwork) = value
        return OrganizeRowItemUM.Network(
            headerRowUM = TangemHeaderRowUM(
                id = groupNetwork.id.toString(),
                title = stringReference(groupNetwork.name),
                tailUM = TangemRowTailUM.Draggable(R.drawable.ic_group_drop_24),
            ),
            accountId = accountId.value,
        )
    }
}