@file:Suppress("MagicNumber", "LongMethod")

package com.tangem.feature.tester.presentation.storybook.page.placeholder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.ChipShimmer
import com.tangem.core.ui.components.CircleShimmer
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.SmallButtonShimmer
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.res.TangemTheme

@Composable
internal fun PlaceholderStory(modifier: Modifier = Modifier) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .statusBarsPadding()
            .fillMaxSize()
            .background(TangemTheme.colors2.surface.level1),
    ) {
        item("section_rectangle") {
            SectionTitle(text = "RectangleShimmer")
        }

        item("rect_default") {
            ShimmerRow(label = "Default") {
                RectangleShimmer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp),
                )
            }
        }

        item("rect_narrow") {
            ShimmerRow(label = "Narrow (40%)") {
                RectangleShimmer(
                    modifier = Modifier
                        .fillMaxWidth(fraction = 0.4f)
                        .height(16.dp),
                )
            }
        }

        item("rect_tall") {
            ShimmerRow(label = "Tall (48dp)") {
                RectangleShimmer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                )
            }
        }

        item("rect_custom_radius") {
            ShimmerRow(label = "Custom radius (16dp)") {
                RectangleShimmer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp),
                    radius = 16.dp,
                )
            }
        }

        item("section_circle") {
            SectionTitle(text = "CircleShimmer")
        }

        item("circle_sizes") {
            ShimmerRow(label = "Sizes: 24, 32, 40, 48") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircleShimmer(modifier = Modifier.size(24.dp))
                    CircleShimmer(modifier = Modifier.size(32.dp))
                    CircleShimmer(modifier = Modifier.size(40.dp))
                    CircleShimmer(modifier = Modifier.size(48.dp))
                }
            }
        }

        item("section_text") {
            SectionTitle(text = "TextShimmer")
        }

        item("text_title") {
            ShimmerRow(label = "titleRegular44") {
                TextShimmer(
                    style = TangemTheme.typography2.titleRegular44,
                    modifier = Modifier.fillMaxWidth(fraction = 0.5f),
                )
            }
        }

        item("text_heading") {
            ShimmerRow(label = "headingSemibold22") {
                TextShimmer(
                    style = TangemTheme.typography2.headingSemibold22,
                    modifier = Modifier.fillMaxWidth(fraction = 0.6f),
                )
            }
        }

        item("text_body") {
            ShimmerRow(label = "bodyRegular16") {
                TextShimmer(
                    style = TangemTheme.typography2.bodyRegular16,
                    modifier = Modifier.fillMaxWidth(fraction = 0.7f),
                )
            }
        }

        item("text_caption") {
            ShimmerRow(label = "captionRegular12") {
                TextShimmer(
                    style = TangemTheme.typography2.captionRegular12,
                    modifier = Modifier.fillMaxWidth(fraction = 0.4f),
                )
            }
        }

        item("text_size_height") {
            ShimmerRow(label = "bodyRegular16 (textSizeHeight)") {
                TextShimmer(
                    style = TangemTheme.typography2.bodyRegular16,
                    textSizeHeight = true,
                    modifier = Modifier.fillMaxWidth(fraction = 0.5f),
                )
            }
        }

        item("section_button_chip") {
            SectionTitle(text = "SmallButtonShimmer & ChipShimmer")
        }

        item("small_button") {
            ShimmerRow(label = "SmallButtonShimmer") {
                SmallButtonShimmer()
            }
        }

        item("small_button_icon") {
            ShimmerRow(label = "SmallButtonShimmer (with icon)") {
                SmallButtonShimmer(withIcon = true)
            }
        }

        item("chip") {
            ShimmerRow(label = "ChipShimmer") {
                ChipShimmer()
            }
        }

        item("section_composition") {
            SectionTitle(text = "Skeleton composition")
        }

        item("card_skeleton") {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                Text(
                    text = "Typical card loading state",
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors.text.secondary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircleShimmer(modifier = Modifier.size(40.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextShimmer(
                            style = TangemTheme.typography2.headingSemibold17,
                            modifier = Modifier.width(120.dp),
                        )
                        TextShimmer(
                            style = TangemTheme.typography2.captionRegular13,
                            modifier = Modifier.width(80.dp),
                        )
                    }
                }
            }
        }

        item("list_skeleton") {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                Text(
                    text = "List loading state",
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors.text.secondary,
                )
                repeat(3) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        RectangleShimmer(modifier = Modifier.size(40.dp))
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            TextShimmer(
                                style = TangemTheme.typography2.bodyMedium16,
                                modifier = Modifier.fillMaxWidth(fraction = 0.6f),
                            )
                            TextShimmer(
                                style = TangemTheme.typography2.captionRegular13,
                                modifier = Modifier.fillMaxWidth(fraction = 0.4f),
                            )
                        }
                    }
                }
            }
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
private fun ShimmerRow(label: String, content: @Composable () -> Unit) {
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
        content()
    }
}