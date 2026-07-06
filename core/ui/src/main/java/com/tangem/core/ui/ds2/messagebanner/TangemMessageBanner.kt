@file:Suppress("MagicNumber")

package com.tangem.core.ui.ds2.messagebanner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.glowring.TangemGlowRing
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.clickableSingle
import com.tangem.core.ui.extensions.resolveAnnotatedReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_cross_circle_20_filled

/**
 * Design-system v2 (DS3) **Message Banner** — low-level slot API: a [content] block above an
 * optional action-button row. For the common title/description layout, prefer the `title` overload.
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=5475-7680&m=dev)
 *
 * @param variant Visual appearance — background color + glow ring.
 * @param showGlowRing Whether the glow ring is drawn around the banner. `false` shows only the
 * background.
 * @param secondaryButton Start action. `null` hides it.
 * @param primaryButton End action. `null` hides it.
 * @param content The banner body above the buttons.
 */
@Composable
fun TangemMessageBanner(
    modifier: Modifier = Modifier,
    variant: TangemMessageBanner.Variant = TangemMessageBanner.Variant.Default,
    showGlowRing: Boolean = true,
    secondaryButton: TangemMessageBanner.Button? = null,
    primaryButton: TangemMessageBanner.Button? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = variant.tokens()

    Box(modifier = modifier) {
        TangemSurface(
            modifier = Modifier.fillMaxWidth(),
            color = tokens.background,
            shape = RoundedCornerShape(28.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                content()
                MessageBannerButtons(secondaryButton = secondaryButton, primaryButton = primaryButton)
            }
        }
        if (showGlowRing) {
            TangemGlowRing(
                modifier = Modifier.matchParentSize(),
                variant = tokens.glowRing,
                cornerRadius = 28.dp,
            )
        }
    }
}

/**
 * Design-system v2 (DS3) **Message Banner** — title/description header with optional slots and an
 * action-button row.
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=5475-7680&m=dev)
 *
 * @param title Banner headline.
 * @param variant Visual appearance — background color + glow ring.
 * @param contentAlign Horizontal alignment of the text block.
 * @param showGlowRing Whether the glow ring is drawn around the banner. `false` shows only the
 * background.
 * @param description Secondary line under the [title]. `null` hides it.
 * @param secondaryButton Start action. `null` hides it.
 * @param primaryButton End action. `null` hides it.
 * @param slotStart Leading slot before the title. `null` hides it.
 * @param slotEnd Trailing slot after the title (e.g. the [CloseButton] preset). `null` hides it.
 * @param extraBottomSlot Slot under the description, inside the text column.
 */
@Suppress("LongParameterList")
@Composable
fun TangemMessageBanner(
    title: TextReference,
    modifier: Modifier = Modifier,
    variant: TangemMessageBanner.Variant = TangemMessageBanner.Variant.Default,
    contentAlign: TangemMessageBanner.ContentAlign = TangemMessageBanner.ContentAlign.Start,
    showGlowRing: Boolean = true,
    description: TextReference? = null,
    secondaryButton: TangemMessageBanner.Button? = null,
    primaryButton: TangemMessageBanner.Button? = null,
    slotStart: (@Composable () -> Unit)? = null,
    slotEnd: (@Composable () -> Unit)? = null,
    extraBottomSlot: (@Composable ColumnScope.() -> Unit)? = null,
) {
    TangemMessageBanner(
        modifier = modifier,
        variant = variant,
        showGlowRing = showGlowRing,
        secondaryButton = secondaryButton,
        primaryButton = primaryButton,
    ) {
        MessageBannerContentRow(
            title = title,
            description = description,
            contentAlign = contentAlign,
            slotStart = slotStart,
            slotEnd = slotEnd,
            extraBottomSlot = extraBottomSlot,
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun MessageBannerContentRow(
    title: TextReference,
    description: TextReference?,
    contentAlign: TangemMessageBanner.ContentAlign,
    slotStart: (@Composable () -> Unit)?,
    slotEnd: (@Composable () -> Unit)?,
    extraBottomSlot: (@Composable ColumnScope.() -> Unit)?,
) {
    val textWrapper: @Composable (Modifier) -> Unit = { textModifier ->
        MessageBannerTextWrapper(
            modifier = textModifier,
            title = title,
            description = description,
            contentAlign = contentAlign,
            extraBottomSlot = extraBottomSlot,
        )
    }
    when (contentAlign) {
        TangemMessageBanner.ContentAlign.Start -> Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            slotStart?.let { slot -> Box(modifier = Modifier.align(Alignment.Top)) { slot() } }
            textWrapper(Modifier.weight(1f).align(Alignment.Top))
            slotEnd?.let { slot -> Box(modifier = Modifier.align(Alignment.Top)) { slot() } }
        }
        TangemMessageBanner.ContentAlign.Center -> Box(modifier = Modifier.fillMaxWidth()) {
            textWrapper(Modifier.fillMaxWidth())
            slotStart?.let { slot -> Box(modifier = Modifier.align(Alignment.TopStart)) { slot() } }
            slotEnd?.let { slot -> Box(modifier = Modifier.align(Alignment.TopEnd)) { slot() } }
        }
    }
}

@Composable
private fun MessageBannerTextWrapper(
    title: TextReference,
    description: TextReference?,
    contentAlign: TangemMessageBanner.ContentAlign,
    extraBottomSlot: (@Composable ColumnScope.() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val isCenter = contentAlign == TangemMessageBanner.ContentAlign.Center
    val textAlign = if (isCenter) TextAlign.Center else TextAlign.Start
    Column(
        modifier = if (isCenter) modifier.padding(horizontal = 32.dp) else modifier,
        horizontalAlignment = if (isCenter) Alignment.CenterHorizontally else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title.resolveAnnotatedReference(),
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.primary,
            textAlign = textAlign,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (description != null) {
            Text(
                text = description.resolveAnnotatedReference(),
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.secondary,
                textAlign = textAlign,
            )
        }
        extraBottomSlot?.let { slot ->
            Column(modifier = Modifier.padding(top = 12.dp)) { slot() }
        }
    }
}

@Composable
private fun MessageBannerButtons(
    secondaryButton: TangemMessageBanner.Button?,
    primaryButton: TangemMessageBanner.Button?,
) {
    if (secondaryButton == null && primaryButton == null) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        secondaryButton?.let { button ->
            TangemButton(
                modifier = Modifier.weight(1f),
                variant = TangemButton.Variant.Secondary,
                text = button.text,
                iconStart = button.iconStart,
                iconEnd = button.iconEnd,
                isEnabled = button.isEnabled,
                isLoading = button.isLoading,
                onClick = button.onClick,
            )
        }
        primaryButton?.let { button ->
            TangemButton(
                modifier = Modifier.weight(1f),
                variant = TangemButton.Variant.Primary,
                text = button.text,
                iconStart = button.iconStart,
                iconEnd = button.iconEnd,
                isEnabled = button.isEnabled,
                isLoading = button.isLoading,
                onClick = button.onClick,
            )
        }
    }
}

/** Public API surface of [TangemMessageBanner]. */
object TangemMessageBanner {

    /** Visual appearance — background color + glow ring color. */
    enum class Variant {
        /** Neutral opaque background with a multi-color "magic" glow ring. */
        Default,

        /** Neutral tertiary (filled) background with a multi-color "magic" glow ring. */
        Solid,

        /** Subtle success-green background and matching glow ring. */
        Success,

        /** Subtle error-red background and matching glow ring. */
        Error,

        /** Subtle warning-yellow background and matching glow ring. */
        Warning,

        /** Subtle info-blue background and matching glow ring. */
        Info,
    }

    /** Horizontal alignment of the text block. */
    enum class ContentAlign {
        Start,
        Center,
    }

    /** An action button shown in the banner's button row. */
    @Immutable
    data class Button(
        val text: TextReference,
        val onClick: () -> Unit,
        val iconStart: TangemIconUM? = null,
        val iconEnd: TangemIconUM? = null,
        val isEnabled: Boolean = true,
        val isLoading: Boolean = false,
    )
}

/**
 * Dismiss-button preset for [TangemMessageBanner] — a filled cross-circle to pass as `slotEnd`.
 *
 * @param contentDescription Accessibility label announced by TalkBack (e.g. `"Dismiss"`).
 */
@Composable
fun TangemMessageBanner.CloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Icon(
        imageVector = Icons.ic_cross_circle_20_filled,
        contentDescription = null,
        tint = TangemTheme.colors3.icon.secondary,
        modifier = modifier
            .size(20.dp)
            .clip(RoundedCornerShape(percent = 50))
            .clickableSingle(onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription?.let { this.contentDescription = it }
            },
    )
}

/** Resolved appearance tokens for a [TangemMessageBanner.Variant]. */
private data class MessageBannerTokens(val background: Color, val glowRing: TangemGlowRing.Variant)

@Composable
@ReadOnlyComposable
private fun TangemMessageBanner.Variant.tokens(): MessageBannerTokens {
    val colors = TangemTheme.colors3
    return when (this) {
        TangemMessageBanner.Variant.Default -> MessageBannerTokens(
            background = colors.bg.opaque.primary,
            glowRing = TangemGlowRing.Variant.Magic,
        )
        TangemMessageBanner.Variant.Solid -> MessageBannerTokens(
            background = colors.bg.tertiary,
            glowRing = TangemGlowRing.Variant.Magic,
        )
        TangemMessageBanner.Variant.Success -> MessageBannerTokens(
            background = colors.bg.status.successSubtle,
            glowRing = TangemGlowRing.Variant.Success,
        )
        TangemMessageBanner.Variant.Error -> MessageBannerTokens(
            background = colors.bg.status.errorSubtle,
            glowRing = TangemGlowRing.Variant.Error,
        )
        TangemMessageBanner.Variant.Warning -> MessageBannerTokens(
            background = colors.bg.status.warningSubtle,
            glowRing = TangemGlowRing.Variant.Warning,
        )
        TangemMessageBanner.Variant.Info -> MessageBannerTokens(
            background = colors.bg.status.infoSubtle,
            glowRing = TangemGlowRing.Variant.Info,
        )
    }
}

@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun TangemMessageBannerPreview() {
    PreviewContainer {
        TangemMessageBanner(
            modifier = Modifier.fillMaxWidth(),
            title = stringReference("Title"),
            description = stringReference("Description"),
            slotEnd = { TangemMessageBanner.CloseButton(onClick = {}, contentDescription = "Dismiss") },
            secondaryButton = TangemMessageBanner.Button(text = stringReference("Label"), onClick = {}),
            primaryButton = TangemMessageBanner.Button(text = stringReference("Label"), onClick = {}),
        )
        TangemMessageBanner(
            modifier = Modifier.fillMaxWidth(),
            title = stringReference("Invite friends. Earn 10 USDT."),
            description = stringReference("Share Tangem, give 10% OFF, and earn 10 USDT."),
            slotStart = {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(TangemTheme.colors3.bg.tertiary),
                )
            },
            primaryButton = TangemMessageBanner.Button(text = stringReference("Invite friends"), onClick = {}),
        )
    }
}

@Preview(name = "Variants Light", showBackground = true)
@Preview(name = "Variants Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun TangemMessageBannerVariantsPreview() {
    PreviewContainer {
        TangemMessageBanner.Variant.entries.forEach { variant ->
            TangemMessageBanner(
                modifier = Modifier.fillMaxWidth(),
                variant = variant,
                title = stringReference(variant.name),
                description = stringReference("Description"),
                secondaryButton = TangemMessageBanner.Button(text = stringReference("Label"), onClick = {}),
                primaryButton = TangemMessageBanner.Button(text = stringReference("Label"), onClick = {}),
            )
        }
    }
}

@Preview(name = "Align Light", showBackground = true)
@Preview(name = "Align Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun TangemMessageBannerContentAlignPreview() {
    PreviewContainer {
        TangemMessageBanner.ContentAlign.entries.forEach { align ->
            TangemMessageBanner(
                modifier = Modifier.fillMaxWidth(),
                contentAlign = align,
                title = stringReference("Content align ${align.name}"),
                description = stringReference("Share Tangem, give 10% OFF, and earn 10 USDT."),
                secondaryButton = TangemMessageBanner.Button(text = stringReference("Later"), onClick = {}),
                primaryButton = TangemMessageBanner.Button(text = stringReference("Invite"), onClick = {}),
            )
        }
    }
}

@Composable
private fun PreviewContainer(content: @Composable ColumnScope.() -> Unit) {
    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .background(TangemTheme.colors3.bg.primary)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}