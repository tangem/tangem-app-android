package com.tangem.features.swap.v2.impl.sendviaswap.success.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.amountScreen.utils.getFiatReference
import com.tangem.common.ui.navigationButtons.NavigationButton
import com.tangem.common.ui.navigationButtons.NavigationButtonsBlockV2
import com.tangem.common.ui.navigationButtons.NavigationUM
import com.tangem.core.ui.components.BottomFade
import com.tangem.core.ui.components.currency.icon.CurrencyIcon
import com.tangem.core.ui.components.icons.identicon.IdentIcon
import com.tangem.core.ui.components.inputrow.InputRowBestRate
import com.tangem.core.ui.components.rows.SelectorRowItem
import com.tangem.core.ui.components.transactions.TransactionDoneTitle
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fee
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.core.ui.utils.toTimeFormat
import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.express.models.ExpressProviderType
import com.tangem.domain.swap.models.SwapDataModel
import com.tangem.domain.swap.models.SwapDataTransactionModel
import com.tangem.domain.utils.convertToSdkAmount
import com.tangem.features.send.v2.api.entity.FeeExtraInfo
import com.tangem.features.send.v2.api.entity.FeeItem
import com.tangem.features.send.v2.api.entity.FeeNonce
import com.tangem.features.send.v2.api.entity.FeeSelectorUM
import com.tangem.features.send.v2.api.subcomponents.destination.entity.DestinationTextFieldUM
import com.tangem.features.send.v2.api.subcomponents.destination.entity.DestinationUM
import com.tangem.features.swap.v2.impl.R
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountFieldUM
import com.tangem.features.swap.v2.impl.amount.entity.SwapAmountUM
import com.tangem.features.swap.v2.impl.amount.ui.preview.SwapAmountContentPreview
import com.tangem.features.swap.v2.impl.common.entity.ConfirmUM
import com.tangem.features.swap.v2.impl.common.entity.SwapQuoteUM
import com.tangem.features.swap.v2.impl.sendviaswap.entity.SendWithSwapUM
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal

@Composable
internal fun SendWithSwapSuccessContent(sendWithSwapUM: SendWithSwapUM) {
    if (sendWithSwapUM.navigationUM !is NavigationUM.Content) return

    val confirmUM = sendWithSwapUM.confirmUM as? ConfirmUM.Success ?: return
    val amountUM = sendWithSwapUM.amountUM as? SwapAmountUM.Content ?: return
    val quoteUM = amountUM.selectedQuote as? SwapQuoteUM.Content ?: return
    val destinationUM = sendWithSwapUM.destinationUM as? DestinationUM.Content ?: return
    val feeSelectorUM = sendWithSwapUM.feeSelectorUM as? FeeSelectorUM.Content ?: return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TangemTheme.colors.background.tertiary),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TransactionDoneTitle(
                title = resourceReference(R.string.sent_transaction_sent_title),
                subtitle = resourceReference(
                    id = R.string.send_date_format,
                    formatArgs = wrappedList(
                        confirmUM.transactionDate.toTimeFormat(DateTimeFormatters.dateFormatter),
                        confirmUM.transactionDate.toTimeFormat(),
                    ),
                ),
                modifier = Modifier.padding(vertical = 12.dp),
            )
            SwapAmountBlock(amountUM = amountUM)
            InputRowBestRate(
                imageUrl = confirmUM.provider.imageLarge,
                title = stringReference(confirmUM.provider.name),
                titleExtra = stringReference(confirmUM.provider.type.typeName),
                subtitle = quoteUM.rate,
                modifier = Modifier
                    .clip(TangemTheme.shapes.roundedCornersXMedium)
                    .background(TangemTheme.colors.background.action),
            )
            DestinationBlock(destinationUM.addressTextField)
            FeeBlock(feeSelectorUM = feeSelectorUM)
            Spacer(Modifier.height(60.dp))
        }
        BottomFade(Modifier.align(Alignment.BottomCenter), TangemTheme.colors.background.tertiary)
        NavigationButtonsBlockV2(
            navigationUM = sendWithSwapUM.navigationUM,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp,
                ),
        )
    }
}

@Composable
private fun SwapAmountBlock(amountUM: SwapAmountUM.Content) {
    AmountBlock(
        title = resourceReference(
            id = R.string.send_from_wallet_name,
            formatArgs = wrappedList(
                (amountUM.primaryAmount.amountField as? AmountState.Data)?.title
                    ?: TextReference.EMPTY,
            ),
        ),
        amountFieldUM = amountUM.primaryAmount,
    )
    AmountBlock(
        title = resourceReference(R.string.send_with_swap_recipient_amount_success_title),
        amountFieldUM = amountUM.secondaryAmount,
    )
}

@Composable
private fun AmountBlock(title: TextReference, amountFieldUM: SwapAmountFieldUM, modifier: Modifier = Modifier) {
    val amountFieldData = amountFieldUM.amountField as? AmountState.Data ?: return
    val cryptoAmount = amountFieldData.amountTextField.cryptoAmount
    val fiatAmount = amountFieldData.amountTextField.fiatAmount

    Column(
        modifier = modifier
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(TangemTheme.colors.background.action)
            .padding(12.dp),
    ) {
        Text(
            text = title.resolveReference(),
            style = TangemTheme.typography.subtitle2,
            color = TangemTheme.colors.text.tertiary,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        ) {
            CurrencyIcon(
                state = amountFieldData.tokenIconState,
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = cryptoAmount.value?.format {
                        crypto(
                            symbol = cryptoAmount.currencySymbol,
                            decimals = cryptoAmount.decimals,
                        )
                    }.orEmpty(),
                    style = TangemTheme.typography.subtitle2,
                    color = TangemTheme.colors.text.primary1,
                )
                Text(
                    text = fiatAmount.value.format {
                        fiat(
                            fiatCurrencyCode = amountFieldData.appCurrency.code,
                            fiatCurrencySymbol = amountFieldData.appCurrency.symbol,
                        )
                    },
                    style = TangemTheme.typography.caption2,
                    color = TangemTheme.colors.text.tertiary,
                )
            }
        }
    }
}

@Composable
private fun FeeBlock(feeSelectorUM: FeeSelectorUM.Content) {
    val feeExtraInfo = feeSelectorUM.feeExtraInfo
    val feeFiatRateUM = feeSelectorUM.feeFiatRateUM
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(TangemTheme.colors.background.action)
            .padding(TangemTheme.dimens.spacing12),
    ) {
        Text(
            text = stringResourceSafe(R.string.common_network_fee_title),
            style = TangemTheme.typography.subtitle2,
            color = TangemTheme.colors.text.tertiary,
        )

        Box(modifier = Modifier.padding(top = TangemTheme.dimens.spacing8)) {
            val feeItemUM = feeSelectorUM.selectedFeeItem
            val feeAmount = feeItemUM.fee.amount
            SelectorRowItem(
                title = feeItemUM.title,
                iconRes = feeItemUM.iconRes,
                preDot = stringReference(
                    feeAmount.value.format {
                        crypto(
                            symbol = feeAmount.currencySymbol,
                            decimals = feeAmount.decimals,
                        ).fee(canBeLower = feeExtraInfo.isFeeApproximate)
                    },
                ),
                postDot = if (feeExtraInfo.isFeeConvertibleToFiat && feeFiatRateUM != null) {
                    getFiatReference(feeAmount.value, feeFiatRateUM.rate, feeFiatRateUM.appCurrency)
                } else {
                    null
                },
                ellipsizeOffset = feeAmount.currencySymbol.length,
                isSelected = true,
                showDivider = false,
                showSelectedAppearance = false,
                paddingValues = PaddingValues(),
            )
        }
    }
}

@Composable
private fun DestinationBlock(address: DestinationTextFieldUM.RecipientAddress, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(TangemTheme.colors.background.action)
            .padding(12.dp),
    ) {
        Text(
            text = stringResourceSafe(R.string.send_recipient),
            style = TangemTheme.typography.subtitle2,
            color = TangemTheme.colors.text.tertiary,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = address.value,
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.primary1,
            )
            IdentIcon(
                address = address.value,
                modifier = Modifier
                    .size(TangemTheme.dimens.size36)
                    .clip(RoundedCornerShape(18.dp))
                    .background(TangemTheme.colors.background.tertiary),
            )
        }
    }
}

// region Preview
@Suppress("LongMethod")
@Composable
@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Preview(showBackground = true, widthDp = 360, heightDp = 720, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun SendWithSwapSuccessContent_Preview() {
    TangemThemePreview {
        SendWithSwapSuccessContent(
            sendWithSwapUM = SendWithSwapUM(
                amountUM = SwapAmountContentPreview.defaultState,
                destinationUM = DestinationUM.Content(
                    isPrimaryButtonEnabled = false,
                    addressTextField = DestinationTextFieldUM.RecipientAddress(
                        value = "0x391316d97a07027a0702c8A002c8A0C25d8470",
                        keyboardOptions = KeyboardOptions(),
                        placeholder = TextReference.EMPTY,
                        label = resourceReference(R.string.send_recipient),
                        isError = false,
                        error = null,
                        isValuePasted = false,
                        blockchainAddress = "0x391316d97a07027a0702c8A002c8A0C25d8470",
                    ),
                    memoTextField = null,
                    recent = persistentListOf(),
                    wallets = persistentListOf(),
                    networkName = "Polygon",
                    isValidating = false,
                    isInitialized = false,
                    isRedesignEnabled = false,
                    isRecentHidden = false,
                ),
                feeSelectorUM = FeeSelectorUM.Content(
                    fees = TransactionFee.Single(
                        normal = Fee.Common(
                            BigDecimal.ONE.convertToSdkAmount(
                                SwapAmountContentPreview.cryptoCurrencyStatus.currency,
                            ),
                        ),
                    ),
                    feeItems = persistentListOf(),
                    selectedFeeItem = FeeItem.Market(
                        Fee.Common(
                            BigDecimal.ONE.convertToSdkAmount(
                                SwapAmountContentPreview.cryptoCurrencyStatus.currency,
                            ),
                        ),
                    ),
                    feeExtraInfo = FeeExtraInfo(
                        isFeeApproximate = false,
                        isFeeConvertibleToFiat = false,
                        isTronToken = false,
                    ),
                    feeFiatRateUM = null,
                    feeNonce = FeeNonce.None,
                    isPrimaryButtonEnabled = false,
                ),
                confirmUM = ConfirmUM.Success(
                    isPrimaryButtonEnabled = true,
                    transactionDate = 8960,
                    txUrl = "https://tangem.com",
                    provider = ExpressProvider(
                        providerId = "changelly",
                        rateTypes = listOf(),
                        name = "Changelly",
                        type = ExpressProviderType.CEX,
                        imageLarge = "https://s3.eu-central-1.amazonaws.com/tangem.api/express/changelly-1024.png",
                        termsOfUse = "",
                        privacyPolicy = "",
                        isRecommended = false,
                        slippage = null,
                    ),
                    swapDataModel = SwapDataModel(
                        toTokenAmount = BigDecimal.TEN,
                        transaction = SwapDataTransactionModel.CEX(
                            fromAmount = BigDecimal.TEN,
                            toAmount = BigDecimal.TEN,
                            txValue = "cetero",
                            txId = "oporteat",
                            txTo = "graecis",
                            txExtraId = "expetendis",
                            externalTxId = "atomorum",
                            externalTxUrl = "https://tangem.com",
                            txExtraIdName = "Jeffry Blackwell",
                        ),
                    ),
                ),
                navigationUM = NavigationUM.Content(
                    title = TextReference.EMPTY,
                    subtitle = null,
                    backIconRes = R.drawable.ic_close_24,
                    backIconClick = {},
                    additionalIconRes = null,
                    additionalIconClick = null,
                    primaryButton = NavigationButton(
                        textReference = resourceReference(R.string.common_close),
                        isEnabled = true,
                        onClick = {},
                    ),
                    secondaryPairButtonsUM = NavigationButton(
                        textReference = resourceReference(R.string.common_explore),
                        iconRes = R.drawable.ic_web_24,
                        isEnabled = true,
                        onClick = {},
                    ) to NavigationButton(
                        textReference = resourceReference(R.string.common_share),
                        iconRes = R.drawable.ic_share_24,
                        isEnabled = true,
                        onClick = {},
                    ),
                ),
            ),
        )
    }
}
// endregion