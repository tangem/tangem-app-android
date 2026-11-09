package com.tangem.data.tangempay.util

import com.tangem.domain.models.account.TangemPayTariffPlanTransition
import com.tangem.domain.tangempay.model.OrderDetails
import com.tangem.domain.tangempay.model.OrderErrorCode
import com.tangem.domain.tangempay.model.OrderStep
import com.tangem.domain.tangempay.model.TangemPayOrder
import com.tangem.spend.datasource.tangempay.models.response.OrderDataResponse
import com.tangem.spend.datasource.tangempay.models.response.TangemPayOrderResponse
import com.tangem.utils.converter.Converter

internal object TangemPayOrderConverter : Converter<TangemPayOrderResponse.Result, TangemPayOrder> {

    override fun convert(value: TangemPayOrderResponse.Result): TangemPayOrder {
        return TangemPayOrder(
            id = value.id,
            customerId = value.customerId,
            status = TangemPayOrder.Status.fromString(value.status),
            step = OrderStep.fromString(value.step),
            stepChangeCode = value.stepChangeCode,
            createdAt = value.createdAt,
            updatedAt = value.updatedAt,
            errorCode = value.errorCode?.let(OrderErrorCode::fromString),
            details = convertDetails(value.data),
        )
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    private fun convertDetails(data: OrderDataResponse): OrderDetails = when (data) {
        is OrderDataResponse.CardFreeze -> OrderDetails.CardFreeze(productInstanceId = data.productInstanceId)
        is OrderDataResponse.CardUnfreeze -> OrderDetails.CardUnfreeze(productInstanceId = data.productInstanceId)
        is OrderDataResponse.CardClose -> OrderDetails.CardClose(productInstanceId = data.productInstanceId)
        is OrderDataResponse.CardActivationPlastic -> OrderDetails.CardActivationPlastic(
            productInstanceId = data.productInstanceId,
        )
        is OrderDataResponse.CardReissueVirtual -> OrderDetails.CardReissueVirtual(
            productInstanceId = data.productInstanceId,
        )
        is OrderDataResponse.CardReissuePlastic -> OrderDetails.CardReissuePlastic(
            sourceProductInstanceId = data.sourceProductInstanceId,
            productInstanceId = data.productInstanceId,
            embossName = data.embossName,
        )
        is OrderDataResponse.CardIssueVirtual -> OrderDetails.CardIssueVirtual(
            specificationName = data.specificationName,
            customerWalletAddress = data.customerWalletAddress,
            embossName = data.embossName,
            productInstanceId = data.productInstanceId,
            paymentAccountId = data.paymentAccountId,
        )
        is OrderDataResponse.CardIssueVirtualKyc -> OrderDetails.CardIssueVirtualKyc(
            specificationName = data.specificationName,
            customerWalletAddress = data.customerWalletAddress,
            embossName = data.embossName,
            productInstanceId = data.productInstanceId,
            paymentAccountId = data.paymentAccountId,
        )
        is OrderDataResponse.CardIssueVirtualKycV2 -> OrderDetails.CardIssueVirtualKycV2(
            specificationName = data.specificationName,
            customerWalletAddress = data.customerWalletAddress,
            embossName = data.embossName,
            productInstanceId = data.productInstanceId,
            paymentAccountId = data.paymentAccountId,
        )
        is OrderDataResponse.CardIssuePlastic -> OrderDetails.CardIssuePlastic(
            specificationName = data.specificationName,
            customerWalletAddress = data.customerWalletAddress,
            embossName = data.embossName,
            productInstanceId = data.productInstanceId,
            paymentAccountId = data.paymentAccountId,
        )
        is OrderDataResponse.AccountIssueVirtual -> OrderDetails.AccountIssueVirtual(
            specificationName = data.specificationName,
            depositAddress = data.depositAddress,
            productInstanceId = data.productInstanceId,
            paymentAccountId = data.paymentAccountId,
            automationId = data.automationId,
        )
        is OrderDataResponse.AccountIssueBanking -> OrderDetails.AccountIssueBanking(
            specificationName = data.specificationName,
            depositAddress = data.depositAddress,
            productInstanceId = data.productInstanceId,
            paymentAccountId = data.paymentAccountId,
            automationId = data.automationId,
        )
        is OrderDataResponse.Withdrawal -> OrderDetails.Withdrawal(
            chainId = data.chainId,
            transactionHash = data.transactionHash,
        )
        is OrderDataResponse.BankOfframp -> OrderDetails.BankOfframp(
            chainId = data.chainId,
            transactionHash = data.transactionHash,
            bankAccountId = data.bankAccountId,
            withdrawData = data.withdrawData?.let { withdrawData ->
                OrderDetails.BankOfframp.WithdrawData(
                    hash = withdrawData.hash,
                    salt = withdrawData.salt,
                    senderAddress = withdrawData.senderAddress,
                    structuredData = withdrawData.structuredData,
                )
            },
        )
        is OrderDataResponse.SmartContractIssue -> OrderDetails.SmartContractIssue(chainId = data.chainId)
        is OrderDataResponse.TariffPlanTransition -> OrderDetails.TariffPlanTransition(
            customerWalletAddress = data.customerWalletAddress,
            targetTariffPlanId = data.targetTariffPlanId,
            transitionType = TangemPayTariffPlanTransition.Type.fromString(data.transitionType),
        )
        is OrderDataResponse.Onboarding -> OrderDetails.Onboarding(
            customerWalletAddress = data.customerWalletAddress,
            tariffPlanTransition = data.tariffPlanTransition?.let { transition ->
                OrderDetails.Onboarding.TariffPlanTransition(
                    targetTariffPlanId = transition.targetTariffPlanId,
                    transitionType = TangemPayTariffPlanTransition.Type.fromString(transition.transitionType),
                )
            },
            accountIssueBankingDepositAddress = data.accountIssueBanking?.depositAddress,
            tariffPlanTransitionOrderId = data.tariffPlanTransitionOrderId,
            accountIssueBankingOrderId = data.accountIssueBankingOrderId,
        )
        is OrderDataResponse.Unknown -> OrderDetails.Unknown
    }
}