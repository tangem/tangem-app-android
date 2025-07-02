package com.tangem.features.nft.receive.entity.transformer

import com.tangem.common.ui.bottomsheet.receive.TokenReceiveBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.features.nft.receive.entity.NFTReceiveUM
import com.tangem.utils.transformer.Transformer
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.lib.crypto.BlockchainUtils
import kotlinx.collections.immutable.toPersistentList

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
                customNotifications = buildList {
                    if (BlockchainUtils.isSolana(network.rawId)) {
                        add(
                            NotificationConfig(
                                title = resourceReference(R.string.nft_receive_unsupported_types),
                                subtitle = resourceReference(R.string.nft_receive_unsupported_types_description),
                                iconResId = R.drawable.ic_alert_circle_24,
                                iconTint = NotificationConfig.IconTint.Attention,
                            ),
                        )
                    }
                }.toPersistentList(),
                onCopyClick = onCopyClick,
                onShareClick = onShareClick,
            ),
        ),
    )
}