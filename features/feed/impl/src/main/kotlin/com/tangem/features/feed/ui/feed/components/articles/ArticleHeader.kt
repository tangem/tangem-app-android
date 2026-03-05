package com.tangem.features.feed.ui.feed.components.articles

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerW
import com.tangem.core.ui.components.label.Label
import com.tangem.core.ui.components.label.entity.LabelLeadingContentUM
import com.tangem.core.ui.components.label.entity.LabelUM
import com.tangem.core.ui.ds.badge.TangemBadge
import com.tangem.core.ui.ds.badge.TangemBadgeColor
import com.tangem.core.ui.ds.badge.TangemBadgeIconPosition
import com.tangem.core.ui.ds.badge.TangemBadgeShape
import com.tangem.core.ui.ds.badge.TangemBadgeSize
import com.tangem.core.ui.ds.badge.TangemBadgeType
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemThemePreviewRedesign
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

@OptIn(ExperimentalLayoutApi::class)
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
        Row(
            modifier = Modifier
                .heightIn(min = 66.dp)
                .padding(top = 16.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            DateBlock(
                modifier = Modifier.weight(1f),
                createdAt = createdAt,
            )
            SpacerW(30.dp)
            VerticalDivider(
                modifier = Modifier
                    .height(46.dp)
                    .padding(bottom = 4.dp),
                color = TangemTheme.colors2.border.neutral.primary,
            )
            SpacerW(30.dp)
            ScoreBlock(
                modifier = Modifier.weight(1f),
                score = score,
                isTrending = isTrending,
            )
        }

        Text(
            modifier = Modifier.padding(vertical = 36.dp),
            text = title,
            style = TangemTheme.typography2.headingBold34,
            color = TangemTheme.colors2.text.neutral.primary,
        )

        if (tags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                tags.forEach { tag ->
                    TangemBadge(
                        text = tag.text,
                        tangemIconUM = when (val content = tag.leadingContent) {
                            LabelLeadingContentUM.None -> null
                            is LabelLeadingContentUM.Token -> TangemIconUM.Url(content.iconUrl)
                        },
                        shape = TangemBadgeShape.Rounded,
                        size = TangemBadgeSize.X9,
                        type = TangemBadgeType.Tinted,
                        color = TangemBadgeColor.Gray,
                        iconPosition = when (tag.leadingContent) {
                            LabelLeadingContentUM.None -> TangemBadgeIconPosition.None
                            is LabelLeadingContentUM.Token -> TangemBadgeIconPosition.Start
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ScoreBlock(score: Float, isTrending: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                imageVector = ImageVector.vectorResource(R.drawable.ic_wrapped_circle_star_16),
                tint = if (isTrending) {
                    TangemTheme.colors2.fill.status.attention
                } else {
                    TangemTheme.colors2.graphic.neutral.primary
                },
                contentDescription = null,
            )
            Text(
                text = score.toString(),
                style = TangemTheme.typography2.bodyRegular16,
                color = if (isTrending) {
                    TangemTheme.colors2.text.status.attention
                } else {
                    TangemTheme.colors2.text.neutral.primary
                },
            )
        }
        Text(
            text = stringResourceSafe(R.string.news_trending_score),
            style = TangemTheme.typography2.captionSemibold13,
            color = TangemTheme.colors2.text.neutral.tertiary,
        )
    }
}

@Composable
private fun DateBlock(createdAt: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            imageVector = ImageVector.vectorResource(R.drawable.ic_calendar_20),
            tint = TangemTheme.colors2.fill.neutral.primary,
            contentDescription = null,
        )
        Text(
            text = createdAt,
            style = TangemTheme.typography2.captionSemibold13,
            color = TangemTheme.colors2.text.neutral.tertiary,
        )
    }
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