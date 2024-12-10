@file:Suppress("MagicNumber", "UnnecessaryParentheses", "CyclomaticComplexMethod", "LongMethod")

package com.tangem.features.onboarding.v2.multiwallet.impl.ui

import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.onboarding.v2.impl.R
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

sealed class WalletArtworksState {
    data object Hidden : WalletArtworksState()

    data object Folded : WalletArtworksState()

    data object Fan : WalletArtworksState()

    data class Stack(
        val threeCards: Boolean = true,
    ) : WalletArtworksState()

    data class Unfold(
        val step: Step = Step.First,
    ) : WalletArtworksState() {
        enum class Step {
            First, Second, Third
        }
    }

    data class Leapfrog(
        val threeCards: Boolean = true,
        val step: Step = Step.Second,
    ) : WalletArtworksState() {
        enum class Step {
            Second, Third
        }
    }
}

@Composable
fun WalletArtworks(
    url1: String?,
    url2: String?,
    url3: String?,
    state: WalletArtworksState,
    modifier: Modifier = Modifier,
) {
    if (state is WalletArtworksState.Hidden) return

    BoxWithConstraints(
        modifier
            .heightIn(min = 180.dp)
            .widthIn(max = 450.dp),
    ) {
        val animationWidth = maxWidth
        val density = LocalDensity.current.density

        var animationState by remember {
            mutableStateOf(
                state.toTransitionSetState(
                    maxWidthDp = animationWidth.value,
                    density = density,
                    maxHeightDp = maxHeight.value,
                ).first(),
            )
        }

        val transition1 = updateTransition(animationState.walletCard1, label = "transition")
        val transition2 = updateTransition(animationState.walletCard2, label = "transition")
        val transition3 = updateTransition(animationState.walletCard3, label = "transition")

        LaunchedEffect(state) {
            val animationSetState = state.toTransitionSetState(
                maxWidthDp = animationWidth.value,
                density = density,
                maxHeightDp = maxHeight.value,
            )
            animationSetState.fastForEach {
                animationState = it
                val maxTime = maxOf(
                    transition1.totalDurationNanos,
                    transition2.totalDurationNanos,
                    transition3.totalDurationNanos,
                )
                delay(TimeUnit.NANOSECONDS.toMillis(maxTime))
            }
        }

        val circleColor = TangemTheme.colors.background.secondary

        Canvas(
            modifier = Modifier
                .align(Alignment.Center)
                .size(254.dp)
                .fillMaxSize(),
        ) {
            drawCircle(
                color = circleColor,
                radius = size.width / 2,
                center = center,
            )
        }

        AnimatedWalletCards(
            url1 = url1,
            url2 = url2,
            url3 = url3,
            modifier = Modifier
                .widthIn(max = 450.dp)
                .matchParentSize(),
            transition1 = transition1,
            transition2 = transition2,
            transition3 = transition3,
        )
    }
}

private data class CardsTransitionState(
    val walletCard1: WalletCardTransitionState,
    val walletCard2: WalletCardTransitionState,
    val walletCard3: WalletCardTransitionState,
)

private data class WalletCardTransitionState(
    val alpha: Float = 1f,
    val xTranslation: Float = 0f,
    val yTranslation: Float = 0f,
    val xScale: Float = 1f,
    val yScale: Float = 1f,
    val rotation: Float = 0f,
    val zIndex: Float = 0f,
)

@Suppress("LongParameterList")
@Composable
private fun AnimatedWalletCards(
    url1: String?,
    url2: String?,
    url3: String?,
    transition1: Transition<WalletCardTransitionState>,
    transition2: Transition<WalletCardTransitionState>,
    transition3: Transition<WalletCardTransitionState>,
    modifier: Modifier = Modifier,
) {
    val alpha1 by transition1.animateFloat(label = "alpha1") { it.alpha }
    val translationX1 by transition1.animateFloat(label = "translationX1") { it.xTranslation }
    val translationY1 by transition1.animateFloat(label = "translationY1") { it.yTranslation }
    val rotation1 by transition1.animateFloat(label = "rotation1") { it.rotation }
    val scaleX1 by transition1.animateFloat(label = "scaleX1") { it.xScale }
    val scaleY1 by transition1.animateFloat(label = "scaleY1") { it.yScale }
    val zIndex1 = transition1.currentState.zIndex

    val alpha2 by transition2.animateFloat(label = "alpha2") { it.alpha }
    val translationX2 by transition2.animateFloat(label = "translationX2") { it.xTranslation }
    val translationY2 by transition2.animateFloat(label = "translationY2") { it.yTranslation }
    val rotation2 by transition2.animateFloat(label = "rotation2") { it.rotation }
    val scaleX2 by transition2.animateFloat(label = "scaleX2") { it.xScale }
    val scaleY2 by transition2.animateFloat(label = "scaleY2") { it.yScale }
    val zIndex2 = transition2.currentState.zIndex

    val alpha3 by transition3.animateFloat(label = "alpha3") { it.alpha }
    val translationX3 by transition3.animateFloat(label = "translationX3") { it.xTranslation }
    val translationY3 by transition3.animateFloat(label = "translationY3") { it.yTranslation }
    val rotation3 by transition3.animateFloat(label = "rotation3") { it.rotation }
    val scaleX3 by transition3.animateFloat(label = "scaleX3") { it.xScale }
    val scaleY3 by transition3.animateFloat(label = "scaleY3") { it.yScale }
    val zIndex3 = transition3.targetState.zIndex

    Box(modifier) {
        WalletCard(
            modifier = Modifier
                .zIndex(zIndex3)
                .matchParentSize()
                .graphicsLayer {
                    translationX = translationX3
                    translationY = translationY3
                }
                .graphicsLayer {
                    alpha = alpha3
                    scaleX = scaleX3
                    scaleY = scaleY3
                    rotationZ = rotation3
                },
            url = url3,
        )

        WalletCard(
            modifier = Modifier
                .zIndex(zIndex2)
                .matchParentSize()
                .graphicsLayer {
                    translationX = translationX2
                    translationY = translationY2
                }
                .graphicsLayer {
                    alpha = alpha2
                    rotationZ = rotation2

                    scaleX = scaleX2
                    scaleY = scaleY2
                },
            url = url2,
        )

        WalletCard(
            modifier = Modifier
                .zIndex(zIndex1)
                .matchParentSize()
                .graphicsLayer {
                    scaleX = scaleX1
                    scaleY = scaleY1
                }
                .graphicsLayer {
                    alpha = alpha1
                    translationX = translationX1
                    translationY = translationY1
                    rotationZ = rotation1
                },
            url = url1,
        )
    }
}

@Composable
private fun WalletCard(url: String?, modifier: Modifier = Modifier) {
    AsyncImage(
        modifier = modifier,
        model = ImageRequest.Builder(LocalContext.current)
            .data(url)
            .crossfade(true)
            .build(),
        placeholder = painterResource(R.drawable.card_placeholder_black),
        error = painterResource(R.drawable.card_placeholder_black),
        fallback = painterResource(R.drawable.card_placeholder_black),
        contentDescription = null,
    )
}

private fun WalletArtworksState.toTransitionSetState(maxWidthDp: Float, maxHeightDp: Float, density: Float) =
    when (this) {
        WalletArtworksState.Hidden -> listOf()
        is WalletArtworksState.Folded -> listOf(
            CardsTransitionState(
                WalletCardTransitionState(),
                WalletCardTransitionState(),
                WalletCardTransitionState(),
            ),
        )
        is WalletArtworksState.Stack -> listOf(
            CardsTransitionState(
                walletCard1 = WalletCardTransitionState(),
                walletCard2 = WalletCardTransitionState(),
                walletCard3 = WalletCardTransitionState(),
            ),
            CardsTransitionState(
                walletCard1 = WalletCardTransitionState().copy(
                    alpha = 1f,
                    xTranslation = 0f,
                    yTranslation = 0f,
                    rotation = 0f,
                    xScale = 1f,
                    yScale = 1f,
                ),
                walletCard2 = WalletCardTransitionState().copy(
                    alpha = 0.65f,
                    xTranslation = 0f,
                    yTranslation = ((maxHeightDp * (1 - 0.83f)) / 4 + 12) * density,
                    rotation = 0f,
                    xScale = 0.83f,
                    yScale = 0.83f,
                ),
                walletCard3 = WalletCardTransitionState().copy(
                    alpha = if (threeCards) 0.55f else 0f,
                    xTranslation = 0f,
                    yTranslation = (maxHeightDp * (1 - 0.72f) / 4 + 24) * density,
                    rotation = 0f,
                    xScale = 0.72f,
                    yScale = 0.72f,
                ),
            ),
        )
        is WalletArtworksState.Unfold -> listOfNotNull(
            CardsTransitionState(
                walletCard1 = WalletCardTransitionState(),
                walletCard2 = WalletCardTransitionState().copy(
                    alpha = 0.65f,
                ),
                walletCard3 = WalletCardTransitionState().copy(
                    alpha = 0.55f,
                ),
            ).takeIf { step == WalletArtworksState.Unfold.Step.First },
            CardsTransitionState(
                walletCard1 = WalletCardTransitionState().copy(
                    rotation = 5f,
                ),
                walletCard2 = WalletCardTransitionState().copy(
                    alpha = when (step) {
                        WalletArtworksState.Unfold.Step.First -> 0.65f
                        WalletArtworksState.Unfold.Step.Second -> 1f
                        WalletArtworksState.Unfold.Step.Third -> 1f
                    },
                    rotation = -6f,
                    xTranslation = 0f,
                    yTranslation = -(maxHeightDp * (1 - 0.88f) / 4) * density,
                    xScale = 0.88f,
                    yScale = 0.88f,
                ),
                walletCard3 = WalletCardTransitionState().copy(
                    alpha = when (step) {
                        WalletArtworksState.Unfold.Step.First -> 0.55f
                        WalletArtworksState.Unfold.Step.Second -> 0.55f
                        WalletArtworksState.Unfold.Step.Third -> 1f
                    },
                    rotation = -16f,
                    xTranslation = 0f,
                    yTranslation = -(maxHeightDp * (1 - 0.81f) / 4 + 2) * density,
                    xScale = 0.81f,
                    yScale = 0.81f,
                ),
            ),
        )
        WalletArtworksState.Fan -> {
            listOf(
                CardsTransitionState(
                    walletCard1 = WalletCardTransitionState().copy(
                        xTranslation = maxWidthDp * 0.4f * density,
                        yTranslation = 35 * density,
                        rotation = 75f,
                        xScale = .85f,
                        yScale = .85f,
                    ),
                    walletCard2 = WalletCardTransitionState().copy(
                        xTranslation = maxWidthDp * -0.4f * density,
                        yTranslation = -5 * density,
                        rotation = 75f,
                        xScale = .85f,
                        yScale = .85f,
                    ),
                    walletCard3 = WalletCardTransitionState().copy(
                        rotation = 105f,
                        xScale = .85f,
                        yScale = .85f,
                    ),
                ),
            )
        }
        is WalletArtworksState.Leapfrog -> {
            val translationY = -(maxHeightDp / 2) * density
            val secondCardPlace = WalletCardTransitionState().copy(
                alpha = 0.65f,
                xTranslation = 0f,
                yTranslation = ((maxHeightDp * (1 - 0.83f)) / 4 + 12) * density,
                rotation = 0f,
                xScale = 0.83f,
                yScale = 0.83f,
            )
            val thirdCardPlace = WalletCardTransitionState().copy(
                alpha = if (threeCards) 0.55f else 0f,
                xTranslation = 0f,
                yTranslation = (maxHeightDp * (1 - 0.72f) / 4 + 24) * density,
                rotation = 0f,
                xScale = 0.72f,
                yScale = 0.72f,
            )

            if (step == WalletArtworksState.Leapfrog.Step.Second) {
                listOf(
                    CardsTransitionState(
                        walletCard1 = WalletCardTransitionState(),
                        walletCard2 = secondCardPlace,
                        walletCard3 = thirdCardPlace,
                    ),
                    CardsTransitionState(
                        walletCard1 = WalletCardTransitionState().copy(
                            yTranslation = translationY,
                        ),
                        walletCard2 = WalletCardTransitionState().copy(
                            alpha = 1f,
                            zIndex = 4f,
                        ),
                        walletCard3 = thirdCardPlace,
                    ),
                    CardsTransitionState(
                        walletCard1 = WalletCardTransitionState().copy(
                            zIndex = 1f,
                        ),
                        walletCard2 = WalletCardTransitionState().copy(
                            zIndex = 4f,
                        ),
                        walletCard3 = secondCardPlace,
                    ),
                )
            } else {
                listOf(
                    CardsTransitionState(
                        walletCard1 = WalletCardTransitionState().copy(
                            zIndex = 2f,
                        ),
                        walletCard2 = WalletCardTransitionState().copy(
                            zIndex = 3f,
                        ),
                        walletCard3 = secondCardPlace,
                    ),
                    CardsTransitionState(
                        walletCard1 = WalletCardTransitionState().copy(
                            yTranslation = translationY,
                        ),
                        walletCard2 = WalletCardTransitionState().copy(
                            zIndex = 3f,
                            yTranslation = translationY,
                        ),
                        walletCard3 = WalletCardTransitionState().copy(
                            alpha = 1f,
                            zIndex = 4f,
                        ),
                    ),
                    CardsTransitionState(
                        walletCard1 = WalletCardTransitionState().copy(
                            zIndex = 1f,
                        ),
                        walletCard2 = WalletCardTransitionState().copy(
                            zIndex = 4f,
                        ),
                        walletCard3 = WalletCardTransitionState().copy(
                            zIndex = 5f,
                        ),
                    ),
                )
            }
        }
    }

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun Preview() {
    TangemThemePreview {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            var state: WalletArtworksState by remember { mutableStateOf(WalletArtworksState.Folded) }

            WalletArtworks(
                url1 = null,
                url2 = null,
                url3 = null,
                state = state,
                modifier = Modifier.fillMaxWidth(),
            )

            var index by remember { mutableIntStateOf(0) }

            Button(
                modifier = Modifier
                    .zIndex(5f)
                    .align(Alignment.TopStart),
                onClick = {
                    val list = listOf(
                        WalletArtworksState.Folded,
                        WalletArtworksState.Stack(),
                        WalletArtworksState.Fan,
                        WalletArtworksState.Unfold(step = WalletArtworksState.Unfold.Step.First),
                        WalletArtworksState.Unfold(step = WalletArtworksState.Unfold.Step.Second),
                        WalletArtworksState.Unfold(step = WalletArtworksState.Unfold.Step.Third),
                        WalletArtworksState.Stack(),
                        WalletArtworksState.Leapfrog(step = WalletArtworksState.Leapfrog.Step.Second),
                        WalletArtworksState.Leapfrog(step = WalletArtworksState.Leapfrog.Step.Third),
                    )

                    state = list[index % list.size]
                    index++
                },
            ) {
                Text("Animate")
            }
        }
    }
}