package com.tangem.feature.tester.presentation.storybook.entity

internal fun interface StoryPageFactory {
    fun create(updatePage: ((StoryBookPage) -> StoryBookPage) -> Unit): StoryBookPage
}