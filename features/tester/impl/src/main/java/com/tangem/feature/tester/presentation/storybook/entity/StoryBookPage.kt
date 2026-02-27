package com.tangem.feature.tester.presentation.storybook.entity

internal sealed interface StoryBookPage

internal data object StoryList : StoryBookPage

internal data object ButtonsStory : StoryBookPage

internal data class NorthernLightsStory(
    val variant: Variant,
    val onVariantChange: (Variant) -> Unit,
) : StoryBookPage {
    enum class Variant {
        Shader,
        Simple,
    }
}