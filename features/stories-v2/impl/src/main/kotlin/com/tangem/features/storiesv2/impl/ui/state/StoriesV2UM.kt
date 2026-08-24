package com.tangem.features.storiesv2.impl.ui.state

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.storiesv2.impl.content.StoryV2ContentMode
import com.tangem.features.storiesv2.impl.content.StoryV2MediaRef
import kotlinx.collections.immutable.ImmutableList

/**
 * Everything the story screen draws that changes at slide granularity. The position inside a slide is deliberately
 * absent — see [com.tangem.features.storiesv2.impl.ui.StoryPlayerHandle].
 */
@Immutable
internal data class StoriesV2UM(
    val slideCount: Int,
    val slideIndex: Int,
    val playToken: Int,
    val slide: SlideUM,
    val actions: ImmutableList<ActionUM>,
    val isPaused: Boolean,
    val onTapForward: () -> Unit,
    val onTapBack: () -> Unit,
    val onHoldChange: (Boolean) -> Unit,
    val onDragChange: (Boolean) -> Unit,
    val onCloseClick: () -> Unit,
    val onSwipeDown: () -> Unit,
) {

    /**
     * @property poster       drawn *over* the asset until it produces a frame of its own, and instead of it when it
     *                        cannot play at all
     * @property isAssetReady the asset has a frame on screen, so the poster can step aside
     */
    @Immutable
    data class SlideUM(
        val id: String,
        val title: TextReference?,
        val subtitle: TextReference?,
        val poster: StoryV2MediaRef,
        val durationMs: Long,
        val content: Content,
        val contentMode: StoryV2ContentMode,
        val isAssetReady: Boolean,
    ) {

        @Immutable
        sealed interface Content {

            data object Video : Content

            /** The poster is the whole slide: a still image, or an asset degraded to it. */
            data object Still : Content

            data class VectorAnimation(val source: StoryV2MediaRef) : Content
        }
    }

    @Immutable
    data class ActionUM(
        val id: String,
        val label: TextReference,
        val isPrimary: Boolean,
        @DrawableRes val iconRes: Int?,
        val onClick: () -> Unit,
    )
}