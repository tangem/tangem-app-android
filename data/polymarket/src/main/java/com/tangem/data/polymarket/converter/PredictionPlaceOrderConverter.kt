package com.tangem.data.polymarket.converter

import com.tangem.grow.datasource.polymarket.clob.models.PolymarketPlaceOrderResponse
import com.tangem.domain.polymarket.model.PredictionPlaceOrderResult
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

internal object PredictionPlaceOrderConverter :
    Converter<PolymarketPlaceOrderResponse, PredictionPlaceOrderResult> {

    override fun convert(value: PolymarketPlaceOrderResponse): PredictionPlaceOrderResult = PredictionPlaceOrderResult(
        isSuccess = value.isSuccess,
        orderId = value.orderId,
        status = value.status,
        makingAmount = value.makingAmount?.let(::BigDecimal),
        takingAmount = value.takingAmount?.let(::BigDecimal),
        transactionHashes = value.transactionHashes.orEmpty(),
        errorMessage = value.errorMessage,
    )
}