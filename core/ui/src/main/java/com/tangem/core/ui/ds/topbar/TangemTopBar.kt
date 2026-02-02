package com.tangem.core.ui.ds.topbar

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
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
 * @param startIconRes          Optional drawable resource ID for the start icon.
 * @param onStartContentClick   Optional click action for the start icon.
 * @param endIconRes            Optional drawable resource ID for the end icon.
 * @param onEndContentClick     Optional click action for the end icon.
 * @param isGhostButtons        Flag to determine if ghost button styling should be applied.
 *
[REDACTED_AUTHOR]
 */
@Composable
fun TangemTopBar(
    modifier: Modifier = Modifier,
    title: TextReference? = null,
    subtitle: TextReference? = null,
    @DrawableRes startIconRes: Int? = null,
    onStartContentClick: (() -> Unit)? = null,
    @DrawableRes endIconRes: Int? = null,
    onEndContentClick: (() -> Unit)? = null,
    @DrawableRes titleIconRes: Int? = null,
    titleStyle: TextStyle = TangemTheme.typography2.headingSemibold17,
    isGhostButtons: Boolean = false,
) {
    TangemTopBarInner(
        modifier = modifier,
        content = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x0_5),
            ) {
                TangemTopBarTitle(title = title, titleIconRes = titleIconRes, titleStyle = titleStyle)
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
        },
        startContent = if (startIconRes != null) {
            { TangemTopBarIcon(iconRes = startIconRes) }
        } else {
            null
        },
        onStartContentClick = onStartContentClick,
        endContent = if (endIconRes != null) {
            { TangemTopBarIcon(iconRes = endIconRes) }
        } else {
            null
        },
        onEndContentClick = onEndContentClick,
        isGhostButtons = isGhostButtons,
    )
}

@Composable
private fun TangemTopBarTitle(title: TextReference?, @DrawableRes titleIconRes: Int?, titleStyle: TextStyle) {
    AnimatedVisibility(
        visible = title != null,
        label = "Title Visibility",
    ) {
        val wrappedTitle = remember(this) { requireNotNull(title) }

        Row(
            horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x1),
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
                style = titleStyle,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun TangemTopBarIcon(@DrawableRes iconRes: Int) {
    Icon(
        imageVector = ImageVector.vectorResource(id = iconRes),
        contentDescription = null,
        tint = TangemTheme.colors2.graphic.neutral.primary,
        modifier = Modifier.fillMaxSize(),
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
            startIconRes = params.startIconRes,
            endIconRes = params.endIconRes,
            titleIconRes = params.titleIconRes,
            isGhostButtons = params.isGhostButtons,
            onStartContentClick = {},
            onEndContentClick = {},
            modifier = Modifier.background(TangemTheme.colors2.surface.level1),
        )
    }
}

private class TangemTopBarPreviewData(
    val title: TextReference? = null,
    val subtitle: TextReference? = null,
    val isGhostButtons: Boolean = false,
    val titleIconRes: Int? = null,
    val startIconRes: Int? = null,
    val endIconRes: Int? = null,
)

private class PreviewProvider : PreviewParameterProvider<TangemTopBarPreviewData> {
    override val values: Sequence<TangemTopBarPreviewData>
        get() = sequenceOf(
            TangemTopBarPreviewData(
                title = stringReference("Title"),
                startIconRes = R.drawable.ic_tangem_24,
                endIconRes = R.drawable.ic_more_vertical_24,
                isGhostButtons = true,
            ),
            TangemTopBarPreviewData(
                title = stringReference("Title"),
                subtitle = stringReference("Subtitle"),
                startIconRes = R.drawable.ic_tangem_24,
                endIconRes = R.drawable.ic_more_vertical_24,
                isGhostButtons = true,
            ),
            TangemTopBarPreviewData(
                title = stringReference("Title"),
                subtitle = stringReference("Subtitle"),
                titleIconRes = R.drawable.ic_tangem_24,
                startIconRes = R.drawable.ic_tangem_24,
                endIconRes = R.drawable.ic_more_vertical_24,
                isGhostButtons = true,
            ),
            TangemTopBarPreviewData(
                subtitle = stringReference("Subtitle"),
                titleIconRes = R.drawable.ic_tangem_24,
                startIconRes = R.drawable.ic_tangem_24,
                endIconRes = R.drawable.ic_more_vertical_24,
                isGhostButtons = true,
            ),
            TangemTopBarPreviewData(
                title = stringReference("Title"),
                endIconRes = R.drawable.ic_more_vertical_24,
                isGhostButtons = true,
            ),
            TangemTopBarPreviewData(
                title = stringReference("Title"),
                startIconRes = R.drawable.ic_tangem_24,
                isGhostButtons = true,
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
                startIconRes = R.drawable.ic_tangem_24,
                endIconRes = R.drawable.ic_more_vertical_24,
            ),
        )
}
// endregion