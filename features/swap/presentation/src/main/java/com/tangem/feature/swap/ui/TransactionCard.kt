package com.tangem.feature.swap.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.R
import com.tangem.core.ui.components.CurrencyPlaceholderIcon
import com.tangem.core.ui.components.FontSizeRange
import com.tangem.core.ui.components.ResizableText
import com.tangem.core.ui.components.SpacerH4
import com.tangem.core.ui.components.SpacerW12
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.swap.models.TransactionCardType
import com.valentinilk.shimmer.shimmer

@Composable
fun TransactionCard(
    modifier: Modifier = Modifier,
    type: TransactionCardType,
    amount: String?,
    amountEquivalent: String?,
    tokenIconUrl: String,
    tokenCurrency: String,
    @DrawableRes networkIconRes: Int? = null,
    onChangeTokenClick: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(TangemTheme.dimens.radius12),
        backgroundColor = TangemTheme.colors.background.primary,
        elevation = TangemTheme.dimens.elevation2,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TangemTheme.dimens.size116),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start,
            ) {

                Header(type = type)

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
                        .clickable { onChangeTokenClick() },
                )
            }
        }

    }
}

@Composable
private fun Header(
    type: TransactionCardType,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                bottom = dimensionResource(id = R.dimen.spacing8),
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
            modifier = Modifier.padding(top = TangemTheme.dimens.spacing12),
        )
        if (type is TransactionCardType.SendCard) {
            Text(
                text = stringResource(R.string.common_balance, type.balance),
                color = TangemTheme.colors.text.tertiary,
                maxLines = 1,
                style = MaterialTheme.typography.body2,
                modifier = Modifier.padding(top = TangemTheme.dimens.spacing12),
            )
        }
    }
}

@Composable
private fun Content(
    type: TransactionCardType,
    amount: String?,
    amountEquivalent: String?,
) {
    Row(
        modifier = Modifier
            .padding(
                top = TangemTheme.dimens.spacing8,
                start = TangemTheme.dimens.spacing16,
                bottom = TangemTheme.dimens.spacing16,
            ),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {

        if (type is TransactionCardType.SendCard && !type.permissionIsGiven) {
            Box(
                modifier = Modifier
                    .size(TangemTheme.dimens.size56)
                    .background(
                        color = TangemTheme.colors.button.secondary,
                        shape = RoundedCornerShape(TangemTheme.dimens.size12),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_locked_24),
                    contentDescription = null,
                    tint = TangemTheme.colors.icon.primary1,
                )
            }

            SpacerW12()
        }

        Column(
            modifier = Modifier
                .padding(
                    end = TangemTheme.dimens.spacing92,
                ),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start,
        ) {

            if (amount != null) {
                ResizableText(
                    text = amount,
                    color = TangemTheme.colors.text.primary1,
                    style = TangemTheme.typography.h2,
                    fontSizeRange =
                    FontSizeRange(min = 16.sp, max = TangemTheme.typography.h2.fontSize),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(width = TangemTheme.dimens.size102, height = TangemTheme.dimens.size24)
                        .shimmer()
                        .background(
                            color = TangemTheme.colors.button.secondary,
                            shape = RoundedCornerShape(TangemTheme.dimens.radius6),
                        ),
                )
            }

            Spacer(
                modifier = Modifier
                    .size(TangemTheme.dimens.size4)
                    .weight(1f),
            )

            if (amountEquivalent != null) {
                Text(
                    text = amountEquivalent,
                    color = TangemTheme.colors.text.tertiary,
                    maxLines = 1,
                    style = TangemTheme.typography.body2,
                )
            } else {
                Box(
                    modifier = Modifier
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

@Composable
fun Token(
    tokenIconUrl: String,
    tokenCurrency: String,
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
                .padding(
                    end = TangemTheme.dimens.spacing16,
                )
                .size(TangemTheme.dimens.size42),
        ) {
            val tokenImageModifier = Modifier
                .align(Alignment.BottomStart)
                .size(TangemTheme.dimens.size36)

            SubcomposeAsyncImage(
                modifier = tokenImageModifier,
                model = ImageRequest.Builder(LocalContext.current)
                    .data(tokenIconUrl)
                    .crossfade(true)
                    .build(),
                loading = { TokenImageShimmer(modifier = tokenImageModifier) },
                error = { CurrencyPlaceholderIcon(modifier = tokenImageModifier, tokenCurrency) },
                contentDescription = null,
            )

            if (networkIconRes != null) {
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
                    Image(
                        modifier = Modifier.padding(all = (0.5).dp),
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
            modifier = Modifier
                .defaultMinSize(minWidth = TangemTheme.dimens.size80),
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
fun Preview_SwapMainCard_InLightTheme() {
    TangemTheme(isDark = false) {
        TransactionCardPreview()
    }
}

@Preview(widthDp = 328, heightDp = 116, showBackground = true)
@Composable
fun Preview_SwapMainCard_InDarkTheme() {
    TangemTheme(isDark = true) {
        TransactionCardPreview()
    }
}

@Composable
private fun TransactionCardPreview() {
    TransactionCard(
        type = TransactionCardType.SendCard("123", false),
        amount = "1 000 000",
        amountEquivalent = "1 000 000",
        tokenIconUrl = "",
        tokenCurrency = "DAI",
        networkIconRes = R.drawable.img_polygon_22,
        onChangeTokenClick = {},
    )
}

// endregion preview