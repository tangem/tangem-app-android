package com.tangem.features.foryou

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.foryou.model.ForYouPeriod
import kotlinx.coroutines.flow.Flow

/**
 * Embeddable "Token summary" block (For You).
 *
 * Unlike the full-screen [TokenSummaryComponent], this is an inline [ComposableContentComponent] that is embedded into
 * a parent screen (e.g. the market details screen). A tap on the block is delegated to [Params.onClick], which the
 * parent uses to open the full [TokenSummaryComponent].
 */
interface TokenSummaryBlockComponent : ComposableContentComponent {

    /**
     * @property symbol        coin symbol the summary is built for (used to fetch indicators).
     * @property selectedPeriod reactive token-summary period owned by the parent screen (already capped at month).
     *  The block re-derives the sentiment whenever the parent changes it.
     * @property onClick       invoked on tap.
     */
    data class Params(
        val symbol: String,
        val selectedPeriod: Flow<ForYouPeriod>,
        val onClick: () -> Unit,
    )

    interface Factory : ComponentFactory<Params, TokenSummaryBlockComponent>
}