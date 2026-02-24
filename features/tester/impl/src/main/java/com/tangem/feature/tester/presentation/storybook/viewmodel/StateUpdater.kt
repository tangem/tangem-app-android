package com.tangem.feature.tester.presentation.storybook.viewmodel

import com.tangem.feature.tester.presentation.storybook.entity.StoryBookPage
import com.tangem.feature.tester.presentation.storybook.entity.StoryPageFactory

internal interface StateUpdater<T> {
    fun updateStory(update: (T) -> T)
}

internal inline fun <reified T : StoryBookPage> storyPageFactory(
    crossinline build: StateUpdater<T>.() -> T,
): StoryPageFactory = StoryPageFactory { updatePage ->
    val updater = object : StateUpdater<T> {
        override fun updateStory(update: (T) -> T) {
            updatePage { current ->
                if (current is T) update(current) else current
            }
        }
    }
    updater.build()
}