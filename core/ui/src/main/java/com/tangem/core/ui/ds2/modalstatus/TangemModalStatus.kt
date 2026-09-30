@file:Suppress("MagicNumber")

package com.tangem.core.ui.ds2.modalstatus

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.DsVersion
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.loader.TangemLoader
import com.tangem.core.ui.ds2.loader.TangemLoaderSize
import com.tangem.core.ui.ds2.modalstatus.TangemModalStatus.Avatar
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveAnnotatedReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.*
import com.tangem.core.ui.test.ModalStatusTestTags

/**
 * Design-system v2 modal status block: a round status asset, a centered title with an optional
 * description and badge, and optional detail rows — the body of a status modal. The modal sibling of
 * [com.tangem.core.ui.ds2.statusscreen.TangemStatusScreen]; place it in a
 * [com.tangem.core.ui.ds2.modal.TangemModal] with the actions in a footer.
 *
 * [Figma](https://www.figma.com/design/AsnJ5CPHib4Qxw12gszjMS/%F0%9F%92%A0-DS-Components?node-id=10884-41257)
 *
 * Behavior notes:
 * - [variant] only colors the default asset ([TangemModalStatus.Avatar]); the texts, badge and details
 *   look the same in every variant. A custom [asset] ignores [variant] entirely.
 * - [TangemModalStatus.TitleStyle.Amount] enlarges the title for a money amount (`-$166.84`) and
 *   lifts the description to subheading size; the layout does not change.
 * - The block is as tall as its content and does not scroll — the hosting modal scrolls it.
 *
 * Accessibility: the default asset is decorative (`contentDescription = null`) because the title
 * announces the status; give a custom [asset] a description only if it carries information the title
 * does not.
 *
 * @param variant Status mood — picks the default asset's background, glyph and tint.
 * @param title Headline, centered.
 * @param modifier Modifier for the root container.
 * @param description Secondary line under the title; `null` hides it.
 * @param titleStyle Text scale of the title block. See [TangemModalStatus.TitleStyle].
 * @param asset Slot above the title; `null` shows the [variant]'s [TangemModalStatus.Avatar]. Pass a
 *   72dp element (a token icon, a merchant logo) to replace it.
 * @param extraBottom Slot under the texts, typically a [TangemBadge]; `null` hides it.
 * @param details Slot under the header for Title—Value rows or cards, laid out full width; `null`
 *   hides it.
 */
@Suppress("LongParameterList")
@Composable
fun TangemModalStatus(
    variant: TangemModalStatus.Variant,
    title: TextReference,
    modifier: Modifier = Modifier,
    description: TextReference? = null,
    titleStyle: TangemModalStatus.TitleStyle = TangemModalStatus.TitleStyle.Status,
    asset: (@Composable () -> Unit)? = null,
    extraBottom: (@Composable () -> Unit)? = null,
    details: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (asset != null) asset() else TangemModalStatus.Avatar(variant = variant)
            StatusTitle(title = title, description = description, style = titleStyle)
            extraBottom?.invoke()
        }
        if (details != null) {
            Column(modifier = Modifier.fillMaxWidth(), content = details)
        }
    }
}

@Composable
private fun StatusTitle(title: TextReference, description: TextReference?, style: TangemModalStatus.TitleStyle) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(ModalStatusTestTags.TITLE),
            text = title.resolveReference(),
            style = style.titleTextStyle(),
            color = TangemTheme.colors3.text.primary,
            textAlign = TextAlign.Center,
        )
        if (description != null) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(ModalStatusTestTags.DESCRIPTION),
                text = description.resolveAnnotatedReference(),
                style = style.descriptionTextStyle(),
                color = TangemTheme.colors3.text.secondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

object TangemModalStatus {

    /** Public API surface of [TangemModalStatus]. */
    val version = DsVersion(major = 1, minor = 0, patch = 0)

    /** Status mood of the block — drives the default asset's colors and glyph. */
    enum class Variant {
        /** In progress or undetermined: a spinner on a neutral fill. */
        Neutral,
        Success,
        Error,
        Warning,
        Info,
        ;

        @ReadOnlyComposable
        @Composable
        internal fun backgroundColor(): Color = when (this) {
            Neutral -> TangemTheme.colors3.bg.opaque.primary
            Success -> TangemTheme.colors3.bg.status.successSubtle
            Error -> TangemTheme.colors3.bg.status.errorSubtle
            Warning -> TangemTheme.colors3.bg.status.warningSubtle
            Info -> TangemTheme.colors3.bg.status.infoSubtle
        }

        @ReadOnlyComposable
        @Composable
        internal fun tint(): Color = when (this) {
            Neutral -> TangemTheme.colors3.icon.secondary
            Success -> TangemTheme.colors3.icon.status.success
            Error -> TangemTheme.colors3.icon.status.error
            Warning -> TangemTheme.colors3.icon.status.warning
            Info -> TangemTheme.colors3.icon.status.info
        }

        /** `null` for [TangemModalStatus.Variant.Neutral], which shows a spinner instead of a static glyph. */
        internal fun glyph(): ImageVector? = when (this) {
            Neutral -> null
            Success -> Icons.ic_checkmark_28
            Error -> Icons.ic_heart_broken_28
            Warning -> Icons.ic_warning_28
            Info -> Icons.ic_info_28
        }
    }

    /** Text scale of the title block. */
    enum class TitleStyle {
        /** A status headline (`KYC in progress`). */
        Status,

        /** A money amount as the headline (`-$166.84`), with a larger description under it. */
        Amount,

        ;

        @ReadOnlyComposable
        @Composable
        internal fun titleTextStyle(): TextStyle = when (this) {
            Status -> TangemTheme.typography3.heading.small
            Amount -> TangemTheme.typography3.display.medium
        }

        @ReadOnlyComposable
        @Composable
        internal fun descriptionTextStyle(): TextStyle = when (this) {
            Status -> TangemTheme.typography3.caption.medium
            Amount -> TangemTheme.typography3.subheading.medium
        }
    }

    /**
     * Default asset of [TangemModalStatus]: a 72dp circle in the [variant]'s subtle status fill with its
     * glyph — a spinner for [Variant.Neutral], a static status icon otherwise. Decorative for
     * accessibility; the title carries the status.
     *
     * @param variant Status mood that picks the fill, glyph and tint.
     * @param modifier Modifier for the circle.
     */
    @Composable
    fun Avatar(variant: Variant, modifier: Modifier = Modifier) {
        AvatarCircle(variant = variant, modifier = modifier) {
            val glyph = variant.glyph()
            if (glyph == null) {
                TangemLoader(color = variant.tint(), size = TangemLoaderSize.X32)
            } else {
                Icon(
                    modifier = Modifier.size(28.dp),
                    imageVector = glyph,
                    contentDescription = null,
                    tint = variant.tint(),
                )
            }
        }
    }

    /**
     * [Avatar] with a caller-supplied [glyph] instead of the variant's own — for statuses whose icon is
     * specific to the message (a shield, a card) but should still read as that [variant]. Decorative for
     * accessibility, like the default avatar.
     *
     * @param variant Status mood that picks the fill and the default [tint].
     * @param imageVector 28dp icon drawn in the middle of the circle.
     * @param modifier Modifier for the circle.
     * @param tint Glyph tint; defaults to the [variant]'s status color. Pass [Color.Unspecified] to keep a
     *   multicolor drawable's own colors.
     */
    @Composable
    fun Avatar(
        variant: Variant,
        imageVector: ImageVector,
        modifier: Modifier = Modifier,
        tint: Color = variant.tint(),
    ) {
        AvatarCircle(variant = variant, modifier = modifier) {
            Icon(
                modifier = Modifier.size(28.dp),
                imageVector = imageVector,
                contentDescription = null,
                tint = tint,
            )
        }
    }
}

@Composable
private fun AvatarCircle(
    variant: TangemModalStatus.Variant,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(72.dp)
            .background(color = variant.backgroundColor(), shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 386)
@Preview(showBackground = true, widthDp = 386, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemModalStatusPreview(@PreviewParameter(VariantProvider::class) variant: TangemModalStatus.Variant) {
    TangemThemePreviewRedesign {
        TangemModalStatus(
            modifier = Modifier.background(TangemTheme.colors3.bg.primary),
            variant = variant,
            title = stringReference(value = "Title"),
            description = stringReference(value = "Description"),
        )
    }
}

@Preview(showBackground = true, widthDp = 386)
@Preview(showBackground = true, widthDp = 386, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemModalStatusAmountPreview() {
    TangemThemePreviewRedesign {
        TangemModalStatus(
            modifier = Modifier.background(TangemTheme.colors3.bg.primary),
            variant = TangemModalStatus.Variant.Error,
            title = stringReference(value = "-$166.84"),
            description = stringReference(value = "Will Ethereum reach $5,000 before Sep 30?"),
            titleStyle = TangemModalStatus.TitleStyle.Amount,
            extraBottom = {
                TangemBadge(
                    text = stringReference(value = "Good luck next time"),
                    status = TangemBadge.Status.Error,
                )
            },
            details = {
                repeat(times = 3) { index ->
                    TangemRow(
                        divider = index < 2,
                        titleSlot = {
                            Text(
                                text = "Title",
                                style = TangemTheme.typography3.body.medium,
                                color = TangemTheme.colors3.text.primary,
                            )
                        },
                        valueSlot = {
                            Text(
                                text = "Value",
                                style = TangemTheme.typography3.body.medium,
                                color = TangemTheme.colors3.text.secondary,
                            )
                        },
                    )
                }
            },
        )
    }
}

private class VariantProvider : CollectionPreviewParameterProvider<TangemModalStatus.Variant>(
    collection = TangemModalStatus.Variant.entries,
)
// endregion