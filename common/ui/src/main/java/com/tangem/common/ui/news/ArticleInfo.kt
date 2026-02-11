package com.tangem.common.ui.news

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.res.TangemTheme

@Composable
internal fun ArticleInfo(score: Float, createdAt: String, modifier: Modifier = Modifier) {
    val dotColor = TangemTheme.colors.text.secondary
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            imageVector = ImageVector.vectorResource(R.drawable.ic_start_circle_12),
            contentDescription = null,
        )

        Text(
            text = score.toString(),
            style = TangemTheme.typography.subtitle2,
            color = TangemTheme.colors.text.secondary,
        )

        Spacer(
            modifier = Modifier
                .size(4.dp)
                .drawWithCache {
                    val radius = size.minDimension / 2f
                    onDrawBehind {
                        drawCircle(
                            color = dotColor,
                            radius = radius,
                        )
                    }
                },
        )

        Text(
            text = createdAt,
            style = TangemTheme.typography.subtitle2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = TangemTheme.colors.text.secondary,
        )
    }
}