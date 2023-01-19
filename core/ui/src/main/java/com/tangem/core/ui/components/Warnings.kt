package com.tangem.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.res.TangemTheme

/**
 * A card with a warning icon to the left and title with description shown to the right of it.
 *
 * @param title title of the warning in bold
 * @param description description of the warning
 *
 * @see <a href = "https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?node-id=290%3A217&t=yMepJZTRh5bLkOoJ-1"
 * >Figma component</a>
 */
@Composable
fun WarningCard(
    title: String,
    description: String,
    icon: @Composable (() -> Unit)? = null,
) {
    WarningCardSurface(
        content = {
            WarningBody(
                title = title,
                description = description,
                icon = icon,
            )
        },
    )
}

/**
 * [WarningCard], but clickable (with an 'greater then' icon to the left)
 *
 * @param title title of the warning in bold
 * @param description description of the warning
 * @param onClick action to be performed when warning is clicked
 *
 * @see <a href = "https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?node-id=402%3A528&t=yMepJZTRh5bLkOoJ-1"
 * >Figma component</a>
 */
@Composable
fun ClickableWarningCard(
    title: String,
    description: String,
    icon: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    WarningCardSurface(
        content = {
            WarningBody(title = title, description = description, icon = icon) {
                SpacerW12()
                Icon(
                    painter = painterResource(id = R.drawable.ic_chevron_right_24),
                    tint = TangemTheme.colors.icon.informative,
                    contentDescription = null,
                )
            }
        },
        onClick = onClick,
    )
}

/**
 * [WarningCard], but clickable and with a 'refresh' icon at the right edge
 *
 * @param title title of the warning in bold
 * @param description description of the warning
 * @param onClick action to be performed when warning is clicked
 *
 * @see <a href = "https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?node-id=290%3A217&t=yMepJZTRh5bLkOoJ-1"
 * >Figma component</a>
 */
@Composable
fun RefreshableWaringCard(
    title: String,
    description: String,
    icon: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    WarningCardSurface(
        content = {
            WarningBody(title = title, description = description, icon = icon) {
                SpacerW12()
                Icon(
                    painter = painterResource(id = R.drawable.ic_refresh_24),
                    tint = TangemTheme.colors.icon.primary1,
                    contentDescription = null,
                )
            }
        },
        onClick = onClick,
    )
}

// region elements

@Composable
private fun WarningBody(
    title: String,
    description: String,
    icon: @Composable (() -> Unit)? = null,
    additionalContent: @Composable () -> Unit = {},
) {
    IconWithTitleAndDescription(
        title = title,
        description = description,
        additionalContent = additionalContent,
        icon = icon ?: {
            Image(
                painter = painterResource(id = R.drawable.img_attention_20),
                contentDescription = null,
            )
        },
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun WarningCardSurface(
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(TangemTheme.dimens.size12),
        backgroundColor = TangemTheme.colors.background.primary,
        elevation = TangemTheme.dimens.elevation2,
        onClick = { onClick ?: Unit },
        enabled = onClick != null,
    ) {
        content()
    }
}

// endregion elements

// region Preview

@Composable
private fun WarningsPreview() {
    Column(modifier = Modifier.fillMaxWidth()) {
        WarningCard(
            title = "Exchange rate has expired",
            description = "To access all the networks, you need to scan the card.",
        )
        SpacerH32()
        ClickableWarningCard(
            title = "Exchange rate has expired",
            description = "To access all the networks, you need to scan the card.",
            onClick = {},
        )
        SpacerH32()
        RefreshableWaringCard(
            title = "Exchange rate has expired",
            description = "To access all the networks, you need to scan the card.",
            onClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview_Warning_InLightTheme() {
    TangemTheme(isDark = false) {
        WarningsPreview()
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview_Warning_InDarkTheme() {
    TangemTheme(isDark = true) {
        WarningsPreview()
    }
}

// endregion Preview
