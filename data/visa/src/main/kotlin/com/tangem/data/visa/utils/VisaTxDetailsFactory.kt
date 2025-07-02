package com.tangem.data.visa.utils

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.externallinkprovider.TxExploreState
import com.tangem.datasource.api.pay.models.response.VisaTxHistoryResponse
import com.tangem.domain.visa.model.VisaTxDetails

internal class VisaTxDetailsFactory {

    fun create(transaction: VisaTxHistoryResponse.Transaction, walletBlockchain: Blockchain): VisaTxDetails {
        return VisaTxDetails(
            id = transaction.transactionId.toString(),
            type = transaction.transactionType,
            status = transaction.transactionStatus,
            blockchainAmount = transaction.blockchainAmount,
            transactionAmount = transaction.transactionAmount,
            transactionCurrencyCode = transaction.transactionCurrencyCode,
            merchantName = transaction.merchantName,
            merchantCity = transaction.merchantCity,
            merchantCountryCode = transaction.merchantCountryCode,
            merchantCategoryCode = transaction.merchantCategoryCode,
            fiatCurrency = findCurrencyByNumericCode(transaction.transactionCurrencyCode),
            requests = transaction.requests.map { createRequest(it, walletBlockchain) },
        )
    }

    private fun createRequest(
        request: VisaTxHistoryResponse.Transaction.Request,
        walletBlockchain: Blockchain,
    ): VisaTxDetails.Request {
        return VisaTxDetails.Request(
            billingAmount = request.billingAmount,
            billingCurrencyCode = request.billingCurrencyCode,
            blockchainAmount = request.blockchainAmount,
            errorCode = request.errorCode,
            requestDate = request.requestDt,
            requestStatus = request.requestStatus,
            requestType = request.requestType,
            transactionAmount = request.transactionAmount,
            txCurrencyCode = request.transactionCurrencyCode,
            id = request.transactionRequestId.toString(),
            txHash = request.txHash,
            txStatus = request.txStatus,
            fiatCurrency = findCurrencyByNumericCode(request.transactionCurrencyCode),
            exploreUrl = request.txHash?.let {
                when (val txUrl = walletBlockchain.getExploreTxUrl(it)) {
                    is TxExploreState.Url -> txUrl.url
                    is TxExploreState.Unsupported -> ""
                }
            },
        )
    }
}