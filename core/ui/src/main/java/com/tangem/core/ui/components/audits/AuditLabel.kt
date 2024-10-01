package com.tangem.core.ui.components.audits

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
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
        textColor = getColorByType(state.type),
        backgroundColor = getColorByType(state.type).copy(alpha = 0.1f),
        modifier = modifier,
    )
}

@Composable
private fun AuditLabelInternal(
    textReference: TextReference,
    textColor: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(
            color = backgroundColor,
            shape = TangemTheme.shapes.roundedCornersSmall2,
        ),
    ) {
        Text(
            text = textReference.resolveReference(),
            style = TangemTheme.typography.caption1,
            color = textColor,
            modifier = Modifier.padding(horizontal = TangemTheme.dimens.spacing4),
        )
    }
}

@ReadOnlyComposable
@Composable
private fun getColorByType(type: AuditLabelUM.Type): Color {
    return when (type) {
        AuditLabelUM.Type.Prohibition -> TangemTheme.colors.text.warning
        AuditLabelUM.Type.Warning -> TangemTheme.colors.text.attention
        AuditLabelUM.Type.Permit -> TangemTheme.colors.text.accent
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