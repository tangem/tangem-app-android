package com.tangem.tap.features.tokens.impl.presentation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.tangem.core.ui.res.TangemTheme
import com.tangem.tap.features.tokens.impl.presentation.states.NetworkItemState
import kotlinx.collections.immutable.ImmutableCollection

/** This const configures how many items show to user and hide more than*/
private const val MAX_VISIBLE_BRIEF_ICONS = 9

/**
 * @author Andrew Khokhlov on 27/03/2023
 */
@Composable
internal fun BriefNetworksList(
    isCollapsed: Boolean,
    networks: ImmutableCollection<NetworkItemState>,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = isCollapsed,
        modifier = modifier,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing4)) {
            val iterator = networks.iterator()
            var index = 0
            while (iterator.hasNext()) {
                val network = iterator.next()
                if (index < MAX_VISIBLE_BRIEF_ICONS) {
                    key(network.name + network.protocolName) {
                        BriefNetworkItem(model = network)
                    }
                } else {
                    if (networks.size < MAX_VISIBLE_BRIEF_ICONS + 1) {
                        key(network.name + network.protocolName) {
                            BriefNetworkItem(model = network)
                        }
                    } else {
                        HasMoreItem(moreCount = networks.size - index)
                        break
                    }
                }
                index++
            }
        }
    }
}

/**
 * @author Andrew Khokhlov on 27/03/2023
 */
@Composable
internal fun BriefNetworkItem(model: NetworkItemState, modifier: Modifier = Modifier) {
    val isAdded = model is NetworkItemState.ManageContent && model.isAdded.value
    Box(modifier = modifier.size(size = TangemTheme.dimens.size20)) {
        if (!isAdded) {
            Box(
                modifier = Modifier
                    .size(TangemTheme.dimens.size20)
                    .clip(CircleShape)
                    .background(TangemTheme.colors.control.unchecked),
            )
        }
        Icon(
            painter = painterResource(id = model.iconResId.value),
            contentDescription = null,
            modifier = Modifier
                .size(size = TangemTheme.dimens.size20)
                .clip(CircleShape),
            tint = if (isAdded) Color.Unspecified else TangemTheme.colors.text.tertiary,
        )

        if (model.isMainNetwork) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(TangemTheme.dimens.size7)
                    .clip(CircleShape)
                    .background(TangemTheme.colors.background.primary),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(TangemTheme.dimens.size5)
                        .clip(CircleShape)
                        .background(TangemTheme.colors.icon.accent),
                )
            }
        }
    }
}

@Composable
internal fun HasMoreItem(moreCount: Int) {
    Box(
        modifier = Modifier
            .size(size = TangemTheme.dimens.size20)
            .clip(CircleShape)
            .background(TangemTheme.colors.control.unchecked),
    ) {
        Text(
            modifier = Modifier.align(Alignment.Center),
            text = "+$moreCount",
            style = TangemTheme.typography.overline,
            color = TangemTheme.colors.text.tertiary,
        )
    }
}
