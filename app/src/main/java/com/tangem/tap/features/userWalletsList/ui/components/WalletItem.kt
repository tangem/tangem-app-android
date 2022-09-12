@file:OptIn(ExperimentalComposeUiApi::class)

package com.tangem.tap.features.userWalletsList.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.google.accompanist.appcompattheme.AppCompatTheme
import com.tangem.tap.common.compose.TangemTypography
import com.tangem.tap.common.entities.FiatCurrency
import com.tangem.tap.features.userWalletsList.ui.Mock
import com.tangem.tap.features.userWalletsList.ui.WalletSelectorScreenState
import com.tangem.wallet.R

@Composable
internal fun WalletItem(
    modifier: Modifier = Modifier,
    wallet: WalletSelectorScreenState.UserWallet,
    isSelected: Boolean,
    fiatCurrency: FiatCurrency,
    isChecked: Boolean,
    isLocked: Boolean,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(62.dp)
                .height(42.dp),
        ) {
            val cardImageModifier = Modifier
                .align(Alignment.Center)
                .width(50.dp)
                .height(30.dp)
                .clip(RoundedCornerShape(2.dp))
            SubcomposeAsyncImage(
                modifier = cardImageModifier,
                model = ImageRequest.Builder(LocalContext.current)
                    .data(wallet.imageUrl)
                    .crossfade(true)
                    .build(),
                loading = { CardImagePlaceholder(modifier = cardImageModifier) },
                error = { CardImagePlaceholder(modifier = cardImageModifier) },
                contentDescription = "${wallet.name} logo",
            )
            when {
                isChecked -> {
                    Box(
                        modifier = cardImageModifier
                            .background(
                                color = colorResource(id = R.color.accent_disabled),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_check_24),
                            tint = colorResource(id = R.color.icon_primary_2),
                            contentDescription = null,
                        )
                    }
                }

                isSelected -> {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(18.dp)
                            .background(color = Color.White, shape = CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            modifier = Modifier
                                .padding(all = (0.5).dp),
                            painter = painterResource(id = R.drawable.ic_check_circle_18),
                            tint = colorResource(id = R.color.icon_accent),
                            contentDescription = null,
                        )
                    }
                }

                else -> Unit
            }
        }
        Spacer(modifier = Modifier.width(6.dp))
        Column(
            modifier = Modifier.weight(weight = .6f),
            verticalArrangement = Arrangement.SpaceAround,
        ) {
            val nameColorRes = if (isSelected) R.color.text_accent else R.color.text_primary_1
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = wallet.name,
                color = colorResource(id = nameColorRes),
                style = TangemTypography.subtitle1,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = when (val type = wallet.type) {
                    is WalletSelectorScreenState.UserWallet.Type.MultiCurrency -> pluralStringResource(
                        id = R.plurals.card_label_card_count,
                        count = type.cardsInWallet,
                        type.cardsInWallet,
                    )

                    is WalletSelectorScreenState.UserWallet.Type.SingleCurrency -> type.tokenName
                },
                color = colorResource(id = R.color.text_tertiary),
                style = TangemTypography.caption,
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        if (isLocked) {
            Box(
                modifier = Modifier
                    .padding(vertical = 2.dp)
                    .background(
                        color = colorResource(id = R.color.button_secondary),
                        shape = RoundedCornerShape(size = 8.dp),
                    ),
            ) {
                Icon(
                    modifier = Modifier
                        .padding(
                            vertical = 8.dp,
                            horizontal = 12.dp,
                        )
                        .size(20.dp),
                    painter = painterResource(id = R.drawable.ic_locked_24),
                    tint = colorResource(id = R.color.icon_primary_1),
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
                    text = "${wallet.amount} ${fiatCurrency.symbol}",
                    color = colorResource(id = R.color.text_primary_1),
                    style = TangemTypography.subtitle1,
                    textAlign = TextAlign.End,
                )
                when (val type = wallet.type) {
                    is WalletSelectorScreenState.UserWallet.Type.MultiCurrency -> {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = pluralStringResource(
                                id = R.plurals.tokens_count,
                                count = type.tokensCount,
                                type.tokensCount,
                            ),
                            color = colorResource(id = R.color.text_tertiary),
                            style = TangemTypography.caption,
                            textAlign = TextAlign.End,
                        )
                    }

                    is WalletSelectorScreenState.UserWallet.Type.SingleCurrency -> Unit
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

// region Preview
@Composable
private fun WalletItemSample(
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Mock.userWalletList.forEachIndexed { index, wallet ->
            WalletItem(
                modifier = Modifier.padding(16.dp),
                wallet = wallet,
                isSelected = index == 0,
                fiatCurrency = FiatCurrency.Default,
                isChecked = index == 2,
                isLocked = index == 1,
            )
            Divider()
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun WalletItemPreview() {
    AppCompatTheme {
        WalletItemSample()
    }
}
// endregion Preview
