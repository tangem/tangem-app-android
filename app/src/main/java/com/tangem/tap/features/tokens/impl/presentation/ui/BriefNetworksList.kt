package com.tangem.tap.features.tokens.impl.presentation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
import com.tangem.tap.features.tokens.impl.presentation.states.NetworkItemState
import kotlinx.collections.immutable.ImmutableCollection

/**
[REDACTED_AUTHOR]
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
            networks.forEach { network ->
                key(network.name + network.protocolName) {
                    BriefNetworkItem(model = network)
                }
            }
        }
    }
}

/**
[REDACTED_AUTHOR]
 */
@Composable
internal fun BriefNetworkItem(model: NetworkItemState) {
    Box(modifier = Modifier.size(size = TangemTheme.dimens.size20)) {
        Image(
            painter = painterResource(id = model.iconResId.value),
            contentDescription = null,
            modifier = Modifier.size(size = TangemTheme.dimens.size20),
        )

        if (model.isMainNetwork) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(TangemTheme.dimens.size7)
                    .clip(CircleShape)
                    .background(TangemColorPalette.White),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(TangemTheme.dimens.size5)
                        .clip(CircleShape)
                        .background(TangemColorPalette.Meadow),
                )
            }
        }
    }
}