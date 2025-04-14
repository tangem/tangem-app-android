package com.tangem.common.ui.bottomsheet.receive

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.R
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.model.NetworkAddress
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

data class TokenReceiveBottomSheetConfig(
    val asset: Asset,
    val network: String,
    val addresses: ImmutableList<AddressModel>,
    val showMemoDisclaimer: Boolean,
    val onCopyClick: (String) -> Unit,
    val onShareClick: (String) -> Unit,
) : TangemBottomSheetConfigContent {

    constructor(
        asset: Asset,
        network: Network,
        networkAddress: NetworkAddress,
        showMemoDisclaimer: Boolean,
        onCopyClick: (String) -> Unit,
        onShareClick: (String) -> Unit,
    ) : this(
        asset = asset,
        network = network.name,
        addresses = networkAddress.availableAddresses
            .mapToAddressModels(asset, network)
            .toImmutableList(),
        showMemoDisclaimer = showMemoDisclaimer,
        onCopyClick = onCopyClick,
        onShareClick = onShareClick,
    )

    @Immutable
    sealed class Asset {
        abstract val displaySymbol: TextReference
        data class Currency(val name: String, val symbol: String) : Asset() {
            override val displaySymbol = stringReference(symbol)
        }
        data object NFT : Asset() {
            override val displaySymbol = resourceReference(R.string.common_nft)
        }
    }
}