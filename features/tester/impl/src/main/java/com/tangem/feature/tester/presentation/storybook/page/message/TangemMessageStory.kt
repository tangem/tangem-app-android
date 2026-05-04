@file:Suppress("MagicNumber", "LongMethod")

package com.tangem.feature.tester.presentation.storybook.page.message

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.message.TangemMessage
import com.tangem.core.ui.ds.message.TangemMessageButtonUM
import com.tangem.core.ui.ds.message.TangemMessageEffect
import com.tangem.core.ui.ds.message.TangemMessageUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tester.presentation.storybook.entity.TangemMessageStory
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun TangemMessageStory(state: TangemMessageStory, modifier: Modifier = Modifier) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(TangemTheme.colors2.surface.level1),
    ) {
        stickyHeader("effect_toggle") {
            EffectToggle(
                selected = state.selectedEffect,
                onSelect = state.onEffectChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TangemTheme.colors2.surface.level1)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        item("plain") {
            VariantSection(label = "No icon, no buttons") {
                TangemMessage(
                    messageUM = TangemMessageUM(
                        id = "plain",
                        title = stringReference("Update available"),
                        subtitle = stringReference("A new firmware version is ready to install on your card."),
                        messageEffect = state.selectedEffect,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item("icon") {
            VariantSection(label = "With icon") {
                TangemMessage(
                    messageUM = TangemMessageUM(
                        id = "icon",
                        title = stringReference("Wallet backup missing"),
                        subtitle = stringReference("To protect your assets, complete the backup process."),
                        messageEffect = state.selectedEffect,
                        iconUM = TangemIconUM.Icon(R.drawable.ic_attention_default_24),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item("centered") {
            VariantSection(label = "Centered") {
                TangemMessage(
                    messageUM = TangemMessageUM(
                        id = "centered",
                        title = stringReference("Scan your card"),
                        subtitle = stringReference("Hold the card to the back of your phone."),
                        messageEffect = state.selectedEffect,
                        iconUM = TangemIconUM.Icon(R.drawable.ic_attention_default_24),
                        isCentered = true,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item("1btn") {
            VariantSection(label = "With 1 button") {
                TangemMessage(
                    messageUM = TangemMessageUM(
                        id = "1btn",
                        title = stringReference("Generate addresses"),
                        subtitle = stringReference("Generate addresses for 2 new networks using your card."),
                        messageEffect = state.selectedEffect,
                        iconUM = TangemIconUM.Icon(R.drawable.ic_attention_default_24),
                        buttonsUM = persistentListOf(
                            TangemMessageButtonUM(
                                text = stringReference("Generate"),
                                type = TangemButtonType.Primary,
                                tangemIconUM = TangemIconUM.Icon(
                                    iconRes = R.drawable.ic_tangem_24,
                                    tintReference = { TangemTheme.colors2.graphic.neutral.primary },
                                ),
                                onClick = {},
                            ),
                        ),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item("2btn") {
            VariantSection(label = "With 2 buttons") {
                TangemMessage(
                    messageUM = TangemMessageUM(
                        id = "2btn",
                        title = stringReference("Rate the app"),
                        subtitle = stringReference("How do you like Tangem so far?"),
                        messageEffect = state.selectedEffect,
                        iconUM = TangemIconUM.Icon(R.drawable.ic_attention_default_24),
                        buttonsUM = persistentListOf(
                            TangemMessageButtonUM(
                                text = stringReference("Love it!"),
                                type = TangemButtonType.PrimaryInverse,
                                onClick = {},
                            ),
                            TangemMessageButtonUM(
                                text = stringReference("Can be better"),
                                type = TangemButtonType.Primary,
                                onClick = {},
                            ),
                        ),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item("close") {
            VariantSection(label = "With close button") {
                TangemMessage(
                    messageUM = TangemMessageUM(
                        id = "close",
                        title = stringReference("Note top up"),
                        subtitle = stringReference("To activate the card, top it up with at least 1 XLM."),
                        messageEffect = state.selectedEffect,
                        iconUM = TangemIconUM.Icon(R.drawable.ic_attention_default_24),
                        onCloseClick = {},
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun EffectToggle(
    selected: TangemMessageEffect,
    onSelect: (TangemMessageEffect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .clip(shape)
            .background(TangemTheme.colors2.surface.level2)
            .border(
                width = 1.dp,
                color = TangemTheme.colors2.border.neutral.secondary,
                shape = shape,
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TangemMessageEffect.entries.forEach { effect ->
            EffectChip(
                label = effect.name,
                selected = effect == selected,
                onClick = { onSelect(effect) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun EffectChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val chipShape = RoundedCornerShape(50)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(chipShape)
            .background(
                if (selected) {
                    TangemTheme.colors2.surface.level3
                } else {
                    TangemTheme.colors2.surface.level2
                },
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
    ) {
        Text(
            text = label,
            style = TangemTheme.typography.caption2,
            color = if (selected) {
                TangemTheme.colors.text.primary1
            } else {
                TangemTheme.colors.text.secondary
            },
        )
    }
}

@Composable
private fun VariantSection(label: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = label,
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.secondary,
        )
        content()
    }
}