package com.tangem.feature.tester.presentation.storybook.entity

import androidx.compose.runtime.Immutable
import androidx.compose.ui.state.ToggleableState
import com.tangem.core.ui.ds.badge.TangemBadgeColor
import com.tangem.core.ui.ds.field.search.TangemFieldShape
import com.tangem.core.ui.ds.message.TangemMessageEffect
import com.tangem.core.ui.ds.topbar.TangemTopBarType
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.fade.TangemFade
import com.tangem.core.ui.ds2.loader.TangemLoaderSize
import com.tangem.core.ui.ds2.row.TangemRowContentLead
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation

internal sealed interface StoryBookPage

/**
 * Marker for pages that live inside the DS components sub-list.
 * The view model uses it to route back navigation to the DS list
 * instead of the root [StoryList].
 */
internal sealed interface DsStoryBookPage : StoryBookPage

internal data object StoryList : StoryBookPage

internal data object ButtonsStory : StoryBookPage

internal data class TangemBadgeStory(
    val selectedColor: TangemBadgeColor,
    val onColorChange: (TangemBadgeColor) -> Unit,
) : StoryBookPage

internal data object OpportunitiesBGStory : StoryBookPage

internal data class TangemCheckboxStory(
    val isRoundedChecked: Boolean,
    val onRoundedCheckedChange: (Boolean) -> Unit,
    val isCircleChecked: Boolean,
    val onCircleCheckedChange: (Boolean) -> Unit,
) : StoryBookPage

internal data object TangemSegmentedPickerStory : StoryBookPage

internal data class TangemMessageStory(
    val selectedEffect: TangemMessageEffect,
    val onEffectChange: (TangemMessageEffect) -> Unit,
) : StoryBookPage

internal data class NorthernLightsStory(
    val variant: Variant,
    val onVariantChange: (Variant) -> Unit,
) : StoryBookPage {
    enum class Variant {
        Shader,
        Simple,
    }
}

internal data class TangemTokenRowStory(
    val isBalanceHidden: Boolean,
    val onBalanceHiddenToggle: () -> Unit,
) : StoryBookPage

internal data class TangemContextMenuStory(
    val isExpanded: Boolean,
    val onExpandedChange: (Boolean) -> Unit,
) : StoryBookPage

internal data class TangemHeaderRowStory(
    val isBalanceHidden: Boolean,
    val onBalanceHiddenToggle: () -> Unit,
) : StoryBookPage

internal data class TangemSearchFieldStory(
    val selectedShape: TangemFieldShape,
    val onShapeChange: (TangemFieldShape) -> Unit,
) : StoryBookPage

internal data class TypographyStory(
    val isFontScaleDefault: Boolean,
    val onFontScaleToggle: () -> Unit,
) : StoryBookPage

internal data class TangemTopBarStory(
    val selectedType: TangemTopBarType,
    val onTypeChange: (TangemTopBarType) -> Unit,
) : StoryBookPage

internal data class TangemTabStory(
    val checkedIndex: Int,
    val onCheckedIndexChange: (Int) -> Unit,
) : StoryBookPage

internal data object TangemPagerIndicatorStory : StoryBookPage

internal data object PlaceholderStory : StoryBookPage

internal data object ProgressIndicatorStory : StoryBookPage

internal data object DeviceIconStory : StoryBookPage

internal data class DsComponentsListStory(
    val onStoryClick: (StoryPageFactory) -> Unit,
) : StoryBookPage

internal data class TangemLoaderStory(
    val selectedSize: TangemLoaderSize,
    val onSizeChange: (TangemLoaderSize) -> Unit,
) : DsStoryBookPage

@Immutable
internal data class TangemShimmerStory(
    val textStyle: TextStyleOption,
    val textPosition: TextPositionOption,
    val radius: RadiusOption,
    val rectangleWidth: RectangleWidthOption,
    val rectangleHeight: RectangleHeightOption,
    val onTextStyleChange: (TextStyleOption) -> Unit,
    val onTextPositionChange: (TextPositionOption) -> Unit,
    val onRadiusChange: (RadiusOption) -> Unit,
    val onRectangleWidthChange: (RectangleWidthOption) -> Unit,
    val onRectangleHeightChange: (RectangleHeightOption) -> Unit,
) : DsStoryBookPage {

    /** Selectable typography preset for the `TangemShimmer` text variant. */
    enum class TextStyleOption {
        DISPLAY,
        HEADING_MEDIUM,
        HEADING_SMALL,
        BODY,
        SUBHEADING,
        CAPTION,
    }

    /** Horizontal position of the `TangemShimmer` text block within the parent width. */
    enum class TextPositionOption(val label: String) {
        START("Start"),
        CENTER("Center"),
        END("End"),
    }

    /** Selectable corner radius (matches `borderRadius` tokens). */
    enum class RadiusOption(val label: String) {
        R4("4dp"),
        R8("8dp"),
        R16("16dp"),
        R24("24dp"),
        R32("32dp"),
        FULL("full"),
    }

    enum class RectangleWidthOption(val label: String) {
        W80("80dp"),
        W160("160dp"),
        W240("240dp"),
        FILL("fill"),
    }

    enum class RectangleHeightOption(val label: String) {
        H16("16dp"),
        H24("24dp"),
        H40("40dp"),
        H64("64dp"),
    }
}

internal data class TangemButtonStory(
    val variant: TangemButton.Variant,
    val size: TangemButton.Size,
    val background: Background,
    val isLoading: Boolean,
    val isEnabled: Boolean,
    val hasIconStart: Boolean,
    val hasIconEnd: Boolean,
    val hasText: Boolean,
    val isBlurEnabled: Boolean,
    val textScale: Float,
    val onVariantChange: (TangemButton.Variant) -> Unit,
    val onSizeChange: (TangemButton.Size) -> Unit,
    val onBackgroundChange: (Background) -> Unit,
    val onLoadingToggle: () -> Unit,
    val onEnabledToggle: () -> Unit,
    val onIconStartToggle: () -> Unit,
    val onIconEndToggle: () -> Unit,
    val onTextToggle: () -> Unit,
    val onBlurToggle: () -> Unit,
    val onTextScaleChange: (Float) -> Unit,
) : DsStoryBookPage {

    /** Backdrop the button preview is rendered on top of. */
    enum class Background(val label: String) {
        Rainbow("rainbow"),
        BgPrimary("bg.primary"),
        BgSecondary("bg.secondary"),
        BgBrand("bg.brand"),
        BgInverse("bg.inverse"),
    }
}

@Suppress("BooleanPropertyNaming")
internal data class TangemRowStory(
    val contentLead: TangemRowContentLead,
    val verticalAlignment: TangemRowVerticalAlignment,
    val background: Background,
    val divider: Boolean,
    val includeInnerPaddings: Boolean,
    val isClickable: Boolean,
    val hasStartSlot: Boolean,
    val hasEndSlot: Boolean,
    val hasSubtitle: Boolean,
    val hasValue: Boolean,
    val hasSubvalue: Boolean,
    val hasExtraBottom: Boolean,
    val longTitle: Boolean,
    val textScale: Float,
    val onContentLeadChange: (TangemRowContentLead) -> Unit,
    val onVerticalAlignmentChange: (TangemRowVerticalAlignment) -> Unit,
    val onBackgroundChange: (Background) -> Unit,
    val onDividerToggle: () -> Unit,
    val onInnerPaddingsToggle: () -> Unit,
    val onClickableToggle: () -> Unit,
    val onStartSlotToggle: () -> Unit,
    val onEndSlotToggle: () -> Unit,
    val onSubtitleToggle: () -> Unit,
    val onValueToggle: () -> Unit,
    val onSubvalueToggle: () -> Unit,
    val onExtraBottomToggle: () -> Unit,
    val onLongTitleToggle: () -> Unit,
    val onTextScaleChange: (Float) -> Unit,
) : DsStoryBookPage {

    /** Backdrop the row preview is rendered on top of. */
    enum class Background(val label: String) {
        BgPrimary("bg.primary"),
        BgSecondary("bg.secondary"),
        BgBrand("bg.brand"),
        BgInverse("bg.inverse"),
    }
}
internal data class TangemFadeStory(
    val variant: TangemFade.Variant,
    val position: TangemFade.Position,
    val isBlur: Boolean,
    val onVariantChange: (TangemFade.Variant) -> Unit,
    val onPositionChange: (TangemFade.Position) -> Unit,
    val onBlurToggle: () -> Unit,
) : DsStoryBookPage

@Suppress("BooleanPropertyNaming")
internal data class TangemTopNavigationStory(
    val contentAlign: TangemTopNavigation.ContentAlign,
    val background: Background,
    val contentMode: ContentMode,
    val hasBack: Boolean,
    val hasSubtitle: Boolean,
    val longTitle: Boolean,
    val endButton: EndButton,
    val endGroup: EndGroup,
    val useStatusBarInsets: Boolean,
    val isBlurEnabled: Boolean,
    val onContentAlignChange: (TangemTopNavigation.ContentAlign) -> Unit,
    val onBackgroundChange: (Background) -> Unit,
    val onContentModeChange: (ContentMode) -> Unit,
    val onBackToggle: () -> Unit,
    val onSubtitleToggle: () -> Unit,
    val onLongTitleToggle: () -> Unit,
    val onEndButtonChange: (EndButton) -> Unit,
    val onEndGroupChange: (EndGroup) -> Unit,
    val onStatusBarInsetsToggle: () -> Unit,
    val onBlurToggle: () -> Unit,
) : DsStoryBookPage {

    /**
     * What goes inside the center `contentColumn` slot.
     *
     * - [Plain] — basic Title / Subtitle text, drives the `hasSubtitle` + `longTitle` toggles.
     * - [Rich]  — `AnnotatedString` with multi-color spans, an inline swap emoji, and emoji-only
     *   second line. Demonstrates that the slot accepts arbitrary composables, not just plain text.
     */
    enum class ContentMode(val label: String) {
        Plain("plain"),
        Rich("rich"),
    }

    /** Backdrop the navigation preview sits on, to verify haze/blur behavior. */
    enum class Background(val label: String) {
        Rainbow("rainbow"),
        BgPrimary("bg.primary"),
        BgSecondary("bg.secondary"),
        BgBrand("bg.brand"),
        BgInverse("bg.inverse"),
    }

    /** Trailing slot variant — None / Close / Loader / Custom pill ("How it works?"). */
    enum class EndButton(val label: String) {
        None("none"),
        Close("close"),
        Loader("loader"),
        HowItWorks("How it works?"),
    }

    /** Secondary-actions pill: 0..3 icon buttons. */
    enum class EndGroup(val label: String) {
        None("0"),
        One("1"),
        Two("2"),
        Three("3"),
    }
}

@Suppress("BooleanPropertyNaming")
internal data class TangemSearchStory(
    val background: Background,
    val placeholder: Placeholder,
    val hasCloseButton: Boolean,
    val onBackgroundChange: (Background) -> Unit,
    val onPlaceholderChange: (Placeholder) -> Unit,
    val onCloseButtonToggle: () -> Unit,
) : DsStoryBookPage {

    /** Backdrop the search preview is rendered on top of. */
    enum class Background(val label: String) {
        Rainbow("rainbow"),
        BgPrimary("bg.primary"),
        BgSecondary("bg.secondary"),
        BgBrand("bg.brand"),
        BgInverse("bg.inverse"),
    }

    /** Placeholder length variants — short typical label vs. long string to test layout. */
    enum class Placeholder(val label: String, val text: String) {
        Short("short", "Search"),
        Medium("medium", "Filter tokens"),
        Long("long", "Search by name, symbol or contract address"),
    }
}

internal data class TangemCheckboxV2Story(
    val state: ToggleableState,
    val isEnabled: Boolean,
    val onStateChange: (ToggleableState) -> Unit,
    val onEnabledToggle: () -> Unit,
) : DsStoryBookPage

internal data class TangemCheckmarkStory(
    val isChecked: Boolean,
    val isEnabled: Boolean,
    val onCheckedChange: (Boolean) -> Unit,
    val onEnabledToggle: () -> Unit,
) : DsStoryBookPage

internal data class TangemBadgeV2Story(
    val variant: TangemBadge.Variant,
    val status: TangemBadge.Status,
    val size: TangemBadge.Size,
    val background: Background,
    val hasIconStart: Boolean,
    val hasIconEnd: Boolean,
    val textScale: Float,
    val onVariantChange: (TangemBadge.Variant) -> Unit,
    val onStatusChange: (TangemBadge.Status) -> Unit,
    val onSizeChange: (TangemBadge.Size) -> Unit,
    val onBackgroundChange: (Background) -> Unit,
    val onIconStartToggle: () -> Unit,
    val onIconEndToggle: () -> Unit,
    val onTextScaleChange: (Float) -> Unit,
) : DsStoryBookPage {

    /** Backdrop the badge preview is rendered on top of. */
    enum class Background(val label: String) {
        Rainbow("rainbow"),
        BgPrimary("bg.primary"),
        BgSecondary("bg.secondary"),
        BgBrand("bg.brand"),
        BgInverse("bg.inverse"),
    }
}