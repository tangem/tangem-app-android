package com.tangem.core.ui.components.pager

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.min

@Stable
internal class PagerIndicatorAnimationState(
    private val totalPages: Int,
    initialCurrentPage: Int,
    private val density: Density,
) {
    var displayLower by mutableIntStateOf(0)
        private set
    var displayUpper by mutableIntStateOf(0)
        private set

    val slideOffset = Animatable(0f)
    var isSliding by mutableStateOf(false)
        private set
    var slideDirection by mutableIntStateOf(0)
        private set
    val fadeProgress = Animatable(0f)
    private var fadeJob by mutableStateOf<Job?>(null)

    private var prevTargetLower by mutableIntStateOf(0)

    init {
        val (lower, upper) = getWindowBounds(totalPages, initialCurrentPage)
        displayLower = lower
        displayUpper = upper
        prevTargetLower = lower
    }

    suspend fun onBoundsChange(scope: CoroutineScope, targetLower: Int, targetUpper: Int) {
        if (targetLower == prevTargetLower || totalPages <= MAX_VISIBLE_DOTS) {
            return
        }

        fadeJob?.cancel()
        slideOffset.stop()
        fadeProgress.stop()

        val dir = if (targetLower > prevTargetLower) 1 else -1
        val edgeDotSize = with(density) { (HINT_DOT_SIZE.width + SPACING).toPx() }
        val halfEdge = edgeDotSize / 2

        isSliding = true
        slideDirection = dir
        fadeProgress.snapTo(0f)

        if (dir > 0) {
            displayUpper = targetUpper
            slideOffset.snapTo(halfEdge)
        } else {
            displayLower = targetLower
            displayUpper = prevTargetLower + MAX_VISIBLE_DOTS
            slideOffset.snapTo(-halfEdge)
        }

        prevTargetLower = targetLower

        fadeJob = scope.launch {
            fadeProgress.animateTo(1f, tween(ANIMATION_DURATION))
        }
        slideOffset.animateTo(
            if (dir > 0) -halfEdge else halfEdge,
            tween(ANIMATION_DURATION),
        )

        displayLower = targetLower
        displayUpper = targetUpper
        slideOffset.snapTo(0f)
        isSliding = false
        slideDirection = 0
    }
}

internal fun getWindowBounds(totalPages: Int, currentIndex: Int): Pair<Int, Int> {
    if (totalPages <= MAX_VISIBLE_DOTS) {
        return 0 to totalPages
    }
    val lowerBound = when {
        currentIndex <= 1 -> 0
        currentIndex >= totalPages - 2 -> totalPages - MAX_VISIBLE_DOTS
        else -> currentIndex - 2
    }
    val upperBound = min(lowerBound + MAX_VISIBLE_DOTS, totalPages)
    return lowerBound to upperBound
}