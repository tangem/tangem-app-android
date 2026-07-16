package com.tangem.feature.swap.ui

import com.tangem.common.ui.navigationButtons.NavigationButton
import com.tangem.common.ui.navigationButtons.NavigationUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.feature.swap.presentation.R

/**
 * Builds the [NavigationUM] for the swap success screen, mirroring
 * `SendConfirmSuccessModel.configConfirmSuccessNavigation`.
 *
 * The explore button is always the single [NavigationUM.Content.primaryButton]; a transfer also
 * gets a share button, surfaced together as [NavigationUM.Content.secondaryPairButtonsUM] (the same
 * explore + share pair the send flow shows). [shareClick] is `null` for a regular swap, so the pair
 * is absent and only the explore button is rendered.
 */
internal fun swapSuccessNavigation(
    txUrl: String,
    exploreClick: () -> Unit,
    shareClick: (() -> Unit)? = null,
): NavigationUM {
    val exploreButton = NavigationButton(
        textReference = resourceReference(R.string.common_explore),
        iconRes = R.drawable.ic_web_24,
        onClick = exploreClick,
    )
    val pairButtons = shareClick
        ?.takeIf { txUrl.isNotEmpty() }
        ?.let { onShare ->
            exploreButton to NavigationButton(
                textReference = resourceReference(R.string.common_share),
                iconRes = R.drawable.ic_share_24,
                onClick = onShare,
            )
        }

    return NavigationUM.Content(
        source = SOURCE,
        title = TextReference.EMPTY,
        subtitle = null,
        backIconRes = R.drawable.ic_close_24,
        backIconClick = {},
        primaryButton = exploreButton,
        secondaryPairButtonsUM = pairButtons,
    )
}

private const val SOURCE = "SwapSuccess"