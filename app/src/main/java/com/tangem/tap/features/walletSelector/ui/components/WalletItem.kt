@file:OptIn(ExperimentalComposeUiApi::class)

package com.tangem.tap.features.walletSelector.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.tangem.tap.features.walletSelector.ui.model.UserWalletItem
import com.tangem.wallet.R

@Composable
internal fun WalletItem(
    modifier: Modifier = Modifier,
    wallet: UserWalletItem,
    isSelected: Boolean,
    isChecked: Boolean,
    isLocked: Boolean,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(TangemTheme.dimens.size62)
                .height(TangemTheme.dimens.size42),
        ) {
            val cardImageModifier = Modifier
                .align(Alignment.Center)
                .width(TangemTheme.dimens.size56)
                .height(TangemTheme.dimens.size32)
                .clip(TangemTheme.shapes.roundedCornersSmall)
            SubcomposeAsyncImage(
                modifier = cardImageModifier,
                model = ImageRequest.Builder(LocalContext.current)
                    .data(wallet.imageUrl)
                    .crossfade(true)
                    .build(),
                loading = { CardImagePlaceholder(modifier = cardImageModifier) },
                error = { CardImagePlaceholder(modifier = cardImageModifier) },
                contentDescription = null,
            )
            when {
                isChecked -> {
                    Box(
                        modifier = cardImageModifier
                            .background(
                                color = TangemTheme.colors.icon.accent.copy(alpha = .5f),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_check_24),
                            tint = TangemTheme.colors.icon.primary2,
                            contentDescription = null,
                        )
                    }
                }
                isSelected -> {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(TangemTheme.dimens.size18)
                            .background(
                                color = Color.White,
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            modifier = Modifier
                                .padding(all = (0.5).dp),
                            painter = painterResource(id = R.drawable.ic_check_circle_18),
                            tint = TangemTheme.colors.icon.accent,
                            contentDescription = null,
                        )
                    }
                }
                else -> Unit
            }
        }
        SpacerW8()
        Column(
            modifier = Modifier.weight(weight = .6f),
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
                text = when (val type = wallet.type) {
                    is UserWalletItem.Type.MultiCurrency -> pluralStringResource(
                        id = R.plurals.card_label_card_count,
                        count = type.cardsInWallet,
                        type.cardsInWallet,
                    )

                    is UserWalletItem.Type.SingleCurrency -> type.tokenName
                },
                style = TangemTheme.typography.caption,
                color = TangemTheme.colors.text.tertiary,
            )
        }
        SpacerW6()
        if (isLocked) {
            Box(
                modifier = Modifier
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
        } else {
            Column(
                modifier = Modifier.weight(weight = .4f),
                verticalArrangement = Arrangement.SpaceAround,
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = wallet.balance.amount,
                    style = TangemTheme.typography.subtitle1,
                    color = TangemTheme.colors.text.primary1,
                    textAlign = TextAlign.End,
                )
                when (val type = wallet.type) {
                    is UserWalletItem.Type.MultiCurrency -> {
                        SpacerH2()
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = pluralStringResource(
                                id = R.plurals.tokens_count,
                                count = type.tokensCount,
                                type.tokensCount,
                            ),
                            style = TangemTheme.typography.caption,
                            color = TangemTheme.colors.text.tertiary,
                            textAlign = TextAlign.End,
                        )
                    }

                    is UserWalletItem.Type.SingleCurrency -> Unit
                }
            }
        }
    }
}

@Composable
private fun CardImagePlaceholder(
    modifier: Modifier = Modifier,
) {
    Image(
        modifier = modifier,
        painter = painterResource(id = R.drawable.card_placeholder_black),
        contentDescription = null,
    )
}