package com.tangem.datasource.api.promotion

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.tangem.datasource.api.promotion.models.PromotionRegistrationsBody
import com.tangem.datasource.api.promotion.models.PromotionRegistrationsBody.EnrollmentDto
import com.tangem.datasource.api.promotion.models.PromotionRegistrationsResponse
import com.tangem.datasource.api.promotion.models.PromotionRegistrationsResponse.PayoutDto
import com.tangem.datasource.api.promotion.models.PromotionRegistrationsResponse.ResultDto
import com.tangem.datasource.api.promotion.models.PromotionRegistrationsResponse.Status
import com.tangem.datasource.api.promotion.models.PromotionWalletType
import org.junit.jupiter.api.Test

internal class PromotionRegistrationsResponseTest {

    private val moshi = Moshi.Builder()
        .addPromotionEnumFallbackAdapters()
        .build()

    @Test
    fun `GIVEN registration results WHEN parsed THEN statuses and payouts mapped`() {
        // Arrange
        val json = """
            {"id":1,"results":[
              {"walletId":"wallet_A","status":"enrolled","payout":{"userAddress":"0xA",
              "tokenAddress":"0xdAC","networkId":"ethereum"}},
              {"walletId":"wallet_B","status":"already_enrolled","payout":{"userAddress":"0xOld",
              "tokenAddress":"0xdAC","networkId":"ethereum"}},
              {"walletId":"wallet_C","status":"ineligible","payout":null},
              {"walletId":"wallet_D","status":"waitlisted"}]}
        """.trimIndent()

        // Act
        val actual = moshi.adapter(PromotionRegistrationsResponse::class.java).fromJson(json)

        // Assert
        val expected = PromotionRegistrationsResponse(
            id = 1,
            results = listOf(
                ResultDto("wallet_A", Status.ENROLLED, PayoutDto("0xA", "0xdAC", "ethereum")),
                ResultDto("wallet_B", Status.ALREADY_ENROLLED, PayoutDto("0xOld", "0xdAC", "ethereum")),
                ResultDto("wallet_C", Status.INELIGIBLE, payout = null),
                ResultDto("wallet_D", Status.UNKNOWN, payout = null),
            ),
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `GIVEN card and mobile enrollments WHEN serialized THEN null fields omitted and wallet type lowercase`() {
        // Arrange
        val body = PromotionRegistrationsBody(
            enrollments = listOf(
                EnrollmentDto(
                    walletId = "wallet_A",
                    walletType = PromotionWalletType.CARD,
                    batchId = "AF99 0088",
                    userAddress = "0xA",
                    tokenAddress = "0xdAC",
                    networkId = "ethereum",
                    tokenId = "tether",
                    isMain = true,
                ),
                EnrollmentDto(
                    walletId = "wallet_B",
                    walletType = PromotionWalletType.MOBILE,
                    batchId = null,
                    userAddress = null,
                    tokenAddress = null,
                    networkId = null,
                    tokenId = null,
                    isMain = false,
                ),
            ),
        )

        // Act
        val actual = moshi.adapter(PromotionRegistrationsBody::class.java).toJson(body)

        // Assert
        val expected = """{"enrollments":[""" +
            """{"walletId":"wallet_A","walletType":"card","batchId":"AF99 0088","userAddress":"0xA",""" +
            """"tokenAddress":"0xdAC","networkId":"ethereum","tokenId":"tether","main":true},""" +
            """{"walletId":"wallet_B","walletType":"mobile","main":false}]}"""
        assertThat(actual).isEqualTo(expected)
    }
}