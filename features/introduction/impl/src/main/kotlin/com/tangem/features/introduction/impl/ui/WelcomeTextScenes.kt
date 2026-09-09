package com.tangem.features.introduction.impl.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme

/**
 * The scenes are composed on top of each other and only their alpha and offset move, so a scene change never
 * recomposes anything: the position is read inside the draw phase. Without a playing video the first scene stays.
 */
@Composable
internal fun WelcomeTextScenes(isAnimated: Boolean, videoPositionMs: () -> Long, modifier: Modifier = Modifier) {
    val positionMs = remember { mutableLongStateOf(videoPositionMs()) }
    if (isAnimated) {
        LaunchedEffect(Unit) {
            while (true) {
                withFrameMillis { positionMs.longValue = videoPositionMs() }
            }
        }
    }
    val activeScene by remember(isAnimated) {
        derivedStateOf { if (isAnimated) WelcomeTextTimeline.activeScene(positionMs.longValue) else 0 }
    }
    fun frameOf(scene: Int, line: Int): WelcomeTextTimeline.Frame = when {
        isAnimated -> WelcomeTextTimeline.frame(positionMs = positionMs.longValue, scene = scene, line = line)
        scene == 0 -> WelcomeTextTimeline.steady
        else -> WelcomeTextTimeline.hidden
    }
    Box(modifier = modifier) {
        SceneTexts.forEachIndexed { scene, texts ->
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .then(if (scene == activeScene) Modifier else Modifier.clearAndSetSemantics {}),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SceneLine(
                    text = texts.title,
                    color = TangemTheme.colors3.text.staticDark.primary,
                    frame = { frameOf(scene, line = 0) },
                )
                SceneLine(
                    text = texts.subtitle,
                    color = TangemTheme.colors3.text.staticDark.secondary,
                    frame = { frameOf(scene, line = 1) },
                )
            }
        }
    }
}

@Composable
private fun SceneLine(text: TextReference, color: Color, frame: () -> WelcomeTextTimeline.Frame) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                val current = frame()
                alpha = current.alpha
                translationY = current.offsetDp.dp.toPx()
            },
        text = text.resolveReference(),
        style = TangemTheme.typography3.heading.medium,
        color = color,
    )
}

private data class SceneText(val title: TextReference, val subtitle: TextReference)

private val SceneTexts = listOf(
    SceneText(
        title = resourceReference(R.string.introduction_title),
        subtitle = resourceReference(R.string.introduction_subtitle),
    ),
    SceneText(
        title = resourceReference(R.string.introduction_scene_two_title),
        subtitle = resourceReference(R.string.introduction_scene_two_subtitle),
    ),
    SceneText(
        title = resourceReference(R.string.introduction_scene_three_title),
        subtitle = resourceReference(R.string.introduction_scene_three_subtitle),
    ),
)