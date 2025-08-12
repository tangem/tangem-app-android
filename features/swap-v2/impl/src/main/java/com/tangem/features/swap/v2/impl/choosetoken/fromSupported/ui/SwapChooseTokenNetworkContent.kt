package com.tangem.features.swap.v2.impl.choosetoken.fromSupported.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.message.*
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheet
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.decorations.roundedShapeItemDecoration
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.swap.v2.impl.R
import com.tangem.features.swap.v2.impl.choosetoken.fromSupported.entity.SwapChooseNetworkUM
import com.tangem.features.swap.v2.impl.choosetoken.fromSupported.entity.SwapChooseTokenNetworkContentUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

@Composable
internal fun SwapChooseTokenNetworkBottomSheet(config: TangemBottomSheetConfig) {
    TangemModalBottomSheet<SwapChooseTokenNetworkContentUM>(
        config = config,
        containerColor = TangemTheme.colors.background.tertiary,
        title = {
            AnimatedContent(
                targetState = config.content is SwapChooseTokenNetworkContentUM.Content,
            ) { isContent ->
                TangemModalBottomSheetTitle(
                    title = resourceReference(R.string.common_choose_network).takeIf { isContent },
                    endIconRes = R.drawable.ic_close_24,
                    onEndClick = config.onDismissRequest,
                )
            }
        },
        content = { contentConfig ->
            SwapChooseTokenNetworkContent(contentConfig)
        },
    )
}

@Composable
internal fun SwapChooseTokenNetworkContent(state: SwapChooseTokenNetworkContentUM) {
    AnimatedContent(
        targetState = state,
        transitionSpec = { fadeIn().togetherWith(fadeOut()) },
    ) { targetState ->
        when (targetState) {
            is SwapChooseTokenNetworkContentUM.Content -> SwapChooseTokenNetworkContentList(targetState.swapNetworks)
            else -> {
                Box {
                    MessageBottomSheetV2Content(
                        state = targetState.messageContent,
                        modifier = Modifier
                            .conditional(targetState is SwapChooseTokenNetworkContentUM.Loading) {
                                alpha(0f)
                            },
                    )
                    if (targetState is SwapChooseTokenNetworkContentUM.Loading) {
                        CircularProgressIndicator(
                            color = TangemTheme.colors.icon.inactive,
                            modifier = Modifier
                                .align(Alignment.Center),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SwapChooseTokenNetworkContentList(swapNetworks: ImmutableList<SwapChooseNetworkUM>) {
    Column(
        modifier = Modifier.padding(
            top = 8.dp,
            start = 16.dp,
            end = 16.dp,
            bottom = 16.dp,
        ),
    ) {
        swapNetworks.fastForEachIndexed { index, network ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .roundedShapeItemDecoration(
                        currentIndex = index,
                        lastIndex = swapNetworks.lastIndex,
                        addDefaultPadding = false,
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(),
                        onClick = { network.onNetworkClick() },
                    )
                    .padding(vertical = 12.dp, horizontal = 14.dp),
            ) {
                Image(
                    modifier = Modifier.size(36.dp),
                    painter = painterResource(id = network.iconResId),
                    contentDescription = null,
                )
                Text(
                    modifier = Modifier.padding(start = 6.dp),
                    text = network.title.resolveReference(),
                    style = TangemTheme.typography.subtitle2,
                    color = TangemTheme.colors.text.primary1,
                )
                Text(
                    text = network.subtitle.resolveReference(),
                    style = TangemTheme.typography.caption1,
                    color = if (network.isMainNetwork) {
                        TangemTheme.colors.text.accent
                    } else {
                        TangemTheme.colors.text.tertiary
                    },
                )
                if (network.hasFixedRate) {
                    SpacerWMax()
                    Text(
                        text = stringResourceSafe(R.string.swap_fixed_rate),
                        style = TangemTheme.typography.body2,
                        color = TangemTheme.colors.text.tertiary,
                        modifier = Modifier
                            .border(
                                width = 1.dp,
                                color = TangemTheme.colors.stroke.primary,
                                shape = RoundedCornerShape(6.dp),
                            )
                            .padding(vertical = 2.dp, horizontal = 4.dp),
                    )
                }
            }
        }
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun SwapChooseTokenNetworkContent_Preview(
    @PreviewParameter(PreviewProvider::class) params: SwapChooseTokenNetworkContentUM,
) {
    TangemThemePreview {
        SwapChooseTokenNetworkContent(state = params)
    }
}

private class PreviewProvider : PreviewParameterProvider<SwapChooseTokenNetworkContentUM> {
    override val values: Sequence<SwapChooseTokenNetworkContentUM>
        get() = sequenceOf(
            SwapChooseTokenNetworkContentUM.Loading(
                messageContent = messageBottomSheetUM {
                    infoBlock {
                        icon(R.drawable.ic_alert_triangle_20) {
                            type = MessageBottomSheetUMV2.Icon.Type.Attention
                            backgroundType = MessageBottomSheetUMV2.Icon.BackgroundType.Unspecified
                        }
                        title = stringReference("Shiba Inu is not suppoerted")
                        body = stringReference("This token is not supported. Please choose a different token to swap.")
                    }
                    secondaryButton {
                        text = resourceReference(R.string.warning_button_ok)
                        onClick { closeBs() }
                    }
                },
            ),
            SwapChooseTokenNetworkContentUM.Error(
                messageContent = messageBottomSheetUM {
                    infoBlock {
                        icon(R.drawable.ic_alert_triangle_20) {
                            type = MessageBottomSheetUMV2.Icon.Type.Attention
                            backgroundType = MessageBottomSheetUMV2.Icon.BackgroundType.SameAsTint
                        }
                        title = stringReference("Shiba Inu is not supported")
                        body = stringReference("This token is not supported. Please choose a different token to swap.")
                    }
                    secondaryButton {
                        text = resourceReference(R.string.warning_button_ok)
                        onClick { closeBs() }
                    }
                },
            ),
            SwapChooseTokenNetworkContentUM.Content(
                messageContent = messageBottomSheetUM {
                    infoBlock {
                        icon(R.drawable.img_attention_20) {
                            type = MessageBottomSheetUMV2.Icon.Type.Attention
                            backgroundType = MessageBottomSheetUMV2.Icon.BackgroundType.SameAsTint
                        }
                        title = stringReference("Shiba Inu is not suppoerted")
                        body = stringReference("This token is not supported. Please choose a different token to swap.")
                    }
                    secondaryButton {
                        text = resourceReference(R.string.warning_button_ok)
                        onClick { closeBs() }
                    }
                },
                swapNetworks = persistentListOf(
                    SwapChooseNetworkUM(
                        title = stringReference("Cardano"),
                        subtitle = stringReference("MAIN"),
                        iconResId = R.drawable.img_cardano_22,
                        hasFixedRate = false,
                        onNetworkClick = {},
                        isMainNetwork = true,
                    ),
                ),
            ),
            SwapChooseTokenNetworkContentUM.Content(
                messageContent = messageBottomSheetUM {
                    infoBlock {
                        icon(R.drawable.img_attention_20) {
                            type = MessageBottomSheetUMV2.Icon.Type.Attention
                            backgroundType = MessageBottomSheetUMV2.Icon.BackgroundType.SameAsTint
                        }
                        title = stringReference("Shiba Inu is not suppoerted")
                        body = stringReference("This token is not supported. Please choose a different token to swap.")
                    }
                    secondaryButton {
                        text = resourceReference(R.string.warning_button_ok)
                        onClick { closeBs() }
                    }
                },
                swapNetworks = persistentListOf(
                    SwapChooseNetworkUM(
                        title = stringReference("Cardano"),
                        subtitle = stringReference("MAIN"),
                        iconResId = R.drawable.img_cardano_22,
                        hasFixedRate = false,
                        onNetworkClick = {},
                        isMainNetwork = true,
                    ),
                    SwapChooseNetworkUM(
                        title = stringReference("BNB Smart Chain"),
                        subtitle = stringReference("BEP20"),
                        iconResId = R.drawable.img_bsc_22,
                        hasFixedRate = false,
                        onNetworkClick = {},
                        isMainNetwork = false,
                    ),
                    SwapChooseNetworkUM(
                        title = stringReference("BNB Smart Chain"),
                        subtitle = stringReference("BEP20"),
                        iconResId = R.drawable.img_bsc_22,
                        hasFixedRate = true,
                        onNetworkClick = {},
                        isMainNetwork = false,
                    ),
                    SwapChooseNetworkUM(
                        title = stringReference("BNB Smart Chain"),
                        subtitle = stringReference("BEP20"),
                        iconResId = R.drawable.img_bsc_22,
                        hasFixedRate = false,
                        onNetworkClick = {},
                        isMainNetwork = false,
                    ),
                ),
            ),
            SwapChooseTokenNetworkContentUM.Content(
                messageContent = messageBottomSheetUM {
                    infoBlock {
                        icon(R.drawable.img_attention_20) {
                            type = MessageBottomSheetUMV2.Icon.Type.Attention
                            backgroundType = MessageBottomSheetUMV2.Icon.BackgroundType.SameAsTint
                        }
                        title = stringReference("Shiba Inu is not suppoerted")
                        body = stringReference("This token is not supported. Please choose a different token to swap.")
                    }
                    secondaryButton {
                        text = resourceReference(R.string.warning_button_ok)
                        onClick { closeBs() }
                    }
                },
                swapNetworks = buildList {
                    repeat(10) {
                        add(
                            SwapChooseNetworkUM(
                                title = stringReference("Cardano"),
                                subtitle = stringReference("MAIN"),
                                iconResId = R.drawable.img_cardano_22,
                                hasFixedRate = false,
                                onNetworkClick = {},
                                isMainNetwork = true,
                            ),
                        )
                    }
                }.toPersistentList(),
            ),
        )
}
// endregion