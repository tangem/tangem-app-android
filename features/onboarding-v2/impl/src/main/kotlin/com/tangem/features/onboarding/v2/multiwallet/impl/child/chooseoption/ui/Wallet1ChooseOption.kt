package com.tangem.features.onboarding.v2.multiwallet.impl.child.chooseoption.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.onboarding.v2.impl.R

@Composable
fun Wallet1ChooseOption(
    canSkipBackup: Boolean,
    onSkipClick: () -> Unit,
    onBackupClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.Bottom,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val pagerState = rememberPagerState { CarouselItems.size }

            HorizontalPager(pagerState) { selectedIndex ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 32.dp, end = 32.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val item = CarouselItems[selectedIndex]
                    Text(
                        text = item.title.resolveReference(),
                        style = TangemTheme.typography.h2,
                        color = TangemTheme.colors.text.primary1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                    Text(
                        text = item.subtitle.resolveReference(),
                        style = TangemTheme.typography.body1,
                        color = TangemTheme.colors.text.secondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }

            Dots(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                count = CarouselItems.size,
                selectedIndex = pagerState.currentPage,
            )
        }

        PrimaryButton(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                .fillMaxWidth(),
            text = stringResourceSafe(R.string.onboarding_button_backup_now),
            onClick = onBackupClick,
        )

        if (canSkipBackup) {
            SecondaryButton(
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    .fillMaxWidth(),
                text = stringResourceSafe(R.string.onboarding_button_skip_backup),
                onClick = onSkipClick,
            )
        }
    }
}

@Composable
private fun Dots(count: Int, selectedIndex: Int, modifier: Modifier = Modifier) {
    val generalColor = TangemTheme.colors.icon.informative
    val selectedColor = TangemTheme.colors.icon.primary1

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(count) { index ->
            val color by animateColorAsState(if (index == selectedIndex) selectedColor else generalColor)

            Canvas(Modifier.size(7.dp)) {
                drawCircle(
                    color = color,
                    radius = size.width / 2f,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    TangemThemePreview {
        Wallet1ChooseOption(
            canSkipBackup = true,
            onSkipClick = {},
            onBackupClick = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}