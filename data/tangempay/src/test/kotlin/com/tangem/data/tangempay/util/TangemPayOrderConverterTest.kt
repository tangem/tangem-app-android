package com.tangem.data.tangempay.util

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.account.TangemPayTariffPlanTransition
import com.tangem.domain.tangempay.model.OrderDetails
import com.tangem.domain.tangempay.model.OrderStep
import com.tangem.domain.tangempay.model.TangemPayOrder
import com.tangem.spend.datasource.tangempay.models.response.OrderDataResponse
import com.tangem.spend.datasource.tangempay.models.response.TangemPayOrderResponse
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayOrderConverterTest {

    @Test
    fun `GIVEN order response WHEN convert THEN common fields are mapped`() {
        // Act
        val actual = TangemPayOrderConverter.convert(result(data = UNKNOWN_DATA))

        // Assert
        assertThat(actual).isEqualTo(
            TangemPayOrder(
                id = "order-1",
                status = TangemPayOrder.Status.PROCESSING,
                step = OrderStep.STARTING,
                details = OrderDetails.Unknown,
            ),
        )
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN status WHEN convert THEN mapped status returned`(model: StatusModel) {
        // Act
        val actual = TangemPayOrderConverter.convert(result(data = UNKNOWN_DATA, status = model.status))

        // Assert
        assertThat(actual.status).isEqualTo(model.expected)
    }

    @ParameterizedTest
    @MethodSource("provideDetailsModels")
    fun `GIVEN order data WHEN convert THEN typed details returned`(model: DetailsModel) {
        // Act
        val actual = TangemPayOrderConverter.convert(result(data = model.data))

        // Assert
        assertThat(actual.details).isEqualTo(model.expected)
    }

    private fun provideTestModels() = listOf(
        StatusModel(status = "new", expected = TangemPayOrder.Status.NEW),
        StatusModel(status = "NEW", expected = TangemPayOrder.Status.NEW),
        StatusModel(status = "PROCESSING", expected = TangemPayOrder.Status.PROCESSING),
        StatusModel(status = "COMPLETED", expected = TangemPayOrder.Status.COMPLETED),
        StatusModel(status = "CANCELED", expected = TangemPayOrder.Status.CANCELED),
        StatusModel(status = "SOMETHING_NEW", expected = TangemPayOrder.Status.UNKNOWN),
    )

    @Suppress("LongMethod")
    private fun provideDetailsModels() = listOf(
        DetailsModel(
            data = OrderDataResponse.CardFreeze(productInstanceId = "pi-1"),
            expected = OrderDetails.CardFreeze(productInstanceId = "pi-1"),
        ),
        DetailsModel(
            data = OrderDataResponse.CardUnfreeze(productInstanceId = "pi-1"),
            expected = OrderDetails.CardUnfreeze(productInstanceId = "pi-1"),
        ),
        DetailsModel(
            data = OrderDataResponse.CardClose(productInstanceId = "pi-1"),
            expected = OrderDetails.CardClose(productInstanceId = "pi-1"),
        ),
        DetailsModel(
            data = OrderDataResponse.CardActivationPlastic(productInstanceId = "pi-1"),
            expected = OrderDetails.CardActivationPlastic(productInstanceId = "pi-1"),
        ),
        DetailsModel(
            data = OrderDataResponse.CardReissueVirtual(productInstanceId = "pi-1"),
            expected = OrderDetails.CardReissueVirtual(productInstanceId = "pi-1"),
        ),
        DetailsModel(
            data = OrderDataResponse.CardReissuePlastic(
                sourceProductInstanceId = "pi-src",
                productInstanceId = "pi-new",
                embossName = "JOHN DOE",
            ),
            expected = OrderDetails.CardReissuePlastic(
                sourceProductInstanceId = "pi-src",
                productInstanceId = "pi-new",
                embossName = "JOHN DOE",
            ),
        ),
        DetailsModel(
            data = OrderDataResponse.CardIssuePlastic(
                specificationName = "SP_1",
                customerWalletAddress = "0xabc",
                embossName = null,
                productInstanceId = null,
                paymentAccountId = "pa-1",
            ),
            expected = OrderDetails.CardIssuePlastic(
                specificationName = "SP_1",
                customerWalletAddress = "0xabc",
                embossName = null,
                productInstanceId = null,
                paymentAccountId = "pa-1",
            ),
        ),
        DetailsModel(
            data = OrderDataResponse.AccountIssueVirtual(
                specificationName = "SP_2",
                depositAddress = "0xdep",
                productInstanceId = null,
                paymentAccountId = null,
                automationId = "auto-1",
            ),
            expected = OrderDetails.AccountIssueVirtual(
                specificationName = "SP_2",
                depositAddress = "0xdep",
                productInstanceId = null,
                paymentAccountId = null,
                automationId = "auto-1",
            ),
        ),
        DetailsModel(
            data = OrderDataResponse.Withdrawal(chainId = 137, transactionHash = "0xhash"),
            expected = OrderDetails.Withdrawal(chainId = 137, transactionHash = "0xhash"),
        ),
        DetailsModel(
            data = OrderDataResponse.BankOfframp(
                chainId = 137,
                transactionHash = null,
                bankAccountId = "ba-1",
                withdrawData = OrderDataResponse.BankOfframp.WithdrawData(
                    hash = "0xh",
                    salt = "0xs",
                    senderAddress = "0xsender",
                    structuredData = """{"chainId":137}""",
                ),
            ),
            expected = OrderDetails.BankOfframp(
                chainId = 137,
                transactionHash = null,
                bankAccountId = "ba-1",
                withdrawData = OrderDetails.BankOfframp.WithdrawData(
                    hash = "0xh",
                    salt = "0xs",
                    senderAddress = "0xsender",
                    structuredData = """{"chainId":137}""",
                ),
            ),
        ),
        DetailsModel(
            data = OrderDataResponse.SmartContractIssue(chainId = 137),
            expected = OrderDetails.SmartContractIssue(chainId = 137),
        ),
        DetailsModel(
            data = OrderDataResponse.TariffPlanTransition(
                customerWalletAddress = "0xabc",
                targetTariffPlanId = "plan-1",
                transitionType = "DOWNGRADE",
            ),
            expected = OrderDetails.TariffPlanTransition(
                customerWalletAddress = "0xabc",
                targetTariffPlanId = "plan-1",
                transitionType = TangemPayTariffPlanTransition.Type.DOWNGRADE,
            ),
        ),
        DetailsModel(
            data = OrderDataResponse.Onboarding(
                customerWalletAddress = "0xabc",
                tariffPlanTransition = OrderDataResponse.Onboarding.TariffPlanTransitionData(
                    targetTariffPlanId = "plan-1",
                    transitionType = "ACTIVATION",
                ),
                accountIssueBanking = OrderDataResponse.Onboarding.AccountIssueBankingData(depositAddress = "0xdep"),
                tariffPlanTransitionOrderId = "order-tp",
                accountIssueBankingOrderId = "order-ab",
            ),
            expected = OrderDetails.Onboarding(
                customerWalletAddress = "0xabc",
                tariffPlanTransition = OrderDetails.Onboarding.TariffPlanTransition(
                    targetTariffPlanId = "plan-1",
                    transitionType = TangemPayTariffPlanTransition.Type.ACTIVATION,
                ),
                accountIssueBankingDepositAddress = "0xdep",
                tariffPlanTransitionOrderId = "order-tp",
                accountIssueBankingOrderId = "order-ab",
            ),
        ),
        DetailsModel(
            data = OrderDataResponse.CardIssueVirtual(
                specificationName = "SP_1",
                customerWalletAddress = "0xabc",
                embossName = null,
                productInstanceId = null,
                paymentAccountId = null,
            ),
            expected = OrderDetails.CardIssueVirtual(
                specificationName = "SP_1",
                customerWalletAddress = "0xabc",
                embossName = null,
                productInstanceId = null,
                paymentAccountId = null,
            ),
        ),
        DetailsModel(
            data = OrderDataResponse.CardIssueVirtualKyc(
                specificationName = "SP_1",
                customerWalletAddress = "0xabc",
                embossName = null,
                productInstanceId = null,
                paymentAccountId = null,
            ),
            expected = OrderDetails.CardIssueVirtualKyc(
                specificationName = "SP_1",
                customerWalletAddress = "0xabc",
                embossName = null,
                productInstanceId = null,
                paymentAccountId = null,
            ),
        ),
        DetailsModel(
            data = OrderDataResponse.CardIssueVirtualKycV2(
                specificationName = "SP_1",
                customerWalletAddress = "0xabc",
                embossName = null,
                productInstanceId = null,
                paymentAccountId = null,
            ),
            expected = OrderDetails.CardIssueVirtualKycV2(
                specificationName = "SP_1",
                customerWalletAddress = "0xabc",
                embossName = null,
                productInstanceId = null,
                paymentAccountId = null,
            ),
        ),
        DetailsModel(
            data = OrderDataResponse.AccountIssueBanking(
                specificationName = "SP_2",
                depositAddress = "0xdep",
                productInstanceId = "pi-1",
                paymentAccountId = "pa-1",
                automationId = null,
            ),
            expected = OrderDetails.AccountIssueBanking(
                specificationName = "SP_2",
                depositAddress = "0xdep",
                productInstanceId = "pi-1",
                paymentAccountId = "pa-1",
                automationId = null,
            ),
        ),
        DetailsModel(data = UNKNOWN_DATA, expected = OrderDetails.Unknown),
    )

    private fun result(data: OrderDataResponse, status: String = "PROCESSING") = TangemPayOrderResponse.Result(
        id = "order-1",
        status = status,
        step = "STARTING",
        data = data,
    )

    private companion object {
        val UNKNOWN_DATA = OrderDataResponse.Unknown(type = "SOMETHING_NEW", payload = null)
    }

    internal data class StatusModel(val status: String, val expected: TangemPayOrder.Status)

    internal data class DetailsModel(val data: OrderDataResponse, val expected: OrderDetails)
}