package com.tangem.data.pay.util

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import com.tangem.domain.pay.model.CustomerOrderRequest
import com.tangem.domain.pay.model.OrderType
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.spend.datasource.pay.models.request.OrderRequest
import com.tangem.utils.converter.Converter
import com.tangem.utils.logging.TangemLogger

internal object OrderRequestConverter : Converter<OrderRequestConverter.Input, Either<VisaApiError, OrderRequest>> {

    data class Input(
        val customerWalletAddress: String,
        val request: CustomerOrderRequest,
        val idempotencyKey: String,
    )

    override fun convert(value: Input): Either<VisaApiError, OrderRequest> = either {
        val address = value.customerWalletAddress
        val data = when (val request = value.request) {
            is CustomerOrderRequest.CardIssue -> convertCardIssue(address = address, request = request)
            is CustomerOrderRequest.PlasticCardIssue -> OrderRequest.PlasticCardIssue(
                customerWalletAddress = address,
                specificationName = request.specificationName,
                embossName = request.order.embossName,
                shippingAddress = ShippingAddressRequestConverter.convert(request.order.shippingAddress),
            )
            is CustomerOrderRequest.PlasticCardReissue -> OrderRequest.PlasticCardReissue(
                customerWalletAddress = address,
                sourceProductInstanceId = request.sourceProductInstanceId,
                embossName = request.order.embossName,
                shippingAddress = ShippingAddressRequestConverter.convert(request.order.shippingAddress),
            )
            is CustomerOrderRequest.CardActivation -> OrderRequest.PlasticCardActivation(
                customerWalletAddress = address,
                productInstanceId = request.order.productInstanceId,
                lastFourDigits = request.order.lastFourDigits,
            )
            is CustomerOrderRequest.TariffPlanTransition -> OrderRequest.TariffPlanTransition(
                customerWalletAddress = address,
                targetTariffPlanId = request.targetTariffPlanId,
                tariffPlanTransitionType = request.transitionType.name,
            )
            is CustomerOrderRequest.SmartContractIssue -> OrderRequest.SmartContractIssue(
                customerWalletAddress = address,
                chainId = request.chainId,
            )
        }
        OrderRequest(data = data, idempotencyKey = value.idempotencyKey)
    }

    private fun Raise<VisaApiError>.convertCardIssue(
        address: String,
        request: CustomerOrderRequest.CardIssue,
    ): OrderRequest.Data = when (request.type) {
        OrderType.CARD_ISSUE_VIRTUAL_RAIN -> OrderRequest.VirtualCardIssue(
            customerWalletAddress = address,
            specificationName = request.specificationName,
            deeplinkValue = request.deeplinkValue,
        )
        OrderType.CARD_ISSUE_VIRTUAL_RAIN_KYC -> OrderRequest.VirtualCardIssueKyc(
            customerWalletAddress = address,
            specificationName = request.specificationName,
            deeplinkValue = request.deeplinkValue,
        )
        OrderType.CARD_ISSUE_VIRTUAL_RAIN_KYC_V2 -> OrderRequest.VirtualCardIssueKycV2(
            customerWalletAddress = address,
            specificationName = request.specificationName,
            deeplinkValue = request.deeplinkValue,
        )
        OrderType.CARD_ISSUE_ADDITIONAL -> OrderRequest.AdditionalCardIssue(
            customerWalletAddress = address,
            specificationName = request.specificationName,
            deeplinkValue = request.deeplinkValue,
        )
        else -> {
            TangemLogger.e("Order type ${request.type} has no card issue request")
            raise(VisaApiError.Unspecified)
        }
    }
}