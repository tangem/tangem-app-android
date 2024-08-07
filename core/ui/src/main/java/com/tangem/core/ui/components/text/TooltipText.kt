package com.tangem.core.ui.components.text

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

@Composable
fun TooltipText(
    text: TextReference,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TangemTheme.typography.caption2,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onInfoClick,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        Text(
            modifier = Modifier.weight(1f, fill = false),
            text = text.resolveReference(),
            style = textStyle,
            color = TangemTheme.colors.text.tertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        IconButton(
            modifier = Modifier
                .padding(horizontal = TangemTheme.dimens.spacing4)
                .requiredSize(TangemTheme.dimens.size16),
            interactionSource = interactionSource,
            onClick = onInfoClick,
        ) {
            Icon(
                modifier = Modifier.size(TangemTheme.dimens.size16),
                painter = painterResource(id = R.drawable.ic_information_24),
                tint = TangemTheme.colors.icon.informative,
                contentDescription = null,
            )
        }
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_TooltipText() {
    TangemThemePreview {
        Box(
            modifier = Modifier
                .width(width = 90.dp)
                .background(color = TangemTheme.colors.background.primary),
        ) {
            TooltipText(
                text = stringReference("Text"),
                onInfoClick = { /*TODO*/ },
            )
        }
    }
}
// endregion Preview
