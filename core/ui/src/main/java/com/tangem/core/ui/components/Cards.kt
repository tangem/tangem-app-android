package com.tangem.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.states.Item
import com.tangem.core.ui.components.states.SelectableItemsState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.valentinilk.shimmer.shimmer
import kotlinx.collections.immutable.toImmutableList

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
fun SmallInfoCard(startText: String, endText: String, isLoading: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(TangemTheme.dimens.radius12),
        color = TangemTheme.colors.background.primary,
        elevation = TangemTheme.dimens.elevation2,
    ) {
        CardInfoBox(
            startText = startText,
            endText = endText,
            isLoading = isLoading,
            disclaimer = null,
        )
    }
}

/**
 * Small card with text information attached to the edges
 *
 * @param startText text information attached to the left edge
 * @param endText   text information attached to the right edge
 * @param disclaimer closable description text
 * @param isLoading  if true, shimmer is shown instead of [startText] and [endText]
 *
 * @see <a href = "https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?node-id=1007%3A709&t=X19ZWlYP0rnmdcDO-4"
 * >Figma component</a>
 */
@Composable
fun SmallInfoCardWithDisclaimer(startText: String, endText: String, disclaimer: String, isLoading: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(TangemTheme.dimens.radius12),
        color = TangemTheme.colors.background.primary,
        elevation = TangemTheme.dimens.elevation2,
    ) {
        CardInfoBox(
            startText = startText,
            endText = endText,
            isLoading = isLoading,
            disclaimer = disclaimer,
        )
    }
}

/**
 * [SmallInfoCard] with warning information shown underneath it.
 *
 * @param startText text information attached to the left edge
 * @param endText   text information attached to the right edge
 * @param warningText text of the warning
 * @param disclaimer closable description text
 *
 * @see <a href = "https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?node-id=515%3A1541&t=qYGQJdtf1eeUPdkR-1"
 * >Figma component</a>
 */
@Composable
fun SmallInfoCardWithWarning(startText: String, endText: String, warningText: String, disclaimer: String? = null) {
    Surface(
        shape = RoundedCornerShape(TangemTheme.dimens.size12),
        color = TangemTheme.colors.background.primary,
        elevation = TangemTheme.dimens.elevation2,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CardInfoBox(startText = startText, endText = endText, disclaimer = disclaimer)

            Divider(
                color = TangemTheme.colors.stroke.primary,
                thickness = TangemTheme.dimens.size0_5,
                modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing12),
            )

            WarningItem(warningText = warningText)
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
 * @see <a href =
 * "https://www.figma.com/file/Vs6SkVsFnUPsSCNwlnVf5U/Android-%E2%80%93-UI?node-id=1123%3A4040&t=izokIIb9WWetO32R-1"
 * >Figma component</a>
 */
@Composable
fun CardWithIcon(
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
 * @see <a href =
 * "https://www.figma.com/file/Vs6SkVsFnUPsSCNwlnVf5U/Android-%E2%80%93-UI?node-id=1123%3A4040&t=izokIIb9WWetO32R-1"
 * >Figma component</a>
 */
@Composable
fun IconWithTitleAndDescription(
    title: String,
    description: String,
    icon: @Composable () -> Unit,
    additionalContent: @Composable () -> Unit = {},
) {
    Row(
        modifier = Modifier
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

/**
 * Card with text information attached to the edges and selectable items
 *
 * @param state state of block with items
 * @param isLoading  if true, shimmer is shown instead of data
 * @param disclaimer description text
 * @param onSelect listener on item selection
 *
 * @see <a href = "https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?type=design&node-id=290-217&t=FMnRqkWPAgdZSdhv-0"
 * >Figma component</a>
 */
@Composable
fun <T> SelectableInfoCard(
    state: SelectableItemsState<T>,
    onSelect: (Item<T>) -> Unit,
    isLoading: Boolean = false,
    disclaimer: String? = null,
) {
    Surface(
        shape = RoundedCornerShape(TangemTheme.dimens.radius12),
        color = TangemTheme.colors.background.primary,
        elevation = TangemTheme.dimens.elevation2,
    ) {
        SelectableInfoBlock(
            state = state,
            isLoading = isLoading,
            disclaimer = disclaimer,
            onSelect = onSelect,
        )
    }
}

/**
 * Card with text information attached to the edges, selectable items and warning
 *
 * @param state state of block with items
 * @param isLoading  if true, shimmer is shown instead of data
 * @param disclaimer description text
 * @param onSelect listener on item selection
 *
 * @see <a href = "https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?type=design&node-id=290-217&t=FMnRqkWPAgdZSdhv-0"
 * >Figma component</a>
 */
@Composable
fun <T> SelectableInfoCardWithWarning(
    state: SelectableItemsState<T>,
    warningText: String,
    onSelect: (Item<T>) -> Unit,
    isLoading: Boolean = false,
    disclaimer: String? = null,
) {
    Surface(
        shape = RoundedCornerShape(TangemTheme.dimens.radius12),
        color = TangemTheme.colors.background.primary,
        elevation = TangemTheme.dimens.elevation2,
    ) {
        SelectableInfoBlock(
            state = state,
            isLoading = isLoading,
            disclaimer = disclaimer,
            warningText = warningText,
            onSelect = onSelect,
        )
    }
}

// region elements

@Composable
private fun CardInfoBox(startText: String, endText: String, disclaimer: String?, isLoading: Boolean = false) {
    val isOpened = remember { mutableStateOf(false) }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(TangemTheme.dimens.size48)
                .clickable { isOpened.value = !isOpened.value }
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
                ShimmerItem(width = TangemTheme.dimens.size40, height = TangemTheme.dimens.size12)
            } else {
                Row {
                    Text(
                        text = endText,
                        color = TangemTheme.colors.text.primary1,
                        maxLines = 1,
                        style = TangemTheme.typography.body2,
                    )
                    if (disclaimer != null) {
                        val chevronIcon = if (isOpened.value) {
                            painterResource(id = R.drawable.ic_chevron_up_24)
                        } else {
                            painterResource(id = R.drawable.ic_chevron_24)
                        }
                        Icon(
                            modifier = Modifier.size(TangemTheme.dimens.size20),
                            painter = chevronIcon,
                            tint = TangemTheme.colors.icon.primary1,
                            contentDescription = null,
                        )
                    }
                }
            }
        }
        if (isOpened.value && disclaimer != null) {
            DisclaimerItem(disclaimer)
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun <T> SelectableInfoBlock(
    state: SelectableItemsState<T>,
    onSelect: (Item<T>) -> Unit,
    isLoading: Boolean = false,
    disclaimer: String? = null,
    warningText: String? = null,
) {
    var isOpened by remember { mutableStateOf(false) }
    Column {
        if (isLoading) {
            ShimmerItem(width = TangemTheme.dimens.size40, height = TangemTheme.dimens.size12)
        } else {
            val selectedItem = state.selectedItem
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(TangemTheme.dimens.size48)
                    .clickable { isOpened = !isOpened }
                    .padding(
                        horizontal = TangemTheme.dimens.spacing16,
                        vertical = TangemTheme.dimens.spacing12,
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = selectedItem.startText.resolveReference(),
                    color = TangemTheme.colors.text.tertiary,
                    maxLines = 1,
                    style = TangemTheme.typography.subtitle2,
                )

                Row {
                    Text(
                        text = selectedItem.endText.resolveReference(),
                        color = TangemTheme.colors.text.primary1,
                        maxLines = 1,
                        style = TangemTheme.typography.body2,
                    )
                    val chevronIcon = if (isOpened) {
                        painterResource(id = R.drawable.ic_chevron_up_24)
                    } else {
                        painterResource(id = R.drawable.ic_chevron_24)
                    }
                    Icon(
                        modifier = Modifier.size(TangemTheme.dimens.size20),
                        painter = chevronIcon,
                        tint = TangemTheme.colors.icon.primary1,
                        contentDescription = null,
                    )
                }
            }

            Divider(
                color = TangemTheme.colors.stroke.primary,
                thickness = TangemTheme.dimens.size0_5,
                modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing12),
            )

            if (isOpened) {
                state.items.forEach { item ->
                    SelectableItem(item) {
                        onSelect.invoke(item)
                    }
                    Divider(
                        color = TangemTheme.colors.stroke.primary,
                        thickness = TangemTheme.dimens.size0_5,
                        modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing12),
                    )
                }
                if (disclaimer != null) {
                    DisclaimerItem(disclaimer)
                    Divider(
                        color = TangemTheme.colors.stroke.primary,
                        thickness = TangemTheme.dimens.size0_5,
                        modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing12),
                    )
                }
            }
            if (warningText?.isNotEmpty() == true) {
                WarningItem(warningText = warningText)
            }
        }
    }
}

@Composable
private fun <T> SelectableItem(item: Item<T>, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(TangemTheme.dimens.size48)
            .clickable { onSelect() }
            .padding(
                horizontal = TangemTheme.dimens.spacing16,
                vertical = TangemTheme.dimens.spacing12,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = item.startText.resolveReference(),
            color = TangemTheme.colors.text.primary1,
            maxLines = 1,
            style = TangemTheme.typography.subtitle2,
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = item.endText.resolveReference(),
                color = TangemTheme.colors.text.primary1,
                maxLines = 1,
                style = TangemTheme.typography.body2,
            )
            val size = TangemTheme.dimens.size24
            if (item.isSelected) {
                Icon(
                    modifier = Modifier.size(size),
                    painter = painterResource(id = R.drawable.ic_check_24),
                    tint = TangemTheme.colors.icon.accent,
                    contentDescription = null,
                )
            } else {
                SpacerW(width = size)
            }
        }
    }
}

@Composable
private fun ShimmerItem(width: Dp, height: Dp) {
    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .shimmer()
            .background(
                color = TangemTheme.colors.button.secondary,
                shape = RoundedCornerShape(TangemTheme.dimens.radius3),
            ),
    )
}

@Composable
private fun DisclaimerItem(disclaimer: String) {
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
            Icon(
                painter = painterResource(id = R.drawable.ic_heart_24),
                tint = TangemTheme.colors.icon.secondary,
                contentDescription = null,
            )
        }
        SpacerW12()
        Text(
            text = disclaimer,
            color = TangemTheme.colors.text.tertiary,
            style = TangemTheme.typography.body2,
        )
    }
}

@Composable
private fun WarningItem(warningText: String) {
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

// endregion elements

// region Preview

@Suppress("MagicNumber")
@Composable
private fun CardsPreview() {
    Column(modifier = Modifier.fillMaxWidth()) {
        SmallInfoCard(
            startText = "Balance",
            endText = "0.4405434 BTC",
        )

        SpacerH32()

        SmallInfoCardWithDisclaimer(
            startText = "Balance",
            endText = "0.4405434 BTC",
            disclaimer = "Not enough funds for fee on your Polygon wallet  to create a transaction." +
                "Top up your Polygon wallet first.",
        )

        SpacerH32()

        SmallInfoCardWithWarning(
            startText = "Balance",
            endText = "0.4405434 BTC",
            warningText = "Not enough funds for fee on your Polygon wallet  to create a transaction. " +
                "Top up your Polygon wallet first.",
            disclaimer = "Not enough funds for fee on your Polygon wallet  to create a transaction." +
                "Top up your Polygon wallet first.",
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

        SpacerH32()

        val state = SelectableItemsState(
            selectedItem = Item(0, TextReference.Str("Balance"), TextReference.Str("0.4405434 BTC"), true, ""),
            items = listOf(
                Item(0, TextReference.Str("Normal"), TextReference.Str("0.4405434 BTC"), true, ""),
                Item(1, TextReference.Str("Priority"), TextReference.Str("0.46 BTC"), false, ""),
            ).toImmutableList(),
        )
        SelectableInfoCard(
            state = state,
            isLoading = false,
            disclaimer = "Not enough funds for fee on your Polygon wallet  to create a transaction",
            onSelect = {},
        )

        SpacerH32()

        SelectableInfoCardWithWarning(
            state = state,
            isLoading = false,
            warningText = "Not enough funds for fee on your Polygon wallet  to create a transaction. " +
                "Top up your Polygon wallet first.",
            disclaimer = "Not enough funds for fee on your Polygon wallet  to create a transaction",
            onSelect = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview_Cards_InLightTheme() {
    TangemTheme(isDark = false) {
        CardsPreview()
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview_InfoCardWithWarning_InDarkTheme() {
    TangemTheme(isDark = true) {
        CardsPreview()
    }
}

@Preview(widthDp = 328, heightDp = 48, showBackground = true)
@Composable
private fun Preview_SimpleInfoCard_InLightTheme() {
    TangemTheme(isDark = false) {
        SmallInfoCard(startText = "Balance", endText = "0.4405434 BTC")
        SmallInfoCardWithDisclaimer(startText = "Balance", endText = "0.4405434 BTC", disclaimer = "test")
    }
}

@Preview(widthDp = 328, heightDp = 48, showBackground = true)
@Composable
private fun Preview_SimpleInfoCard_InDarkTheme() {
    TangemTheme(isDark = true) {
        SmallInfoCard(startText = "Balance", endText = "0.4405434 BTC")
        SmallInfoCardWithDisclaimer(startText = "Balance", endText = "0.4405434 BTC", disclaimer = "test")
    }
}
// endregion Preview