@file:Suppress("MagicNumber", "LongMethod")

package com.tangem.feature.tester.presentation.storybook.page.deviceicon

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds.image.DeviceIconUM
import com.tangem.core.ui.ds.image.TangemDeviceIcon
import com.tangem.core.ui.res.TangemTheme

private val CardBlue = Color(0xFF1C5FBF)
private val CardGold = Color(0xFFD4A017)
private val CardPurple = Color(0xFF7B2FBE)
private val RingGreen = Color(0xFF2ECC71)

@Composable
internal fun DeviceIconStory(modifier: Modifier = Modifier) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .statusBarsPadding()
            .fillMaxSize()
            .background(TangemTheme.colors2.surface.level1),
    ) {
        item("section_cards") {
            SectionTitle(text = "Cards")
        }

        item("card_1") {
            DeviceIconRow(
                label = "Single card",
                state = DeviceIconUM.Card(mainColor = CardBlue, secondColor = null),
            )
        }

        item("card_2") {
            DeviceIconRow(
                label = "Two cards",
                state = DeviceIconUM.Card(mainColor = CardBlue, secondColor = CardGold),
            )
        }

        item("card_3") {
            DeviceIconRow(
                label = "Three cards",
                state = DeviceIconUM.Card(
                    mainColor = CardBlue,
                    secondColor = CardGold,
                    thirdColor = CardPurple,
                ),
            )
        }

        item("section_rings") {
            SectionTitle(text = "Rings")
        }

        item("ring_solo") {
            DeviceIconRow(
                label = "Ring only",
                state = DeviceIconUM.Ring(mainColor = RingGreen),
            )
        }

        item("ring_card") {
            DeviceIconRow(
                label = "Ring + card",
                state = DeviceIconUM.Ring(mainColor = RingGreen, cardColor = CardBlue),
            )
        }

        item("ring_two_cards") {
            DeviceIconRow(
                label = "Ring + 2 cards",
                state = DeviceIconUM.Ring(
                    mainColor = RingGreen,
                    cardColor = CardBlue,
                    secondCardColor = CardGold,
                ),
            )
        }

        item("section_stubs") {
            SectionTitle(text = "Stubs")
        }

        repeat(3) { count ->
            item("stub_$count") {
                DeviceIconRow(
                    label = "Stub ($count card${if (count > 0) "s" else ""})",
                    state = DeviceIconUM.Stub(cardsCount = count),
                )
            }
        }

        item("section_mobile") {
            SectionTitle(text = "Mobile")
        }

        item("mobile") {
            DeviceIconRow(
                label = "Mobile wallet",
                state = DeviceIconUM.Mobile,
            )
        }

        item("section_sizes") {
            SectionTitle(text = "Sizes")
        }

        item("sizes") {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                listOf(24, 32, 40, 48).forEach { size ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        TangemDeviceIcon(
                            state = DeviceIconUM.Card(
                                mainColor = CardBlue,
                                secondColor = CardGold,
                                thirdColor = CardPurple,
                            ),
                            modifier = Modifier.size(size.dp),
                        )
                        Text(
                            text = "${size}dp",
                            style = TangemTheme.typography.caption2,
                            color = TangemTheme.colors.text.tertiary,
                        )
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
private fun DeviceIconRow(label: String, state: DeviceIconUM) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        TangemDeviceIcon(
            state = state,
            modifier = Modifier.size(40.dp),
        )
        Text(
            text = label,
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.secondary,
        )
    }
}