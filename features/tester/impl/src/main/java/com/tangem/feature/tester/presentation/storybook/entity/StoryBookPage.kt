package com.tangem.feature.tester.presentation.storybook.entity

import androidx.compose.runtime.Immutable
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
import com.tangem.core.ui.ds2.shimmers.TextShimmerStyle

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
    val textStyle: TextShimmerStyle,
    val radius: RadiusOption,
    val rectangleWidth: RectangleWidthOption,
    val rectangleHeight: RectangleHeightOption,
    val onTextStyleChange: (TextShimmerStyle) -> Unit,
    val onRadiusChange: (RadiusOption) -> Unit,
    val onRectangleWidthChange: (RectangleWidthOption) -> Unit,
    val onRectangleHeightChange: (RectangleHeightOption) -> Unit,
) : DsStoryBookPage {

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