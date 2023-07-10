package com.tangem.tap.features.wallet.ui.view

import android.content.Context
import android.util.AttributeSet
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.tap.common.entities.FiatCurrency
import com.tangem.tap.domain.model.TotalFiatBalance
import com.tangem.tap.features.wallet.redux.utils.UNKNOWN_AMOUNT_SIGN
import com.tangem.wallet.R
import com.valentinilk.shimmer.shimmer
import timber.log.Timber
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

internal class TotalBalanceCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AbstractComposeView(context, attrs, defStyleAttr) {
    private var state by mutableStateOf<TotalBalanceCardState>(TotalBalanceCardState.Empty)

    var status: TotalFiatBalance? = null
        set(value) {
            if (field == value) return
            field = value
            updateState(value, fiatCurrency, onChangeFiatCurrencyClick)
        }

    var onChangeFiatCurrencyClick: () -> Unit = { /* no-op */ }
        set(value) {
            if (field == value) return
            field = value
            updateState(status, fiatCurrency, value)
        }

    var fiatCurrency: FiatCurrency = FiatCurrency.Default
        set(value) {
            if (field == value) return
            field = value
            updateState(status, value, onChangeFiatCurrencyClick)
        }

    @Composable
    override fun Content() {
        TangemTheme {
            TotalBalanceCardContent(state = state)
        }
    }

    override fun getAccessibilityClassName(): CharSequence {
        return javaClass.name
    }

    private fun updateState(status: TotalFiatBalance?, fiatCurrency: FiatCurrency, onChangeCurrencyClick: () -> Unit) {
        state = when (status) {
            null -> TotalBalanceCardState.Empty
            is TotalFiatBalance.Failed -> TotalBalanceCardState.Failure(
                fiatCurrency = fiatCurrency,
                onChangeFiatCurrencyClick = onChangeCurrencyClick,
            )
            is TotalFiatBalance.Loading -> TotalBalanceCardState.Loading(
                fiatCurrency = fiatCurrency,
                onChangeFiatCurrencyClick = onChangeCurrencyClick,
            )
            is TotalFiatBalance.Loaded -> TotalBalanceCardState.Success(
                amount = status.amount,
                showWarning = status.isWarning,
                onChangeFiatCurrencyClick = onChangeCurrencyClick,
                fiatCurrency = fiatCurrency,
            )
        }
    }
}

@Composable
private fun TotalBalanceCardContent(state: TotalBalanceCardState, modifier: Modifier = Modifier) {
    TotalBalanceCardScaffold(
        modifier = modifier,
        title = {
            Text(
                text = stringResource(id = R.string.main_page_balance),
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.tertiary,
            )
        },
        amount = {
            when (state) {
                is TotalBalanceCardState.Empty,
                is TotalBalanceCardState.Loading,
                -> LoadingAmount()
                is TotalBalanceCardState.Failure,
                is TotalBalanceCardState.Success,
                -> LoadedAmount(
                    amount = buildAmountString(
                        amount = state.amount,
                        fiatCurrency = state.fiatCurrency,
                    ),
                )
            }
        },
        currencySelector = {
            if (state !is TotalBalanceCardState.Empty) {
                SelectorButton(
                    text = state.fiatCurrency.code,
                    onClick = state.onChangeFiatCurrencyClick,
                )
            }
        },
        warningText = {
            AnimatedVisibility(visible = state.showWarning) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(id = R.string.main_processing_full_amount),
                    style = TangemTheme.typography.caption,
                    color = TangemTheme.colors.text.attention,
                )
            }
        },
    )
}

@Composable
private fun TotalBalanceCardScaffold(
    title: @Composable () -> Unit,
    amount: @Composable () -> Unit,
    currencySelector: @Composable () -> Unit,
    warningText: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    amountWeight: Float = 0.8f,
) {
    Surface(
        modifier = modifier,
        shape = TangemTheme.shapes.roundedCornersMedium,
        color = TangemTheme.colors.background.plain,
        elevation = TangemTheme.dimens.elevation1,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                SpacerW16()
                Column(
                    modifier = Modifier.weight(amountWeight),
                ) {
                    SpacerH12()
                    title()
                    SpacerH4()
                    amount()
                }
                currencySelector()
                SpacerW4()
            }
            SpacerH4()
            Box(
                modifier = Modifier.padding(
                    horizontal = TangemTheme.dimens.spacing16,
                ),
            ) {
                warningText()
            }
            SpacerH12()
        }
    }
}

@Composable
private fun LoadingAmount(modifier: Modifier = Modifier) {
    Box(modifier = modifier.shimmer()) {
        Box(
            modifier = Modifier
                .width(TangemTheme.dimens.size116)
                .height(TangemTheme.dimens.size32)
                .background(
                    color = TangemTheme.colors.stroke.primary,
                    shape = TangemTheme.shapes.roundedCornersSmall2,
                ),
        )
    }
}

@Composable
private fun LoadedAmount(amount: AnnotatedString, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Text(
            text = amount,
            style = TangemTheme.typography.h2,
            color = TangemTheme.colors.text.primary1,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
    }
}

@Composable
private fun buildAmountString(amount: BigDecimal?, fiatCurrency: FiatCurrency): AnnotatedString {
    if (amount == null) return AnnotatedString(text = UNKNOWN_AMOUNT_SIGN)

    val locale = Locale.getDefault()
    val fractionDigits = 2
    val formatter = NumberFormat.getCurrencyInstance(locale) as? DecimalFormat
        ?: return AnnotatedString("${amount.toPlainString()} ${fiatCurrency.symbol}")

    val currencyToShow = "${fiatCurrency.symbol} "
    val scaledAmount = try {
        Currency.getInstance(fiatCurrency.code)?.let { currency ->
            formatter.currency = currency
            formatter.maximumFractionDigits = fractionDigits
            formatter.minimumFractionDigits = fractionDigits
            formatter.isGroupingUsed = true
            formatter.roundingMode = RoundingMode.HALF_UP
            formatter.format(amount).replace(currency.symbol, currencyToShow)
        } ?: formatter.format(amount)
    } catch (e: IllegalArgumentException) {
        Timber.e("TotalBalanceCard buildAmountString currencyCode is not a supported ISO 4217 code: $e")
        formatter.currency?.let {
            formatter.format(amount).replace(it.symbol, currencyToShow)
        } ?: formatter.format(amount)
    }

    val integer = scaledAmount.substringBefore(formatter.decimalFormatSymbols.decimalSeparator)
    var reminder = scaledAmount.substringAfter(formatter.decimalFormatSymbols.decimalSeparator)

    // if locale formatted currency at the end, remember it and place out of AnnotatedString
    val currency = if (reminder.endsWith(currencyToShow)) {
        reminder = reminder.dropLast(currencyToShow.length)
        currencyToShow
    } else {
        ""
    }

    return buildAnnotatedString {
        append(integer)
        append(formatter.decimalFormatSymbols.decimalSeparator)
        append(
            AnnotatedString(
                text = reminder,
                spanStyle = TangemTheme.typography.h3.toSpanStyle(),
            ),
        )
        append(currency) // it is not empty if was placed at the end after locale formatting
    }
}

private sealed interface TotalBalanceCardState {
    val amount: BigDecimal?
    val showWarning: Boolean
    val fiatCurrency: FiatCurrency
    val onChangeFiatCurrencyClick: () -> Unit

    object Empty : TotalBalanceCardState {
        override val amount: BigDecimal? = null
        override val showWarning: Boolean = false
        override val fiatCurrency: FiatCurrency = FiatCurrency.Default
        override val onChangeFiatCurrencyClick: () -> Unit = { /* no-op */ }
    }

    data class Loading(
        override val fiatCurrency: FiatCurrency,
        override val onChangeFiatCurrencyClick: () -> Unit,
    ) : TotalBalanceCardState {
        override val amount: BigDecimal = BigDecimal.ZERO
        override val showWarning: Boolean = false
    }

    data class Failure(
        override val fiatCurrency: FiatCurrency,
        override val onChangeFiatCurrencyClick: () -> Unit,
    ) : TotalBalanceCardState {
        override val amount: BigDecimal? = null
        override val showWarning: Boolean = true
    }

    data class Success(
        override val amount: BigDecimal,
        override val showWarning: Boolean,
        override val fiatCurrency: FiatCurrency,
        override val onChangeFiatCurrencyClick: () -> Unit,
    ) : TotalBalanceCardState
}

// region Preview
@Composable
private fun TotalBalanceCardContentSample(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(TangemTheme.colors.background.primary)
            .padding(all = TangemTheme.dimens.spacing16),
    ) {
        TotalBalanceCardContent(state = TotalBalanceCardState.Empty)
        Divider(modifier = Modifier.padding(vertical = TangemTheme.dimens.spacing8))
        TotalBalanceCardContent(
            state = TotalBalanceCardState.Loading(FiatCurrency("USD", "USD", "$")) {},
        )
        Divider(modifier = Modifier.padding(vertical = TangemTheme.dimens.spacing8))
        TotalBalanceCardContent(
            state = TotalBalanceCardState.Failure(
                onChangeFiatCurrencyClick = {},
                fiatCurrency = FiatCurrency("USD", "USD", "$"),
            ),
        )
        Divider(modifier = Modifier.padding(vertical = TangemTheme.dimens.spacing8))
        TotalBalanceCardContent(
            state = TotalBalanceCardState.Success(
                amount = BigDecimal("9917.72"),
                showWarning = false,
                onChangeFiatCurrencyClick = {},
                fiatCurrency = FiatCurrency("USD", "USD", "$"),
            ),
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun TotalBalanceCardContentPreview_Light() {
    TangemTheme {
        TotalBalanceCardContentSample()
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun TotalBalanceCardContentPreview_Dark() {
    TangemTheme(isDark = true) {
        TotalBalanceCardContentSample()
    }
}
// endregion Preview
