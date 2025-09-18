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
    val minHeight = if (action == null) TangemTheme.dimens.size36 else TangemTheme.dimens.size40

    val padding = if (action == null) {
        PaddingValues(
            start = TangemTheme.dimens.spacing12,
            top = TangemTheme.dimens.spacing12,
            end = TangemTheme.dimens.spacing12,
            bottom = TangemTheme.dimens.spacing4,
        )
    } else {
        PaddingValues(
            start = TangemTheme.dimens.spacing12,
            top = TangemTheme.dimens.spacing11,
            end = TangemTheme.dimens.spacing12,
            bottom = TangemTheme.dimens.spacing5,
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
                .weight(weight = 1f)
                .heightIn(min = TangemTheme.dimens.size20),
            contentAlignment = Alignment.CenterStart,
            content = title,
        )

        if (action != null) {
            Spacer(modifier = Modifier.size(TangemTheme.dimens.spacing8))
            Box(
                modifier = Modifier
                    .weight(weight = 1f)
                    .heightIn(min = TangemTheme.dimens.size24),
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

private object NetworkTitleIconVisibilityProvider : CollectionPreviewParameterProvider<Boolean>(listOf(true, false))