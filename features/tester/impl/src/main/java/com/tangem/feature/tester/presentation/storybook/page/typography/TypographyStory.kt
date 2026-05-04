package com.tangem.feature.tester.presentation.storybook.page.typography

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tester.presentation.storybook.entity.TypographyStory as TypographyStoryState

private data class TypographyItem(val name: String, val style: TextStyle)

@Composable
internal fun TypographyStory(state: TypographyStoryState, modifier: Modifier = Modifier) {
    if (state.isFontScaleDefault) {
        val density = LocalDensity.current
        val overriddenDensity = remember(density) {
            Density(density = density.density, fontScale = 1f)
        }
        CompositionLocalProvider(LocalDensity provides overriddenDensity) {
            TypographyContent(state = state, modifier = modifier)
        }
    } else {
        TypographyContent(state = state, modifier = modifier)
    }
}

@Suppress("LongMethod", "ModifierHeightWithText")
@Composable
private fun TypographyContent(state: TypographyStoryState, modifier: Modifier = Modifier) {
    val typography2 = TangemTheme.typography2
    val items = remember(typography2) {
        listOf(
            TypographyItem("titleRegular44", typography2.titleRegular44),
            TypographyItem("headingRegular34", typography2.headingRegular34),
            TypographyItem("headingBold34", typography2.headingBold34),
            TypographyItem("headingRegular28", typography2.headingRegular28),
            TypographyItem("headingSemibold28", typography2.headingSemibold28),
            TypographyItem("headingBold28", typography2.headingBold28),
            TypographyItem("headingRegular22", typography2.headingRegular22),
            TypographyItem("headingSemibold22", typography2.headingSemibold22),
            TypographyItem("headingBold22", typography2.headingBold22),
            TypographyItem("headingRegular20", typography2.headingRegular20),
            TypographyItem("headingSemibold20", typography2.headingSemibold20),
            TypographyItem("headingRegular17", typography2.headingRegular17),
            TypographyItem("headingMedium17", typography2.headingMedium17),
            TypographyItem("headingSemibold17", typography2.headingSemibold17),
            TypographyItem("bodyRegular16", typography2.bodyRegular16),
            TypographyItem("bodyMedium16", typography2.bodyMedium16),
            TypographyItem("bodySemibold16", typography2.bodySemibold16),
            TypographyItem("calloutRegular15", typography2.calloutRegular15),
            TypographyItem("calloutSemibold15", typography2.calloutSemibold15),
            TypographyItem("subheadlineRegular14", typography2.subheadlineRegular14),
            TypographyItem("subheadlineMedium14", typography2.subheadlineMedium14),
            TypographyItem("captionRegular13", typography2.captionRegular13),
            TypographyItem("captionMedium13", typography2.captionMedium13),
            TypographyItem("captionRegular12", typography2.captionRegular12),
            TypographyItem("captionMedium12", typography2.captionMedium12),
            TypographyItem("captionRegular11", typography2.captionRegular11),
            TypographyItem("captionMedium11", typography2.captionMedium11),
        )
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(32.dp),
        modifier = modifier
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(top = 48.dp)
            .fillMaxSize()
            .background(TangemTheme.colors2.surface.level1),
    ) {
        item(key = "toggle") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Text(
                    text = "Force fontScale = 1.0",
                    style = TangemTheme.typography2.bodyMedium16,
                    color = TangemTheme.colors2.text.neutral.primary,
                )
                Switch(
                    checked = state.isFontScaleDefault,
                    onCheckedChange = { state.onFontScaleToggle() },
                )
            }
        }
        itemsIndexed(items = items, key = { _, item -> item.name }) { index, item ->
            Row(
                modifier = Modifier
                    .border(width = 1.dp, color = TangemTheme.colors2.surface.level2)
                    .height(if (index == 0) 48.dp else 44.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Lorem ipsum",
                    style = item.style,
                    color = TangemTheme.colors2.text.neutral.primary,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                Text(
                    text = item.name,
                    style = TangemTheme.typography2.captionRegular12,
                    color = TangemTheme.colors2.text.neutral.tertiary,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        }
    }
}