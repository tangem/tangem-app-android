package com.tangem.spend.datasource.pay.models.request

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.json.NetworkJsonFactory
import com.tangem.test.core.ProvideTestModels
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OrderRequestSerializationTest {

    private val json = NetworkJsonFactory.create()

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN order data WHEN encode THEN type discriminator and declared fields are written`(model: EncodeModel) {
        // Arrange
        val request = OrderRequest(data = model.data)

        // Act
        val encoded = json.encodeToJsonElement(OrderRequest.serializer(), request).jsonObject
        val data = encoded.getValue("data").jsonObject

        // Assert
        assertThat(encoded.keys).containsExactly("data")
        assertThat(data.getValue("type").jsonPrimitive.content).isEqualTo(model.expectedType)
        assertThat(data.keys).containsExactlyElementsIn(model.expectedKeys + "type")
    }

    private fun provideTestModels() = listOf(
        EncodeModel(
            data = OrderRequest.VirtualCardIssue(
                customerWalletAddress = WALLET,
                specificationName = SPEC,
                deeplinkValue = "tpay_for_oxford",
            ),
            expectedType = "CARD_ISSUE_VIRTUAL_RAIN",
            expectedKeys = setOf("customer_wallet_address", "specification_name", "deeplink_value"),
        ),
        EncodeModel(
            data = OrderRequest.VirtualCardIssueKyc(customerWalletAddress = WALLET, specificationName = SPEC),
            expectedType = "CARD_ISSUE_VIRTUAL_RAIN_KYC",
            expectedKeys = setOf("customer_wallet_address", "specification_name"),
        ),
        EncodeModel(
            data = OrderRequest.VirtualCardIssueKycV2(customerWalletAddress = WALLET, specificationName = null),
            expectedType = "CARD_ISSUE_VIRTUAL_RAIN_KYC_V2",
            expectedKeys = setOf("customer_wallet_address"),
        ),
        EncodeModel(
            data = OrderRequest.AdditionalCardIssue(customerWalletAddress = WALLET, specificationName = SPEC),
            expectedType = "CARD_ISSUE_ADDITIONAL",
            expectedKeys = setOf("customer_wallet_address", "specification_name"),
        ),
        EncodeModel(
            data = OrderRequest.PlasticCardIssue(
                customerWalletAddress = WALLET,
                specificationName = SPEC,
                embossName = "JOHN DOE",
                shippingAddress = shippingAddress(),
            ),
            expectedType = "CARD_ISSUE_PLASTIC_RAIN",
            expectedKeys = setOf("customer_wallet_address", "specification_name", "emboss_name", "shipping_address"),
        ),
        EncodeModel(
            data = OrderRequest.PlasticCardReissue(
                customerWalletAddress = WALLET,
                sourceProductInstanceId = "pi-1",
                embossName = "JOHN DOE",
                shippingAddress = shippingAddress(),
            ),
            expectedType = "CARD_REISSUE_PLASTIC_RAIN",
            expectedKeys = setOf(
                "customer_wallet_address",
                "source_product_instance_id",
                "emboss_name",
                "shipping_address",
            ),
        ),
        EncodeModel(
            data = OrderRequest.PlasticCardActivation(
                customerWalletAddress = WALLET,
                productInstanceId = "pi-1",
                lastFourDigits = "1234",
            ),
            expectedType = "CARD_ACTIVATION_PLASTIC_RAIN",
            expectedKeys = setOf("customer_wallet_address", "product_instance_id", "last_four_digits"),
        ),
        EncodeModel(
            data = OrderRequest.TariffPlanTransition(
                customerWalletAddress = WALLET,
                targetTariffPlanId = "plan-plus",
                tariffPlanTransitionType = "UPGRADE",
            ),
            expectedType = "TARIFF_PLAN_TRANSITION",
            expectedKeys = setOf("customer_wallet_address", "target_tariff_plan_id", "tariff_plan_transition_type"),
        ),
        EncodeModel(
            data = OrderRequest.SmartContractIssue(customerWalletAddress = WALLET, chainId = 137),
            expectedType = "SMART_CONTRACT_ISSUE_RAIN",
            expectedKeys = setOf("customer_wallet_address", "chain_id"),
        ),
        EncodeModel(
            data = OrderRequest.VirtualAccountIssue(depositAddress = "0xdeposit"),
            expectedType = "ACCOUNT_ISSUE_VIRTUAL_RAIN",
            expectedKeys = setOf("deposit_address", "specification_name"),
        ),
        EncodeModel(
            data = OrderRequest.BankingAccountIssue(depositAddress = "0xdeposit", signature = "0xsig"),
            expectedType = "ACCOUNT_ISSUE_BANKING_RAIN",
            expectedKeys = setOf("deposit_address", "signature", "specification_name"),
        ),
    )

    internal data class EncodeModel(
        val data: OrderRequest.Data,
        val expectedType: String,
        val expectedKeys: Set<String>,
    ) {
        override fun toString(): String = expectedType
    }

    private fun shippingAddress() = OrderRequest.ShippingAddress(
        firstName = "John",
        lastName = "Doe",
        line1 = "Line 1",
        line2 = null,
        city = "City",
        region = "Region",
        postalCode = "00000",
        phoneNumber = "+10000000000",
    )

    private companion object {
        const val WALLET = "0xWALLET"
        const val SPEC = "SP_000010"
    }
}