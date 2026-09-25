package com.tangem.features.tangempay.withdraw.hub.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_heart_20
import com.tangem.core.ui.utils.dashedBorder
import com.tangem.features.tangempay.details.impl.R

internal val CardWidth = 156.dp
internal val RecentsCardHeight = 136.dp
internal val ActionCardHeight = 64.dp
internal val RecentsCardCornerRadius = 24.dp
internal val ActionCardCornerRadius = 20.dp
internal val IconCircleSize = 32.dp

private val DashLength = 6.dp
private val DashGap = 4.dp

internal const val SHIMMER_CARDS_COUNT = 3

internal fun LazyListScope.recentsShimmerItems(keyPrefix: String) {
    items(count = SHIMMER_CARDS_COUNT, key = { index -> "${keyPrefix}_$index" }) {
        TangemShimmer(
            modifier = Modifier.size(width = CardWidth, height = RecentsCardHeight),
            radius = RecentsCardCornerRadius,
        )
    }
}

@Composable
internal fun RecentsCard(
    isDashed: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(RecentsCardCornerRadius)
    val surface = if (isDashed) {
        Modifier
            .dashedBorder(
                color = TangemTheme.colors3.border.secondary,
                shape = shape,
                dashLength = DashLength,
                gapLength = DashGap,
            )
            .clip(shape)
    } else {
        Modifier
            .clip(shape)
            .background(TangemTheme.colors3.bg.secondary)
    }
    val click = if (onClick != null) Modifier.clickableSingle(role = Role.Button, onClick = onClick) else Modifier
    Column(
        modifier = Modifier
            .size(width = CardWidth, height = RecentsCardHeight)
            .then(surface)
            .then(click)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        content = content,
    )
}

@Composable
internal fun RecentsEmptyCard() {
    RecentsCard(isDashed = true) {
        SecondaryIconCircle(imageVector = Icons.ic_heart_20)
        Text(
            text = resourceReference(R.string.tangempay_withdraw_hub_recents_empty).resolveReference(),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun SecondaryIconCircle(imageVector: ImageVector) {
    IconCircle(background = TangemTheme.colors3.bg.opaque.primary) {
        TangemIcon(
            modifier = Modifier.size(20.dp),
            tangemIconUM = TangemIconUM.Icon(
                imageVector = imageVector,
                tintReference = { TangemTheme.colors3.icon.primary },
            ),
        )
    }
}

@Composable
internal fun IconCircle(background: Color, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(IconCircleSize)
            .background(color = background, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}