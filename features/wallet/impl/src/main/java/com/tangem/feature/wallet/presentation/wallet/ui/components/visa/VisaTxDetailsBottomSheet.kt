package com.tangem.feature.wallet.presentation.wallet.ui.components.visa

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.components.SpacerW12
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.extensions.stringReference
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

@Composable
private fun VisaTxDetailsBottomSheetContent(config: VisaTxDetailsBottomSheetConfig, modifier: Modifier = Modifier) {
    ContentContainer(
        modifier = modifier,
        blocksCount = config.requests.size.inc(),
        title = {
            Text(
                text = "Transaction Details",
                style = TangemTheme.typography.subtitle1,
                color = TangemTheme.colors.text.primary1,
            )
        },
        block = { index ->
            if (index == 0) {
                TransactionBlock(config.transaction)
            } else {
                BlockchainRequestBlock(config.requests[index - 1])
            }
        },
    )
}

@Composable
private fun TransactionBlock(transaction: VisaTxDetailsBottomSheetConfig.Transaction, modifier: Modifier = Modifier) {
    BlockContent(
        modifier = modifier,
        title = stringReference(value = "Transaction"),
        content = {
            BlockItem(
                title = stringReference(value = "Type"),
                value = transaction.type,
            )
            BlockItem(
                title = stringReference(value = "Status"),
                value = transaction.status,
            )
            BlockItem(
                title = stringReference(value = "Blockchain Amount"),
                value = transaction.blockchainAmount,
            )
            BlockItem(
                title = stringReference(value = "Blockchain Fee"),
                value = transaction.blockchainFee,
            )
            BlockItem(
                title = stringReference(value = "Transaction Amount"),
                value = transaction.transactionAmount,
            )
            BlockItem(
                title = stringReference(value = "Currency Code"),
                value = transaction.transactionCurrencyCode,
            )
            BlockItem(
                title = stringReference(value = "Merchant Name"),
                value = transaction.merchantName,
            )
            BlockItem(
                title = stringReference(value = "Merchant City"),
                value = transaction.merchantCity,
            )
            BlockItem(
                title = stringReference(value = "Merchant Country Code"),
                value = transaction.merchantCountryCode,
            )
            BlockItem(
                title = stringReference(value = "Merchant Category Code"),
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
        title = stringReference(value = "Blockchain request"),
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
                        text = "Explore",
                        color = TangemTheme.colors.text.tertiary,
                        style = TangemTheme.typography.caption1,
                    )
                }
                SpacerW12()
            }
        },
        content = {
            BlockItem(
                title = stringReference(value = "Type"),
                value = request.type,
            )
            BlockItem(
                title = stringReference(value = "Status"),
                value = request.status,
            )
            BlockItem(
                title = stringReference(value = "Blockchain Amount"),
                value = request.blockchainAmount,
            )
            BlockItem(
                title = stringReference(value = "Blockchain Fee"),
                value = request.blockchainFee,
            )
            BlockItem(
                title = stringReference(value = "Transaction Amount"),
                value = request.transactionAmount,
            )
            BlockItem(
                title = stringReference(value = "Currency Code"),
                value = request.currencyCode,
            )
            BlockItem(
                title = stringReference(value = "Error Code"),
                value = request.errorCode.toString(),
            )
            BlockItem(
                title = stringReference(value = "Date"),
                value = request.date,
            )
            BlockItem(
                title = stringReference(value = "Tx Hash"),
                value = request.txHash,
            )
            BlockItem(
                title = stringReference(value = "Tx Status"),
                value = request.txStatus,
            )
        },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ContentContainer(
    blocksCount: Int,
    title: @Composable BoxScope.() -> Unit,
    block: @Composable ColumnScope.(Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.background(TangemTheme.colors.background.secondary),
        contentPadding = PaddingValues(
            bottom = TangemTheme.dimens.spacing16,
        ),
        verticalArrangement = Arrangement.spacedBy(space = TangemTheme.dimens.spacing12),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        stickyHeader {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = TangemTheme.dimens.size44)
                    .background(TangemTheme.colors.background.secondary),
                contentAlignment = Alignment.Center,
                content = title,
            )
        }
        items(blocksCount) { index ->
            Column {
                block(index)
            }
        }
    }
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
                    blockchainFee = "0.12",
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
                        blockchainFee = "0.10",
                        transactionAmount = "0.99 €",
                        currencyCode = "978",
                        errorCode = 0,
                        date = "2023-12-01 14:20:09.230 +0300",
                        txHash = "0xc458f0204fe43b82c775004baabb38435b5595f4307d8c3ac74625c827be7c29",
                        txStatus = "confirmed",
                        onExploreClick = {},
                    ),
                    VisaTxDetailsBottomSheetConfig.Request(
                        id = "524582128501966799",
                        type = "settlement",
                        status = "accepted",
                        blockchainAmount = "1.0614 USDT",
                        blockchainFee = "0.12",
                        transactionAmount = "0.99 €",
                        currencyCode = "978",
                        errorCode = 0,
                        date = "2023-12-01 00:01:00.000 +0300",
                        txHash = "0x635841d5fbdf1087cdd929019c863ee88a7165e4340bc17ddd0b1d04dfb11daa",
                        txStatus = "confirmed",
                        onExploreClick = {},
                    ),
                ),
            ),
        ),
    )

// endregion Preview