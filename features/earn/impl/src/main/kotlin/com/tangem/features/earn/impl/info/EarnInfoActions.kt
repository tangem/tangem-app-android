package com.tangem.features.earn.impl.info

import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.features.earn.product.api.product.EarnActionItem
import com.tangem.features.earn.product.api.product.EarnBlock
import com.tangem.features.earn.product.api.session.EarnAction
import javax.inject.Inject

internal class EarnInfoActions @Inject constructor() {

    fun button(
        item: EarnActionItem,
        type: TangemButtonType,
        onActionClick: (EarnAction) -> Unit,
        onBlocked: (EarnBlock) -> Unit,
    ): TangemButtonUM {
        val block = item.block

        return TangemButtonUM(
            text = item.label,
            isDimmed = block != null,
            type = type,
            onClick = if (block != null) {
                { onBlocked(block) }
            } else {
                { onActionClick(item.action) }
            },
        )
    }
}