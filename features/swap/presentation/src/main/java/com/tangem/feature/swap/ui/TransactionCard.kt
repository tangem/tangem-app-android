package com.tangem.feature.swap.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ParagraphIntrinsics
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.swap.models.TransactionCardType
import com.valentinilk.shimmer.shimmer

@Suppress("LongParameterList")
@Composable
fun TransactionCard(
    type: TransactionCardType,
    balance: String,
    amount: String?,
    amountEquivalent: String?,
    tokenIconUrl: String,
    tokenCurrency: String,
    @DrawableRes networkIconRes: Int? = null,
    onChangeTokenClick: (() -> Unit)? = null,
) {
    Card(
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
        Text(
            text = stringResource(R.string.common_balance, balance),
            color = TangemTheme.colors.text.tertiary,
            maxLines = 1,
            style = MaterialTheme.typography.body2,
            modifier = Modifier.padding(top = TangemTheme.dimens.spacing12),
        )
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
                top = TangemTheme.dimens.spacing8,
                start = TangemTheme.dimens.spacing16,
                bottom = TangemTheme.dimens.spacing16,
            ),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(
            modifier = Modifier
                .padding(
                    end = TangemTheme.dimens.spacing92,
                ),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start,
        ) {
            when (type) {
                is TransactionCardType.ReceiveCard -> {
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
                }
                is TransactionCardType.SendCard -> {
                    AutoSizeTextField(
                        amount = amount ?: "1",
                        onAmountChange = { type.onAmountChanged(it) },
                    )
                }
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

@Suppress("MagicNumber")
@Composable
private fun AutoSizeTextField(amount: String, onAmountChange: (String) -> Unit) {
    val focusManager = LocalFocusManager.current
    val textFieldFocusRequester = remember { FocusRequester() }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        var shrunkFontSize = TangemTheme.typography.h2.fontSize
        val calculateIntrinsics = @Composable {
            ParagraphIntrinsics(
                text = amount,
                style = TangemTheme.typography.h2.copy(
                    color = TangemTheme.colors.text.primary1,
                    fontSize = shrunkFontSize,
                ),
                density = LocalDensity.current,
                fontFamilyResolver = createFontFamilyResolver(LocalContext.current),
            )
        }

        var intrinsics = calculateIntrinsics()

        with(LocalDensity.current) {
            while (intrinsics.maxIntrinsicWidth > maxWidth.toPx()) {
                shrunkFontSize *= 0.9f
                intrinsics = calculateIntrinsics()
            }
        }
        val customTextSelectionColors = TextSelectionColors(
            handleColor = Transparent,
            backgroundColor = TangemTheme.colors.text.secondary.copy(alpha = 0.4f),
        )
        CompositionLocalProvider(
            LocalTextSelectionColors provides customTextSelectionColors,
        ) {
            BasicTextField(
                value = amount,
                onValueChange = onAmountChange,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(textFieldFocusRequester),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Decimal,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                    },
                ),
                textStyle = TangemTheme.typography.h2.copy(
                    color = TangemTheme.colors.text.primary1,
                    fontSize = shrunkFontSize,
                ),
                cursorBrush = SolidColor(TangemTheme.colors.text.primary1),
            )
        }
    }
}

@Suppress("MagicNumber")
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
                .padding(end = TangemTheme.dimens.spacing16)
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
        type = TransactionCardType.SendCard {},
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
