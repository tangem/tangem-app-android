package com.tangem.data.yield.supply.promo.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.datasource.api.promotion.models.YieldBoostStatusResponse
import com.tangem.domain.yield.supply.models.YieldBoostStatus
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Test

class YieldBoostStatusConverterTest {

    private val qualificationEnd = "2026-06-01T00:00:00Z"

    @Test
    fun `GIVEN promoEnrollmentStatus notStarted WHEN convert THEN returns NotStarted`() {
        val dto = dto(promoEnrollmentStatus = "notStarted")

        val result = YieldBoostStatusConverter.convert(dto)

        assertThat(result).isEqualTo(YieldBoostStatus.NotStarted)
    }

    @Test
    fun `GIVEN active backend status with valid date WHEN convert THEN returns Enrolled`() {
        val dto = dto(
            promoEnrollmentStatus = "active",
            tokenName = "USD Coin",
            networkId = "ethereum",
            moduleAddress = "0xmodule",
            userAddress = "0xuser",
            contractAddress = "0xcontract",
            qualificationEndDate = qualificationEnd,
        )

        val result = YieldBoostStatusConverter.convert(dto)

        assertThat(result).isInstanceOf(YieldBoostStatus.Enrolled::class.java)
        val enrolled = result as YieldBoostStatus.Enrolled
        assertThat(enrolled.tokenName).isEqualTo("USD Coin")
        assertThat(enrolled.networkId).isEqualTo("ethereum")
        assertThat(enrolled.contractAddress).isEqualTo("0xcontract")
        assertThat(enrolled.qualificationEndDate).isEqualTo(Instant.parse(qualificationEnd))
    }

    @Test
    fun `GIVEN active status missing qualificationEndDate WHEN convert THEN returns Enrolled with null date`() {
        val dto = dto(
            promoEnrollmentStatus = "active",
            contractAddress = "0xcontract",
            qualificationEndDate = null,
        )

        val result = YieldBoostStatusConverter.convert(dto)

        assertThat(result).isInstanceOf(YieldBoostStatus.Enrolled::class.java)
        assertThat((result as YieldBoostStatus.Enrolled).qualificationEndDate).isNull()
    }

    @Test
    fun `GIVEN active status with malformed qualificationEndDate WHEN convert THEN returns Enrolled with null date`() {
        val dto = dto(
            promoEnrollmentStatus = "active",
            contractAddress = "0xcontract",
            qualificationEndDate = "not-an-iso",
        )

        val result = YieldBoostStatusConverter.convert(dto)

        assertThat(result).isInstanceOf(YieldBoostStatus.Enrolled::class.java)
        assertThat((result as YieldBoostStatus.Enrolled).qualificationEndDate).isNull()
    }

    @Test
    fun `GIVEN completed status with valid date WHEN convert THEN returns Enrolled`() {
        val dto = dto(
            promoEnrollmentStatus = "completed",
            tokenName = "USDT",
            networkId = "ethereum",
            moduleAddress = "0xmodule",
            userAddress = "0xuser",
            contractAddress = "0xcontract",
            qualificationEndDate = "2026-05-01T00:00:00Z",
        )

        val result = YieldBoostStatusConverter.convert(dto)

        assertThat(result).isInstanceOf(YieldBoostStatus.Enrolled::class.java)
        assertThat((result as YieldBoostStatus.Enrolled).qualificationEndDate)
            .isEqualTo(Instant.parse("2026-05-01T00:00:00Z"))
    }

    @Test
    fun `GIVEN disqualified frod reason WHEN convert THEN returns Disqualified with FROD reason`() {
        val dto = dto(
            promoEnrollmentStatus = "disqualified",
            disqualificationReason = "frod",
        )

        val result = YieldBoostStatusConverter.convert(dto)

        assertThat(result).isEqualTo(YieldBoostStatus.Disqualified(YieldBoostStatus.Disqualified.Reason.FROD))
    }

    @Test
    fun `GIVEN disqualified less1usd reason WHEN convert THEN returns Disqualified with LESS_THAN_1_USD reason`() {
        val dto = dto(
            promoEnrollmentStatus = "disqualified",
            disqualificationReason = "less1usd",
        )

        val result = YieldBoostStatusConverter.convert(dto)

        assertThat(result).isEqualTo(
            YieldBoostStatus.Disqualified(YieldBoostStatus.Disqualified.Reason.LESS_THAN_1_USD),
        )
    }

    @Test
    fun `GIVEN disqualified closed reason WHEN convert THEN returns Disqualified with CLOSED reason`() {
        val dto = dto(
            promoEnrollmentStatus = "disqualified",
            disqualificationReason = "closed",
        )

        val result = YieldBoostStatusConverter.convert(dto)

        assertThat(result).isEqualTo(YieldBoostStatus.Disqualified(YieldBoostStatus.Disqualified.Reason.CLOSED))
    }

    @Test
    fun `GIVEN disqualified unknown reason WHEN convert THEN returns Disqualified with UNKNOWN reason`() {
        val dto = dto(
            promoEnrollmentStatus = "disqualified",
            disqualificationReason = "alien_invasion",
        )

        val result = YieldBoostStatusConverter.convert(dto)

        assertThat(result).isEqualTo(YieldBoostStatus.Disqualified(YieldBoostStatus.Disqualified.Reason.UNKNOWN))
    }

    @Test
    fun `GIVEN unknown promoEnrollmentStatus WHEN convert THEN returns NotStarted`() {
        val dto = dto(promoEnrollmentStatus = "futureBackendStatus")

        val result = YieldBoostStatusConverter.convert(dto)

        assertThat(result).isEqualTo(YieldBoostStatus.NotStarted)
    }

    @Test
    fun `GIVEN status with uppercase casing WHEN convert THEN normalizes correctly`() {
        val dto = dto(
            promoEnrollmentStatus = "ACTIVE",
            tokenName = "USDT",
            networkId = "ethereum",
            moduleAddress = "0xmodule",
            userAddress = "0xuser",
            contractAddress = "0xcontract",
            qualificationEndDate = qualificationEnd,
        )

        val result = YieldBoostStatusConverter.convert(dto)

        assertThat(result).isInstanceOf(YieldBoostStatus.Enrolled::class.java)
    }

    private fun dto(
        promoEnrollmentStatus: String,
        tokenName: String? = null,
        networkId: String? = null,
        moduleAddress: String? = null,
        userAddress: String? = null,
        contractAddress: String? = null,
        qualificationEndDate: String? = null,
        disqualificationReason: String? = null,
    ) = YieldBoostStatusResponse(
        tokenName = tokenName,
        networkId = networkId,
        moduleAddress = moduleAddress,
        userAddress = userAddress,
        contractAddress = contractAddress,
        promoEnrollmentStatus = promoEnrollmentStatus,
        qualificationEndDate = qualificationEndDate,
        disqualificationReason = disqualificationReason,
    )
}