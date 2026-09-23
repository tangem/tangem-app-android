package com.tangem.features.collectibles.impl.machinedetails.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.shimmers.ProvideTangemShimmer
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.collectibles.impl.machinedetails.ui.components.PRIZE_CARD_ASPECT_RATIO

private const val TIER_ROWS = 3
private const val PRIZE_ROWS = 2
private const val CARDS_PER_ROW = 2

@Composable
internal fun CollectiblesPackDetailsSkeleton(contentPadding: PaddingValues, modifier: Modifier = Modifier) {
    ProvideTangemShimmer {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center,
            ) {
                TangemShimmer(modifier = Modifier.size(width = 204.dp, height = 294.dp), radius = 20.dp)
            }

            repeat(TIER_ROWS) { HeadingAndCards(showHeading = it == 0) }

            CollectiblesPrizeGridSkeleton()
        }
    }
}

@Composable
internal fun CollectiblesPrizeGridSkeleton(modifier: Modifier = Modifier) {
    ProvideTangemShimmer {
        Column(modifier = modifier) {
            repeat(PRIZE_ROWS) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    repeat(CARDS_PER_ROW) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            TangemShimmer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(PRIZE_CARD_ASPECT_RATIO),
                                radius = 20.dp,
                            )
                            TangemShimmer(style = TangemTheme.typography3.subheading.medium)
                            TangemShimmer(style = TangemTheme.typography3.subheading.medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeadingAndCards(showHeading: Boolean, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        if (showHeading) {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                TangemShimmer(style = TangemTheme.typography3.subheading.medium, modifier = Modifier.width(140.dp))
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(CARDS_PER_ROW) {
                TangemShimmer(
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp),
                    radius = 20.dp,
                )
            }
        }
    }
}