package com.tangem.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.res.TangemTheme
import com.valentinilk.shimmer.shimmer

/**
 * Small card with text information attached to the edges
 *
 * @param startText text information attached to the left edge
 * @param endText   text information attached to the right edge
 * @param isLoading  if true, shimmer is shown instead of [startText] and [endText]
 *
 * @see <a href = "https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?node-id=290%3A339&t=WdN5XpixzZLlQAZO-4"
 * >Figma component</a>
 */
@Composable
fun SmallInfoCard(
    modifier: Modifier = Modifier,
    startText: String,
    endText: String,
    isLoading: Boolean = false,
) {
    Surface(
        shape = RoundedCornerShape(TangemTheme.dimens.radius12),
        color = TangemTheme.colors.background.primary,
        elevation = TangemTheme.dimens.elevation2,
    ) {
        CardInfoBox(
            modifier = modifier,
            startText = startText,
            endText = endText,
            isLoading = isLoading,
        )
    }
}

/**
 * [SmallInfoCard] with warning information shown underneath it.
 *
 * @param startText text information attached to the left edge
 * @param endText   text information attached to the right edge
 * @param warningText text of the warning
 *
 * @see <a href = "https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?node-id=515%3A1541&t=qYGQJdtf1eeUPdkR-1"
 * >Figma component</a>
 */
@Composable
fun SmallInfoCardWithWarning(
    modifier: Modifier = Modifier,
    startText: String,
    endText: String,
    warningText: String,
) {
    Surface(
        shape = RoundedCornerShape(TangemTheme.dimens.size12),
        color = TangemTheme.colors.background.primary,
        elevation = TangemTheme.dimens.elevation2,
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            CardInfoBox(startText = startText, endText = endText)

            Divider(
                color = TangemTheme.colors.stroke.primary,
                thickness = TangemTheme.dimens.size0_5,
                modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing12),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = TangemTheme.dimens.spacing14,
                        bottom = TangemTheme.dimens.spacing16,
                        start = TangemTheme.dimens.spacing16,
                        end = TangemTheme.dimens.spacing16,
                    ),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            color = TangemTheme.colors.background.secondary,
                            shape = CircleShape,
                        )
                        .height(TangemTheme.dimens.size40)
                        .width(TangemTheme.dimens.size40),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_attention_20),
                        contentDescription = null,
                    )
                }
                SpacerW12()
                Text(
                    text = warningText,
                    color = TangemTheme.colors.text.primary1,
                    style = TangemTheme.typography.body2,
                )
            }
        }
    }
}

/**
 * Small card with an icon to the left and title with description shown to the right of it.
 * Additional context can be shown to the right of it.
 *
 * @param title shown to the right of icon
 * @param description shown to the right of icon
 * @param icon icon to the left
 * @param additionalContent shown at the right edge of the card
 *
 * @see <a href = "https://www.figma.com/file/Vs6SkVsFnUPsSCNwlnVf5U/Android-%E2%80%93-UI?node-id=1123%3A4040&t=izokIIb9WWetO32R-1"
 * >Figma component</a>
 */
@Composable
fun CardWithIcon(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    icon: @Composable () -> Unit,
    additionalContent: @Composable () -> Unit = {},
) {
    Surface(
        shape = RoundedCornerShape(TangemTheme.dimens.radius12),
        color = TangemTheme.colors.background.primary,
        elevation = TangemTheme.dimens.elevation2,
    ) {
        IconWithTitleAndDescription(
            modifier = modifier,
            title = title,
            description = description,
            icon = icon,
            additionalContent = additionalContent,
        )
    }
}

/**
 * A widget with an icon to the left and title with description shown to the right of it.
 * Additional context can be shown to the right of it.
 *
 * @param title shown to the right of icon
 * @param description shown to the right of icon
 * @param icon icon to the left
 * @param additionalContent shown at the right edge of the card
 *
 * @see <a href = "https://www.figma.com/file/Vs6SkVsFnUPsSCNwlnVf5U/Android-%E2%80%93-UI?node-id=1123%3A4040&t=izokIIb9WWetO32R-1"
 * >Figma component</a>
 */
@Composable
fun IconWithTitleAndDescription(
    title: String,
    description: String,
    icon: @Composable () -> Unit,
    additionalContent: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .wrapContentHeight()
            .fillMaxWidth()
            .padding(
                horizontal = TangemTheme.dimens.spacing16,
                vertical = TangemTheme.dimens.spacing12,
            ),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {

        Box(
            modifier = Modifier
                .background(
                    color = TangemTheme.colors.background.secondary,
                    shape = CircleShape,
                )
                .height(TangemTheme.dimens.size40)
                .width(TangemTheme.dimens.size40),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }

        SpacerW12()

        Column(
            modifier = Modifier
                .weight(weight = 1f, fill = true),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = title,
                color = TangemTheme.colors.text.primary1,
                style = TangemTheme.typography.subtitle1,
            )
            SpacerH4()
            Text(
                text = description,
                color = TangemTheme.colors.text.secondary,
                style = TangemTheme.typography.body2,
            )
        }

        additionalContent()
    }
}

// region elements

@Composable
private fun CardInfoBox(
    modifier: Modifier = Modifier,
    startText: String,
    endText: String,
    isLoading: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(TangemTheme.dimens.size48)
            .padding(
                horizontal = TangemTheme.dimens.spacing16,
                vertical = TangemTheme.dimens.spacing12,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = startText,
            color = TangemTheme.colors.text.tertiary,
            maxLines = 1,
            style = TangemTheme.typography.subtitle2,
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .shimmer()
                    .size(
                        width = TangemTheme.dimens.size80,
                        height = TangemTheme.dimens.size20,
                    ),
            )
        } else {
            Text(
                text = endText,
                color = TangemTheme.colors.text.primary1,
                maxLines = 1,
                style = TangemTheme.typography.body2,
            )
        }
    }
}

// endregion elements

// region Preview

@Composable
private fun CardsPreview() {
    Column(modifier = Modifier.fillMaxWidth()) {
        SmallInfoCard(startText = "Balance", endText = "0.4405434 BTC")

        SpacerH32()

        SmallInfoCardWithWarning(
            startText = "Balance",
            endText = "0.4405434 BTC",
            warningText =
            "Not enough funds for fee on your Polygon wallet  to create a transaction. Top up your Polygon wallet first.",
        )

        SpacerH32()

        CardWithIcon(
            title = "Permit is valid until",
            description = "26:30",
            icon = {
                Icon(
                    painterResource(id = R.drawable.ic_clock_24),
                    contentDescription = null,
                    tint = TangemTheme.colors.icon.primary1,
                )
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
fun Preview_Cards_InLightTheme() {
    TangemTheme(isDark = false) {
        CardsPreview()
    }
}

@Preview(showBackground = true)
@Composable
fun Preview_InfoCardWithWarning_InDarkTheme() {
    TangemTheme(isDark = true) {
        CardsPreview()
    }
}

// endregion Preview
