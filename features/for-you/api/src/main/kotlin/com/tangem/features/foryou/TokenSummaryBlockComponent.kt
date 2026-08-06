package com.tangem.features.foryou

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.foryou.model.ForYouPeriod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

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
     * @property isHostLoading `true` while the host screen is still loading its own content. The block keeps its
     *  placeholder until it emits `false`, so both shimmers stay on screen for the same time — the block's data
     *  usually arrives first, and without this the loaded card would sit above a still-shimmering screen.
     * @property callbacks     parent callbacks (e.g. tap handling).
     */
    data class Params(
        val symbol: String,
        val selectedPeriod: Flow<ForYouPeriod>,
        val callbacks: Callbacks,
        val isHostLoading: Flow<Boolean> = flowOf(false),
    )

    interface Callbacks {
        fun onClick()
    }

    interface Factory : ComponentFactory<Params, TokenSummaryBlockComponent>
}