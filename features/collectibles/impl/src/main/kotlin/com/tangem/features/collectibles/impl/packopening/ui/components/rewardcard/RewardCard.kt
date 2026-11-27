package com.tangem.features.collectibles.impl.packopening.ui.components.rewardcard

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.res.LocalHazeState
import com.tangem.core.ui.res.LocalMaterialShadowEnabled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.collectibles.impl.common.ui.rewardcard.DEFAULT_CARD_ASPECT
import com.tangem.features.collectibles.impl.common.ui.rewardcard.RewardCardTiming
import com.tangem.features.collectibles.impl.common.ui.rewardcard.RewardCase
import com.tangem.features.collectibles.impl.common.ui.rewardcard.rewardCaseCameraDistance
import com.tangem.features.collectibles.impl.common.ui.rewardcard.rewardCaseSize
import com.tangem.features.collectibles.impl.common.ui.rewardcard.state.RewardCardProgress
import com.tangem.features.collectibles.impl.packopening.ui.state.CollectiblesPackOpeningUM.SellBackButtonUM
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.tangem.features.collectibles.impl.packopening.ui.state.RewardCardUM

/** Easings from the AE handoff (`Gacha_Card_Reveal_motion.json`). */
private val APPEAR_EASING = CubicBezierEasing(a = 0.30f, b = 0.26f, c = 0.14f, d = 1.00f)
private val GROW_EASING = CubicBezierEasing(a = 0.25f, b = 0.00f, c = 0.20f, d = 1.00f)
private val SETTLE_EASING = CubicBezierEasing(a = 0.80f, b = 0.00f, c = 0.20f, d = 1.00f)
private val SPIN_EASING = CubicBezierEasing(a = 0.60f, b = 0.00f, c = 0.10f, d = 1.00f)

/** Gaps above and under the case at their design size; spare height is split between them in this ratio. */
private val TOP_GAP = 24.dp
private val CASE_GAP = 32.dp

private enum class RewardSlot { Case, Title, Footer }

@Composable
internal fun RewardCard(
    um: RewardCardUM,
    sellBack: SellBackButtonUM,
    timing: RewardCardTiming,
    onKeepCardClick: () -> Unit,
    onSellBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardAspect: Float? = null,
    isKeepCardEnabled: Boolean = true,
) {
    RewardCardContent(
        um = um,
        sellBack = sellBack,
        isKeepCardEnabled = isKeepCardEnabled,
        timing = timing,
        progress = rememberRewardCardAnimation(timing),
        cardAspect = cardAspect,
        onKeepCardClick = onKeepCardClick,
        onSellBackClick = onSellBackClick,
        modifier = modifier,
    )
}

@Composable
private fun rememberRewardCardAnimation(timing: RewardCardTiming): RewardCardProgress {
    val grow = remember { Animatable(0f) }
    val settle = remember { Animatable(0f) }
    val spin = remember { Animatable(0f) }
    val appear = remember { Animatable(0f) }
    val chrome = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // The card, the title and the footer start together; the chrome's own curves come off a linear clock
        launch { appear.animateTo(1f, tween(timing.appearMs, easing = APPEAR_EASING)) }
        launch { chrome.animateTo(1f, tween(timing.chromeMs, easing = LinearEasing)) }
        launch { spin.animateTo(1f, tween(timing.spinMs, easing = SPIN_EASING)) }

        grow.animateTo(1f, tween(timing.growMs, easing = GROW_EASING))
        settle.animateTo(1f, tween(timing.settleMs, easing = SETTLE_EASING))
    }

    return RewardCardProgress(
        grow = grow.value,
        settle = settle.value,
        spin = spin.value,
        appear = appear.value,
        chrome = chrome.value,
    )
}

@Composable
internal fun RewardCardContent(
    um: RewardCardUM,
    sellBack: SellBackButtonUM,
    isKeepCardEnabled: Boolean,
    timing: RewardCardTiming,
    progress: RewardCardProgress,
    modifier: Modifier = Modifier,
    cardAspect: Float? = null,
    onKeepCardClick: () -> Unit = {},
    onSellBackClick: () -> Unit = {},
) {
    val containerColor = if (progress.settle > 0f) TangemTheme.colors3.bg.primary else Color.Transparent
    CompositionLocalProvider(LocalMaterialShadowEnabled provides (progress.settle >= 1f)) {
        TangemTopBarScaffold(
            modifier = modifier,
            containerColor = containerColor,
            topBar = {
                TangemTopNavigation(
                    // Holds the bar at its height with a button, which the layout is designed against
                    startButton = { Spacer(modifier = Modifier.size(44.dp)) },
                    // Nothing scrolls under the bar here, and its fade would cover the video during the reveal
                    fadeEnabled = false,
                )
            },
        ) { contentPadding ->
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val cameraDistance = rewardCaseCameraDistance(widthPx = constraints.maxWidth)
                val caseBackdrop = rememberHazeState()

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .hazeSourceTangem(state = caseBackdrop)
                        .background(containerColor),
                )

                RewardLayout(
                    isChromeShown = progress.chrome > 0f,
                    cardAspect = cardAspect ?: DEFAULT_CARD_ASPECT,
                    modifier = Modifier.padding(top = contentPadding.calculateTopPadding()),
                    case = {
                        CompositionLocalProvider(LocalHazeState provides caseBackdrop) {
                            RewardCase(
                                frontImageUrl = um.frontImageUrl,
                                backImageUrl = um.backImageUrl,
                                accent = um.accent,
                                timing = timing,
                                progress = progress,
                                cameraDistance = cameraDistance,
                            )
                        }
                    },
                    title = { RewardTitle(um = um, progress = progress.chrome) },
                    footer = {
                        RewardFooter(
                            sellBack = sellBack,
                            isKeepCardEnabled = isKeepCardEnabled,
                            progress = progress.chrome,
                            onKeepCardClick = onKeepCardClick,
                            onSellBackClick = onSellBackClick,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun RewardLayout(
    isChromeShown: Boolean,
    cardAspect: Float,
    modifier: Modifier = Modifier,
    case: @Composable () -> Unit,
    title: @Composable () -> Unit,
    footer: @Composable () -> Unit,
) {
    Layout(
        modifier = modifier.fillMaxSize(),
        content = {
            Box(modifier = Modifier.layoutId(RewardSlot.Case)) { case() }
            Box(modifier = Modifier.layoutId(RewardSlot.Title)) { title() }
            Box(modifier = Modifier.layoutId(RewardSlot.Footer)) { footer() }
        },
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val titlePlaceable = measurables.first { it.layoutId == RewardSlot.Title }.measure(loose)
        val footerPlaceable = measurables.first { it.layoutId == RewardSlot.Footer }.measure(loose)

        val chromeHeight = titlePlaceable.height + footerPlaceable.height
        val minGaps = (TOP_GAP + CASE_GAP).roundToPx()
        val caseSize = rewardCaseSize(
            maxWidth = constraints.maxWidth.toDp(),
            maxHeight = (constraints.maxHeight - chromeHeight - minGaps).toDp(),
            cardAspect = cardAspect,
        )
        val casePlaceable = measurables.first { it.layoutId == RewardSlot.Case }.measure(
            Constraints.fixed(width = caseSize.width.roundToPx(), height = caseSize.height.roundToPx()),
        )

        val gaps = (constraints.maxHeight - casePlaceable.height - chromeHeight).coerceAtLeast(0)
        val topGap = gaps * TOP_GAP.value / (TOP_GAP + CASE_GAP).value

        layout(constraints.maxWidth, constraints.maxHeight) {
            casePlaceable.place(x = (constraints.maxWidth - casePlaceable.width) / 2, y = topGap.roundToInt())

            // Laid out from the start so the case never jumps; left unplaced while hidden so it takes no taps
            if (isChromeShown) {
                titlePlaceable.place(
                    x = (constraints.maxWidth - titlePlaceable.width) / 2,
                    y = gaps + casePlaceable.height,
                )
                footerPlaceable.place(
                    x = (constraints.maxWidth - footerPlaceable.width) / 2,
                    y = constraints.maxHeight - footerPlaceable.height,
                )
            }
        }
    }
}