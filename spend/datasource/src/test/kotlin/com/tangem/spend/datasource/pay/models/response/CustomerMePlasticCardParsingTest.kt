package com.tangem.spend.datasource.pay.models.response

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class CustomerMePlasticCardParsingTest {

    private class BigDecimalAdapter {
        @FromJson
        fun fromJson(value: String) = BigDecimal(value)

        @ToJson
        fun toJson(value: BigDecimal) = value.toString()
    }

    private val moshi = Moshi.Builder().add(BigDecimalAdapter()).build()
    private val adapter = moshi.adapter(CustomerMeResponse.Result::class.java)

    @Test
    fun `GIVEN plastic card awaiting activation WHEN parse THEN card type status and PI status are mapped`() {
        // Act
        val result = adapter.fromJson(PLASTIC_AWAITING_ACTIVATION_JSON)!!

        // Assert
        val plasticCard = result.cards.single { it.cardType == "PHYSICAL" }
        assertThat(plasticCard.cardStatus).isEqualTo("INACTIVE")
        assertThat(plasticCard.cardNumberEnd).isEqualTo("8890")

        val plasticInstance = result.productInstances.single { it.cardId == plasticCard.id }
        assertThat(plasticInstance.status).isEqualTo(CustomerMeResponse.ProductInstance.Status.SENT_TO_DELIVERY)
        assertThat(plasticInstance.specificationDataType)
            .isEqualTo(CustomerMeResponse.ProductInstance.SpecificationDataType.CARD)
    }

    @Test
    fun `GIVEN plastic card awaiting activation WHEN parse THEN virtual card stays active`() {
        // Act
        val result = adapter.fromJson(PLASTIC_AWAITING_ACTIVATION_JSON)!!

        // Assert
        val virtualCard = result.cards.single { it.cardType == "VIRTUAL" }
        assertThat(virtualCard.cardStatus).isEqualTo("ACTIVE")

        val virtualInstance = result.productInstances.single { it.cardId == virtualCard.id }
        assertThat(virtualInstance.status).isEqualTo(CustomerMeResponse.ProductInstance.Status.ACTIVE)
    }

    @Test
    fun `GIVEN plastic card awaiting activation WHEN parse THEN order data fields come from the profile`() {
        // Act
        val result = adapter.fromJson(PLASTIC_AWAITING_ACTIVATION_JSON)!!

        // Assert
        assertThat(result.profile).isEqualTo(
            CustomerMeResponse.Profile(
                country = "US",
                phoneMask = "+1 ###-###-####",
                email = "j.silverhand@gmail.com",
                embossName = "JOHNNY SILVERHAND",
            ),
        )
    }

    @Test
    fun `GIVEN decimal amounts sent as JSON numbers WHEN parse THEN amounts are read`() {
        // Act
        val result = adapter.fromJson(PLASTIC_AWAITING_ACTIVATION_JSON)!!

        // Assert
        assertThat(result.balance?.fiat?.availableBalance).isEqualTo(BigDecimal("103.77"))
        assertThat(result.productInstances.first().actualCardLimit?.amount).isEqualTo(BigDecimal("50000"))
    }

    private companion object {
        val PLASTIC_AWAITING_ACTIVATION_JSON = """
            {
              "id": "11111111-1111-4111-8111-111111111111",
              "state": "ACTIVE",
              "created_at": "2026-01-15T10:00:00Z",
              "deposit_address": "0x110300B1b5D42FB06f5F552dA63BEef7B1a295A8",
              "payment_account": {
                "id": "93dea48e-33fa-457f-8709-e2fe754ccc58",
                "address": "0x110300b1b5d42fb06f5f552da63beef7b1a295a8",
                "customer_wallet_address": "0xc8674cedc86fead91dfead6a490c586d18d0efda"
              },
              "kyc": {
                "id": "c668248e-2b09-425d-b453-e812bc5c0e4b",
                "provider": "SUMSUB",
                "status": "APPROVED",
                "risk": "LOW",
                "review_answer": "GREEN",
                "created_at": "2026-01-15T10:00:00Z"
              },
              "balance": {
                "fiat": {
                  "currency": "USD",
                  "available_balance": 103.77,
                  "credit_limit": 280.00,
                  "pending_charges": 11.23,
                  "posted_charges": 165.00,
                  "balance_due": 0.00
                },
                "crypto": {
                  "id": "39538052-202b-4f5d-a7a6-e39a786927b2",
                  "chain_id": 84532,
                  "deposit_address": "0x110300B1b5D42FB06f5F552dA63BEef7B1a295A8",
                  "token_contract_address": "0x10b5Be494C2962A7B318aFB63f0Ee30b959D000b",
                  "balance": 280.0
                },
                "available_for_withdrawal": { "currency": "USD", "amount": 103.77 }
              },
              "product_instances": [
                {
                  "id": "98da8654-c9a4-46c5-8d99-31fbf6a41e20",
                  "cid": null,
                  "card_id": "bfb41dd0-ef45-4c25-a52b-25d18506355d",
                  "card_wallet_address": null,
                  "status": "ACTIVE",
                  "updated_at": "2026-01-15T10:00:00Z",
                  "payment_account_id": "93dea48e-33fa-457f-8709-e2fe754ccc58",
                  "display_name": "Basic Card",
                  "actual_card_limit": { "amount": 50000, "period_type": "DAY" },
                  "admin_card_limit": { "amount": 50000, "period_type": "DAY" },
                  "product_specification_data_type": "CARD"
                },
                {
                  "id": "5a988104-b5e3-448e-b6e2-651e6f94a925",
                  "cid": null,
                  "card_id": "4a8d6f2e-2657-4980-8a63-96c963f01be2",
                  "card_wallet_address": null,
                  "status": "SENT_TO_DELIVERY",
                  "updated_at": "2026-02-01T12:00:00Z",
                  "payment_account_id": "93dea48e-33fa-457f-8709-e2fe754ccc58",
                  "display_name": null,
                  "actual_card_limit": null,
                  "admin_card_limit": null,
                  "product_specification_data_type": "CARD"
                }
              ],
              "cards": [
                {
                  "id": "bfb41dd0-ef45-4c25-a52b-25d18506355d",
                  "token": "cc91b5bb-a258-471f-b4f0-b5972c1442ac",
                  "expiration_month": "1",
                  "expiration_year": "2030",
                  "emboss_name": "JOHN APPROVED",
                  "card_type": "VIRTUAL",
                  "card_status": "ACTIVE",
                  "card_number_end": "2882",
                  "is_pin_set": false,
                  "images": []
                },
                {
                  "id": "4a8d6f2e-2657-4980-8a63-96c963f01be2",
                  "token": "01b08e35-2224-4e53-88a9-42d028bdf831",
                  "expiration_month": "11",
                  "expiration_year": "2031",
                  "emboss_name": "JOHN APPROVED",
                  "card_type": "PHYSICAL",
                  "card_status": "INACTIVE",
                  "card_number_end": "8890",
                  "is_pin_set": false,
                  "images": []
                }
              ],
              "profile": {
                "country": "US",
                "phone_mask": "+1 ###-###-####",
                "email": "j.silverhand@gmail.com",
                "emboss_name": "JOHNNY SILVERHAND"
              }
            }
        """.trimIndent()
    }
}