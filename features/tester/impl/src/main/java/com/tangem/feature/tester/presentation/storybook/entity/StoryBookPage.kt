package com.tangem.feature.tester.presentation.storybook.entity

import com.tangem.core.ui.ds.badge.TangemBadgeColor
import com.tangem.core.ui.ds.message.TangemMessageEffect

internal sealed interface StoryBookPage

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