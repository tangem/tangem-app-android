package com.tangem.core.ui.components.buttons.segmentedbutton

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

/**
 * Segmented buttons
 *
 * [Figma component](https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?type=design&node-id=1961-2004&mode=design&t=OFPQ18YhLHVAANab-4)
 *
 * @param config list of buttons in SegmentedButtons
 * @param onClick button click
 * @param modifier component modifier
 * @param color default button color
 * @param selectedColor selected button color
 * @param dividerColor border and divider color
 * @param showIndication show ripple indication
 * @param buttonContent content as separate button
 */
@Composable
inline fun <reified T> SegmentedButtons(
    config: ImmutableList<T>,
    crossinline onClick: (T) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = TangemTheme.colors.background.tertiary,
    selectedColor: Color = TangemTheme.colors.background.action,
    dividerColor: Color = TangemTheme.colors.stroke.primary,
    showIndication: Boolean = true,
    initialSelectedItem: T? = null,
    isEnabled: Boolean = true,
    crossinline buttonContent: @Composable BoxScope.(T) -> Unit,
) {
    if (config.isEmpty() || config.size == 1) return

    var selectedIndex by remember {
        val index = if (initialSelectedItem == null) 0 else config.indexOf(initialSelectedItem)
        mutableIntStateOf(index)
    }
    val shape = RoundedCornerShape(TangemTheme.dimens.radius26)

    Row(
        modifier = modifier
            .clip(shape)
            .background(dividerColor)
            .border(BorderStroke(1.dp, dividerColor), shape = shape)
            .height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing1),
    ) {
        repeat(config.size) { index ->
            val leftRadius = if (index == 0) TangemTheme.dimens.radius26 else TangemTheme.dimens.radius0
            val rightRadius = if (index == config.lastIndex) TangemTheme.dimens.radius26 else TangemTheme.dimens.radius0

            val animateColor by animateColorAsState(
                targetValue = if (index == selectedIndex) selectedColor else color,
                label = "Segmented Button Selected Color Animation",
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = animateColor,
                        shape = RoundedCornerShape(
                            topStart = leftRadius,
                            topEnd = rightRadius,
                            bottomEnd = rightRadius,
                            bottomStart = leftRadius,
                        ),
                    )
                    .clickable(
                        enabled = isEnabled,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = if (showIndication) LocalIndication.current else null,
                    ) {
                        selectedIndex = index
                        onClick(config[index])
                    },
            ) {
                buttonContent.invoke(this, config[index])
            }
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SegmentedButtonsPreview(
    @PreviewParameter(SegmentedButtonsPreviewProvider::class) config: PersistentList<SegmentedButtonsConfigPreview>,
) {
    TangemThemePreview {
        SegmentedButtons(
            config = config,
            onClick = {},
        ) {
            Text(
                text = it.text,
                modifier = Modifier.padding(TangemTheme.dimens.spacing16),
            )
        }
    }
}

//region Preview config
/**
 * Segmented buttons preview model
 *
 * @param text button title
 */
internal data class SegmentedButtonsConfigPreview(
    val text: String,
)

/**
 * Segmented button preview provider
 */
internal class SegmentedButtonsPreviewProvider :
    CollectionPreviewParameterProvider<PersistentList<SegmentedButtonsConfigPreview>>(
        collection = listOf(
            persistentListOf(
                SegmentedButtonsConfigPreview(
                    text = "Title 1",
                ),
                SegmentedButtonsConfigPreview(
                    text = "Title 2",
                ),
                SegmentedButtonsConfigPreview(
                    text = "Title 3",
                ),
            ),
            persistentListOf(
                SegmentedButtonsConfigPreview(
                    text = "Title 1",
                ),
                SegmentedButtonsConfigPreview(
                    text = "Title 2",
                ),
            ),
        ),
    )

//endregion