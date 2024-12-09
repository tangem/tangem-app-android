package com.tangem.common.ui.expressStatus.state

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Stable
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

/**
 * UI data holder for express status block
 *
 * @property title block title
 * @property link provider web link
 * @property statuses list of possible and active statuses
 */
data class ExpressStatusUM(
    val title: TextReference,
    val link: ExpressLinkUM,
    val statuses: ImmutableList<ExpressStatusItemUM>,
)

/**
 * Provider web link for express status block.
 * [Empty] if no link needed
 * [Content] if link is provided and displayed
 */
@Stable
sealed class ExpressLinkUM {
    data object Empty : ExpressLinkUM()
    data class Content(
        @DrawableRes val icon: Int,
        val text: TextReference,
        val onClick: () -> Unit,
    ) : ExpressLinkUM()
}

/**
 * Single status item in express status block
 */
data class ExpressStatusItemUM(
    val text: TextReference,
    val state: ExpressStatusItemState,
)

/**
 * Available status states for express status block
 */
enum class ExpressStatusItemState {
    Active,
    Default,
    Done,
    Warning,
    Error,
    ;
}