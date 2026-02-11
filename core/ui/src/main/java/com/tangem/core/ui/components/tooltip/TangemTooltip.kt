package com.tangem.core.ui.components.tooltip

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.resolveAnnotatedReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.LocalWindowSize
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import kotlinx.coroutines.launch

/**
 * A Tangem-themed tooltip component that displays a tooltip with the provided text when the content is clicked.
 *
 * @param text The text to be displayed inside the tooltip.
 * @param modifier The modifier to be applied to the tooltip component.
 * @param enabled If false, the tooltip will not be shown when the content is clicked.
 * @param content The content that triggers the tooltip when clicked.
 */
@Composable
fun TangemTooltip(
    text: TextReference,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable (Modifier) -> Unit,
) {
    InternalTangemTooltip(
        modifier = modifier,
        enabled = enabled,
        tooltipContent = {
            Text(
                modifier = Modifier
                    .background(TangemTheme.colors.icon.secondary)
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                text = text.resolveAnnotatedReference(),
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.primary2,
            )
        },
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InternalTangemTooltip(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tooltipContent: @Composable () -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    val tooltipState = rememberTooltipState(isPersistent = true)
    val coroutineScope = rememberCoroutineScope()

    val windowSize = LocalWindowSize.current.width

    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            positioning = TooltipAnchorPosition.Above,
            spacingBetweenTooltipAndAnchor = 8.dp,
        ),
        state = tooltipState,
        modifier = modifier,
        tooltip = {
            PlainTooltip(
                modifier = Modifier.padding(end = 12.dp),
                shape = RoundedCornerShape(14.dp),
                caretShape = TooltipDefaults.caretShape(),
                maxWidth = windowSize - 24.dp,
                contentColor = TangemTheme.colors.text.primary2,
                containerColor = TangemTheme.colors.icon.secondary,
                content = { tooltipContent() },
            )
        },
        content = {
            val contentModifier = if (enabled) {
                Modifier.clickableSingle(
                    onClick = { coroutineScope.launch { tooltipState.show() } },
                )
            } else {
                Modifier
            }
            content(contentModifier)
        },
    )
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Preview(showBackground = true, widthDp = 360, heightDp = 720, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TangemTooltip_Preview() {
    TangemThemePreview {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TangemTheme.colors.background.tertiary),
        ) {
            TangemTooltip(
                modifier = Modifier,
                text = stringReference("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed venenatis."),
                content = { contentModifier ->
                    Icon(
                        modifier = contentModifier,
                        painter = painterResource(R.drawable.ic_token_info_24),
                        tint = TangemTheme.colors.icon.informative,
                        contentDescription = null,
                    )
                },
            )
        }
    }
}