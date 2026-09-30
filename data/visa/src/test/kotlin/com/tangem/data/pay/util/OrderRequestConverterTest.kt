package com.tangem.data.pay.util

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.json.NetworkJsonFactory
import com.tangem.domain.models.account.TangemPayTariffPlanTransition
import com.tangem.domain.pay.model.CardActivationOrder
import com.tangem.domain.pay.model.CustomerOrderRequest
import com.tangem.domain.pay.model.OrderType
import com.tangem.domain.pay.model.PlasticCardOrder
import com.tangem.domain.pay.model.ShippingAddress
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.spend.datasource.pay.models.request.OrderRequest
import com.tangem.test.core.ProvideTestModels
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OrderRequestConverterTest {

    private val json = NetworkJsonFactory.create()

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN order request WHEN convert THEN the matching wire request is built`(model: ConvertModel) {
        // Act
        val actual = convert(model.request)

        // Assert
        assertThat(actual).isEqualTo(OrderRequest(data = model.expected, idempotencyKey = IDEMPOTENCY_KEY).right())
    }

    @ParameterizedTest
    @MethodSource("provideNonCardIssueTypes")
    fun `GIVEN card issue request with a type without a card issue contract WHEN convert THEN unspecified error`(
        type: OrderType,
    ) {
        // Act
        val actual = convert(CustomerOrderRequest.CardIssue(type = type, specificationName = SPEC))

        // Assert
        assertThat(actual).isEqualTo(VisaApiError.Unspecified.left())
    }

    @Test
    fun `GIVEN plastic card issue WHEN convert THEN only the plastic issue fields are sent`() {
        // Act
        val actual = convert(
            CustomerOrderRequest.PlasticCardIssue(specificationName = SPEC, order = plasticCardOrder()),
        )

        // Assert
        val request = requireNotNull(actual.getOrNull())
        val data = json.encodeToJsonElement(OrderRequest.serializer(), request).jsonObject.getValue("data").jsonObject
        assertThat(data.keys).containsExactly(
            "type",
            "customer_wallet_address",
            "specification_name",
            "emboss_name",
            "shipping_address",
        )
        assertThat(data.getValue("shipping_address").jsonObject.keys).containsExactly(
            "first_name",
            "last_name",
            "line1",
            "line2",
            "city",
            "region",
            "postal_code",
            "phone_number",
        )
    }

    @ParameterizedTest
    @MethodSource("provideOptionalLineModels")
    fun `GIVEN an optional address line WHEN convert THEN it is carried over verbatim`(model: OptionalLineModel) {
        // Act
        val actual = convert(
            CustomerOrderRequest.PlasticCardIssue(
                specificationName = SPEC,
                order = plasticCardOrder(line2 = model.line2),
            ),
        )

        // Assert
        val data = requireNotNull(actual.getOrNull()).data as OrderRequest.PlasticCardIssue
        assertThat(data.shippingAddress.line2).isEqualTo(model.line2)
    }

    private fun convert(request: CustomerOrderRequest) = OrderRequestConverter.convert(
        OrderRequestConverter.Input(
            customerWalletAddress = WALLET,
            request = request,
            idempotencyKey = IDEMPOTENCY_KEY,
        ),
    )

    private fun provideTestModels() = listOf(
        ConvertModel(
            name = "virtual card issue",
            request = cardIssue(OrderType.CARD_ISSUE_VIRTUAL_RAIN),
            expected = OrderRequest.VirtualCardIssue(WALLET, SPEC, DEEPLINK),
        ),
        ConvertModel(
            name = "virtual card issue with kyc",
            request = cardIssue(OrderType.CARD_ISSUE_VIRTUAL_RAIN_KYC),
            expected = OrderRequest.VirtualCardIssueKyc(WALLET, SPEC, DEEPLINK),
        ),
        ConvertModel(
            name = "virtual card issue with kyc v2",
            request = cardIssue(OrderType.CARD_ISSUE_VIRTUAL_RAIN_KYC_V2),
            expected = OrderRequest.VirtualCardIssueKycV2(WALLET, SPEC, DEEPLINK),
        ),
        ConvertModel(
            name = "additional card issue",
            request = cardIssue(OrderType.CARD_ISSUE_ADDITIONAL),
            expected = OrderRequest.AdditionalCardIssue(WALLET, SPEC, DEEPLINK),
        ),
        ConvertModel(
            name = "plastic card issue",
            request = CustomerOrderRequest.PlasticCardIssue(specificationName = SPEC, order = plasticCardOrder()),
            expected = OrderRequest.PlasticCardIssue(
                customerWalletAddress = WALLET,
                specificationName = SPEC,
                embossName = EMBOSS_NAME,
                shippingAddress = shippingAddressRequest(),
            ),
        ),
        ConvertModel(
            name = "plastic card reissue",
            request = CustomerOrderRequest.PlasticCardReissue(
                sourceProductInstanceId = SOURCE_PRODUCT_INSTANCE_ID,
                order = plasticCardOrder(),
            ),
            expected = OrderRequest.PlasticCardReissue(
                customerWalletAddress = WALLET,
                sourceProductInstanceId = SOURCE_PRODUCT_INSTANCE_ID,
                embossName = EMBOSS_NAME,
                shippingAddress = shippingAddressRequest(),
            ),
        ),
        ConvertModel(
            name = "card activation",
            request = CustomerOrderRequest.CardActivation(
                order = CardActivationOrder(productInstanceId = PRODUCT_INSTANCE_ID, lastFourDigits = LAST_FOUR_DIGITS),
            ),
            expected = OrderRequest.PlasticCardActivation(
                customerWalletAddress = WALLET,
                productInstanceId = PRODUCT_INSTANCE_ID,
                lastFourDigits = LAST_FOUR_DIGITS,
            ),
        ),
        ConvertModel(
            name = "tariff plan transition",
            request = CustomerOrderRequest.TariffPlanTransition(
                targetTariffPlanId = TARGET_PLAN,
                transitionType = TangemPayTariffPlanTransition.Type.UPGRADE,
            ),
            expected = OrderRequest.TariffPlanTransition(WALLET, TARGET_PLAN, "UPGRADE"),
        ),
        ConvertModel(
            name = "smart contract issue",
            request = CustomerOrderRequest.SmartContractIssue(chainId = CHAIN_ID),
            expected = OrderRequest.SmartContractIssue(WALLET, CHAIN_ID),
        ),
    )

    private fun provideNonCardIssueTypes() = OrderType.entries - setOf(
        OrderType.CARD_ISSUE_VIRTUAL_RAIN,
        OrderType.CARD_ISSUE_VIRTUAL_RAIN_KYC,
        OrderType.CARD_ISSUE_VIRTUAL_RAIN_KYC_V2,
        OrderType.CARD_ISSUE_ADDITIONAL,
    )

    private fun provideOptionalLineModels() = listOf(
        OptionalLineModel(name = "line2 present", line2 = "Apt. 56"),
        OptionalLineModel(name = "line2 absent", line2 = null),
    )

    private fun cardIssue(type: OrderType) = CustomerOrderRequest.CardIssue(
        type = type,
        specificationName = SPEC,
        deeplinkValue = DEEPLINK,
    )

    private fun plasticCardOrder(line2: String? = "Apt. 56") = PlasticCardOrder(
        embossName = EMBOSS_NAME,
        shippingAddress = ShippingAddress(
            firstName = "Johnny",
            lastName = "Silverhand",
            region = "California",
            city = "Night City",
            line1 = "Crescent st. 24",
            line2 = line2,
            postalCode = "90210",
            phone = "+12345678901",
        ),
    )

    private fun shippingAddressRequest() = OrderRequest.ShippingAddress(
        firstName = "Johnny",
        lastName = "Silverhand",
        line1 = "Crescent st. 24",
        line2 = "Apt. 56",
        city = "Night City",
        region = "California",
        postalCode = "90210",
        phoneNumber = "+12345678901",
    )

    internal data class ConvertModel(
        val name: String,
        val request: CustomerOrderRequest,
        val expected: OrderRequest.Data,
    ) {
        override fun toString(): String = name
    }

    internal data class OptionalLineModel(val name: String, val line2: String?) {
        override fun toString(): String = name
    }

    private companion object {
        const val WALLET = "0xWALLET"
        const val SPEC = "SP_000010"
        const val DEEPLINK = "tpay_for_oxford"
        const val EMBOSS_NAME = "JOHNNY SILVERHAND"
        const val SOURCE_PRODUCT_INSTANCE_ID = "pi_source_0001"
        const val PRODUCT_INSTANCE_ID = "pi-1"
        const val LAST_FOUR_DIGITS = "8252"
        const val TARGET_PLAN = "plan-plus"
        const val CHAIN_ID = 137
        const val IDEMPOTENCY_KEY = "6f1c9e2a-0b3d-4c5e-8a7b-9d0e1f2a3b4c"
    }
}