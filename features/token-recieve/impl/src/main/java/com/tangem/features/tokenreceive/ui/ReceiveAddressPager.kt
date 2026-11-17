package com.tangem.features.tokenreceive.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.pagination.TangemPagination
import com.tangem.features.tokenreceive.ui.state.ReceiveScreenUM
import kotlinx.coroutines.launch
import kotlin.math.abs

@Suppress("MagicNumber")
@Composable
internal fun ReceiveAddressPager(state: ReceiveScreenUM, reveal: () -> Float, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val sideInset = ((maxWidth - 274.dp) / 2).coerceAtLeast(0.dp)
        val pagerState = rememberPagerState { state.addresses.size }
        val scope = rememberCoroutineScope()
        val onPageChange by rememberUpdatedState(state.onAddressPageChange)
        val progress = { pagerState.currentPage + pagerState.currentPageOffsetFraction }

        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.currentPage }.collect { onPageChange(it) }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            HorizontalPager(
                state = pagerState,
                pageSize = PageSize.Fixed(274.dp),
                pageSpacing = sideInset,
                contentPadding = PaddingValues(horizontal = sideInset, vertical = 8.dp),
                verticalAlignment = Alignment.Top,
                beyondViewportPageCount = 1,
            ) { page ->
                ReceiveAddressCard(
                    address = state.addresses[page],
                    iconState = state.iconState,
                    extrasHint = state.extrasHint,
                    ensName = state.ensName.takeIf { page == 0 },
                    networkNotice = state.networkNotice,
                    reveal = reveal,
                    onCopyEnsClick = state.onCopyEnsClick,
                    pressDelayMillis = 80L,
                    modifier = Modifier.graphicsLayer {
                        alpha = 1f - smoothstep(edge0 = 0.72f, edge1 = 0.97f, x = abs(progress() - page))
                    },
                )
            }
            TangemPagination(
                pagerState = pagerState,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                ) {
                    scope.launch {
                        pagerState.animateScrollToPage(
                            page = (pagerState.currentPage + 1) % pagerState.pageCount,
                            animationSpec = tween(
                                durationMillis = 600,
                                easing = CubicBezierEasing(a = 0.8f, b = 0f, c = 0.2f, d = 1f),
                            ),
                        )
                    }
                },
            )
        }
    }
}

@Suppress("MagicNumber")
private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
    val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3 - 2 * t)
}