package com.tangem.core.ui.components.stories.inner

import androidx.compose.runtime.IntState
import androidx.compose.runtime.asIntState
import androidx.compose.runtime.mutableIntStateOf
import kotlinx.collections.immutable.ImmutableList

class StoriesStepStateMachine<T>(
    private val stories: ImmutableList<T>,
    private val isRepeatable: Boolean,
) {

    private var _currentIndex = mutableIntStateOf(FIRST_INDEX)

    val steps = stories.lastIndex
    val currentIndex: IntState
        get() = _currentIndex.asIntState()

    val currentStory
        get() = stories[currentIndex.intValue.coerceIn(FIRST_INDEX, steps)]

    fun nextStory() {
        _currentIndex.intValue = if (currentIndex.intValue == steps && isRepeatable) {
            FIRST_INDEX
        } else {
            currentIndex.intValue.inc().coerceAtMost(stories.size)
        }
    }

    fun prevStory() {
        _currentIndex.intValue = currentIndex.intValue.dec().coerceAtLeast(FIRST_INDEX)
    }

    fun hasNext(): Boolean {
        return currentIndex.intValue <= steps
    }

    private companion object {
        const val FIRST_INDEX = 0
    }
}