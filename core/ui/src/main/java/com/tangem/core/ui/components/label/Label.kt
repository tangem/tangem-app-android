package com.tangem.core.ui.components.label

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.label.entity.LabelStyle
import com.tangem.core.ui.components.label.entity.LabelUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview

/**
 * Label component
 *
 * @param state     component state
 * @param modifier  composable modifier
 *
 * @see <a href="https://www.figma.com/design/14ISV23YB1yVW1uNVwqrKv/Android?node-id=4480-1459&t=2QTpi1G7FeTexTFS-4">Figma</a>
 */
@Composable
fun Label(state: LabelUM, modifier: Modifier = Modifier) {
    val backgroundColor by animateColorAsState(
        targetValue = when (state.style) {
            LabelStyle.ACCENT -> TangemTheme.colors.text.accent.copy(alpha = 0.1f)
            LabelStyle.REGULAR -> TangemTheme.colors.control.unchecked
            LabelStyle.WARNING -> TangemTheme.colors.text.warning.copy(alpha = 0.1f)
        },
    )

    val textColor by animateColorAsState(
        targetValue = when (state.style) {
            LabelStyle.ACCENT -> TangemTheme.colors.text.accent
            LabelStyle.REGULAR -> TangemTheme.colors.text.secondary
            LabelStyle.WARNING -> TangemTheme.colors.text.warning
        },
    )

    AnimatedContent(targetState = state.text) { text ->
        Box(
            modifier = modifier
                .padding(horizontal = 4.dp)
                .background(
                    color = backgroundColor,
                    shape = TangemTheme.shapes.roundedCorners8,
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(
                text = text.resolveReference(),
                style = TangemTheme.typography.caption1,
                color = textColor,
            )
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LabelPreview() {
    TangemThemePreview {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Label(
                state = LabelUM(
                    text = TextReference.Str("Regular Label"),
                    style = LabelStyle.REGULAR,
                ),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Label(
                state = LabelUM(
                    text = TextReference.Str("Accent Label"),
                    style = LabelStyle.ACCENT,
                ),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Label(
                state = LabelUM(
                    text = TextReference.Str("Warning Label"),
                    style = LabelStyle.WARNING,
                ),
            )
        }
    }
}