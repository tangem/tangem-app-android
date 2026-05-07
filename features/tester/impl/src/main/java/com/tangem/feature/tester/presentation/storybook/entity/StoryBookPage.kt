package com.tangem.feature.tester.presentation.storybook.entity

import com.tangem.core.ui.ds.badge.TangemBadgeColor
import com.tangem.core.ui.ds.field.search.TangemFieldShape
import com.tangem.core.ui.ds2.loader.TangemLoaderSize
import com.tangem.core.ui.ds.message.TangemMessageEffect
import com.tangem.core.ui.ds.topbar.TangemTopBarType

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