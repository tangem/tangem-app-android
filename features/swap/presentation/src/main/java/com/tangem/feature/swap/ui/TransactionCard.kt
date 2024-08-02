package com.tangem.feature.swap.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.R
import com.tangem.core.ui.components.*
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.utils.ImageBackgroundContrastChecker
import com.tangem.feature.swap.domain.models.ui.PriceImpact
import com.tangem.feature.swap.models.TransactionCardType
import kotlinx.coroutines.launch

@Suppress("LongParameterList")
@Composable
fun TransactionCard(
    type: TransactionCardType,
    balance: String,
    tokenIconUrl: String,
    tokenCurrency: String,
    amountEquivalent: String?,
    priceImpact: PriceImpact,
    textFieldValue: TextFieldValue?,
    modifier: Modifier = Modifier,
    @DrawableRes iconPlaceholder: Int? = null,
    @DrawableRes networkIconRes: Int? = null,
    onChangeTokenClick: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .background(
                shape = RoundedCornerShape(TangemTheme.dimens.radius16),
                color = TangemTheme.colors.background.primary,
            )
            .fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start,
        ) {
            Header(balance = stringResource(R.string.common_balance, balance), type = type)

            Content(
                type = type,
                amountEquivalent = amountEquivalent,
                textFieldValue = textFieldValue,
                priceImpact = priceImpact,
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

@Composable
fun TransactionCardEmpty(
    type: TransactionCardType,
    amountEquivalent: String?,
    textFieldValue: TextFieldValue?,
    modifier: Modifier = Modifier,
    onChangeTokenClick: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .background(
                shape = RoundedCornerShape(TangemTheme.dimens.radius12),
                color = TangemTheme.colors.background.primary,
            )
            .fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start,
        ) {
            Header(
                balance = stringResource(id = R.string.swapping_token_not_available),
                type = type,
            )

            Content(
                type = type,
                amountEquivalent = amountEquivalent,
                textFieldValue = textFieldValue,
                priceImpact = PriceImpact.Empty(),
            )
        }

        Box(modifier = Modifier.align(Alignment.BottomEnd)) {
            Token(
                tokenIconUrl = "",
                tokenCurrency = "",
                iconPlaceholder = R.drawable.ic_no_token_44,
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

@Composable
private fun Header(type: TransactionCardType, balance: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                bottom = TangemTheme.dimens.spacing8,
                top = TangemTheme.dimens.spacing14,
                start = TangemTheme.dimens.spacing12,
                end = TangemTheme.dimens.spacing12,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val titleColor = if (type.isError) {
            TangemTheme.colors.text.warning
        } else {
            TangemTheme.colors.text.tertiary
        }
        Text(
            text = type.header.resolveReference(),
            color = titleColor,
            maxLines = 1,
            style = MaterialTheme.typography.subtitle2,
            modifier = Modifier
                .align(Alignment.CenterVertically),
        )
        SpacerW16()
        if (balance.isNotBlank()) {
            AnimatedContent(targetState = balance, label = "") {
                Text(
                    text = it,
                    color = TangemTheme.colors.text.tertiary,
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier
                        .align(Alignment.CenterVertically),
                )
            }
        } else {
            RectangleShimmer(
                modifier = Modifier
                    .width(TangemTheme.dimens.size80)
                    .height(TangemTheme.dimens.size12),
                radius = TangemTheme.dimens.radius3,
            )
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun Content(
    type: TransactionCardType,
    amountEquivalent: String?,
    priceImpact: PriceImpact,
    textFieldValue: TextFieldValue?,
) {
    Row(
        modifier = Modifier
            .padding(
                start = TangemTheme.dimens.spacing12,
                bottom = TangemTheme.dimens.spacing16,
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
                is TransactionCardType.ReadOnly -> {
                    if (textFieldValue != null) {
                        ResizableText(
                            text = textFieldValue.text,
                            color = TangemTheme.colors.text.primary1,
                            style = TangemTheme.typography.h2,
                            fontSizeRange = FontSizeRange(min = 16.sp, max = TangemTheme.typography.h2.fontSize),
                            modifier = sumTextModifier,
                        )
                    } else {
                        RectangleShimmer(
                            modifier = Modifier
                                .padding(vertical = TangemTheme.dimens.spacing4)
                                .width(TangemTheme.dimens.size102)
                                .height(TangemTheme.dimens.size24),
                        )
                    }
                }
                is TransactionCardType.Inputtable -> {
                    val focusRequester = remember { FocusRequester() }

                    AutoSizeTextField(
                        modifier = sumTextModifier,
                        focusRequester = focusRequester,
                        textFieldValue = textFieldValue ?: TextFieldValue(),
                        onAmountChange = { type.onAmountChanged(it) },
                        onFocusChange = type.onFocusChanged,
                    )

                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                    }
                }
            }

            SpacerH4()

            if (amountEquivalent != null) {
                if (type is TransactionCardType.ReadOnly) {
                    Row(
                        modifier = Modifier.defaultMinSize(minHeight = TangemTheme.dimens.size20),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (priceImpact is PriceImpact.Value) {
                            Text(
                                text = makePriceImpactBalanceWarning(
                                    amountEquivalent,
                                    priceImpact.getIntPercentValue(),
                                ),
                                color = TangemTheme.colors.text.tertiary,
                                style = TangemTheme.typography.body2,
                            )
                        } else {
                            AnimatedContent(targetState = amountEquivalent, label = "") {
                                Text(
                                    text = it,
                                    color = TangemTheme.colors.text.tertiary,
                                    style = TangemTheme.typography.body2,
                                )
                            }
                        }
                        if (type.showWarning) {
                            SpacerW4()
                            IconButton(
                                onClick = {
                                    type.onWarningClick?.invoke()
                                },
                                modifier = Modifier.size(size = TangemTheme.dimens.size20),
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_information_24),
                                    contentDescription = null,
                                    tint = if (priceImpact is PriceImpact.Value) {
                                        TangemTheme.colors.text.attention
                                    } else {
                                        TangemTheme.colors.text.tertiary
                                    },
                                    modifier = Modifier.align(Alignment.CenterVertically),
                                )
                            }
                        }
                    }
                } else {
                    AnimatedContent(targetState = amountEquivalent, label = "") {
                        Text(
                            text = it,
                            color = TangemTheme.colors.text.tertiary,
                            style = TangemTheme.typography.body2,
                            modifier = Modifier.defaultMinSize(minHeight = TangemTheme.dimens.size20),
                        )
                    }
                }
            } else {
                RectangleShimmer(
                    modifier = Modifier
                        .padding(vertical = TangemTheme.dimens.spacing4)
                        .width(TangemTheme.dimens.size40)
                        .height(TangemTheme.dimens.size12),
                    radius = TangemTheme.dimens.radius3,
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
        TokenIcon(
            tokenIconUrl = tokenIconUrl,
            tokenCurrency = tokenCurrency,
            iconPlaceholder = iconPlaceholder,
            networkIconRes = networkIconRes,
        )
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
private fun TokenIcon(
    tokenIconUrl: String,
    tokenCurrency: String,
    @DrawableRes iconPlaceholder: Int? = null,
    @DrawableRes networkIconRes: Int? = null,
) {
    var iconBackgroundColor by remember { mutableStateOf(Color.Transparent) }
    var isBackgroundColorDefined by remember { mutableStateOf(false) }
    val itemBackgroundColor = TangemTheme.colors.background.primary.toArgb()
    val isDarkTheme = isSystemInDarkTheme()
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .padding(end = TangemTheme.dimens.spacing16)
            .size(TangemTheme.dimens.size42),
    ) {
        val tokenImageModifier = Modifier
            .align(Alignment.BottomStart)
            .size(TangemTheme.dimens.size36)
            .background(
                color = iconBackgroundColor,
                shape = TangemTheme.shapes.roundedCorners8,
            )
            .clip(TangemTheme.shapes.roundedCorners8)

        val data = tokenIconUrl.ifEmpty { iconPlaceholder }

        val pixelsSize = with(LocalDensity.current) { TangemTheme.dimens.size36.roundToPx() }

        SubcomposeAsyncImage(
            modifier = tokenImageModifier,
            model = ImageRequest.Builder(LocalContext.current)
                .data(data)
                .size(size = pixelsSize)
                .memoryCacheKey(key = data.toString() + pixelsSize)
                .crossfade(true)
                .allowHardware(false)
                .listener(
                    onSuccess = { _, result ->
                        if (isDarkTheme) {
                            coroutineScope.launch {
                                val color = ImageBackgroundContrastChecker(
                                    drawable = result.drawable,
                                    backgroundColor = itemBackgroundColor,
                                    size = pixelsSize,
                                ).getContrastColor(true)
                                iconBackgroundColor = color
                                isBackgroundColorDefined = true
                            }
                        }
                    },
                ).build(),
            loading = { CircleShimmer(modifier = tokenImageModifier) },
            contentDescription = tokenCurrency,
        )

        if (networkIconRes != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(TangemTheme.dimens.size18)
                    .background(color = TangemTheme.colors.background.primary, shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    modifier = Modifier.padding(all = TangemTheme.dimens.spacing2),
                    painter = painterResource(id = networkIconRes),
                    contentDescription = null,
                )
            }
        }
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
            tint = TangemTheme.colors.icon.secondary,
            contentDescription = null,
        )
    }
}

@Composable
private fun makePriceImpactBalanceWarning(value: String, priceImpactPercents: Int): AnnotatedString {
    val fullValue = "$value (-$priceImpactPercents%)"
    return buildAnnotatedString {
        append(fullValue)
        addStyle(
            style = SpanStyle(color = TangemTheme.colors.text.attention),
            start = value.length,
            end = fullValue.length,
        )
    }
}

// region preview

@Preview(widthDp = 328, heightDp = 116, showBackground = true)
@Composable
private fun Preview_TransactionCard_InLightTheme() {
    TangemThemePreview(isDark = false) {
        TransactionCardPreview()
    }
}

@Preview(widthDp = 328, heightDp = 116, showBackground = true)
@Composable
private fun Preview_TransactionCardWithPriceImpact_InLightTheme() {
    TangemThemePreview(isDark = false) {
        TransactionCardPreviewWithPriceImpact()
    }
}

@Preview(widthDp = 328, heightDp = 116, showBackground = true)
@Composable
private fun Preview_TransactionCardWithoutPriceImpact_InLightTheme() {
    TangemThemePreview(isDark = false) {
        TransactionCardPreviewWithoutPriceImpact()
    }
}

@Preview(widthDp = 328, heightDp = 116, showBackground = true)
@Composable
private fun Preview_TransactionCard_InDarkTheme() {
    TangemThemePreview(isDark = false) {
        TransactionCardPreview()
    }
}

@Preview(widthDp = 328, heightDp = 116, showBackground = true)
@Composable
private fun Preview_TransactionCardWithPriceImpact_InDarkTheme() {
    TangemThemePreview(isDark = false) {
        TransactionCardPreviewWithPriceImpact()
    }
}

@Preview(widthDp = 328, heightDp = 116, showBackground = true)
@Composable
private fun Preview_TransactionCardWithoutPriceImpact_InDarkTheme() {
    TangemThemePreview(isDark = false) {
        TransactionCardPreviewWithoutPriceImpact()
    }
}

@Composable
private fun TransactionCardPreview() {
    TransactionCard(
        type = TransactionCardType.Inputtable({}, {}, false),
        amountEquivalent = "1 000 000",
        tokenIconUrl = "",
        tokenCurrency = "DAI",
        networkIconRes = R.drawable.img_polygon_22,
        onChangeTokenClick = {},
        balance = "123",
        textFieldValue = TextFieldValue(),
        priceImpact = PriceImpact.Empty(),
    )
}

@Composable
@Suppress("MagicNumber")
private fun TransactionCardPreviewWithPriceImpact() {
    TransactionCard(
        type = TransactionCardType.ReadOnly(showWarning = true),
        amountEquivalent = "1 000 000",
        tokenIconUrl = "",
        tokenCurrency = "DAI",
        networkIconRes = R.drawable.img_polygon_22,
        onChangeTokenClick = {},
        balance = "123",
        textFieldValue = TextFieldValue(),
        priceImpact = PriceImpact.Value(0.15F),
    )
}

@Composable
@Suppress("MagicNumber")
private fun TransactionCardPreviewWithoutPriceImpact() {
    TransactionCard(
        type = TransactionCardType.ReadOnly(),
        amountEquivalent = "1 000 000",
        tokenIconUrl = "",
        tokenCurrency = "DAI",
        networkIconRes = R.drawable.img_polygon_22,
        onChangeTokenClick = {},
        balance = "123",
        textFieldValue = TextFieldValue(),
        priceImpact = PriceImpact.Empty(),
    )
}

// endregion preview
