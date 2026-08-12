package com.tangem.data.pay.util

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.pay.model.PlasticCardOrder
import com.tangem.domain.pay.model.ShippingAddress
import com.squareup.moshi.Moshi
import org.json.JSONObject
import com.tangem.spend.datasource.pay.models.request.OrderRequest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PlasticIssueOrderRequestConverterTest {

    @Test
    fun `GIVEN a plastic order WHEN convert THEN the whole request matches the plastic issue contract`() {
        // Act
        val actual = convert(order = plasticCardOrder())

        // Assert
        assertThat(actual).isEqualTo(
            OrderRequest(
                data = OrderRequest.Data(
                    customerWalletAddress = WALLET_ADDRESS,
                    specificationName = SPEC_NAME,
                    type = "CARD_ISSUE_PLASTIC_RAIN",
                    embossName = "JOHNNY SILVERHAND",
                    shippingAddress = OrderRequest.ShippingAddress(
                        firstName = "Johnny",
                        lastName = "Silverhand",
                        line1 = "Crescent st. 24",
                        line2 = "Apt. 56",
                        city = "Night City",
                        region = "California",
                        postalCode = "90210",
                        phone = "+12345678901",
                    ),
                ),
                idempotencyKey = IDEMPOTENCY_KEY,
            ),
        )
    }

    @Test
    fun `GIVEN a plastic order WHEN convert THEN country and email are not sent`() {
        // Act
        val actual = convert(order = plasticCardOrder())

        // Assert
        val json = MOSHI.adapter(OrderRequest::class.java).toJson(actual)
        val data = JSONObject(json).getJSONObject("data")
        val shipping = data.getJSONObject("shipping_address")
        assertThat(data.keys().asSequence().toList()).doesNotContain("country")
        assertThat(shipping.keys().asSequence().toList()).containsExactly(
            "first_name",
            "last_name",
            "line1",
            "line2",
            "city",
            "region",
            "postal_code",
            "phone",
        )
    }

    @Test
    fun `GIVEN a plastic order WHEN convert THEN unrelated order fields stay unset`() {
        // Act
        val actual = convert(order = plasticCardOrder())

        // Assert
        assertThat(actual.data.targetTariffPlanId).isNull()
        assertThat(actual.data.tariffPlanTransitionType).isNull()
        assertThat(actual.data.chainId).isNull()
    }

    @ParameterizedTest
    @MethodSource("provideTestModels")
    fun `GIVEN an optional field WHEN convert THEN it is carried over verbatim`(model: OptionalFieldModel) {
        // Act
        val actual = convert(order = plasticCardOrder(line2 = model.line2))

        // Assert
        assertThat(actual.data.shippingAddress?.line2).isEqualTo(model.line2)
    }

    private fun convert(order: PlasticCardOrder) =
        PlasticIssueOrderRequestConverter.convert(
            customerWalletAddress = WALLET_ADDRESS,
            specificationName = SPEC_NAME,
            order = order,
            idempotencyKey = IDEMPOTENCY_KEY,
        )

    private fun provideTestModels() = listOf(
        OptionalFieldModel(name = "line2 present", line2 = "Apt. 56"),
        OptionalFieldModel(name = "line2 absent", line2 = null),
    )

    internal data class OptionalFieldModel(val name: String, val line2: String?) {
        override fun toString(): String = name
    }

    private companion object {
        val MOSHI: Moshi = Moshi.Builder().build()
        const val WALLET_ADDRESS = "0xC0FFEE0000000000000000000000000000000001"
        const val SPEC_NAME = "SP_000010"
        const val IDEMPOTENCY_KEY = "0a1b2c3d-0000-4000-8000-000000000002"

        fun plasticCardOrder(line2: String? = "Apt. 56") = PlasticCardOrder(
            embossName = "JOHNNY SILVERHAND",
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
    }
}