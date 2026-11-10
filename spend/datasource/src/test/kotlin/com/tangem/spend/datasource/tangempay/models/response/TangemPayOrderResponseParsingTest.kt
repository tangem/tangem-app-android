package com.tangem.spend.datasource.tangempay.models.response

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.json.NetworkJsonFactory
import com.tangem.spend.datasource.tangempay.serialization.TangemPaySerializersModuleProvider
import com.tangem.test.core.ProvideTestModels
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.reflect.KClass
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPayOrderResponseParsingTest {

    private val json = NetworkJsonFactory.create(TangemPaySerializersModuleProvider.provide())

    @Test
    fun `GIVEN full order WHEN parsed THEN all fields are read`() {
        // Arrange
        val raw = """
            {
              "result": {
                "id": "order-1",
                "customer_id": "customer-1",
                "type": "CARD_FREEZE_RAIN",
                "status": "PROCESSING",
                "step": "STARTING",
                "data": { "type": "CARD_FREEZE_RAIN", "product_instance_id": "pi-1" },
                "step_change_code": 2,
                "created_at": "2026-11-09T10:00:00Z",
                "updated_at": "2026-11-09T10:05:00Z",
                "error_code": "REGION_RESTRICTED"
              }
            }
        """.trimIndent()

        // Act
        val actual = json.decodeFromString(TangemPayOrderResponse.serializer(), raw)

        // Assert
        assertThat(actual).isEqualTo(
            TangemPayOrderResponse(
                result = TangemPayOrderResponse.Result(
                    id = "order-1",
                    status = "PROCESSING",
                    step = "STARTING",
                    data = OrderDataResponse.CardFreeze(productInstanceId = "pi-1"),
                ),
            ),
        )
    }

    @Test
    fun `GIVEN orders list with an unknown data type WHEN parsed THEN every order is read`() {
        // Arrange
        val raw = """
            {
              "result": [
                ${orderJson(id = "order-1", data = """{ "type": "CARD_FREEZE_RAIN", "product_instance_id": "pi-1" }""")},
                ${orderJson(id = "order-2", data = """{ "type": "SOMETHING_NEW" }""")}
              ]
            }
        """.trimIndent()

        // Act
        val actual = json.decodeFromString(TangemPayOrdersResponse.serializer(), raw)

        // Assert
        assertThat(actual.result.map { it.id to it.data }).containsExactly(
            "order-1" to OrderDataResponse.CardFreeze(productInstanceId = "pi-1"),
            "order-2" to OrderDataResponse.Unknown(
                type = "SOMETHING_NEW",
                payload = buildJsonObject { put("type", "SOMETHING_NEW") },
            ),
        ).inOrder()
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN order data of a type WHEN parsed THEN the matching data class is returned`(model: DataModel) {
        // Act
        val actual = json.decodeFromString(OrderDataResponse.serializer(), model.raw)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @Test
    fun `GIVEN all order data subtypes WHEN listing cases THEN every subtype is covered`() {
        // Act
        val covered = provideTestModels().map { it.expected::class }.toSet()

        // Assert
        assertThat(covered).containsExactlyElementsIn(OrderDataResponse::class.leafSealedSubclasses())
    }

    private fun KClass<*>.leafSealedSubclasses(): List<KClass<*>> = sealedSubclasses.flatMap { subclass ->
        if (subclass.isSealed) subclass.leafSealedSubclasses() else listOf(subclass)
    }

    @Suppress("LongMethod")
    private fun provideTestModels() = listOf(
        DataModel(
            raw = """{ "type": "CARD_FREEZE_RAIN", "product_instance_id": "pi-1" }""",
            expected = OrderDataResponse.CardFreeze(productInstanceId = "pi-1"),
        ),
        DataModel(
            raw = """{ "type": "CARD_UNFREEZE_RAIN", "product_instance_id": "pi-1" }""",
            expected = OrderDataResponse.CardUnfreeze(productInstanceId = "pi-1"),
        ),
        DataModel(
            raw = """{ "type": "CARD_CLOSE_RAIN", "product_instance_id": "pi-1" }""",
            expected = OrderDataResponse.CardClose(productInstanceId = "pi-1"),
        ),
        DataModel(
            raw = """{ "type": "CARD_ACTIVATION_PLASTIC_RAIN", "product_instance_id": "pi-1" }""",
            expected = OrderDataResponse.CardActivationPlastic(productInstanceId = "pi-1"),
        ),
        DataModel(
            raw = """{ "type": "CARD_REISSUE_VIRTUAL_RAIN", "product_instance_id": "pi-1" }""",
            expected = OrderDataResponse.CardReissueVirtual(productInstanceId = "pi-1"),
        ),
        DataModel(
            raw = """{ "type": "CARD_REISSUE_PLASTIC_RAIN", "source_product_instance_id": "pi-src" }""",
            expected = OrderDataResponse.CardReissuePlastic(
                sourceProductInstanceId = "pi-src",
                productInstanceId = null,
                embossName = null,
            ),
        ),
        DataModel(
            raw = """
                {
                  "type": "CARD_ISSUE_VIRTUAL_RAIN",
                  "specification_name": "SP_1",
                  "customer_wallet_address": "0xabc",
                  "emboss_name": "JOHN DOE",
                  "product_instance_id": "pi-1",
                  "payment_account_id": "pa-1"
                }
            """.trimIndent(),
            expected = OrderDataResponse.CardIssueVirtual(
                specificationName = "SP_1",
                customerWalletAddress = "0xabc",
                embossName = "JOHN DOE",
                productInstanceId = "pi-1",
                paymentAccountId = "pa-1",
            ),
        ),
        DataModel(
            raw = """
                {
                  "type": "CARD_ISSUE_VIRTUAL_RAIN_KYC",
                  "specification_name": "SP_1",
                  "customer_wallet_address": "0xabc",
                  "emboss_name": "JOHN DOE",
                  "product_instance_id": "pi-1",
                  "payment_account_id": "pa-1"
                }
            """.trimIndent(),
            expected = OrderDataResponse.CardIssueVirtualKyc(
                specificationName = "SP_1",
                customerWalletAddress = "0xabc",
                embossName = "JOHN DOE",
                productInstanceId = "pi-1",
                paymentAccountId = "pa-1",
            ),
        ),
        DataModel(
            raw = """
                {
                  "type": "CARD_ISSUE_VIRTUAL_RAIN_KYC_V2",
                  "specification_name": "SP_1",
                  "customer_wallet_address": "0xabc",
                  "emboss_name": "JOHN DOE",
                  "product_instance_id": "pi-1",
                  "payment_account_id": "pa-1"
                }
            """.trimIndent(),
            expected = OrderDataResponse.CardIssueVirtualKycV2(
                specificationName = "SP_1",
                customerWalletAddress = "0xabc",
                embossName = "JOHN DOE",
                productInstanceId = "pi-1",
                paymentAccountId = "pa-1",
            ),
        ),
        DataModel(
            raw = """
                {
                  "type": "CARD_ISSUE_PLASTIC_RAIN",
                  "specification_name": "SP_1",
                  "customer_wallet_address": "0xabc",
                  "emboss_name": "JOHN DOE",
                  "product_instance_id": "pi-1",
                  "payment_account_id": "pa-1"
                }
            """.trimIndent(),
            expected = OrderDataResponse.CardIssuePlastic(
                specificationName = "SP_1",
                customerWalletAddress = "0xabc",
                embossName = "JOHN DOE",
                productInstanceId = "pi-1",
                paymentAccountId = "pa-1",
            ),
        ),
        DataModel(
            raw = """
                {
                  "type": "ACCOUNT_ISSUE_VIRTUAL_RAIN",
                  "specification_name": "SP_2",
                  "deposit_address": "0xdep"
                }
            """.trimIndent(),
            expected = OrderDataResponse.AccountIssueVirtual(
                specificationName = "SP_2",
                depositAddress = "0xdep",
                productInstanceId = null,
                paymentAccountId = null,
                automationId = null,
            ),
        ),
        DataModel(
            raw = """
                {
                  "type": "ACCOUNT_ISSUE_BANKING_RAIN",
                  "specification_name": "SP_2",
                  "deposit_address": "0xdep"
                }
            """.trimIndent(),
            expected = OrderDataResponse.AccountIssueBanking(
                specificationName = "SP_2",
                depositAddress = "0xdep",
                productInstanceId = null,
                paymentAccountId = null,
                automationId = null,
            ),
        ),
        DataModel(
            raw = """{ "type": "WITHDRAWAL_RAIN", "chain_id": 137, "transaction_hash": "0xhash" }""",
            expected = OrderDataResponse.Withdrawal(chainId = 137, transactionHash = "0xhash"),
        ),
        DataModel(
            raw = """
                {
                  "type": "BANK_OFFRAMP_RAIN",
                  "chain_id": 137,
                  "bank_account_id": "ba-1",
                  "withdraw_data": {
                    "hash": "0xh",
                    "salt": "0xs",
                    "sender_address": "0xsender",
                    "structured_data": { "chainId": 137 }
                  }
                }
            """.trimIndent(),
            expected = OrderDataResponse.BankOfframp(
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
        ),
        DataModel(
            raw = """{ "type": "SMART_CONTRACT_ISSUE_RAIN", "chain_id": 137 }""",
            expected = OrderDataResponse.SmartContractIssue(chainId = 137),
        ),
        DataModel(
            raw = """
                {
                  "type": "TARIFF_PLAN_TRANSITION",
                  "customer_wallet_address": "0xabc",
                  "target_tariff_plan_id": "plan-1",
                  "tariff_plan_transition_type": "UPGRADE"
                }
            """.trimIndent(),
            expected = OrderDataResponse.TariffPlanTransition(
                customerWalletAddress = "0xabc",
                targetTariffPlanId = "plan-1",
                transitionType = "UPGRADE",
            ),
        ),
        DataModel(
            raw = """
                {
                  "type": "ONBOARDING_RAIN",
                  "customer_wallet_address": "0xabc",
                  "tariff_plan_transition": {
                    "target_tariff_plan_id": "plan-1",
                    "tariff_plan_transition_type": "ACTIVATION"
                  },
                  "account_issue_banking": { "deposit_address": "0xdep" },
                  "tariff_plan_transition_order_id": "order-tp"
                }
            """.trimIndent(),
            expected = OrderDataResponse.Onboarding(
                customerWalletAddress = "0xabc",
                tariffPlanTransition = OrderDataResponse.Onboarding.TariffPlanTransitionData(
                    targetTariffPlanId = "plan-1",
                    transitionType = "ACTIVATION",
                ),
                accountIssueBanking = OrderDataResponse.Onboarding.AccountIssueBankingData(depositAddress = "0xdep"),
                tariffPlanTransitionOrderId = "order-tp",
                accountIssueBankingOrderId = null,
            ),
        ),
        DataModel(
            raw = """{ "type": "UNDEFINED" }""",
            expected = OrderDataResponse.Unknown(
                type = "UNDEFINED",
                payload = buildJsonObject { put("type", "UNDEFINED") },
            ),
        ),
        DataModel(
            raw = """{ "type": "SOMETHING_NEW", "foo": 1 }""",
            expected = OrderDataResponse.Unknown(
                type = "SOMETHING_NEW",
                payload = buildJsonObject {
                    put("type", "SOMETHING_NEW")
                    put("foo", 1)
                },
            ),
        ),
    )

    private fun orderJson(id: String, data: String) = """
        {
          "id": "$id",
          "customer_id": "customer-1",
          "type": "CARD_FREEZE_RAIN",
          "status": "PROCESSING",
          "data": $data,
          "step_change_code": 0,
          "created_at": "2026-11-09T10:00:00Z",
          "updated_at": "2026-11-09T10:00:00Z"
        }
    """.trimIndent()

    internal data class DataModel(val raw: String, val expected: OrderDataResponse)
}