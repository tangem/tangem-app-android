package com.tangem.core.ui.components.rows

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH28
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

/**
 * Simple clickable action row, without input and icon
 *
 * https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?type=design&node-id=2100-807&mode=design&t=Ygv5sohTTHYAQcBS-4
 */
@Composable
fun SimpleActionRow(title: String, description: String, modifier: Modifier = Modifier, isClickable: Boolean = true) {
    Box(
        modifier = modifier
            .background(color = TangemTheme.colors.background.action)
            .height(TangemTheme.dimens.size44)
            .fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .padding(end = TangemTheme.dimens.spacing48)
                .align(Alignment.CenterStart),
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8),
        ) {
            AnimatedContent(targetState = title, label = "") {
                Text(
                    text = it,
                    style = TangemTheme.typography.caption2,
                    color = TangemTheme.colors.text.secondary,
                )
            }
            AnimatedContent(targetState = description, label = "") {
                Text(
                    text = it,
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors.text.primary1,
                )
            }
        }

        if (isClickable) {
            Icon(
                painter = painterResource(id = R.drawable.ic_chevron_right_24),
                contentDescription = null,
                modifier = Modifier
                    .align(alignment = Alignment.CenterEnd)
                    .padding(end = TangemTheme.dimens.spacing12),
                tint = TangemTheme.colors.icon.informative,
            )
        }
    }
}

@Preview
@Composable
private fun SimpleActionRowPreview() {
    Column {
        TangemThemePreview(isDark = false) {
            SimpleActionRow(
                title = "Title",
                description = "Description",
            )
        }

        SpacerH28()

        TangemThemePreview(isDark = false) {
            SimpleActionRow(
                title = "Title",
                description = "Description",
            )
        }
    }
}