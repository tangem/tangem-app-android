package com.tangem.features.tangempay.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.TangemPayCardDetailsUM

private const val REVEAL_ANIMATION_MILLIS = 500

@Composable
internal fun TangemPayCardDetailsBlock(state: TangemPayCardDetailsUM, modifier: Modifier = Modifier) {
    val alpha = if (state.isLoading) {
        val infiniteTransition = rememberInfiniteTransition()
        val animation by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 0.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(REVEAL_ANIMATION_MILLIS, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        )
        animation
    } else {
        1f
    }
    Column(
        modifier = modifier
            .background(
                color = TangemTheme.colors.background.primary,
                shape = TangemTheme.shapes.roundedCornersMedium,
            ),
    ) {
        CardDetailsBlockHeader(state)
        CardDetailsTextContainer(
            modifier = Modifier
                .graphicsLayer { this.alpha = alpha }
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 4.dp),
            text = state.number,
            onCopy = { state.onCopy(state.number) },
            isHidden = state.isHidden,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp, top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CardDetailsTextContainer(
                modifier = Modifier
                    .graphicsLayer { this.alpha = alpha }
                    .weight(1f),
                text = state.expiry,
                onCopy = { state.onCopy(state.expiry) },
                isHidden = state.isHidden,
            )
            CardDetailsTextContainer(
                modifier = Modifier
                    .graphicsLayer { this.alpha = alpha }
                    .weight(1f),
                text = state.cvv,
                onCopy = { state.onCopy(state.cvv) },
                isHidden = state.isHidden,
            )
        }
    }
}

@Composable
private fun CardDetailsBlockHeader(state: TangemPayCardDetailsUM, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            modifier = Modifier
                .padding(start = 12.dp, top = 12.dp),
            text = stringResourceSafe(R.string.tangempay_card_details_title),
            color = TangemTheme.colors.text.tertiary,
            style = TangemTheme.typography.subtitle2,
        )
        Text(
            modifier = Modifier
                .padding(top = 8.dp, end = 4.dp)
                .clip(TangemTheme.shapes.roundedCornersMedium)
                .clickable(onClick = state.onClick)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            text = state.buttonText.resolveReference(),
            color = TangemTheme.colors.text.accent,
            style = TangemTheme.typography.body2,
        )
    }
}

@Composable
private fun CardDetailsTextContainer(
    text: String,
    isHidden: Boolean,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .background(color = TangemTheme.colors.field.primary, shape = RoundedCornerShape(16.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 12.dp),
            text = text,
            maxLines = 1,
            color = if (isHidden) TangemTheme.colors.text.disabled else TangemTheme.colors.text.primary1,
            style = TangemTheme.typography.body2,
        )

        if (!isHidden) {
            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                modifier = Modifier
                    .padding(end = TangemTheme.dimens.spacing8)
                    .size(TangemTheme.dimens.size32),
                onClick = onCopy,
            ) {
                Icon(
                    modifier = Modifier.size(TangemTheme.dimens.size24),
                    painter = painterResource(id = R.drawable.ic_copy_new_24),
                    tint = TangemTheme.colors.icon.informative,
                    contentDescription = null,
                )
            }
        }
    }
}