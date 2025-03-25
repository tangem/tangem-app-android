package com.tangem.features.onboarding.v2.twin.impl.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.zIndex
import com.tangem.core.ui.components.SpacerH8
import com.tangem.core.ui.components.SpacerHMax
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.domain.wallets.models.Artwork
import com.tangem.features.onboarding.v2.common.ui.RefreshButton
import com.tangem.features.onboarding.v2.common.ui.WalletCard
import com.tangem.features.onboarding.v2.impl.R
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

internal sealed class TwinWalletArtworkState {
    data object Spread : TwinWalletArtworkState()

    data class Leapfrog(
        val step: Step,
    ) : TwinWalletArtworkState() {
        enum class Step {
            FirstCard, SecondCard
        }
    }

    data object TopUp : TwinWalletArtworkState()
}

private data class CardsTransitionState(
    val walletCard1: WalletCardTransitionState,
    val walletCard2: WalletCardTransitionState,
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

@Suppress("LongMethod")
@Composable
internal fun TwinWalletArtworks(
    state: TwinWalletArtworkState,
    balance: String,
    isRefreshing: Boolean,
    onRefreshBalanceClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                )
                delay(TimeUnit.NANOSECONDS.toMillis(maxTime))
            }
        }

        AnimatedVisibility(
            visible = state == TwinWalletArtworkState.TopUp,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .padding(vertical = 24.dp, horizontal = 32.dp)
                    .fillMaxSize()
                    .background(
                        TangemTheme.colors.button.secondary,
                        shape = TangemTheme.shapes.roundedCornersMedium,
                    ),
            )
        }

        AnimatedTwinCards(
            transition1 = transition1,
            transition2 = transition2,
            modifier = Modifier
                .widthIn(max = 450.dp)
                .matchParentSize(),
        )

        AnimatedVisibility(
            modifier = Modifier.align(Alignment.Center),
            visible = state == TwinWalletArtworkState.TopUp,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SpacerHMax()
                Text(
                    text = stringResourceSafe(R.string.common_balance_title),
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors.text.secondary,
                    textAlign = TextAlign.Center,
                )
                SpacerH8()
                Text(
                    text = balance.orEmpty(),
                    style = TangemTheme.typography.h2,
                    color = TangemTheme.colors.text.primary1,
                    textAlign = TextAlign.Center,
                )
                SpacerHMax()
            }
        }

        AnimatedVisibility(
            modifier = Modifier.align(Alignment.BottomCenter),
            visible = state == TwinWalletArtworkState.TopUp,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            RefreshButton(
                isRefreshing = isRefreshing,
                onRefreshBalanceClick = onRefreshBalanceClick,
            )
        }
    }
}

@Composable
private fun AnimatedTwinCards(
    transition1: Transition<WalletCardTransitionState>,
    transition2: Transition<WalletCardTransitionState>,
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

    Box(modifier) {
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
            url = Artwork.TWIN_CARD_2_URL,
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
            url = Artwork.TWIN_CARD_1_URL,
        )
    }
}

@Suppress("MagicNumber", "LongMethod", "UnnecessaryParentheses")
private fun TwinWalletArtworkState.toTransitionSetState(
    maxWidthDp: Float,
    maxHeightDp: Float,
    density: Float,
): List<CardsTransitionState> = when (this) {
    TwinWalletArtworkState.Spread -> {
        val scale = 0.8f
        listOf(
            CardsTransitionState(
                walletCard1 = WalletCardTransitionState(
                    xTranslation = maxWidthDp * -0.15f * density,
                    yTranslation = -65 * density,
                    rotation = -5f,
                    xScale = scale,
                    yScale = scale,
                    zIndex = 2f,
                ),
                walletCard2 = WalletCardTransitionState(
                    xTranslation = maxWidthDp * 0.15f * density,
                    yTranslation = 65 * density,
                    rotation = -5f,
                    xScale = scale,
                    yScale = scale,
                    zIndex = 1f,
                ),
            ),
        )
    }
    is TwinWalletArtworkState.Leapfrog -> {
        val translationY = -(maxHeightDp / 2) * density
        val secondCardPlace = WalletCardTransitionState().copy(
            xTranslation = 0f,
            yTranslation = ((maxHeightDp * (1 - 0.83f)) / 4 + 12) * density,
            rotation = 0f,
            xScale = 0.83f,
            yScale = 0.83f,
        )

        if (step == TwinWalletArtworkState.Leapfrog.Step.FirstCard) {
            listOf(
                CardsTransitionState(
                    walletCard1 = WalletCardTransitionState(
                        yTranslation = translationY,
                        zIndex = 2f,
                    ),
                    walletCard2 = WalletCardTransitionState(),
                ),
                CardsTransitionState(
                    walletCard1 = WalletCardTransitionState(
                        zIndex = 2f,
                    ),
                    walletCard2 = secondCardPlace,
                ),
            )
        } else {
            listOf(
                CardsTransitionState(
                    walletCard1 = WalletCardTransitionState(),
                    walletCard2 = WalletCardTransitionState(
                        yTranslation = translationY,
                        zIndex = 2f,
                    ),
                ),
                CardsTransitionState(
                    walletCard1 = secondCardPlace,
                    walletCard2 = WalletCardTransitionState(
                        zIndex = 2f,
                    ),
                ),
            )
        }
    }
    TwinWalletArtworkState.TopUp -> {
        val scale = 0.4f
        val yTranslation = -maxHeightDp * density - 24 * density
        listOf(
            CardsTransitionState(
                walletCard1 = WalletCardTransitionState(
                    yTranslation = yTranslation,
                    xScale = scale,
                    yScale = scale,
                    zIndex = 2f,
                ),
                walletCard2 = WalletCardTransitionState(
                    yTranslation = yTranslation * 0.35f,
                    xScale = scale * 0.8f,
                    yScale = scale * 0.8f,
                    zIndex = 1f,
                ),
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Preview(showBackground = true, widthDp = 360, heightDp = 640, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview() {
    TangemThemePreview {
        Box(
            modifier = Modifier
                .background(TangemTheme.colors.background.primary)
                .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            var state: TwinWalletArtworkState by remember { mutableStateOf(TwinWalletArtworkState.TopUp) }

            TwinWalletArtworks(
                state = state,
                modifier = Modifier
                    .padding(top = 250.dp)
                    .fillMaxWidth(),
                balance = "1 USD",
                isRefreshing = false,
                onRefreshBalanceClick = {},
            )

            var index by remember { mutableIntStateOf(0) }

            Button(
                modifier = Modifier
                    .zIndex(5f)
                    .align(Alignment.TopStart),
                onClick = {
                    val list = listOf(
                        TwinWalletArtworkState.Spread,
                        TwinWalletArtworkState.Leapfrog(step = TwinWalletArtworkState.Leapfrog.Step.FirstCard),
                        TwinWalletArtworkState.Leapfrog(step = TwinWalletArtworkState.Leapfrog.Step.SecondCard),
                        TwinWalletArtworkState.Leapfrog(step = TwinWalletArtworkState.Leapfrog.Step.FirstCard),
                        TwinWalletArtworkState.Leapfrog(step = TwinWalletArtworkState.Leapfrog.Step.SecondCard),
                        TwinWalletArtworkState.TopUp,
                    )

                    state = list[index % list.size]
                    index++
                },
            ) {
                Text("Animate $state")
            }
        }
    }
}