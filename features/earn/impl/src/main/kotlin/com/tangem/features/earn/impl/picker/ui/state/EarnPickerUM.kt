package com.tangem.features.earn.impl.picker.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.badge.TangemBadgeUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.earn.api.ui.EarnRowUM
import kotlinx.collections.immutable.ImmutableList

/**
 * @property title the fixed screen title over the hero, the same for every product
 * @property barTitle the title of the collapsed top bar: the selected product, the screen title while nothing is loaded
 * @property products the products of the currency, shown only when there are several
 * @property ctaHint the line above the button, `null` when there is nothing to explain
 */
@Immutable
internal data class EarnPickerUM(
    val title: TextReference,
    val subtitle: TextReference,
    val barTitle: TextReference,
    val products: ImmutableList<EarnProductCardUM>,
    val content: ContentUM,
    val cta: TangemButtonUM,
    val ctaHint: TextReference?,
    val onClose: () -> Unit,
) {

    @Immutable
    sealed interface ContentUM {

        data object Loading : ContentUM

        data class Unavailable(val onRetryClick: () -> Unit) : ContentUM

        /**
         * @property incentive the Incentivised badge above the description, `null` without a campaign
         * @property sections the sections of the page in their order; the tabs are anchors to them
         */
        data class Ready(
            val description: TextReference,
            val incentive: TangemBadgeUM?,
            val sections: ImmutableList<EarnPickerTab>,
            val benefits: ImmutableList<EarnBenefitUM>,
            val details: ImmutableList<EarnRowUM>,
        ) : ContentUM
    }
}

@Immutable
internal data class EarnProductCardUM(
    val id: String,
    val title: TextReference,
    val rate: TextReference,
    val isSelected: Boolean,
    val onClick: () -> Unit,
)

@Immutable
internal data class EarnBenefitUM(
    val icon: TangemIconUM.Icon?,
    val title: TextReference,
    val description: TextReference,
)

internal enum class EarnPickerTab { Calculator, Benefits, Details }