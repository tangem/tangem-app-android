@file:Suppress("MagicNumber")

package com.tangem.core.ui.ds2.actionmessage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.DsVersion
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.resolveAnnotatedReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_chevron_right_12
import com.tangem.core.ui.res.generated.icons.ic_info_16

/**
 * Design-system v2 (DS3) **Action Message** — a tappable pill with a leading icon, a single-line
 * caption and a trailing chevron, leading the user to a related action or details screen.
 *
 * Version: 1.0.0
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=12889-2105)
 *
 * The message stretches to the width it is given, keeping the chevron at the end; text that does
 * not fit on one line is ellipsized. TalkBack announces the [text] as a button.
 *
 * @param text Message, rendered in caption/medium typography on a single line.
 * @param icon Leading 16dp icon, tinted with the [variant] icon color.
 * @param onClick Invoked when the message is tapped.
 * @param modifier Modifier applied to the message.
 * @param variant Visual appearance — background, text and icon colors.
 */
@Composable
fun TangemActionMessage(
    text: TextReference,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: TangemActionMessage.Variant = TangemActionMessage.Variant.Neutral,
) {
    val tokens = variant.tokens()

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(tokens.background)
            .clickableSingle(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tokens.icon,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = text.resolveAnnotatedReference(),
            style = TangemTheme.typography3.caption.medium,
            color = tokens.content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.ic_chevron_right_12,
            contentDescription = null,
            tint = tokens.icon,
            modifier = Modifier.size(12.dp),
        )
    }
}

/** Public API surface of [TangemActionMessage]. */
object TangemActionMessage {

    val version = DsVersion(major = 1, minor = 0, patch = 0)

    /** Visual appearance — background, text and icon colors. */
    enum class Variant {
        /** Neutral tertiary background with primary text and secondary icons. */
        Neutral,

        /** Subtle success-green background with success text and icons. */
        Success,

        /** Subtle info-blue background with info text and icons. */
        Info,

        /** Subtle error-red background with error text and icons. */
        Error,

        /** Subtle warning-yellow background with warning text and icons. */
        Warning,
    }
}

private data class ActionMessageTokens(val background: Color, val content: Color, val icon: Color)

@Composable
@ReadOnlyComposable
private fun TangemActionMessage.Variant.tokens(): ActionMessageTokens {
    val colors = TangemTheme.colors3
    return when (this) {
        TangemActionMessage.Variant.Neutral -> ActionMessageTokens(
            background = colors.bg.tertiary,
            content = colors.text.primary,
            icon = colors.icon.secondary,
        )
        TangemActionMessage.Variant.Success -> ActionMessageTokens(
            background = colors.bg.status.successSubtle,
            content = colors.text.status.success,
            icon = colors.icon.status.success,
        )
        TangemActionMessage.Variant.Info -> ActionMessageTokens(
            background = colors.bg.status.infoSubtle,
            content = colors.text.status.info,
            icon = colors.icon.status.info,
        )
        TangemActionMessage.Variant.Error -> ActionMessageTokens(
            background = colors.bg.status.errorSubtle,
            content = colors.text.status.error,
            icon = colors.icon.status.error,
        )
        TangemActionMessage.Variant.Warning -> ActionMessageTokens(
            background = colors.bg.status.warningSubtle,
            content = colors.text.status.warning,
            icon = colors.icon.status.warning,
        )
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun TangemActionMessagePreview() {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .width(360.dp)
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TangemActionMessage.Variant.entries.forEach { variant ->
                TangemActionMessage(
                    text = stringReference("Description"),
                    icon = Icons.ic_info_16,
                    onClick = {},
                    variant = variant,
                )
            }
            TangemActionMessage(
                text = stringReference("A long description that does not fit on a single line and gets ellipsized"),
                icon = Icons.ic_info_16,
                onClick = {},
                variant = TangemActionMessage.Variant.Info,
            )
        }
    }
}