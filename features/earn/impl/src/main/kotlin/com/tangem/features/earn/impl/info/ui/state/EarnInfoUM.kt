package com.tangem.features.earn.impl.info.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.extensions.ImageReference
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.earn.api.ui.EarnRowUM
import com.tangem.features.earn.impl.picker.ui.state.EarnBenefitUM
import kotlinx.collections.immutable.ImmutableList

/**
 * @property earned the Earned chip, `null` while the product reports no earnings
 * @property rewards the claimable rewards banner, `null` without rewards
 * @property staleNotice when the data was last updated, `null` while the data is fresh
 * @property tabs the sections present on the page in their order; the chips are anchors to them
 * @property secondary the buttons next to the primary one, in the order of the product
 */
@Immutable
internal data class EarnInfoUM(
    val title: TextReference,
    val subtitle: TextReference,
    val balanceFiat: TextReference,
    val balanceCrypto: TextReference,
    val earned: TextReference?,
    val rewards: RewardsUM?,
    val staleNotice: TextReference?,
    val tabs: ImmutableList<EarnInfoTab>,
    val myFunds: MyFundsUM,
    val benefits: ImmutableList<EarnBenefitUM>,
    val details: ImmutableList<EarnRowUM>,
    val secondary: ImmutableList<TangemButtonUM>,
    val primary: TangemButtonUM,
    val onClose: () -> Unit,
) {

    /** @property onHowItWorksClick `null` when the product has no "How it works" explanation */
    @Immutable
    data class MyFundsUM(
        val provider: TextReference,
        val providerIcon: ImageReference,
        val description: TextReference?,
        val rows: ImmutableList<EarnRowUM>,
        val onHowItWorksClick: (() -> Unit)?,
    )

    @Immutable
    data class RewardsUM(
        val title: TextReference,
        val description: TextReference,
        val claimLabel: TextReference,
        val onClaimClick: () -> Unit,
    )
}

internal enum class EarnInfoTab { MyFunds, Benefits, Details }