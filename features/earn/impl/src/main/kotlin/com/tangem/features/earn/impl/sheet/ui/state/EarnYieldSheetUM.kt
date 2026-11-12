package com.tangem.features.earn.impl.sheet.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.ds2.badge.TangemBadgeUM
import com.tangem.core.ui.extensions.ImageReference
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.earn.EarnProductId
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class EarnYieldSheetUM(
    val title: TextReference,
    val inactiveTitle: TextReference,
    val active: ImmutableList<EarnYieldRowUM>,
    val inactive: ImmutableList<EarnYieldRowUM>,
    val onDismiss: () -> Unit,
) : TangemBottomSheetConfigContent

/**
 * @property isActive an active product may have neither [amount] nor [caption], so the layout cannot be derived
 * from them
 * @property amount `null` when the position has no known amount, or the product is inactive
 */
@Immutable
internal data class EarnYieldRowUM(
    val id: EarnProductId,
    val title: TextReference,
    val icon: ImageReference,
    val rate: TextReference,
    val isActive: Boolean,
    val amount: TextReference?,
    val amountFiat: TextReference?,
    val caption: TextReference?,
    val risk: TangemBadgeUM?,
    val onClick: () -> Unit,
)