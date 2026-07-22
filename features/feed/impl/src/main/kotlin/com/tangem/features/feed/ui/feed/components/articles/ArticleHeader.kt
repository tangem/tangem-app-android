package com.tangem.features.feed.ui.feed.components.articles

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.label.entity.LabelLeadingContentUM
import com.tangem.core.ui.components.label.entity.LabelUM
import com.tangem.core.ui.ds.badge.*
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.utils.StringsSigns
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ArticleHeader(
    isTrending: Boolean,
    title: String,
    createdAt: String,
    score: Float,
    tags: ImmutableList<LabelUM>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        ArticleHeaderMetaRow(
            isTrending = isTrending,
            score = score,
            createdAt = createdAt,
        )
        ArticleHeaderTitle(title = title)
        ArticleHeaderTags(tags = tags)
    }
}

@Composable
private fun ArticleHeaderMetaRow(isTrending: Boolean, score: Float, createdAt: String) {
    val starTint = if (isTrending) {
        TangemTheme.colors3.icon.accent.yellow
    } else {
        TangemTheme.colors3.icon.secondary
    }
    val scoreColor = if (isTrending) {
        TangemTheme.colors3.text.status.warning
    } else {
        TangemTheme.colors3.text.secondary
    }

    Row(
        modifier = Modifier.padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            imageVector = ImageVector.vectorResource(R.drawable.ic_wrapped_circle_star_16),
            tint = starTint,
            contentDescription = null,
        )
        Text(
            text = score.toString(),
            style = TangemTheme.typography3.body.medium,
            color = scoreColor,
        )
        Text(
            text = StringsSigns.DOT,
            color = TangemTheme.colors3.text.secondary,
            style = TangemTheme.typography3.body.medium,
        )
        Icon(
            modifier = Modifier.size(20.dp),
            imageVector = ImageVector.vectorResource(R.drawable.ic_calendar_20),
            tint = TangemTheme.colors3.icon.secondary,
            contentDescription = null,
        )
        Text(
            text = createdAt,
            color = TangemTheme.colors3.text.secondary,
            style = TangemTheme.typography3.body.medium,
        )
    }
}

@Composable
private fun ArticleHeaderTitle(title: String) {
    Text(
        modifier = Modifier
            .padding(
                top = 16.dp,
                bottom = 6.dp,
                start = 4.dp,
            ),
        text = title,
        style = TangemTheme.typography3.heading.medium,
        color = TangemTheme.colors3.text.primary,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ArticleHeaderTags(tags: ImmutableList<LabelUM>) {
    if (tags.isEmpty()) return

    Spacer(modifier = Modifier.height(24.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tags.forEach { tag ->
            ArticleHeaderTagBadge(tag = tag)
        }
    }
}

@Composable
private fun ArticleHeaderTagBadge(tag: LabelUM) {
    TangemBadge(
        text = tag.text,
        tangemIconUM = labelLeadingIcon(tag.leadingContent),
        shape = TangemBadgeShape.Rounded,
        size = TangemBadgeSize.X9,
        type = TangemBadgeType.Tinted,
        color = TangemBadgeColor.Gray,
        iconPosition = labelLeadingBadgeIconPosition(tag.leadingContent),
    )
}

private fun labelLeadingIcon(content: LabelLeadingContentUM): TangemIconUM? = when (content) {
    LabelLeadingContentUM.None -> null
    is LabelLeadingContentUM.Token -> TangemIconUM.Url(
        url = content.iconUrl,
        fallbackRes = R.drawable.ic_alert_24,
    )
}

private fun labelLeadingBadgeIconPosition(content: LabelLeadingContentUM): TangemBadgeIconPosition = when (content) {
    LabelLeadingContentUM.None -> TangemBadgeIconPosition.None
    is LabelLeadingContentUM.Token -> TangemBadgeIconPosition.Start
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun ArticleHeaderPreview() {
    TangemThemePreviewRedesign {
        ArticleHeader(
            title = "Something going good!",
            createdAt = "1 hour ago",
            score = 5.5f,
            tags = persistentListOf(),
            isTrending = true,
        )
    }
}