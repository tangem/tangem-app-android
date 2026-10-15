package com.tangem.features.collectibles.impl.packopening.ui.state

import android.view.SurfaceView
import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed interface CollectiblesPackOpeningUM {

    val onBackClick: () -> Unit
    val onCloseClick: () -> Unit

    data class Loading(
        override val onBackClick: () -> Unit,
        override val onCloseClick: () -> Unit,
    ) : CollectiblesPackOpeningUM

    data class Error(
        override val onBackClick: () -> Unit,
        override val onCloseClick: () -> Unit,
        val message: TextReference,
        val onReloadClick: () -> Unit,
        val isReloading: Boolean = false,
    ) : CollectiblesPackOpeningUM

    /** @property isNavigationVisible false while a purchase runs; a failure brings it back */
    data class Content(
        override val onBackClick: () -> Unit,
        override val onCloseClick: () -> Unit,
        val order: OrderUM,
        val reveal: RevealUM,
        val onApproveClick: () -> Unit,
        val isNavigationVisible: Boolean,
    ) : CollectiblesPackOpeningUM

    /** @property card null until the pack is opened */
    data class RevealUM(
        val card: RewardCardUM?,
        val onKeepCardClick: () -> Unit,
        val onSellBackClick: () -> Unit,
        val stage: Stage = Stage.Order,
        val isFullReveal: Boolean = true,
        val isSkipped: Boolean = false,
        val onPackAnimationFinished: () -> Unit = {},
        val onRevealTap: () -> Unit = {},
        val onSurfaceAttached: (SurfaceView) -> Unit = {},
        val onSurfaceDetached: (SurfaceView) -> Unit = {},
    )

    /** Order: the confirm screen. Launching: the pack is thrown. Reveal: the video. Reward: the card. */
    enum class Stage { Order, Launching, Reveal, Reward }

    data class OrderUM(
        val rows: ImmutableList<PriceRowUM>,
        val button: ButtonUM,
        val packImageUrl: String? = null,
    )

    data class ButtonUM(val textRef: TextReference, val isEnabled: Boolean)

    data class PriceRowUM(
        val labelRef: TextReference,
        val value: String,
        val isEmphasised: Boolean = false,
        val hasDivider: Boolean = true,
    )
}