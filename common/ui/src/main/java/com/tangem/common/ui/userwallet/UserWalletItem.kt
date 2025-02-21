package com.tangem.common.ui.userwallet

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.common.ui.R
import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.ui.coil.RotationTransformation
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.block.BlockCard
import com.tangem.core.ui.components.block.TangemBlockCardColors
import com.tangem.core.ui.components.text.applyBladeBrush
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.StringsSigns.DASH_SIGN
import com.tangem.utils.StringsSigns.DOT
import com.tangem.utils.StringsSigns.THREE_STARS

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
                balance = state.balance,
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
private fun NameAndInfo(
    name: TextReference,
    information: TextReference,
    balance: UserWalletItemUM.Balance,
    modifier: Modifier = Modifier,
) {
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

        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = information.resolveReference() + " $DOT ",
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
                maxLines = 1,
            )

            AnimatedContent(
                targetState = balance,
                label = "Balance content",
            ) { balance ->
                val (balanceValue, isFlickering) = getBalanceValueAndFlickerState(balance)

                if (balanceValue == null) {
                    TextShimmer(
                        style = TangemTheme.typography.caption2,
                        text = "aaaaa",
                    )
                } else {
                    Text(
                        text = balanceValue,
                        style = TangemTheme.typography.caption2.applyBladeBrush(
                            isEnabled = isFlickering,
                            textColor = TangemTheme.colors.text.tertiary,
                        ),
                        maxLines = 1,
                    )
                }
            }
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

@Composable
fun getBalanceValueAndFlickerState(balance: UserWalletItemUM.Balance): Pair<String?, Boolean> {
    return when (balance) {
        is UserWalletItemUM.Balance.Failed -> DASH_SIGN to false
        is UserWalletItemUM.Balance.Hidden -> THREE_STARS to false
        is UserWalletItemUM.Balance.Loading -> null to false
        is UserWalletItemUM.Balance.Locked -> stringResource(R.string.common_locked) to false
        is UserWalletItemUM.Balance.Loaded -> balance.value to balance.isFlickering
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Preview_UserWalletItem(
    @PreviewParameter(UserWalletItemUMPreviewProvider::class) params: UserWalletItemUM,
) {
    TangemThemePreview {
        UserWalletItem(
            state = params,
        )
    }
}

private class UserWalletItemUMPreviewProvider : PreviewParameterProvider<UserWalletItemUM> {
    override val values: Sequence<UserWalletItemUM>
        get() = sequenceOf(
            UserWalletItemUM(
                id = UserWalletId("user_wallet_1".encodeToByteArray()),
                name = stringReference("My Wallet"),
                information = getInformation(cardCount = 1),
                balance = UserWalletItemUM.Balance.Locked,
                imageUrl = "",
                isEnabled = true,
                onClick = {},
            ),
            UserWalletItemUM(
                id = UserWalletId("user_wallet_2".encodeToByteArray()),
                name = stringReference("Old wallet"),
                information = getInformation(cardCount = 2),
                balance = UserWalletItemUM.Balance.Hidden,
                imageUrl = "",
                isEnabled = true,
                onClick = {},
                endIcon = UserWalletItemUM.EndIcon.Arrow,
            ),
            UserWalletItemUM(
                id = UserWalletId("user_wallet_3".encodeToByteArray()),
                name = stringReference("Multi Card"),
                information = getInformation(cardCount = 3),
                balance = UserWalletItemUM.Balance.Failed,
                imageUrl = "",
                isEnabled = false,
                endIcon = UserWalletItemUM.EndIcon.Checkmark,
                onClick = {},
            ),
            UserWalletItemUM(
                id = UserWalletId("user_wallet_3".encodeToByteArray()),
                name = stringReference("Multi Card"),
                information = getInformation(cardCount = 3),
                balance = UserWalletItemUM.Balance.Loading,
                imageUrl = "",
                isEnabled = false,
                endIcon = UserWalletItemUM.EndIcon.Checkmark,
                onClick = {},
            ),
            UserWalletItemUM(
                id = UserWalletId("user_wallet_3".encodeToByteArray()),
                name = stringReference("Multi Card"),
                information = getInformation(cardCount = 3),
                balance = UserWalletItemUM.Balance.Loaded(
                    value = "1.2345 BTC",
                    isFlickering = false,
                ),
                imageUrl = "",
                isEnabled = false,
                endIcon = UserWalletItemUM.EndIcon.Checkmark,
                onClick = {},
            ),
            UserWalletItemUM(
                id = UserWalletId("user_wallet_3".encodeToByteArray()),
                name = stringReference("Multi Card"),
                information = getInformation(cardCount = 3),
                balance = UserWalletItemUM.Balance.Loaded(
                    value = "1.2345 BTC",
                    isFlickering = true,
                ),
                imageUrl = "",
                isEnabled = false,
                endIcon = UserWalletItemUM.EndIcon.Checkmark,
                onClick = {},
            ),
        )

    private fun getInformation(cardCount: Int): TextReference {
        return TextReference.PluralRes(
            id = R.plurals.card_label_card_count,
            count = cardCount,
            formatArgs = wrappedList(cardCount),
        )
    }
}
// endregion Preview