@file:Suppress("MagicNumber", "LongMethod")

package com.tangem.feature.tester.presentation.storybook.page.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds.progress.TangemLinearProgressIndicatorWithDot
import com.tangem.core.ui.res.TangemTheme

@Composable
internal fun ProgressIndicatorStory(modifier: Modifier = Modifier) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .statusBarsPadding()
            .fillMaxSize()
            .background(TangemTheme.colors2.surface.level1),
    ) {
        item("section_progress") {
            SectionTitle(text = "Progress values")
        }

        listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { progress ->
            item("progress_$progress") {
                ProgressRow(label = "${(progress * 100).toInt()}%", progress = progress)
            }
        }

        item("section_heights") {
            SectionTitle(text = "Track heights")
        }

        listOf(4, 6, 8).forEach { height ->
            item("height_$height") {
                ProgressRow(label = "${height}dp track", progress = 0.5f, height = height)
            }
        }

        item("section_colors") {
            SectionTitle(text = "Color variants")
        }

        item("accent") {
            ProgressRowWithColors(
                label = "Accent",
                progress = 0.6f,
                dotColor = TangemTheme.colors2.fill.status.accent,
                bgColor = TangemTheme.colors2.graphic.neutral.primaryInvertedConstant,
            )
        }

        item("warning") {
            ProgressRowWithColors(
                label = "Warning",
                progress = 0.4f,
                dotColor = TangemTheme.colors2.fill.status.warning,
                bgColor = TangemTheme.colors2.graphic.neutral.primaryInvertedConstant,
            )
        }

        item("attention") {
            ProgressRowWithColors(
                label = "Attention",
                progress = 0.8f,
                dotColor = TangemTheme.colors2.fill.status.attention,
                bgColor = TangemTheme.colors2.graphic.neutral.primaryInvertedConstant,
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = TangemTheme.typography.subtitle1,
        color = TangemTheme.colors.text.primary1,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
    HorizontalDivider(
        color = TangemTheme.colors2.border.neutral.secondary,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

@Composable
private fun ProgressRow(label: String, progress: Float, height: Int = 6) {
    ProgressRowWithColors(
        label = label,
        progress = progress,
        height = height,
        dotColor = TangemTheme.colors2.fill.status.accent,
        bgColor = TangemTheme.colors2.graphic.neutral.primaryInvertedConstant,
    )
}

@Composable
private fun ProgressRowWithColors(
    label: String,
    progress: Float,
    dotColor: androidx.compose.ui.graphics.Color,
    bgColor: androidx.compose.ui.graphics.Color,
    height: Int = 6,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.secondary,
        )
        TangemLinearProgressIndicatorWithDot(
            progress = { progress },
            dotColor = dotColor,
            backgroundColor = bgColor,
            modifier = Modifier
                .fillMaxWidth()
                .height(height.dp),
        )
    }
}