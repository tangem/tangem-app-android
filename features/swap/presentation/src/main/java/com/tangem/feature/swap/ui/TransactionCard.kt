package com.tangem.feature.swap.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.R
import com.tangem.core.ui.components.FontSizeRange
import com.tangem.core.ui.components.ResizableText
import com.tangem.core.ui.components.SpacerH4
import com.tangem.core.ui.components.SpacerH8
import com.tangem.core.ui.components.SpacerW16
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.swap.models.TransactionCardType
import com.valentinilk.shimmer.shimmer

@Suppress("LongParameterList")
@Composable
fun TransactionCard(
    type: TransactionCardType,
    balance: String,
    tokenIconUrl: String,
    tokenCurrency: String,
    amount: String?,
    amountEquivalent: String?,
    modifier: Modifier = Modifier,
    @DrawableRes iconPlaceholder: Int? = null,
    @DrawableRes networkIconRes: Int? = null,
    onChangeTokenClick: (() -> Unit)? = null,
) {
    Card(
        shape = RoundedCornerShape(TangemTheme.dimens.radius12),
        backgroundColor = TangemTheme.colors.background.primary,
        elevation = TangemTheme.dimens.elevation2,
        modifier = modifier,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start,
            ) {
                Header(balance = balance, type = type)

                Content(
                    type = type,
                    amount = amount,
                    amountEquivalent = amountEquivalent,
                )
            }

            Box(modifier = Modifier.align(Alignment.BottomEnd)) {
                Token(
                    tokenIconUrl = tokenIconUrl,
                    tokenCurrency = tokenCurrency,
                    networkIconRes = networkIconRes,
                    iconPlaceholder = iconPlaceholder,
                )
            }

            if (onChangeTokenClick != null) {
                Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                    ChangeTokenSelector()
                }
                Box(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .height(TangemTheme.dimens.size116)
                        .width(TangemTheme.dimens.size102)
                        .clickable(
                            indication = rememberRipple(bounded = false),
                            interactionSource = remember { MutableInteractionSource() },
                        ) { onChangeTokenClick() },
                )
            }
        }
    }
}

@Composable
private fun Header(
    type: TransactionCardType,
    balance: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                bottom = TangemTheme.dimens.spacing8,
                top = TangemTheme.dimens.spacing12,
                start = TangemTheme.dimens.spacing16,
                end = TangemTheme.dimens.spacing16,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val title = when (type) {
            is TransactionCardType.ReceiveCard -> R.string.exchange_receive_view_header
            is TransactionCardType.SendCard -> R.string.exchange_send_view_header
        }
        Text(
            text = stringResource(id = title),
            color = TangemTheme.colors.text.tertiary,
            maxLines = 1,
            style = MaterialTheme.typography.subtitle2,
            modifier = Modifier.defaultMinSize(minHeight = TangemTheme.dimens.size24),
        )
        SpacerW16()
        if (balance.isNotBlank()) {
            Text(
                text = stringResource(R.string.common_balance, balance),
                color = TangemTheme.colors.text.tertiary,
                style = MaterialTheme.typography.body2,
                modifier = Modifier
                    .defaultMinSize(minHeight = TangemTheme.dimens.size20)
                    .padding(top = TangemTheme.dimens.spacing2),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(width = TangemTheme.dimens.size80, height = TangemTheme.dimens.size12)
                    .shimmer()
                    .background(
                        color = TangemTheme.colors.button.secondary,
                        shape = RoundedCornerShape(TangemTheme.dimens.radius3),
                    ),
            )
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun Content(
    type: TransactionCardType,
    amount: String?,
    amountEquivalent: String?,
) {
    Row(
        modifier = Modifier
            .padding(
                start = TangemTheme.dimens.spacing16,
                bottom = TangemTheme.dimens.spacing12,
            ),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier
                .padding(
                    end = TangemTheme.dimens.spacing92,
                ),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start,
        ) {
            val sumTextModifier = Modifier.defaultMinSize(minHeight = TangemTheme.dimens.size32)
            when (type) {
                is TransactionCardType.ReceiveCard -> {
                    if (amount != null) {
                        ResizableText(
                            text = amount,
                            color = TangemTheme.colors.text.primary1,
                            style = TangemTheme.typography.h2,
                            fontSizeRange = FontSizeRange(min = 16.sp, max = TangemTheme.typography.h2.fontSize),
                            modifier = sumTextModifier,
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .padding(vertical = TangemTheme.dimens.spacing4)
                                .size(width = TangemTheme.dimens.size102, height = TangemTheme.dimens.size24)
                                .shimmer()
                                .background(
                                    color = TangemTheme.colors.button.secondary,
                                    shape = RoundedCornerShape(TangemTheme.dimens.radius6),
                                ),
                        )
                    }
                }
                is TransactionCardType.SendCard -> {
                    AutoSizeTextField(
                        modifier = sumTextModifier,
                        amount = amount ?: "",
                        onAmountChange = { type.onAmountChanged(it) },
                        onFocusChange = type.onFocusChanged,
                    )
                }
            }

            SpacerH8()

            if (amountEquivalent != null) {
                Text(
                    text = amountEquivalent,
                    color = TangemTheme.colors.text.tertiary,
                    style = TangemTheme.typography.body2,
                    modifier = Modifier.defaultMinSize(minHeight = TangemTheme.dimens.size20),
                )
            } else {
                Box(
                    modifier = Modifier
                        .padding(vertical = TangemTheme.dimens.spacing4)
                        .size(width = TangemTheme.dimens.size40, height = TangemTheme.dimens.size12)
                        .shimmer()
                        .background(
                            color = TangemTheme.colors.button.secondary,
                            shape = RoundedCornerShape(TangemTheme.dimens.radius3),
                        ),
                )
            }
        }
    }
}

@Suppress("MagicNumber")
@Composable
fun Token(
    tokenIconUrl: String,
    tokenCurrency: String,
    @DrawableRes iconPlaceholder: Int? = null,
    @DrawableRes networkIconRes: Int? = null,
) {
    Column(
        modifier = Modifier
            .padding(
                end = TangemTheme.dimens.spacing12,
                bottom = TangemTheme.dimens.spacing12,
            ),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.End,
    ) {
        Box(
            modifier = Modifier
                .padding(end = TangemTheme.dimens.spacing16)
                .size(TangemTheme.dimens.size42),
        ) {
            val tokenImageModifier = Modifier
                .align(Alignment.BottomStart)
                .size(TangemTheme.dimens.size36)

            val data = tokenIconUrl.ifEmpty {
                iconPlaceholder
            }
            SubcomposeAsyncImage(
                modifier = tokenImageModifier,
                model = ImageRequest.Builder(LocalContext.current)
                    .data(data)
                    .crossfade(true)
                    .build(),
                loading = { TokenImageShimmer(modifier = tokenImageModifier) },
                // error = { CurrencyPlaceholderIcon(modifier = tokenImageModifier, tokenCurrency) },
                contentDescription = tokenCurrency,
            )

            if (networkIconRes != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(TangemTheme.dimens.size18)
                        .background(color = Color.White, shape = CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        modifier = Modifier.padding(all = 0.5.dp),
                        painter = painterResource(id = networkIconRes),
                        contentDescription = null,
                    )
                }
            }
        }

        SpacerH4()

        Text(
            text = tokenCurrency,
            color = TangemTheme.colors.text.primary1,
            maxLines = 1,
            style = TangemTheme.typography.subtitle2,
            textAlign = TextAlign.Center,
            modifier = Modifier.defaultMinSize(minWidth = TangemTheme.dimens.size80),
        )
    }
}

@Composable
fun ChangeTokenSelector() {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .padding(
                top = TangemTheme.dimens.spacing12,
                start = TangemTheme.dimens.spacing24,
                end = TangemTheme.dimens.spacing12,
            ),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Icon(
            modifier = Modifier.size(TangemTheme.dimens.size20),
            painter = painterResource(id = R.drawable.ic_chevron_24),
            contentDescription = null,
        )
    }
}

@Composable
private fun TokenImageShimmer(
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.shimmer()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    color = TangemTheme.colors.button.secondary,
                    shape = CircleShape,
                ),
        )
    }
}

// region preview

@Preview(widthDp = 328, heightDp = 116, showBackground = true)
@Composable
private fun Preview_SwapMainCard_InLightTheme() {
    TangemTheme(isDark = false) {
        TransactionCardPreview()
    }
}

@Preview(widthDp = 328, heightDp = 116, showBackground = true)
@Composable
private fun Preview_SwapMainCard_InDarkTheme() {
    TangemTheme(isDark = true) {
        TransactionCardPreview()
    }
}

@Composable
private fun TransactionCardPreview() {
    TransactionCard(
        type = TransactionCardType.SendCard({}) {},
        amount = "1 000 000",
        amountEquivalent = "1 000 000",
        tokenIconUrl = "",
        tokenCurrency = "DAI",
        networkIconRes = R.drawable.img_polygon_22,
        onChangeTokenClick = {},
        balance = "123",
    )
}

// endregion preview
