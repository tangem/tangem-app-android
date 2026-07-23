package com.tangem.features.foryou

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.foryou.model.ForYouPeriod
import kotlinx.coroutines.flow.Flow

/**
 * Embeddable "Token summary" block (For You).
 *
 * Unlike the full-screen [TokenSummaryComponent], this is an inline [ComposableContentComponent] that is embedded into
 * a parent screen (e.g. the market details screen). A tap on the block is delegated to [Callbacks.onClick], which the
 * parent uses to open the full [TokenSummaryComponent].
 */
interface TokenSummaryBlockComponent : ComposableContentComponent {

    /**
     * @property symbol        coin symbol the summary is built for (used to fetch indicators).
     * @property selectedPeriod reactive token-summary period owned by the parent screen (already capped at month).
     *  The block re-derives the sentiment whenever the parent changes it.
     * @property callbacks     parent callbacks (e.g. tap handling).
     */
    data class Params(
        val symbol: String,
        val selectedPeriod: Flow<ForYouPeriod>,
        val callbacks: Callbacks,
    )

    interface Callbacks {
        fun onClick()
    }

    interface Factory : ComponentFactory<Params, TokenSummaryBlockComponent>
}