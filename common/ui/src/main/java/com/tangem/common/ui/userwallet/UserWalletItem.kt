package com.tangem.common.ui.userwallet

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardColors
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.common.ui.R
import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.TextShimmer
import com.tangem.core.ui.components.block.BlockCard
import com.tangem.core.ui.components.block.TangemBlockCardColors
import com.tangem.core.ui.components.label.Label
import com.tangem.core.ui.components.label.entity.LabelStyle
import com.tangem.core.ui.components.label.entity.LabelUM
import com.tangem.core.ui.components.text.applyBladeBrush
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.domain.models.wallet.UserWalletId
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
            CardImage(state.imageState)
            NameAndInfo(
                modifier = Modifier.weight(1f),
                name = state.name,
                information = state.information,
                balance = state.balance,
            )

            state.label?.let { Label(it) }

            when (state.endIcon) {
                UserWalletItemUM.EndIcon.None -> Unit
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
    information: UserWalletItemUM.Information,
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
            AnimatedContent(
                targetState = information,
                label = "Information content",
            ) { information ->
                val informationValue = getInformationValue(information)

                if (informationValue == null) {
                    TextShimmer(
                        style = TangemTheme.typography.caption2,
                        text = "aaaaa",
                    )
                } else {
                    Text(
                        text = informationValue,
                        style = TangemTheme.typography.caption2,
                        color = TangemTheme.colors.text.tertiary,
                        maxLines = 1,
                    )
                }
            }

            BalanceContent(balance)
        }
    }
}

@Composable
private fun BalanceContent(balance: UserWalletItemUM.Balance, modifier: Modifier = Modifier) {
    AnimatedContent(
        modifier = modifier,
        targetState = balance,
        label = "Balance content",
    ) { balance ->
        when (balance) {
            UserWalletItemUM.Balance.Locked -> {
                Icon(
                    modifier = Modifier
                        .padding(start = 4.dp, bottom = 2.dp, top = 2.dp)
                        .size(12.dp),
                    imageVector = ImageVector.vectorResource(R.drawable.ic_lock_24),
                    tint = TangemTheme.colors.icon.informative,
                    contentDescription = null,
                )
            }
            UserWalletItemUM.Balance.NotShowing -> {
                /** No balance and no dot */
            }
            else -> {
                Row {
                    Text(
                        text = " $DOT ",
                        style = TangemTheme.typography.caption2,
                        color = TangemTheme.colors.text.tertiary,
                        maxLines = 1,
                    )

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
}

@Suppress("MagicNumber")
@Composable
fun CardImage(imageState: UserWalletItemUM.ImageState, modifier: Modifier = Modifier) {
    val imageModifier = modifier
        .width(TangemTheme.dimens.size36)
        .height(TangemTheme.dimens.size24)
        .clip(TangemTheme.shapes.roundedCornersSmall)

    when (imageState) {
        is UserWalletItemUM.ImageState.Loading -> {
            RectangleShimmer(
                modifier = imageModifier,
                radius = TangemTheme.dimens.size2,
            )
        }
        is UserWalletItemUM.ImageState.MobileWallet -> {
            Image(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = TangemTheme.colors.field.focused,
                        shape = RoundedCornerShape(10.dp),
                    )
                    .padding(6.dp),
                imageVector = ImageVector.vectorResource(R.drawable.ic_mobile_wallet_icon_24),
                contentDescription = null,
            )
        }
        is UserWalletItemUM.ImageState.Image -> {
            val verifiedArtwork = imageState.artwork.verifiedArtwork
            if (verifiedArtwork != null) {
                Image(
                    modifier = imageModifier,
                    painter = BitmapPainter(verifiedArtwork),
                    contentScale = ContentScale.Fit,
                    contentDescription = null,
                )
            } else {
                SubcomposeAsyncImage(
                    modifier = imageModifier,
                    model = ImageRequest.Builder(LocalContext.current)
                        .size(
                            width = with(LocalDensity.current) { TangemTheme.dimens.size36.roundToPx() },
                            height = with(LocalDensity.current) { TangemTheme.dimens.size24.roundToPx() },
                        )
                        .data(imageState.artwork.defaultUrl)
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
                            modifier = imageModifier.then(Modifier.rotate(90F)),
                            imageVector = ImageVector.vectorResource(R.drawable.img_card_wallet_2_gray_22_36),
                            contentDescription = null,
                        )
                    },
                    contentDescription = null,
                )
            }
        }
    }
}

@Composable
fun getBalanceValueAndFlickerState(balance: UserWalletItemUM.Balance): Pair<String?, Boolean> {
    return when (balance) {
        is UserWalletItemUM.Balance.Failed -> DASH_SIGN to false
        is UserWalletItemUM.Balance.Hidden -> THREE_STARS to false
        is UserWalletItemUM.Balance.Loaded -> balance.value to balance.isFlickering
        else -> null to false
    }
}

@Composable
fun getInformationValue(information: UserWalletItemUM.Information): String? {
    return when (information) {
        UserWalletItemUM.Information.Failed -> DASH_SIGN
        UserWalletItemUM.Information.Loading -> null
        is UserWalletItemUM.Information.Loaded -> information.value.resolveReference()
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
                id = UserWalletId("user_wallet_0".encodeToByteArray()),
                name = stringReference("Mobile Wallet"),
                information = getInformation(cardCount = 1),
                balance = UserWalletItemUM.Balance.Locked,
                label = LabelUM(
                    text = resourceReference(R.string.hw_backup_no_backup),
                    style = LabelStyle.WARNING,
                ),
                isEnabled = true,
                onClick = {},
            ),
            UserWalletItemUM(
                id = UserWalletId("user_wallet_1".encodeToByteArray()),
                name = stringReference("My Wallet"),
                information = getInformation(cardCount = 1),
                balance = UserWalletItemUM.Balance.Locked,
                isEnabled = true,
                onClick = {},
            ),
            UserWalletItemUM(
                id = UserWalletId("user_wallet_2".encodeToByteArray()),
                name = stringReference("Old wallet"),
                information = getInformation(cardCount = 2),
                balance = UserWalletItemUM.Balance.Hidden,
                isEnabled = true,
                onClick = {},
                endIcon = UserWalletItemUM.EndIcon.Arrow,
            ),
            UserWalletItemUM(
                id = UserWalletId("user_wallet_3".encodeToByteArray()),
                name = stringReference("Multi Card"),
                information = getInformation(cardCount = 3),
                balance = UserWalletItemUM.Balance.Failed,
                isEnabled = false,
                endIcon = UserWalletItemUM.EndIcon.Checkmark,
                onClick = {},
            ),
            UserWalletItemUM(
                id = UserWalletId("user_wallet_3".encodeToByteArray()),
                name = stringReference("Multi Card"),
                information = getInformation(cardCount = 3),
                balance = UserWalletItemUM.Balance.Loading,
                isEnabled = false,
                endIcon = UserWalletItemUM.EndIcon.Checkmark,
                onClick = {},
            ),
            UserWalletItemUM(
                id = UserWalletId("user_wallet_3".encodeToByteArray()),
                name = stringReference("Multi Card"),
                information = UserWalletItemUM.Information.Loading,
                balance = UserWalletItemUM.Balance.Loading,
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
                isEnabled = false,
                endIcon = UserWalletItemUM.EndIcon.Checkmark,
                onClick = {},
            ),
            UserWalletItemUM(
                id = UserWalletId("user_wallet_3".encodeToByteArray()),
                name = stringReference("Multi Card"),
                information = UserWalletItemUM.Information.Loading,
                balance = UserWalletItemUM.Balance.Loaded(
                    value = "1.2345 BTC",
                    isFlickering = false,
                ),
                isEnabled = true,
                onClick = {},
            ),
            UserWalletItemUM(
                id = UserWalletId("user_wallet_3".encodeToByteArray()),
                name = stringReference("Multi Card"),
                information = UserWalletItemUM.Information.Failed,
                balance = UserWalletItemUM.Balance.Loaded(
                    value = "1.2345 BTC",
                    isFlickering = false,
                ),
                isEnabled = true,
                onClick = {},
            ),
            UserWalletItemUM(
                id = UserWalletId("user_wallet_3".encodeToByteArray()),
                name = stringReference("Multi Card"),
                information = UserWalletItemUM.Information.Failed,
                balance = UserWalletItemUM.Balance.Loaded(
                    value = "1.2345 BTC",
                    isFlickering = false,
                ),
                imageState = UserWalletItemUM.ImageState.MobileWallet,
                isEnabled = true,
                onClick = {},
            ),
            UserWalletItemUM(
                id = UserWalletId("user_wallet_3".encodeToByteArray()),
                name = stringReference("Multi Card"),
                information = getInformation(cardCount = 3),
                balance = UserWalletItemUM.Balance.NotShowing,
                imageState = UserWalletItemUM.ImageState.MobileWallet,
                isEnabled = true,
                onClick = {},
            ),
        )

    private fun getInformation(cardCount: Int): UserWalletItemUM.Information.Loaded {
        val text = TextReference.PluralRes(
            id = R.plurals.card_label_card_count,
            count = cardCount,
            formatArgs = wrappedList(cardCount),
        )
        return UserWalletItemUM.Information.Loaded(text)
    }
}
// endregion Preview