package com.tangem.tap.features.walletSelector.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.components.SpacerH2
import com.tangem.core.ui.components.SpacerW6
import com.tangem.core.ui.components.SpacerW8
import com.tangem.core.ui.res.TangemTheme
import com.tangem.tap.common.compose.TangemTypography
import com.tangem.tap.features.walletSelector.ui.model.MultiCurrencyUserWalletItem
import com.tangem.tap.features.walletSelector.ui.model.SingleCurrencyUserWalletItem
import com.tangem.tap.features.walletSelector.ui.model.UserWalletItem
import com.tangem.wallet.R
import com.valentinilk.shimmer.shimmer

@Composable
internal fun WalletItem(
    modifier: Modifier = Modifier,
    wallet: UserWalletItem,
    isSelected: Boolean,
    isChecked: Boolean,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WalletCardImage(
            cardImageUrl = wallet.imageUrl,
            isChecked = isChecked,
            isSelected = isSelected,
        )
        SpacerW8()
        WalletInfo(
            modifier = Modifier.weight(weight = .6f),
            wallet = wallet,
            isSelected = isSelected,
        )
        SpacerW6()
        TokensInfo(
            modifier = Modifier.weight(weight = .4f),
            isLocked = wallet.isLocked,
            balance = wallet.balance,
            tokensCount = (wallet as? MultiCurrencyUserWalletItem)?.tokensCount,
        )
    }
}

@Composable
private fun WalletCardImage(
    modifier: Modifier = Modifier,
    cardImageUrl: String,
    isChecked: Boolean,
    isSelected: Boolean,
) {
    Box(
        modifier = modifier
            .width(TangemTheme.dimens.size62)
            .height(TangemTheme.dimens.size42),
    ) {
        val cardImageModifier = Modifier
            .align(Alignment.Center)
            .width(TangemTheme.dimens.size56)
            .height(TangemTheme.dimens.size32)
            .clip(TangemTheme.shapes.roundedCornersSmall2)

        val tintColor = TangemTheme.colors.icon.accent.copy(alpha = .6f)
        val checkedColorFilter = remember(isChecked) {
            if (isChecked) {
                ColorFilter.tint(
                    color = tintColor,
                    blendMode = BlendMode.SrcOver,
                )
            } else null
        }

        SubcomposeAsyncImage(
            modifier = cardImageModifier,
            model = ImageRequest.Builder(LocalContext.current)
                .data(cardImageUrl)
                .crossfade(true)
                .build(),
            loading = { CardImageShimmer(modifier = cardImageModifier) },
            error = { CardImagePlaceholder(modifier = cardImageModifier) },
            colorFilter = checkedColorFilter,
            contentDescription = null,
        )

        when {
            isChecked -> {
                CheckedWalletMark(
                    modifier = Modifier.matchParentSize(),
                )
            }
            isSelected -> {
                SelectedWalletBadge(
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun WalletInfo(
    modifier: Modifier = Modifier,
    wallet: UserWalletItem,
    isSelected: Boolean,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceAround,
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = wallet.name,
            color = if (isSelected) TangemTheme.colors.text.accent else TangemTheme.colors.text.primary1,
            style = TangemTypography.subtitle1,
        )
        SpacerH2()
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = when (wallet) {
                is MultiCurrencyUserWalletItem -> pluralStringResource(
                    id = R.plurals.card_label_card_count,
                    count = wallet.cardsInWallet,
                    wallet.cardsInWallet,
                )
                is SingleCurrencyUserWalletItem -> wallet.tokenName
            },
            style = TangemTheme.typography.caption,
            color = TangemTheme.colors.text.tertiary,
        )
    }
}

@Composable
private fun TokensInfo(
    modifier: Modifier = Modifier,
    isLocked: Boolean,
    balance: UserWalletItem.Balance,
    tokensCount: Int?,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceAround,
        horizontalAlignment = Alignment.End,
    ) {
        if (isLocked) {
            LockedPlaceholder()
        } else {
            if (balance.isLoading) {
                LoadingTokensInfo(
                    isMultiCurrencyWallet = tokensCount != null,
                )
            } else {
                LoadedTokensInfo(
                    balanceAmount = balance.amount,
                    tokensCount = tokensCount,
                )
            }
        }
    }
}

@Composable
private fun LoadingTokensInfo(
    modifier: Modifier = Modifier,
    isMultiCurrencyWallet: Boolean,
) {
    Column(
        modifier = modifier.shimmer(),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.SpaceAround,
    ) {
        Box(
            modifier = Modifier
                .width(TangemTheme.dimens.size62)
                .height(TangemTheme.dimens.size16)
                .background(
                    color = TangemTheme.colors.button.secondary,
                    shape = TangemTheme.shapes.roundedCornersSmall2,
                ),
        )
        if (isMultiCurrencyWallet) {
            SpacerH2()
            Box(
                modifier = Modifier
                    .width(TangemTheme.dimens.size48)
                    .height(TangemTheme.dimens.size12)
                    .background(
                        color = TangemTheme.colors.button.secondary,
                        shape = TangemTheme.shapes.roundedCornersSmall2,
                    ),
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun LoadedTokensInfo(
    modifier: Modifier = Modifier,
    balanceAmount: String,
    tokensCount: Int?,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
    ) {
        Text(
            text = balanceAmount,
            style = TangemTheme.typography.subtitle1,
            color = TangemTheme.colors.text.primary1,
            textAlign = TextAlign.End,
        )
        if (tokensCount != null) {
            SpacerH2()
            Text(
                text = pluralStringResource(
                    id = R.plurals.tokens_count,
                    count = tokensCount,
                    tokensCount,
                ),
                style = TangemTheme.typography.caption,
                color = TangemTheme.colors.text.tertiary,
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
private fun LockedPlaceholder(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .padding(vertical = TangemTheme.dimens.spacing2)
            .background(
                color = TangemTheme.colors.button.secondary,
                shape = TangemTheme.shapes.roundedCornersMedium,
            ),
    ) {
        Icon(
            modifier = Modifier
                .padding(
                    vertical = TangemTheme.dimens.spacing8,
                    horizontal = TangemTheme.dimens.spacing12,
                )
                .size(TangemTheme.dimens.size20),
            painter = painterResource(id = R.drawable.ic_locked_24),
            tint = TangemTheme.colors.icon.primary1,
            contentDescription = null,
        )
    }
}

@Composable
private fun CardImagePlaceholder(
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Image(
            modifier = Modifier.matchParentSize(),
            painter = painterResource(id = R.drawable.card_placeholder_black),
            contentDescription = null,
        )
    }
}

@Composable
private fun CardImageShimmer(
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.shimmer()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(TangemTheme.colors.button.secondary),
        )
    }
}

@Composable
private fun SelectedWalletBadge(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(TangemTheme.dimens.size18)
            .background(
                color = Color.White,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.padding(all = (0.5).dp),
            painter = painterResource(id = R.drawable.ic_check_circle_18),
            tint = TangemTheme.colors.icon.accent,
            contentDescription = null,
        )
    }
}

@Composable
private fun CheckedWalletMark(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_check_24),
            tint = TangemTheme.colors.icon.primary2,
            contentDescription = null,
        )
    }
}
