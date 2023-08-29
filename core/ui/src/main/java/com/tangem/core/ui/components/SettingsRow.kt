package com.tangem.core.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.tangem.core.ui.res.TangemTheme

/**
 * [Show in Figma](https://www.figma.com/file/14ISV23YB1yVW1uNVwqrKv/Android?type=design&node-id=281-248&mode=design&t=bXqehWPHyATKcZEW-4)
 * */
@Composable
fun SimpleSettingsRow(
    title: String,
    @DrawableRes icon: Int,
    onItemsClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    subtitle: String? = null,
) {
    Row(
        modifier = modifier
            .height(TangemTheme.dimens.size56)
            .fillMaxWidth()
            .clickable(
                onClick = {
                    if (enabled) {
                        onItemsClick()
                    }
                },
            ),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val textColor: Color by animateColorAsState(
            targetValue = if (enabled) {
                TangemTheme.colors.text.primary1
            } else {
                TangemTheme.colors.text.secondary
            },
        )
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing20),
            tint = textColor,
        )
        Column(modifier = Modifier.padding(end = TangemTheme.dimens.spacing20)) {
            Text(
                text = title,
                style = TangemTheme.typography.subtitle1,
                color = textColor,
            )
            AnimatedVisibility(
                visible = !subtitle.isNullOrEmpty(),
            ) {
                Text(
                    text = subtitle ?: "",
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors.text.secondary,
                )
            }
        }
    }
}