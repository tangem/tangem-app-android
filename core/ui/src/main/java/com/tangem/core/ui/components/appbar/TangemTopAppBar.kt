package com.tangem.core.ui.components.appbar

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.core.ui.R
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

/**
 * [TangemTopAppBar] height options.
 * */
enum class TangemTopAppBarHeight {
    BOTTOM_SHEET, DEFAULT,
}

/**
 * Top app bar with two optional buttons: [startButton] and [endButton].
 *
 * @see <a href = "https://www.figma.com/design/14ISV23YB1yVW1uNVwqrKv/Android?node-id=122-47&m=dev">
 * Figma Component</a>
 */
@Composable
fun TangemTopAppBar(
    modifier: Modifier = Modifier,
    startButton: TopAppBarButtonUM? = null,
    endButton: TopAppBarButtonUM? = null,
    textColor: Color = TangemTheme.colors.text.primary1,
    iconTint: Color = TangemTheme.colors.icon.primary1,
    containerColor: Color = Color.Transparent,
    height: TangemTopAppBarHeight = TangemTopAppBarHeight.DEFAULT,
) {
    TangemTopAppBar(
        modifier = modifier,
        title = null,
        startButton = startButton,
        endButton = endButton,
        textColor = textColor,
        iconTint = iconTint,
        containerColor = containerColor,
        height = height,
    )
}

/**
 * Top app bar with [title], optional [subtitle] and two optional buttons: [startButton] and [endButton].
 *
 * Where [title] and [subtitle] are [TextReference].
 *
 * @see <a href = "https://www.figma.com/design/14ISV23YB1yVW1uNVwqrKv/Android?node-id=122-47&m=dev">
 * Figma Component</a>
 */
@Composable
fun TangemTopAppBar(
    title: TextReference,
    modifier: Modifier = Modifier,
    subtitle: TextReference? = null,
    startButton: TopAppBarButtonUM? = null,
    endButton: TopAppBarButtonUM? = null,
    textColor: Color = TangemTheme.colors.text.primary1,
    iconTint: Color = TangemTheme.colors.icon.primary1,
    titleAlignment: Alignment.Horizontal = Alignment.Start,
    containerColor: Color = Color.Transparent,
    height: TangemTopAppBarHeight = TangemTopAppBarHeight.DEFAULT,
) {
    TangemTopAppBar(
        modifier = modifier,
        title = title.resolveReference(),
        subtitle = subtitle?.resolveReference(),
        startButton = startButton,
        endButton = endButton,
        textColor = textColor,
        iconTint = iconTint,
        titleAlignment = titleAlignment,
        containerColor = containerColor,
        height = height,
    )
}

/**
 * Top app bar with [title], optional [subtitle] and two optional buttons: [startButton] and [endButton].
 *
 * Where [title] and [subtitle] are [String].
 *
 * @see <a href = "https://www.figma.com/design/14ISV23YB1yVW1uNVwqrKv/Android?node-id=122-47&m=dev">
 * Figma Component</a>
 */
@Composable
fun TangemTopAppBar(
    title: String?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    startButton: TopAppBarButtonUM? = null,
    endButton: TopAppBarButtonUM? = null,
    textColor: Color = TangemTheme.colors.text.primary1,
    iconTint: Color = TangemTheme.colors.icon.primary1,
    titleAlignment: Alignment.Horizontal = Alignment.Start,
    containerColor: Color = Color.Transparent,
    height: TangemTopAppBarHeight = TangemTopAppBarHeight.DEFAULT,
) {
    TangemTopAppBar(
        title = title,
        modifier = modifier,
        subtitle = subtitle,
        startButton = startButton,
        textColor = textColor,
        iconTint = iconTint,
        titleAlignment = titleAlignment,
        containerColor = containerColor,
        height = height,
        endContent = {
            if (endButton != null) {
                TopAppBarButton(
                    button = endButton,
                    tint = iconTint,
                )
            }
        },
    )
}

/**
 * Top app bar with [title], optional [subtitle], optional [startButton] and [endContent].
 *
 * Where [title] and [subtitle] are [String] and [endContent] is a lambda that provides a [RowScope] to build
 * the end content.
 *
 * @see <a href = "https://www.figma.com/design/14ISV23YB1yVW1uNVwqrKv/Android?node-id=122-47&m=dev">
 * Figma Component</a>
 */
@Composable
fun TangemTopAppBar(
    title: String?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    startButton: TopAppBarButtonUM? = null,
    textColor: Color = TangemTheme.colors.text.primary1,
    iconTint: Color = TangemTheme.colors.icon.primary1,
    titleAlignment: Alignment.Horizontal = Alignment.Start,
    containerColor: Color = Color.Transparent,
    height: TangemTopAppBarHeight = TangemTopAppBarHeight.DEFAULT,
    endContent: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .background(color = containerColor)
            .fillMaxWidth()
            .heightIn(min = height.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = TangemTheme.dimens.spacing12)
                .size(TangemTheme.dimens.size32),
            contentAlignment = Alignment.Center,
        ) {
            if (startButton != null) {
                TopAppBarButton(
                    button = startButton,
                    tint = iconTint,
                )
            }
        }

        TopAppBarTitle(
            modifier = Modifier.weight(1f),
            title = title,
            subtitle = subtitle,
            textColor = textColor,
            titleAlignment = titleAlignment,
        )

        Row(
            modifier = Modifier
                .padding(horizontal = TangemTheme.dimens.spacing12)
                .widthIn(min = TangemTheme.dimens.size32)
                .height(TangemTheme.dimens.size32),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12, Alignment.End),
            content = endContent,
        )
    }
}

@Composable
private fun TopAppBarTitle(
    title: String?,
    subtitle: String?,
    textColor: Color,
    titleAlignment: Alignment.Horizontal,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        AnimatedVisibility(
            modifier = Modifier.fillMaxWidth(),
            visible = !title.isNullOrBlank(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = titleAlignment,
            ) {
                Text(
                    text = title.orEmpty(),
                    style = TangemTheme.typography.subtitle1,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                AnimatedVisibility(
                    visible = !subtitle.isNullOrBlank(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                    label = "Toolbar subtitle visibility",
                ) {
                    Text(
                        text = subtitle.orEmpty(),
                        style = TangemTheme.typography.caption2,
                        color = TangemTheme.colors.text.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private val TangemTopAppBarHeight.dp
    @Composable
    @ReadOnlyComposable
    get() = when (this) {
        TangemTopAppBarHeight.BOTTOM_SHEET -> TangemTheme.dimens.size44
        TangemTopAppBarHeight.DEFAULT -> TangemTheme.dimens.size56
    }

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Preview_BasicTopAppBar(
    @PreviewParameter(BasicTopAppBarPMPreviewProvider::class) params: BasicTopAppBarPM,
) {
    TangemThemePreview {
        TangemTopAppBar(
            title = params.title,
            subtitle = params.subtitle,
            startButton = params.startButton,
            endButton = params.endButton,
            titleAlignment = params.titleAlignment,
            containerColor = TangemTheme.colors.background.secondary,
            height = params.height,
        )
    }
}

private data class BasicTopAppBarPM(
    val title: String? = "Tangem",
    val subtitle: String? = null,
    val startButton: TopAppBarButtonUM? = null,
    val endButton: TopAppBarButtonUM? = null,
    val titleAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    val height: TangemTopAppBarHeight = TangemTopAppBarHeight.DEFAULT,
)

private class BasicTopAppBarPMPreviewProvider : PreviewParameterProvider<BasicTopAppBarPM> {
    override val values: Sequence<BasicTopAppBarPM>
        get() = sequenceOf(
            BasicTopAppBarPM(
                subtitle = "Subtitle",
                titleAlignment = Alignment.Start,
                startButton = TopAppBarButtonUM.Back { },
            ),
            BasicTopAppBarPM(
                title = null,
                startButton = TopAppBarButtonUM.Back { },
            ),
            BasicTopAppBarPM(
                height = TangemTopAppBarHeight.BOTTOM_SHEET,
            ),
            BasicTopAppBarPM(
                startButton = TopAppBarButtonUM(
                    iconRes = R.drawable.ic_scan_24,
                    onIconClicked = {},
                ),
                endButton = TopAppBarButtonUM(
                    iconRes = R.drawable.ic_more_vertical_24,
                    onIconClicked = {},
                ),
            ),
            BasicTopAppBarPM(
                startButton = TopAppBarButtonUM(
                    iconRes = R.drawable.ic_scan_24,
                    onIconClicked = {},
                ),
            ),
            BasicTopAppBarPM(
                endButton = TopAppBarButtonUM(
                    iconRes = R.drawable.ic_more_vertical_24,
                    onIconClicked = {},
                ),
                height = TangemTopAppBarHeight.BOTTOM_SHEET,
            ),
            BasicTopAppBarPM(
                title = "1234567891011121314151617181920",
                subtitle = "12345678910111213141516171819202122232425",
                titleAlignment = Alignment.Start,
                startButton = TopAppBarButtonUM(
                    iconRes = R.drawable.ic_scan_24,
                    onIconClicked = {},
                ),
                endButton = TopAppBarButtonUM(
                    iconRes = R.drawable.ic_more_vertical_24,
                    onIconClicked = {},
                ),
            ),
        )
}
// endregion Preview