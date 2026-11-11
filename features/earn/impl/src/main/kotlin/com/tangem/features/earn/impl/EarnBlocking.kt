package com.tangem.features.earn.impl

import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.features.earn.product.api.product.EarnActionItem
import com.tangem.features.earn.product.api.product.EarnBlock
import com.tangem.features.earn.product.api.product.EarnProductState

private val OutOfDateBlock = EarnBlock(
    reason = resourceReference(R.string.token_button_unavailability_reason_out_of_date_balance),
)

/** Stale data wins over the product's own block: the user must see why the numbers cannot be trusted first. */
internal fun EarnActionItem.guardedBy(ready: EarnProductState.Ready): EarnActionItem =
    guardedBy(isStale = ready.freshness?.isStale == true)

internal fun EarnActionItem.guardedBy(isStale: Boolean): EarnActionItem =
    if (isStale) copy(block = OutOfDateBlock) else this

internal fun UiMessageSender.showReason(block: EarnBlock) {
    send(DialogMessage(message = block.reason))
}