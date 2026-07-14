package com.tangem.features.forceupdate.impl.ui

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_error_24
import com.tangem.core.ui.res.generated.icons.ic_warning_24
import com.tangem.features.forceupdate.impl.R
import com.tangem.features.forceupdate.ForceUpdateComponent
import com.tangem.features.forceupdate.impl.ui.state.ForceUpdateUM
import com.tangem.features.forceupdate.impl.ui.state.ForceUpdateUM.Accent

@Composable
internal fun ForceUpdateContent(state: ForceUpdateUM, modifier: Modifier = Modifier) {
    BackHandler(enabled = !state.isBlocking) {
        state.onLaterClick?.invoke()
    }

    val accentColor = when (state.accent) {
        Accent.Red -> TangemTheme.colors3.icon.accent.red
        Accent.Yellow -> TangemTheme.colors3.icon.accent.yellow
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors3.bg.primary)
            .glow(accentColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 16.dp),
        ) {
            Content(
                state = state,
                accentColor = accentColor,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
            Buttons(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
            )
        }
    }
}

@Composable
private fun Content(state: ForceUpdateUM, accentColor: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(64.dp))
        Icon(
            modifier = Modifier.size(32.dp),
            imageVector = when (state.accent) {
                Accent.Red -> Icons.ic_error_24
                Accent.Yellow -> Icons.ic_warning_24
            },
            contentDescription = null,
            tint = accentColor,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = state.title.resolveReference(),
            style = TangemTheme.typography3.heading.medium,
            color = TangemTheme.colors3.text.primary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = state.description.resolveReference(),
            style = TangemTheme.typography3.heading.medium,
            color = TangemTheme.colors3.text.secondary,
        )
    }
}

@Composable
private fun Buttons(state: ForceUpdateUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        state.onUpdateClick?.let { onClick ->
            TangemButton(
                modifier = Modifier.fillMaxWidth(),
                variant = TangemButton.Variant.Primary,
                text = resourceReference(R.string.force_update_action),
                onClick = onClick,
            )
        }
        state.onLaterClick?.let { onClick ->
            TangemButton(
                modifier = Modifier.fillMaxWidth(),
                variant = TangemButton.Variant.Secondary,
                text = resourceReference(R.string.common_later),
                onClick = onClick,
            )
        }
    }
}

private fun Modifier.glow(color: Color): Modifier = drawBehind {
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = GLOW_ALPHA), Color.Transparent),
            center = Offset(x = size.width * GLOW_CENTER_X_FRACTION, y = 0f),
            radius = size.width,
        ),
    )
}

private const val GLOW_ALPHA = 0.32f
private const val GLOW_CENTER_X_FRACTION = 0.3f

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Preview(showBackground = true, widthDp = 360, heightDp = 780, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewForce() {
    TangemThemePreviewRedesign {
        ForceUpdateContent(
            state = ForceUpdateUM(
                mode = ForceUpdateComponent.Mode.Force,
                accent = Accent.Red,
                title = TextReference.Str("Update Required"),
                description = TextReference.Str("Please update the application to the latest version."),
                isBlocking = true,
                onUpdateClick = {},
                onLaterClick = null,
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Preview(showBackground = true, widthDp = 360, heightDp = 780, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewOptional() {
    TangemThemePreviewRedesign {
        ForceUpdateContent(
            state = ForceUpdateUM(
                mode = ForceUpdateComponent.Mode.Optional,
                accent = Accent.Yellow,
                title = TextReference.Str("Update Required"),
                description = TextReference.Str("Please update the application to the latest version."),
                isBlocking = false,
                onUpdateClick = {},
                onLaterClick = {},
            ),
        )
    }
}