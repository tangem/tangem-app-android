package com.tangem.core.ui.components.stories.inner

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

private const val STORY_HOLD_DELAY = 200

@Composable
fun StoriesClickableArea(onPress: (Boolean) -> Unit, onPreviousStory: () -> Unit, onNextStory: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            val pressStartTime = System.currentTimeMillis()
                            onPress(true)
                            this.tryAwaitRelease()
                            val pressEndTime = System.currentTimeMillis()
                            val totalPressTime = pressEndTime - pressStartTime
                            if (totalPressTime < STORY_HOLD_DELAY) onPreviousStory()
                            onPress(false)
                        },
                    )
                },
        )
        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            val pressStartTime = System.currentTimeMillis()
                            onPress(true)
                            this.tryAwaitRelease()
                            val pressEndTime = System.currentTimeMillis()
                            val totalPressTime = pressEndTime - pressStartTime
                            if (totalPressTime < STORY_HOLD_DELAY) onNextStory()
                            onPress(false)
                        },
                    )
                },
        )
    }
}