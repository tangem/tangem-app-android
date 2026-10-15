package com.tangem.core.ui.components.rows

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

/**
 * Network title component. [title] and [action] composables are placed horizontally
 *
 * @param title    title composable in [BoxScope]
 * @param modifier modifier
 * @param action   optional action composable in [BoxScope]
 *
 * @see <a href = "https://www.figma.com/design/14ISV23YB1yVW1uNVwqrKv/Android?node-id=2760-2854&t=rupZfuJYbBYDidJv-4"
 * >Figma component</a>
 *
[REDACTED_AUTHOR]
 */
@Composable
fun NetworkTitle(
    modifier: Modifier = Modifier,
    title: @Composable BoxScope.() -> Unit,
    action: (@Composable BoxScope.() -> Unit)? = null,
) {
    val minHeight = if (action == null) 36.dp else 40.dp

    val padding = if (action == null) {
        PaddingValues(
            start = 12.dp,
            top = 12.dp,
            end = 12.dp,
            bottom = 4.dp,
        )
    } else {
        PaddingValues(
            start = 12.dp,
            top = 11.dp,
            end = 12.dp,
            bottom = 5.dp,
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .padding(paddingValues = padding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier = Modifier
                .heightIn(min = 20.dp),
            contentAlignment = Alignment.CenterStart,
            content = title,
        )

        if (action != null) {
            Spacer(modifier = Modifier.size(8.dp))
            Box(
                modifier = Modifier
                    .heightIn(min = 24.dp),
                contentAlignment = Alignment.CenterEnd,
                content = action,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NetworkTitlePreview(@PreviewParameter(NetworkTitleIconVisibilityProvider::class) isIconVisible: Boolean) {
    TangemThemePreview {
        NetworkTitle(
            title = { Text(text = "Network") },
            action = {
                if (isIconVisible) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_group_drop_24),
                        tint = TangemTheme.colors.icon.informative,
                        contentDescription = null,
                    )
                }
            },
        )
    }
}

private class NetworkTitleIconVisibilityProvider : CollectionPreviewParameterProvider<Boolean>(listOf(true, false))