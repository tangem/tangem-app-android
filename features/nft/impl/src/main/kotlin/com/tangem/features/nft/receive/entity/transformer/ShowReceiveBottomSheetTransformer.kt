package com.tangem.features.nft.receive.entity.transformer

import com.tangem.common.ui.bottomsheet.receive.TokenReceiveBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.model.NetworkAddress
import com.tangem.features.nft.receive.entity.NFTReceiveUM
import com.tangem.utils.transformer.Transformer

internal class ShowReceiveBottomSheetTransformer(
    private val network: Network,
    private val networkAddress: NetworkAddress,
    private val onDismissBottomSheet: () -> Unit,
    private val onCopyClick: (String) -> Unit,
    private val onShareClick: (String) -> Unit,
) : Transformer<NFTReceiveUM> {

    override fun transform(prevState: NFTReceiveUM): NFTReceiveUM = prevState.copy(
        bottomSheetConfig = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = onDismissBottomSheet,
            content = TokenReceiveBottomSheetConfig(
                asset = TokenReceiveBottomSheetConfig.Asset.NFT,
                network = network,
                networkAddress = networkAddress,
                showMemoDisclaimer = network.transactionExtrasType != Network.TransactionExtrasType.NONE,
                onCopyClick = onCopyClick,
                onShareClick = onShareClick,
            ),
        ),
    )
}