package com.tangem.core.ui.components.label

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
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

    val iconColor by animateColorAsState(
        targetValue = when (state.style) {
            LabelStyle.ACCENT -> TangemTheme.colors.icon.accent
            LabelStyle.REGULAR -> TangemTheme.colors.icon.informative
            LabelStyle.WARNING -> TangemTheme.colors.icon.warning
        },
    )

    AnimatedContent(targetState = state.text) { text ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
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
            AnimatedVisibility(state.icon != null) {
                val wrappedIcon = remember(this) { requireNotNull(state.icon) }
                Icon(
                    imageVector = ImageVector.vectorResource(wrappedIcon),
                    tint = iconColor,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LabelPreview() {
    TangemThemePreview {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Label(
                state = LabelUM(
                    text = TextReference.Str("Regular Label"),
                    style = LabelStyle.REGULAR,
                ),
            )
            Label(
                state = LabelUM(
                    text = TextReference.Str("Accent Label"),
                    style = LabelStyle.ACCENT,
                ),
            )
            Label(
                state = LabelUM(
                    text = TextReference.Str("Warning Label"),
                    style = LabelStyle.WARNING,
                ),
            )
            Label(
                state = LabelUM(
                    text = TextReference.Str("Regular Label"),
                    style = LabelStyle.REGULAR,
                    icon = R.drawable.ic_information_24,
                ),
            )
            Label(
                state = LabelUM(
                    text = TextReference.Str("Accent Label"),
                    style = LabelStyle.ACCENT,
                    icon = R.drawable.ic_information_24,
                ),
            )
            Label(
                state = LabelUM(
                    text = TextReference.Str("Warning Label"),
                    style = LabelStyle.WARNING,
                    icon = R.drawable.ic_information_24,
                ),
            )
        }
    }
}