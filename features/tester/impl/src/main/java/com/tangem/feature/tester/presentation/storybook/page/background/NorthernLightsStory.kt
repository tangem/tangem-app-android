@file:Suppress("MagicNumber", "LongMethod")

package com.tangem.feature.tester.presentation.storybook.page.background

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tangem.core.ui.components.background.northernlights.NorthernLightsBackground
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tester.presentation.storybook.entity.NorthernLightsStory

@Composable
internal fun NorthernLightsStory(state: NorthernLightsStory, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        NorthernLightsBackground(
            modifier = Modifier.fillMaxSize(),
            containerColor = TangemTheme.colors2.surface.level1,
            forceSimpleVersion = state.variant == NorthernLightsStory.Variant.Simple,
        )

        NorthernLightsVariantToggle(
            selected = state.variant,
            onSelect = state.onVariantChange,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
                .padding(horizontal = 24.dp),
        )
    }
}

@Composable
private fun NorthernLightsVariantToggle(
    selected: NorthernLightsStory.Variant,
    onSelect: (NorthernLightsStory.Variant) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .clip(shape)
            .background(Color.Black.copy(alpha = 0.35f))
            .border(width = 1.dp, color = Color.White.copy(alpha = 0.15f), shape = shape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        NorthernLightsStory.Variant.entries.forEach { variant ->
            VariantChip(
                label = variant.label,
                selected = variant == selected,
                onClick = { onSelect(variant) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun VariantChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val chipShape = RoundedCornerShape(50)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(chipShape)
            .background(if (selected) Color.White.copy(alpha = 0.2f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 16.dp),
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 14.sp,
        )
    }
}

private val NorthernLightsStory.Variant.label: String
    get() = when (this) {
        NorthernLightsStory.Variant.Shader -> "Shader"
        NorthernLightsStory.Variant.Simple -> "Simple"
    }