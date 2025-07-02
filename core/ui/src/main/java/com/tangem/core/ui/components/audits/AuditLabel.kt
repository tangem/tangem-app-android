package com.tangem.core.ui.components.audits

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

/**
 * Audit label component
 *
 * @param state    state
 * @param modifier modifier
 *
 * @see <a href="https://www.figma.com/design/14ISV23YB1yVW1uNVwqrKv/Android?node-id=3401-3635&t=kjnj9RhYUOwbO1hn-4">Figma</a>
 *
[REDACTED_AUTHOR]
 */
@Composable
fun AuditLabel(state: AuditLabelUM, modifier: Modifier = Modifier) {
    AuditLabelInternal(
        textReference = state.text,
        type = state.type,
        modifier = modifier,
    )
}

@Composable
private fun AuditLabelInternal(textReference: TextReference, type: AuditLabelUM.Type, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            color = getBackgroundColorByType(type),
            shape = TangemTheme.shapes.roundedCornersSmall2,
        ),
    ) {
        Text(
            text = textReference.resolveReference(),
            style = TangemTheme.typography.caption1,
            color = getTextColorByType(type),
            modifier = Modifier.padding(getPaddingByType(type)),
        )
    }
}

@ReadOnlyComposable
@Composable
private fun getTextColorByType(type: AuditLabelUM.Type): Color {
    return when (type) {
        AuditLabelUM.Type.Prohibition -> TangemTheme.colors.text.warning
        AuditLabelUM.Type.Warning -> TangemTheme.colors.text.attention
        AuditLabelUM.Type.Permit -> TangemTheme.colors.text.accent
        AuditLabelUM.Type.Info -> TangemTheme.colors.text.primary2
    }
}

@ReadOnlyComposable
@Composable
private fun getBackgroundColorByType(type: AuditLabelUM.Type): Color {
    return when (type) {
        AuditLabelUM.Type.Prohibition -> TangemTheme.colors.text.warning.copy(alpha = 0.1f)
        AuditLabelUM.Type.Warning -> TangemTheme.colors.text.attention.copy(alpha = 0.1f)
        AuditLabelUM.Type.Permit -> TangemTheme.colors.text.accent.copy(alpha = 0.1f)
        AuditLabelUM.Type.Info -> TangemTheme.colors.text.accent
    }
}

@ReadOnlyComposable
@Composable
private fun getPaddingByType(type: AuditLabelUM.Type): PaddingValues {
    return when (type) {
        AuditLabelUM.Type.Prohibition,
        AuditLabelUM.Type.Warning,
        AuditLabelUM.Type.Permit,
        -> PaddingValues(horizontal = 4.dp)
        AuditLabelUM.Type.Info -> PaddingValues(horizontal = 6.dp, vertical = 1.dp)
    }
}

@Preview
@Composable
private fun Preview_AuditLabel(@PreviewParameter(AuditLabelUMProvider::class) state: AuditLabelUM) {
    TangemThemePreview {
        AuditLabel(state = state)
    }
}

private class AuditLabelUMProvider : CollectionPreviewParameterProvider<AuditLabelUM>(
    collection = AuditLabelUM.Type.entries.map { type ->
        AuditLabelUM(
            text = TextReference.Str(value = type.name),
            type = type,
        )
    },
)