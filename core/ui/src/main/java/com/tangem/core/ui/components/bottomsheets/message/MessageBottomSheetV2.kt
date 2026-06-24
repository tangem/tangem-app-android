package com.tangem.core.ui.components.bottomsheets.message

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.R
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetType
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds.topbar.TangemTopBarType
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveAnnotatedReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.test.WarningBottomSheetTestTags
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

@Composable
fun MessageBottomSheetV2(state: MessageBottomSheetUM, onDismissRequest: () -> Unit) {
    val stateWithOnDismiss = remember(state) {
        state.copy(
            onDismissRequest = {
                state.onDismissRequest.invoke()
                onDismissRequest()
            },
        )
    }

    val config = TangemBottomSheetConfig(
        isShown = true,
        content = stateWithOnDismiss,
        onDismissRequest = stateWithOnDismiss.onDismissRequest,
    )

    TangemBottomSheet(
        config = config,
        type = TangemBottomSheetType.Modal,
        containerColor = TangemTheme.colors3.bg.secondary,
        title = {
            TangemTopBar(
                type = TangemTopBarType.BottomSheet,
                endContent = {
                    TangemButton(
                        iconStart = TangemIconUM.Icon(iconRes = R.drawable.ic_close_24),
                        onClick = stateWithOnDismiss.onDismissRequest,
                        size = TangemButton.Size.X11,
                        variant = TangemButton.Variant.Material,
                    )
                },
            )
        },
        content = { content: MessageBottomSheetUM ->
            MessageBottomSheetContent(
                modifier = Modifier.padding(vertical = TangemTheme.dimens2.x4),
                state = content,
            )
        },
    )
}

@Composable
fun MessageBottomSheetContentV2(state: MessageBottomSheetUM, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        state.elements.fastForEach { element ->
            when (element) {
                is MessageBottomSheetUM.InfoBlock -> {
                    ContentContainer(
                        modifier = Modifier
                            .heightIn(min = TangemTheme.dimens.size180)
                            .fillMaxWidth()
                            .padding(horizontal = TangemTheme.dimens.spacing16)
                            .padding(bottom = 32.dp),
                        state = element,
                    )
                }
                else -> Unit
            }
        }

        ButtonsContainer(
            modifier = Modifier.fillMaxWidth(),
            closeScope = state.closeScope,
            buttons = state.elements.filterIsInstance<MessageBottomSheetUM.Button>().toPersistentList(),
        )
    }
}

@Composable
private fun ContentContainer(state: MessageBottomSheetUM.InfoBlock, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        BottomSheetIconContainer(state.icon, state.iconImage, state.vector)
        state.title?.let { title ->
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = TangemTheme.dimens2.x8)
                    .testTag(WarningBottomSheetTestTags.TITLE),
                text = title.resolveReference(),
                style = TangemTheme.typography3.heading.small,
                color = TangemTheme.colors3.text.primary,
                textAlign = TextAlign.Center,
            )
        }
        state.body?.let { body ->
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = TangemTheme.dimens2.x2)
                    .testTag(WarningBottomSheetTestTags.MESSAGE),
                text = body.resolveAnnotatedReference(),
                style = TangemTheme.typography3.subheading.medium,
                color = TangemTheme.colors3.text.secondary,
                textAlign = TextAlign.Center,
            )
        }
        state.chip?.let { chip ->
            BottomSheetChip(
                modifier = Modifier.padding(top = TangemTheme.dimens2.x4),
                chip = chip,
            )
        }
    }
}

@Composable
private fun BottomSheetIconContainer(
    icon: MessageBottomSheetUM.Icon?,
    iconImage: MessageBottomSheetUM.IconImage?,
    vector: MessageBottomSheetUM.Vector?,
    modifier: Modifier = Modifier,
) {
    if (icon != null) {
        BottomSheetIcon(icon, modifier)
    } else if (iconImage != null) {
        Image(
            modifier = modifier
                .size(TangemTheme.dimens2.x20)
                .clip(CircleShape),
            painter = painterResource(id = iconImage.res),
            contentDescription = null,
        )
    } else if (vector != null) {
        BottomSheetVector(vector, modifier)
    }
}

@Suppress("MagicNumber")
@Composable
private fun BottomSheetIcon(icon: MessageBottomSheetUM.Icon, modifier: Modifier = Modifier) {
    val tint = when (icon.type) {
        MessageBottomSheetUM.Icon.Type.Unspecified -> Color.Unspecified
        MessageBottomSheetUM.Icon.Type.Accent -> TangemTheme.colors3.icon.status.info
        MessageBottomSheetUM.Icon.Type.Informative -> TangemTheme.colors3.icon.status.info
        MessageBottomSheetUM.Icon.Type.Attention -> TangemTheme.colors3.icon.status.warning
        MessageBottomSheetUM.Icon.Type.Warning -> TangemTheme.colors3.icon.status.error
    }

    val backgroundColor = when (icon.backgroundType) {
        MessageBottomSheetUM.Icon.BackgroundType.Unspecified -> TangemTheme.colors3.bg.tertiary
        MessageBottomSheetUM.Icon.BackgroundType.SameAsTint -> {
            if (tint == Color.Unspecified) Color.Unspecified else tint.copy(alpha = 0.1f)
        }
        MessageBottomSheetUM.Icon.BackgroundType.Accent -> TangemTheme.colors3.bg.status.infoSubtle
        MessageBottomSheetUM.Icon.BackgroundType.Informative -> TangemTheme.colors3.bg.status.infoSubtle
        MessageBottomSheetUM.Icon.BackgroundType.Attention -> TangemTheme.colors3.bg.status.warningSubtle
        MessageBottomSheetUM.Icon.BackgroundType.Warning -> TangemTheme.colors3.bg.status.errorSubtle
    }

    Box(
        modifier = modifier
            .size(TangemTheme.dimens2.x20)
            .clip(CircleShape)
            .background(backgroundColor)
            .testTag(WarningBottomSheetTestTags.ICON),
        contentAlignment = Alignment.Center,
        content = {
            Icon(
                modifier = Modifier.size(28.dp),
                painter = painterResource(icon.res),
                contentDescription = null,
                tint = tint,
            )
        },
    )
}

@Composable
private fun BottomSheetVector(vector: MessageBottomSheetUM.Vector, modifier: Modifier = Modifier) {
    val tint = when (vector.type) {
        MessageBottomSheetUM.Vector.Type.Unspecified -> Color.Unspecified
        MessageBottomSheetUM.Vector.Type.Accent -> TangemTheme.colors3.icon.status.info
        MessageBottomSheetUM.Vector.Type.Informative -> TangemTheme.colors3.icon.status.info
        MessageBottomSheetUM.Vector.Type.Attention -> TangemTheme.colors3.icon.status.warning
        MessageBottomSheetUM.Vector.Type.Warning -> TangemTheme.colors3.icon.status.error
    }

    val backgroundColor = when (vector.backgroundType) {
        MessageBottomSheetUM.Vector.BackgroundType.Unspecified -> TangemTheme.colors3.bg.tertiary
        MessageBottomSheetUM.Vector.BackgroundType.SameAsTint -> tint
        MessageBottomSheetUM.Vector.BackgroundType.Accent -> TangemTheme.colors3.bg.status.infoSubtle
        MessageBottomSheetUM.Vector.BackgroundType.Informative -> TangemTheme.colors3.bg.status.infoSubtle
        MessageBottomSheetUM.Vector.BackgroundType.Attention -> TangemTheme.colors3.bg.status.warningSubtle
        MessageBottomSheetUM.Vector.BackgroundType.Warning -> TangemTheme.colors3.bg.status.errorSubtle
    }

    Box(
        modifier = modifier
            .size(TangemTheme.dimens2.x20)
            .clip(CircleShape)
            .background(backgroundColor)
            .testTag(WarningBottomSheetTestTags.ICON),
        contentAlignment = Alignment.Center,
        content = {
            Icon(
                modifier = Modifier.size(28.dp),
                imageVector = vector.imageVector,
                contentDescription = null,
                tint = tint,
            )
        },
    )
}

@Composable
private fun BottomSheetChip(chip: MessageBottomSheetUM.Chip, modifier: Modifier = Modifier) {
    val color = when (chip.type) {
        MessageBottomSheetUM.Chip.Type.Unspecified -> TangemTheme.colors.text.primary1
        MessageBottomSheetUM.Chip.Type.Warning -> TangemTheme.colors.text.warning
    }

    Text(
        modifier = modifier
            .background(
                shape = RoundedCornerShape(TangemTheme.dimens.radius16),
                color = color.copy(alpha = 0.1F),
            )
            .padding(vertical = TangemTheme.dimens.spacing4, horizontal = TangemTheme.dimens.spacing12),
        text = chip.text.resolveReference(),
        style = TangemTheme.typography.caption1,
        color = color,
    )
}

@Suppress("LongMethod")
@Composable
private fun ButtonsContainer(
    buttons: ImmutableList<MessageBottomSheetUM.Button>,
    closeScope: MessageBottomSheetUM.CloseScope,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(all = TangemTheme.dimens2.x4),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x2),
    ) {
        buttons.fastForEach { button ->
            TangemButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(
                        if (button.isPrimary) {
                            WarningBottomSheetTestTags.BUTTON_PRIMARY
                        } else {
                            WarningBottomSheetTestTags.BUTTON_SECONDARY
                        },
                    ),
                onClick = { button.onClick?.invoke(closeScope) },
                text = button.text ?: TextReference.EMPTY,
                iconStart = if (button.iconOrder == MessageBottomSheetUM.Button.IconOrder.Start) {
                    button.icon?.let(TangemIconUM::Icon)
                } else {
                    null
                },
                iconEnd = if (button.iconOrder == MessageBottomSheetUM.Button.IconOrder.End) {
                    button.icon?.let(TangemIconUM::Icon)
                } else {
                    null
                },
                variant = if (button.isPrimary) {
                    TangemButton.Variant.Primary
                } else {
                    TangemButton.Variant.Secondary
                },
                size = TangemButton.Size.X12,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview() {
    TangemThemePreviewRedesign {
        MessageBottomSheet(
            messageBottomSheetUM {
                infoBlock {
                    icon(R.drawable.img_knight_shield_32) {
                        type = MessageBottomSheetUM.Icon.Type.Attention
                        backgroundType = MessageBottomSheetUM.Icon.BackgroundType.SameAsTint
                    }
                    title = TextReference.Str("Title Title Title")
                    body = TextReference.Str("Body")
                    chip(text = TextReference.Str("Some chip information"))
                }
                primaryButton {
                    text = TextReference.Str("Test")
                    icon = R.drawable.ic_tangem_24
                }
                secondaryButton {
                    icon = R.drawable.ic_tangem_24
                    text = TextReference.Str("asdasd")
                    onClick {
                        closeBs()
                    }
                }
            },
            onDismissRequest = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview2() {
    TangemThemePreviewRedesign {
        MessageBottomSheet(
            messageBottomSheetUM {
                infoBlock {
                    iconImage = MessageBottomSheetUM.IconImage(R.drawable.img_visa_notification)
                    title = TextReference.Str("Title Title Title")
                    body = TextReference.Str("Body")
                    chip(text = TextReference.Str("Some chip information"))
                }
                primaryButton {
                    text = TextReference.Str("Test")
                    icon = R.drawable.ic_tangem_24
                }
                secondaryButton {
                    icon = R.drawable.ic_tangem_24
                    text = TextReference.Str("asdasd")
                    onClick {
                        closeBs()
                    }
                }
            },
            onDismissRequest = {},
        )
    }
}