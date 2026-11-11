package com.tangem.domain.tangempay.model

import com.tangem.domain.models.account.TangemPayTariffPlanTransition

sealed interface OrderDetails {

    data class CardFreeze(val productInstanceId: String) : OrderDetails

    data class CardUnfreeze(val productInstanceId: String) : OrderDetails

    data class CardClose(val productInstanceId: String) : OrderDetails

    data class CardActivationPlastic(val productInstanceId: String) : OrderDetails

    data class CardReissueVirtual(val productInstanceId: String) : OrderDetails

    data class CardReissuePlastic(
        val sourceProductInstanceId: String,
        val productInstanceId: String?,
        val embossName: String?,
    ) : OrderDetails

    sealed interface CardIssue : OrderDetails {
        val specificationName: String
        val customerWalletAddress: String
        val embossName: String?
        val productInstanceId: String?
        val paymentAccountId: String?
    }

    data class CardIssueVirtual(
        override val specificationName: String,
        override val customerWalletAddress: String,
        override val embossName: String?,
        override val productInstanceId: String?,
        override val paymentAccountId: String?,
    ) : CardIssue

    data class CardIssueVirtualKyc(
        override val specificationName: String,
        override val customerWalletAddress: String,
        override val embossName: String?,
        override val productInstanceId: String?,
        override val paymentAccountId: String?,
    ) : CardIssue

    data class CardIssueVirtualKycV2(
        override val specificationName: String,
        override val customerWalletAddress: String,
        override val embossName: String?,
        override val productInstanceId: String?,
        override val paymentAccountId: String?,
    ) : CardIssue

    data class CardIssuePlastic(
        override val specificationName: String,
        override val customerWalletAddress: String,
        override val embossName: String?,
        override val productInstanceId: String?,
        override val paymentAccountId: String?,
    ) : CardIssue

    sealed interface AccountIssue : OrderDetails {
        val specificationName: String
        val depositAddress: String
        val productInstanceId: String?
        val paymentAccountId: String?
        val automationId: String?
    }

    data class AccountIssueVirtual(
        override val specificationName: String,
        override val depositAddress: String,
        override val productInstanceId: String?,
        override val paymentAccountId: String?,
        override val automationId: String?,
    ) : AccountIssue

    data class AccountIssueBanking(
        override val specificationName: String,
        override val depositAddress: String,
        override val productInstanceId: String?,
        override val paymentAccountId: String?,
        override val automationId: String?,
    ) : AccountIssue

    data class Withdrawal(
        val chainId: Int,
        val transactionHash: String?,
    ) : OrderDetails

    data class BankOfframp(
        val chainId: Int,
        val transactionHash: String?,
        val bankAccountId: String?,
        val withdrawData: WithdrawData?,
    ) : OrderDetails {

        data class WithdrawData(
            val hash: String,
            val salt: String,
            val senderAddress: String,
            val structuredData: String,
        )
    }

    data class SmartContractIssue(val chainId: Int) : OrderDetails

    data class TariffPlanTransition(
        val customerWalletAddress: String,
        val targetTariffPlanId: String,
        val transitionType: TangemPayTariffPlanTransition.Type,
    ) : OrderDetails

    data class Onboarding(
        val customerWalletAddress: String,
        val tariffPlanTransition: TariffPlanTransition?,
        val accountIssueBankingDepositAddress: String?,
        val tariffPlanTransitionOrderId: String?,
        val accountIssueBankingOrderId: String?,
    ) : OrderDetails {

        data class TariffPlanTransition(
            val targetTariffPlanId: String,
            val transitionType: TangemPayTariffPlanTransition.Type,
        )
    }

    data object Unknown : OrderDetails
}