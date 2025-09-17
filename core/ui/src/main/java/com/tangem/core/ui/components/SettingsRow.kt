package com.tangem.core.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.test.BaseBottomSheetTestTags

/**
 * [Show in Figma](https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?type=design&node-id=281-248&mode=design&t=bXqehWPHyATKcZEW-4)
 **/
@Composable
fun SimpleSettingsRow(
    title: String,
    @DrawableRes icon: Int,
    onItemsClick: () -> Unit,
    modifier: Modifier = Modifier,
    rowColors: RowColors = getDefaultRowColors(),
    enabled: Boolean = true,
    subtitle: String? = null,
    redesign: Boolean = false,
) {
    Row(
        modifier = modifier
            .height(if (redesign) TangemTheme.dimens.size48 else TangemTheme.dimens.size56)
            .fillMaxWidth()
            .clickable(
                onClick = {
                    if (enabled) {
                        onItemsClick()
                    }
                },
            ).testTag(BaseBottomSheetTestTags.ACTION_TITLE),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            modifier = Modifier
                .padding(horizontal = if (redesign) TangemTheme.dimens.spacing12 else TangemTheme.dimens.spacing20),
            tint = rowColors.iconColor(enabled = enabled).value,
        )
        Column(
            modifier = Modifier
                .padding(
                    end = if (redesign) TangemTheme.dimens.spacing12 else TangemTheme.dimens.spacing20,
                ),
        ) {
            Text(
                text = title,
                style = TangemTheme.typography.subtitle1,
                color = rowColors.titleColor(enabled = enabled).value,
            )
            AnimatedVisibility(
                visible = !subtitle.isNullOrEmpty(),
            ) {
                Text(
                    text = subtitle ?: "",
                    style = TangemTheme.typography.body2,
                    color = rowColors.subtitleColor(enabled = enabled).value,
                )
            }
        }
    }
}

@Composable
fun getWarningRowColors(): RowColors = DefaultRowColors(
    titleColor = TangemTheme.colors.text.warning,
    iconColor = TangemTheme.colors.text.warning,
    disabledTitleColor = TangemTheme.colors.text.secondary,
    disabledIconColor = TangemTheme.colors.text.secondary,
)

@Composable
fun getDefaultRowColors(): RowColors = DefaultRowColors(
    titleColor = TangemTheme.colors.text.primary1,
    iconColor = TangemTheme.colors.text.primary1,
    disabledTitleColor = TangemTheme.colors.text.secondary,
    disabledIconColor = TangemTheme.colors.text.secondary,
)

@Immutable
private class DefaultRowColors(
    private val titleColor: Color,
    private val iconColor: Color,
    private val disabledTitleColor: Color,
    private val disabledIconColor: Color,
) : RowColors {
    @Composable
    override fun titleColor(enabled: Boolean): State<Color> {
        return animateColorAsState(
            targetValue = if (enabled) {
                titleColor
            } else {
                disabledTitleColor
            },
        )
    }

    @Composable
    override fun subtitleColor(enabled: Boolean): State<Color> {
        return rememberUpdatedState(newValue = TangemTheme.colors.text.secondary)
    }

    @Composable
    override fun iconColor(enabled: Boolean): State<Color> {
        return animateColorAsState(
            targetValue = if (enabled) {
                iconColor
            } else {
                disabledIconColor
            },
        )
    }
}

@Stable
interface RowColors {
    @Suppress("TopLevelComposableFunctions")
    @Composable
    fun titleColor(enabled: Boolean): State<Color>

    @Suppress("TopLevelComposableFunctions")
    @Composable
    fun subtitleColor(enabled: Boolean): State<Color>

    @Suppress("TopLevelComposableFunctions")
    @Composable
    fun iconColor(enabled: Boolean): State<Color>
}