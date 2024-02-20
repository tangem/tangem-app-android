package com.tangem.feature.wallet.presentation.wallet.state.model

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import kotlinx.collections.immutable.ImmutableList

internal data class VisaTxDetailsBottomSheetConfig(
    val transaction: Transaction,
    val requests: ImmutableList<Request>,
) : TangemBottomSheetConfigContent {

    data class Transaction(
        val id: String,
        val type: String,
        val status: String,
        val blockchainAmount: String,
        val blockchainFee: String,
        val transactionAmount: String,
        val transactionCurrencyCode: String,
        val merchantName: String,
        val merchantCity: String,
        val merchantCountryCode: String,
        val merchantCategoryCode: String,
    )

    data class Request(
        val id: String,
        val type: String,
        val status: String,
        val blockchainAmount: String,
        val blockchainFee: String,
        val transactionAmount: String,
        val currencyCode: String,
        val errorCode: Int,
        val date: String,
        val txHash: String,
        val txStatus: String,
        val onExploreClick: (() -> Unit)?,
    )
}