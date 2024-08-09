package com.tangem.core.ui.components.inputrow

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.core.ui.R
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.inputrow.inner.DividerContainer
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

/**
 * [Input Row Image](https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?type=design&node-id=2100-813&mode=design&t=IQ5lBJEkFGU4WSvi-4)
 *
 * @param title title reference
 * @param subtitle subtitle reference
 * @param caption caption reference
 * @param tokenIconState token icon state [CurrencyIconState]
 * @param modifier modifier
 * @param titleColor title color
 * @param subtitleColor subtitle color
 * @param captionColor caption color
 * @param iconRes action icon
 * @param iconTint action icon tint
 * @param onIconClick click on action icon
 * @param showNetworkIcon show token network icon
 * @param showDivider show divider
 */
@Composable
fun InputRowImage(
    title: TextReference,
    subtitle: TextReference,
    caption: TextReference,
    tokenIconState: CurrencyIconState,
    modifier: Modifier = Modifier,
    titleColor: Color = TangemTheme.colors.text.secondary,
    subtitleColor: Color = TangemTheme.colors.text.primary1,
    captionColor: Color = TangemTheme.colors.text.tertiary,
    iconRes: Int? = null,
    iconTint: Color = TangemTheme.colors.icon.informative,
    onIconClick: (() -> Unit)? = null,
    showNetworkIcon: Boolean = false,
    showDivider: Boolean = false,
) {
    DividerContainer(
        modifier = modifier,
        showDivider = showDivider,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(TangemTheme.dimens.spacing12),
        ) {
            Text(
                text = title.resolveReference(),
                style = TangemTheme.typography.subtitle2,
                color = titleColor,
            )
            Row(
                modifier = Modifier
                    .padding(
                        top = TangemTheme.dimens.spacing6,
                    ),
            ) {
                CurrencyIcon(
                    state = tokenIconState,
                    shouldDisplayNetwork = showNetworkIcon,
                    modifier = Modifier
                        .size(TangemTheme.dimens.size36),
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = TangemTheme.dimens.spacing12),
                ) {
                    Text(
                        text = subtitle.resolveReference(),
                        style = TangemTheme.typography.subtitle2,
                        color = subtitleColor,
                    )
                    Text(
                        text = caption.resolveReference(),
                        style = TangemTheme.typography.caption2,
                        color = captionColor,
                        modifier = Modifier.padding(top = TangemTheme.dimens.spacing2),
                    )
                }
                iconRes?.let {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier
                            .align(CenterVertically)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = rememberRipple(bounded = false),
                            ) { onIconClick?.invoke() },
                    )
                }
            }
        }
    }
}

//region preview
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun InputRowInputEnterInfoPreview(
    @PreviewParameter(InputRowImagePreviewDataProvider::class) data: InputRowImagePreviewData,
) {
    TangemThemePreview {
        InputRowImage(
            title = data.title,
            modifier = Modifier.background(TangemTheme.colors.background.action),
            subtitle = data.subtitle,
            caption = data.caption,
            tokenIconState = data.iconState,
            iconRes = data.actionIconRes,
            onIconClick = {},
            showNetworkIcon = false,
            showDivider = data.showDivider,
        )
    }
}

private data class InputRowImagePreviewData(
    val title: TextReference,
    val subtitle: TextReference,
    val caption: TextReference,
    val iconState: CurrencyIconState,
    val showDivider: Boolean,
    val actionIconRes: Int?,
    val showNetworkIcon: Boolean = false,
)

private class InputRowImagePreviewDataProvider :
    PreviewParameterProvider<InputRowImagePreviewData> {
    override val values: Sequence<InputRowImagePreviewData>
        get() = sequenceOf(
            InputRowImagePreviewData(
                title = TextReference.Str("title"),
                subtitle = TextReference.Str("subtitle"),
                caption = TextReference.Str("caption"),
                iconState = CurrencyIconState.Locked,
                actionIconRes = null,
                showDivider = false,
                showNetworkIcon = false,
            ),
            InputRowImagePreviewData(
                title = TextReference.Str("title"),
                subtitle = TextReference.Str("subtitle"),
                caption = TextReference.Str("caption"),
                iconState = CurrencyIconState.Locked,
                actionIconRes = R.drawable.ic_chevron_right_24,
                showDivider = true,
                showNetworkIcon = true,
            ),
        )
}
//endregion