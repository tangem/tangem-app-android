package com.tangem.features.earn.flow.impl.ui.state

import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.ds2.statusscreen.TangemStatusScreen
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.earn.api.ui.EarnRowUM
import com.tangem.features.earn.product.api.session.EarnResultAsset
import kotlinx.collections.immutable.ImmutableList

/**
 * A result without buttons of its own gets a close button as [primary]. With [explorer] the footer shows the explorer
 * and share buttons, and [primary] turns into a round "done" button next to them.
 */
internal data class EarnResultUM(
    val icons: EarnIconsUM,
    val asset: EarnResultAsset,
    val variant: TangemStatusScreen.Variant,
    val title: TextReference,
    val subtitle: TextReference?,
    val chip: TextReference?,
    val rows: ImmutableList<EarnRowUM>,
    val primary: TangemButtonUM?,
    val secondary: TangemButtonUM?,
    val explorer: ExplorerUM?,
    val onClose: () -> Unit,
) {

    data class ExplorerUM(val onExploreClick: () -> Unit, val onShareClick: () -> Unit)
}