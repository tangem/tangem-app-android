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
import com.tangem.core.ui.components.label.Label
import com.tangem.core.ui.components.label.entity.LabelLeadingContentUM
import com.tangem.core.ui.components.label.entity.LabelUM
import com.tangem.core.ui.ds.badge.*
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
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
    if (LocalRedesignEnabled.current) {
        ArticleHeaderV2(
            isTrending = isTrending,
            title = title,
            createdAt = createdAt,
            score = score,
            tags = tags,
            modifier = modifier,
        )
    } else {
        ArticleHeaderV1(
            title = title,
            createdAt = createdAt,
            score = score,
            tags = tags,
            modifier = modifier,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ArticleHeaderV1(
    title: String,
    createdAt: String,
    score: Float,
    tags: ImmutableList<LabelUM>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        ArticleInfo(
            score = score,
            createdAt = createdAt,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = title,
            style = TangemTheme.typography.h2,
            color = TangemTheme.colors.text.primary1,
        )

        if (tags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                tags.forEach { tag ->
                    Label(
                        state = tag,
                    )
                }
            }
        }
    }
}

@Composable
private fun ArticleHeaderV2(
    isTrending: Boolean,
    title: String,
    createdAt: String,
    score: Float,
    tags: ImmutableList<LabelUM>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        ArticleHeaderV2MetaRow(
            isTrending = isTrending,
            score = score,
            createdAt = createdAt,
        )
        ArticleHeaderV2Title(title = title)
        ArticleHeaderV2Tags(tags = tags)
    }
}

@Composable
private fun ArticleHeaderV2MetaRow(isTrending: Boolean, score: Float, createdAt: String) {
    val starTint = if (isTrending) {
        TangemTheme.colors2.fill.status.attention
    } else {
        TangemTheme.colors2.graphic.neutral.primary
    }
    val scoreColor = if (isTrending) {
        TangemTheme.colors2.text.status.attention
    } else {
        TangemTheme.colors2.text.neutral.tertiary
    }

    Row(
        modifier = Modifier.padding(horizontal = TangemTheme.dimens2.x1),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x2),
    ) {
        Icon(
            modifier = Modifier.size(TangemTheme.dimens2.x5),
            imageVector = ImageVector.vectorResource(R.drawable.ic_wrapped_circle_star_16),
            tint = starTint,
            contentDescription = null,
        )
        Text(
            text = score.toString(),
            style = TangemTheme.typography2.bodyMedium16,
            color = scoreColor,
        )
        Text(
            text = StringsSigns.DOT,
            color = TangemTheme.colors2.text.neutral.tertiary,
            style = TangemTheme.typography2.bodyMedium16,
        )
        Icon(
            modifier = Modifier.size(20.dp),
            imageVector = ImageVector.vectorResource(R.drawable.ic_calendar_20),
            tint = TangemTheme.colors2.graphic.neutral.secondary,
            contentDescription = null,
        )
        Text(
            text = createdAt,
            color = TangemTheme.colors2.text.neutral.tertiary,
            style = TangemTheme.typography2.bodyMedium16,
        )
    }
}

@Composable
private fun ArticleHeaderV2Title(title: String) {
    Text(
        modifier = Modifier
            .padding(
                top = TangemTheme.dimens2.x4,
                bottom = TangemTheme.dimens2.x1_5,
                start = TangemTheme.dimens2.x1,
            ),
        text = title,
        style = TangemTheme.typography2.headingSemibold28,
        color = TangemTheme.colors2.text.neutral.primary,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ArticleHeaderV2Tags(tags: ImmutableList<LabelUM>) {
    if (tags.isEmpty()) return

    Spacer(modifier = Modifier.height(TangemTheme.dimens2.x6))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x1),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x1),
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
private fun ArticleHeaderPreviewV1() {
    TangemThemePreview {
        ArticleHeader(
            title = "Something going good!",
            createdAt = "1 hour ago",
            score = 5.5f,
            tags = persistentListOf(),
            isTrending = true,
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun ArticleHeaderPreviewV2() {
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