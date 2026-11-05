package com.tangem.core.ui.ds2.togglebadge

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.DsVersion
import com.tangem.core.ui.ds2.badge.BadgeColorTokens
import com.tangem.core.ui.ds2.badge.BadgeContent
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.badge.focusRingBorder
import com.tangem.core.ui.ds2.badge.tokens
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

/**
 * Design-system v2 toggle badge: a [TangemBadge]-shaped pill that switches between an unselected
 * and a selected (inverse) look on tap.
 *
 * Version: 1.0.0
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=13823-1044)
 *
 * Behavior notes:
 * - Icon tints follow the selection state; any tint set on a supplied [TangemIconUM.Icon] is
 *   overridden. Other [TangemIconUM] subtypes pass through with their own colors intact.
 * - The press overlay follows the selection state as well: `interaction.press.default` when
 *   unselected, `interaction.press.inverse` when selected.
 * - A focused badge draws the brand focus ring.
 *
 * @param text Badge label.
 * @param isSelected Current selection state.
 * @param onSelectedChange Called with the new selection state on tap.
 * @param modifier Modifier applied to the badge container.
 * @param size Size preset shared with [TangemBadge]. See [TangemBadge.Size].
 * @param iconStart Optional leading icon.
 * @param iconEnd Optional trailing icon.
 * @param enabled `false` ignores taps and is announced as disabled; the look stays unchanged.
 * @param contentDescription Accessibility label announced by TalkBack. When non-null it overrides
 *   the label text for screen readers.

 */
@Suppress("LongParameterList")
@Composable
fun TangemToggleBadge(
    text: TextReference,
    isSelected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    size: TangemBadge.Size = TangemBadge.Size.X9,
    iconStart: TangemIconUM? = null,
    iconEnd: TangemIconUM? = null,
    enabled: Boolean = true,
    contentDescription: String? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val colorTokens = resolveToggleColorTokens(isSelected = isSelected)
    val sizeTokens = size.tokens()
    val resolvedInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val isFocused by resolvedInteractionSource.collectIsFocusedAsState()

    TangemSurface(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                role = Role.Checkbox
                toggleableState = ToggleableState(isSelected)
                if (!enabled) disabled()
                contentDescription?.let { this.contentDescription = it }
            }
            .heightIn(min = sizeTokens.minHeight),
        onClick = { onSelectedChange(!isSelected) },
        enabled = enabled,
        interactionSource = resolvedInteractionSource,
        color = colorTokens.backgroundColor,
        border = if (isFocused) focusRingBorder() else null,
        shape = RoundedCornerShape(999.dp),
    ) {
        BadgeContent(
            iconStart = iconStart,
            iconEnd = iconEnd,
            text = text,
            colorTokens = colorTokens,
            sizeTokens = sizeTokens,
        )
    }
}

object TangemToggleBadge {

    val version = DsVersion(major = 1, minor = 0, patch = 0)
}

@Composable
@ReadOnlyComposable
private fun resolveToggleColorTokens(isSelected: Boolean): BadgeColorTokens {
    val colors = TangemTheme.colors3
    return if (isSelected) {
        BadgeColorTokens(
            backgroundColor = colors.bg.inverse,
            textColor = colors.text.inverse.primary,
            iconTint = colors.icon.inverse,
        )
    } else {
        BadgeColorTokens(
            backgroundColor = colors.bg.transparent.primary,
            textColor = colors.text.primary,
            iconTint = colors.icon.primary,
        )
    }
}

// region Previews

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun TangemToggleBadgePreview() {
    TangemThemePreviewRedesign {
        val icon = remember { TangemIconUM.Icon(iconRes = R.drawable.ic_information_24) }
        Column(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            listOf(false, true).forEach { isSelectedInitially ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TangemBadge.Size.entries.forEach { size ->
                        var isSelected by remember { mutableStateOf(isSelectedInitially) }
                        TangemToggleBadge(
                            text = stringReference(size.name),
                            isSelected = isSelected,
                            onSelectedChange = { isSelected = it },
                            size = size,
                            iconStart = icon,
                            iconEnd = icon,
                        )
                    }
                }
            }
        }
    }
}

// endregion