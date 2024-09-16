package com.tangem.common.ui.userwallet

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CardColors
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.util.fastForEach
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.block.BlockCard
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.common.ui.R
import com.tangem.core.ui.coil.RotationTransformation
import com.tangem.core.ui.components.block.TangemBlockCardColors
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.collections.immutable.persistentListOf

@Composable
fun UserWalletItem(
    state: UserWalletItemUM,
    modifier: Modifier = Modifier,
    blockColors: CardColors = TangemBlockCardColors,
) {
    BlockCard(
        modifier = modifier,
        colors = blockColors,
        onClick = state.onClick,
        enabled = state.isEnabled,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = TangemTheme.dimens.size68)
                .padding(all = TangemTheme.dimens.spacing12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
        ) {
            CardImage(imageUrl = state.imageUrl)
            NameAndInfo(
                modifier = Modifier.weight(1f),
                name = state.name,
                information = state.information,
            )

            when (state.endIcon) {
                UserWalletItemUM.EndIcon.None -> {}
                UserWalletItemUM.EndIcon.Arrow -> {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_chevron_right_24),
                        tint = TangemTheme.colors.icon.informative,
                        contentDescription = null,
                    )
                }
                UserWalletItemUM.EndIcon.Checkmark -> {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_check_24),
                        tint = TangemTheme.colors.icon.accent,
                        contentDescription = null,
                    )
                }
            }
        }
    }
}

@Composable
private fun NameAndInfo(name: TextReference, information: TextReference, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.heightIn(min = TangemTheme.dimens.size40),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        Text(
            text = name.resolveReference(),
            style = TangemTheme.typography.subtitle1,
            color = TangemTheme.colors.text.primary1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        AnimatedContent(
            targetState = information.resolveReference(),
            label = "User wallet information",
        ) { information ->
            Text(
                text = information,
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CardImage(imageUrl: String, modifier: Modifier = Modifier) {
    val imageModifier = modifier
        .width(TangemTheme.dimens.size24)
        .height(TangemTheme.dimens.size36)
        .clip(TangemTheme.shapes.roundedCornersSmall)

    SubcomposeAsyncImage(
        modifier = imageModifier,
        model = ImageRequest.Builder(LocalContext.current)
            .transformations(RotationTransformation(angle = 90f))
            .size(
                width = with(LocalDensity.current) { TangemTheme.dimens.size36.roundToPx() },
                height = with(LocalDensity.current) { TangemTheme.dimens.size24.roundToPx() },
            )
            .data(imageUrl)
            .crossfade(enable = true)
            .allowHardware(enable = false)
            .build(),
        loading = {
            RectangleShimmer(
                modifier = imageModifier,
                radius = TangemTheme.dimens.size2,
            )
        },
        error = {
            Image(
                modifier = imageModifier,
                imageVector = ImageVector.vectorResource(R.drawable.img_card_wallet_2_gray_22_36),
                contentDescription = null,
            )
        },
        contentDescription = null,
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview() {
    TangemThemePreview {
        val list = persistentListOf(
            UserWalletItemUM(
                id = UserWalletId("user_wallet_1".encodeToByteArray()),
                name = stringReference("My Wallet"),
                information = getInformation(3, "4 496,75 $"),
                imageUrl = "",
                isEnabled = true,
                onClick = {},
            ),
            UserWalletItemUM(
                id = UserWalletId("user_wallet_2".encodeToByteArray()),
                name = stringReference("Old wallet"),
                information = getInformation(3, "4 496,75 $"),
                imageUrl = "",
                isEnabled = true,
                onClick = {},
                endIcon = UserWalletItemUM.EndIcon.Arrow,
            ),
            UserWalletItemUM(
                id = UserWalletId("user_wallet_3".encodeToByteArray()),
                name = stringReference("Multi Card"),
                information = getInformation(3, "4 496,75 $"),
                imageUrl = "",
                isEnabled = false,
                endIcon = UserWalletItemUM.EndIcon.Checkmark,
                onClick = {},
            ),
        )

        Column(
            Modifier
                .background(TangemTheme.colors.background.tertiary)
                .padding(TangemTheme.dimens.spacing12),
            verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
        ) {
            list.fastForEach { userWalletItemUM ->
                UserWalletItem(
                    modifier = Modifier.fillMaxWidth(),
                    state = userWalletItemUM,
                )
            }
        }
    }
}

private fun getInformation(cardCount: Int, totalBalance: String): TextReference {
    val t1 = TextReference.PluralRes(
        id = R.plurals.card_label_card_count,
        count = cardCount,
        formatArgs = wrappedList(cardCount),
    )
    val divider = stringReference(value = " • ")
    val t2 = stringReference(totalBalance)

    return TextReference.Combined(wrappedList(t1, divider, t2))
}