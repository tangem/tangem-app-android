package com.tangem.feature.wallet.presentation.wallet.ui.components.visa

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.components.SecondaryButtonIconStart
import com.tangem.core.ui.components.SpacerW12
import com.tangem.core.ui.components.bottomsheets.sheet.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.wallet.state.model.VisaTxDetailsBottomSheetConfig
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun VisaTxDetailsBottomSheet(config: TangemBottomSheetConfig) {
    TangemBottomSheet(
        config = config,
        containerColor = TangemTheme.colors.background.secondary,
    ) { content: VisaTxDetailsBottomSheetConfig ->
        VisaTxDetailsBottomSheetContent(content)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VisaTxDetailsBottomSheetContent(config: VisaTxDetailsBottomSheetConfig, modifier: Modifier = Modifier) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = TangemTheme.dimens.size44)
                .background(TangemTheme.colors.background.secondary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResourceSafe(R.string.visa_transaction_details_header),
                style = TangemTheme.typography.subtitle1,
                color = TangemTheme.colors.text.primary1,
            )
        }

        LazyColumn(
            modifier = modifier.background(TangemTheme.colors.background.secondary),
            contentPadding = PaddingValues(
                bottom = TangemTheme.dimens.spacing16,
            ),
            verticalArrangement = Arrangement.spacedBy(space = TangemTheme.dimens.spacing12),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                TransactionBlock(config.transaction)
            }

            items(config.requests) { item ->
                BlockchainRequestBlock(item)
            }

            item {
                DisputeButton(config.onDisputeClick)
            }
        }
    }
}

@Composable
private fun TransactionBlock(transaction: VisaTxDetailsBottomSheetConfig.Transaction, modifier: Modifier = Modifier) {
    BlockContent(
        modifier = modifier,
        title = resourceReference(R.string.visa_transaction_details_title),
        content = {
            BlockItem(
                title = resourceReference(R.string.visa_transaction_details_type),
                value = transaction.type,
            )
            BlockItem(
                title = resourceReference(R.string.visa_transaction_details_status),
                value = transaction.status,
            )
            BlockItem(
                title = resourceReference(R.string.visa_transaction_details_blockchain_amount),
                value = transaction.blockchainAmount,
            )
            BlockItem(
                title = resourceReference(R.string.visa_transaction_details_transaction_amount),
                value = transaction.transactionAmount,
            )
            BlockItem(
                title = resourceReference(R.string.visa_transaction_details_currency_code),
                value = transaction.transactionCurrencyCode,
            )
            BlockItem(
                title = resourceReference(R.string.visa_transaction_details_merchant_name),
                value = transaction.merchantName,
            )
            BlockItem(
                title = resourceReference(R.string.visa_transaction_details_merchant_city),
                value = transaction.merchantCity,
            )
            BlockItem(
                title = resourceReference(R.string.visa_transaction_details_merchant_country_code),
                value = transaction.merchantCountryCode,
            )
            BlockItem(
                title = resourceReference(R.string.visa_transaction_details_merchant_category_code),
                value = transaction.merchantCategoryCode,
            )
        },
    )
}

@Suppress("LongMethod")
@Composable
private fun BlockchainRequestBlock(request: VisaTxDetailsBottomSheetConfig.Request, modifier: Modifier = Modifier) {
    BlockContent(
        modifier = modifier,
        title = resourceReference(R.string.visa_transaction_details_transaction_request),
        description = {
            if (request.onExploreClick != null) {
                Row(
                    modifier = Modifier.clickable(onClick = request.onExploreClick),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(space = TangemTheme.dimens.spacing4),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_compass_24),
                        contentDescription = null,
                        modifier = Modifier.size(size = TangemTheme.dimens.size18),
                        tint = TangemTheme.colors.icon.informative,
                    )
                    Text(
                        text = stringResourceSafe(R.string.common_explore),
                        color = TangemTheme.colors.text.tertiary,
                        style = TangemTheme.typography.caption1,
                    )
                }
                SpacerW12()
            }
        },
        content = {
            BlockItem(
                title = resourceReference(R.string.visa_transaction_details_type),
                value = request.type,
            )
            BlockItem(
                title = resourceReference(R.string.visa_transaction_details_status),
                value = request.status,
            )
            BlockItem(
                title = resourceReference(R.string.visa_transaction_details_blockchain_amount),
                value = request.blockchainAmount,
            )
            BlockItem(
                title = resourceReference(R.string.visa_transaction_details_transaction_amount),
                value = request.transactionAmount,
            )
            BlockItem(
                title = resourceReference(R.string.visa_transaction_details_currency_code),
                value = request.currencyCode,
            )
            BlockItem(
                title = resourceReference(R.string.visa_transaction_details_error_code),
                value = request.errorCode.toString(),
            )
            BlockItem(
                title = resourceReference(R.string.visa_transaction_details_date),
                value = request.date,
            )
            BlockItem(
                title = resourceReference(R.string.visa_transaction_details_transaction_hash),
                value = request.txHash,
            )
            BlockItem(
                title = resourceReference(R.string.visa_transaction_details_transaction_status),
                value = request.txStatus,
            )
        },
    )
}

@Composable
private fun DisputeButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    SecondaryButtonIconStart(
        modifier = modifier.fillMaxWidth(),
        text = stringResourceSafe(R.string.visa_tx_dispute_button),
        iconResId = R.drawable.ic_alert_triangle_20,
        onClick = onClick,
    )
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun VisaTxDetailsBottomSheetPreview(
    @PreviewParameter(VisaTxDetailsBottomSheetParameterProvider::class) state: VisaTxDetailsBottomSheetConfig,
) {
    TangemThemePreview {
        VisaTxDetailsBottomSheetContent(state)
    }
}

private class VisaTxDetailsBottomSheetParameterProvider :
    CollectionPreviewParameterProvider<VisaTxDetailsBottomSheetConfig>(
        collection = listOf(
            VisaTxDetailsBottomSheetConfig(
                transaction = VisaTxDetailsBottomSheetConfig.Transaction(
                    id = "518385816101345408",
                    type = "payment",
                    status = "authorized",
                    blockchainAmount = "1.0614 USDT",
                    transactionAmount = "0.99 €",
                    transactionCurrencyCode = "978",
                    merchantName = "SQ *FORMATIVE",
                    merchantCity = "London",
                    merchantCountryCode = "GB",
                    merchantCategoryCode = "5814",
                ),
                requests = persistentListOf(
                    VisaTxDetailsBottomSheetConfig.Request(
                        id = "524582128501966718",
                        type = "authorize_payment",
                        status = "accepted",
                        blockchainAmount = "1.0593 USDT",
                        transactionAmount = "0.99 €",
                        currencyCode = "978",
                        errorCode = 0,
                        date = "2023-12-01 14:20:09.230 +0300",
                        txHash = "0xc458f0204fe43b82c775004baabb38435b5595f4307d8c3ac74625c827be7c29",
                        txStatus = "confirmed",
                        onExploreClick = {},
                    ),
                ),
                onDisputeClick = {},
            ),
        ),
    )

// endregion Preview