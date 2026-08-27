package com.tangem.features.foryou.impl.tokensummary.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

/** State of the single button pinned to the bottom of the token summary. */
@Immutable
internal sealed interface BottomButtonUM {

    /** The user's holdings of the summary token are still being resolved. */
    data object Loading : BottomButtonUM

    /**
     * A ready button: what it offers — topping the token up or swapping it — is already resolved into [text] and
     * [onClick]. A disabled button keeps its [text] but does nothing when tapped.
     */
    data class Content(
        val text: TextReference,
        val isEnabled: Boolean,
        val onClick: () -> Unit,
    ) : BottomButtonUM
}