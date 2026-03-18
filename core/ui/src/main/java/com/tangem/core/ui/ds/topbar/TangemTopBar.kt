package com.tangem.core.ui.ds.topbar

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

/**
 * A top bar composable that displays a title and optional start and end icons.
 * [Figma](https://www.figma.com/design/RU7AIgwHtGdMfy83T5UOoR/Core-Library?node-id=8435-74860&m=dev)
 *
 * @param title                 The title text to be displayed in the center of the top bar.
 * @param modifier              Modifier to be applied to the top bar.
 * @param subtitle              Optional subtitle text to be displayed below the title.
 * @param startActionUM         Optional action data for the start action icon.
 * @param endActionUM           Optional action data for the end action icon.
 * @param titleIconRes          Optional drawable resource ID for the icon to be displayed next to the title.
 *
[REDACTED_AUTHOR]
 */
@Composable
fun TangemTopBar(
    modifier: Modifier = Modifier,
    title: TextReference? = null,
    subtitle: TextReference? = null,
    startActionUM: TangemTopBarActionUM? = null,
    endActionUM: TangemTopBarActionUM? = null,
    @DrawableRes titleIconRes: Int? = null,
) {
    TangemTopBar(
        title = title,
        subtitle = subtitle,
        titleIconRes = titleIconRes,
        modifier = modifier,
        startContent = if (startActionUM != null) {
            { TangemTopBarActionContent(startActionUM) }
        } else {
            null
        },
        endContent = if (endActionUM != null) {
            { TangemTopBarActionContent(endActionUM) }
        } else {
            null
        },
    )
}

/**
 * A top bar composable that displays a title and optional start and end icons.
 * [Figma](https://www.figma.com/design/RU7AIgwHtGdMfy83T5UOoR/Core-Library?node-id=8435-74860&m=dev)
 *
 * @param title                 The title text to be displayed in the center of the top bar.
 * @param modifier              Modifier to be applied to the top bar.
 * @param subtitle              Optional subtitle text to be displayed below the title.
 *
[REDACTED_AUTHOR]
 */
@Composable
fun TangemTopBar(
    modifier: Modifier = Modifier,
    title: TextReference? = null,
    subtitle: TextReference? = null,
    @DrawableRes titleIconRes: Int? = null,
    startContent: @Composable (() -> Unit)? = null,
    endContent: @Composable (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(TangemTheme.dimens2.x16)
            .padding(TangemTheme.dimens2.x4, TangemTheme.dimens2.x3),
    ) {
        AnimatedVisibility(
            visible = startContent != null,
            modifier = Modifier.align(Alignment.CenterStart),
            label = "Start Content Visibility",
        ) {
            startContent?.invoke()
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x0_5),
        ) {
            TangemTopBarTitle(title = title, titleIconRes = titleIconRes)
            AnimatedVisibility(
                visible = subtitle != null,
                label = "Subtitle Visibility",
            ) {
                val wrappedSubtitle = remember(this) { requireNotNull(subtitle) }
                Text(
                    text = wrappedSubtitle.resolveAnnotatedReference(),
                    color = TangemTheme.colors2.text.neutral.secondary,
                    style = TangemTheme.typography2.bodyRegular15,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
        AnimatedVisibility(
            visible = endContent != null,
            modifier = Modifier.align(Alignment.CenterEnd),
            label = "End Content Visibility",
        ) {
            endContent?.invoke()
        }
    }
}

@Composable
private fun TangemTopBarTitle(title: TextReference?, @DrawableRes titleIconRes: Int?) {
    AnimatedVisibility(
        visible = title != null,
        label = "Title Visibility",
        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
    ) {
        val wrappedTitle = remember(this) { requireNotNull(title) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(
                space = TangemTheme.dimens2.x1,
                alignment = Alignment.CenterHorizontally,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedVisibility(
                visible = titleIconRes != null,
                label = "Title Icon Visibility",
            ) {
                val wrappedTitleIconRes = remember(this) {
                    requireNotNull(titleIconRes)
                }
                Icon(
                    imageVector = ImageVector.vectorResource(id = wrappedTitleIconRes),
                    contentDescription = null,
                    tint = TangemTheme.colors2.graphic.neutral.primary,
                    modifier = Modifier.size(TangemTheme.dimens2.x4),
                )
            }

            Text(
                text = wrappedTitle.resolveAnnotatedReference(),
                color = TangemTheme.colors2.text.neutral.primary,
                style = TangemTheme.typography2.headingSemibold17,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun TangemTopBarActionContent(
    actionUM: TangemTopBarActionUM,
    modifier: Modifier = Modifier,
    iconSize: Dp = TangemTheme.dimens2.x8,
) {
    val background = lerp(
        start = Color.Transparent,
        stop = TangemTheme.colors2.button.backgroundSecondary,
        fraction = actionUM.ghostModeProgress,
    )
    val padding = (TangemTheme.dimens2.x10 - iconSize) / 2
    Icon(
        imageVector = ImageVector.vectorResource(id = actionUM.iconRes),
        contentDescription = null,
        tint = TangemTheme.colors2.graphic.neutral.primary,
        modifier = modifier
            .size(TangemTheme.dimens2.x10)
            .clip(CircleShape)
            .conditional(actionUM.isActionable) { background(background) }
            .conditionalCompose(actionUM.isActionable && actionUM.onClick != null) {
                clickableSingle(onClick = requireNotNull(actionUM.onClick))
            }
            .padding(padding),
    )
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 375)
@Preview(showBackground = true, widthDp = 375, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TangemTopBar_Preview(@PreviewParameter(PreviewProvider::class) params: TangemTopBarPreviewData) {
    TangemThemePreviewRedesign {
        TangemTopBar(
            title = params.title,
            subtitle = params.subtitle,
            titleIconRes = params.titleIconRes,
            modifier = Modifier.background(TangemTheme.colors2.surface.level1),
            startContent = params.startActionUM?.let { { TangemTopBarActionContent(it) } },
            endContent = params.endActionUM?.let { { TangemTopBarActionContent(it) } },
        )
    }
}

private class TangemTopBarPreviewData(
    val title: TextReference? = null,
    val subtitle: TextReference? = null,
    val titleIconRes: Int? = null,
    val startActionUM: TangemTopBarActionUM? = null,
    val endActionUM: TangemTopBarActionUM? = null,
)

private class PreviewProvider : PreviewParameterProvider<TangemTopBarPreviewData> {
    override val values: Sequence<TangemTopBarPreviewData>
        get() = sequenceOf(
            TangemTopBarPreviewData(
                title = stringReference("Title"),
                startActionUM = TangemTopBarActionUM(
                    iconRes = R.drawable.ic_tangem_24,
                    onClick = {},
                    isActionable = false,
                ),
                endActionUM = TangemTopBarActionUM(
                    iconRes = R.drawable.ic_more_default_24,
                    onClick = {},
                    isActionable = false,
                ),
            ),
            TangemTopBarPreviewData(
                title = stringReference("Title"),
                subtitle = stringReference("Subtitle"),
                startActionUM = TangemTopBarActionUM(
                    iconRes = R.drawable.ic_tangem_24,
                    onClick = {},
                    isActionable = true,
                    ghostModeProgress = 1f,
                ),
                endActionUM = TangemTopBarActionUM(
                    iconRes = R.drawable.ic_more_default_24,
                    onClick = {},
                    isActionable = true,
                    ghostModeProgress = 1f,
                ),
            ),
            TangemTopBarPreviewData(
                title = stringReference("Title"),
                subtitle = stringReference("Subtitle"),
                titleIconRes = R.drawable.ic_tangem_24,
                startActionUM = TangemTopBarActionUM(
                    iconRes = R.drawable.ic_tangem_24,
                    onClick = {},
                    isActionable = false,
                ),
                endActionUM = TangemTopBarActionUM(
                    iconRes = R.drawable.ic_more_default_24,
                    onClick = {},
                    isActionable = true,
                    ghostModeProgress = 1f,
                ),
            ),
            TangemTopBarPreviewData(
                subtitle = stringReference("Subtitle"),
                titleIconRes = R.drawable.ic_tangem_24,
                startActionUM = TangemTopBarActionUM(
                    iconRes = R.drawable.ic_tangem_24,
                    onClick = {},
                    isActionable = true,
                ),
                endActionUM = TangemTopBarActionUM(
                    iconRes = R.drawable.ic_more_default_24,
                    onClick = {},
                    isActionable = false,
                ),
            ),
            TangemTopBarPreviewData(
                title = stringReference("Title"),
                endActionUM = TangemTopBarActionUM(
                    iconRes = R.drawable.ic_more_default_24,
                    onClick = {},
                    isActionable = false,
                ),
            ),
            TangemTopBarPreviewData(
                title = stringReference("Title"),
                startActionUM = TangemTopBarActionUM(
                    iconRes = R.drawable.ic_tangem_24,
                    onClick = {},
                    isActionable = true,
                    ghostModeProgress = 1f,
                ),
            ),
            TangemTopBarPreviewData(
                title = combinedReference(
                    stringReference("$ 46,112"),
                    styledStringReference(
                        value = ".30",
                        spanStyleReference = {
                            TangemTheme.typography.caption1.copy(TangemTheme.colors2.text.neutral.secondary)
                                .toSpanStyle()
                        },
                    ),
                ),
            ),
            TangemTopBarPreviewData(
                title = combinedReference(
                    stringReference("$ 46,112"),
                    styledStringReference(
                        value = ".30",
                        spanStyleReference = {
                            TangemTheme.typography.caption1.copy(TangemTheme.colors2.text.neutral.secondary)
                                .toSpanStyle()
                        },
                    ),
                ),
                startActionUM = TangemTopBarActionUM(
                    iconRes = R.drawable.ic_tangem_24,
                    onClick = {},
                    isActionable = true,
                ),
                endActionUM = TangemTopBarActionUM(
                    iconRes = R.drawable.ic_more_default_24,
                    onClick = {},
                    isActionable = false,
                ),
            ),
        )
}
// endregion