package com.tangem.core.ui.components.tooltip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import kotlinx.coroutines.launch

@Composable
fun TangemTooltip(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable (Modifier) -> Unit,
) {
    InternalTangemTooltip(
        modifier = modifier,
        enabled = enabled,
        tooltipContent = {
            Text(
                modifier = Modifier.background(TangemTheme.colors.icon.secondary),
                text = text,
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.primary2,
            )
        },
        content = content,
    )
}

@Composable
fun TangemTooltip(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable (Modifier) -> Unit,
) {
    InternalTangemTooltip(
        modifier = modifier,
        enabled = enabled,
        tooltipContent = {
            Text(
                modifier = Modifier.background(TangemTheme.colors.icon.secondary),
                text = text,
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
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(spacingBetweenTooltipAndAnchor = 8.dp),
        state = tooltipState,
        modifier = modifier,
        tooltip = {
            PlainTooltip(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                caretSize = DpSize(width = 14.dp, height = 8.dp),
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

@Preview
@Composable
private fun TangemTooltip_Preview() {
    TangemThemePreview {
        Box(
            modifier = Modifier
                .size(500.dp)
                .background(TangemTheme.colors.background.secondary),
            contentAlignment = Alignment.Center,
        ) {
            TangemTooltip(
                modifier = Modifier
                    .background(TangemTheme.colors.background.secondary)
                    .size(64.dp),
                text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed venenatis.",
                content = { contentModifier ->
                    Icon(
                        modifier = contentModifier.size(64.dp),
                        painter = painterResource(R.drawable.ic_token_info_24),
                        tint = TangemTheme.colors.icon.informative,
                        contentDescription = null,
                    )
                },
            )
        }
    }
}