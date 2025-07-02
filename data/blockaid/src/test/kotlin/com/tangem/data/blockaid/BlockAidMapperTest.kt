package com.tangem.data.blockaid

import com.domain.blockaid.models.dapp.CheckDAppResult
import com.domain.blockaid.models.transaction.SimulationResult
import com.domain.blockaid.models.transaction.ValidationResult
import com.domain.blockaid.models.transaction.simultation.AmountInfo
import com.domain.blockaid.models.transaction.simultation.SimulationData
import com.google.common.truth.Truth
import com.tangem.datasource.api.common.blockaid.models.response.*
import org.junit.Test
import java.math.BigDecimal

class BlockAidMapperTest {

    private val mapper = BlockAidMapper

    @Test
    fun `when status hit and is malicious false then map to domain returns safe`() {
        val response = DomainScanResponse(status = "hit", isMalicious = false)
        val result = mapper.mapToDomain(response)
        Truth.assertThat(result).isEqualTo(CheckDAppResult.SAFE)
    }

    @Test
    fun `when status hit and is malicious true then map to domain returns unsafe`() {
        val response = DomainScanResponse(status = "hit", isMalicious = true)
        val result = mapper.mapToDomain(response)
        Truth.assertThat(result).isEqualTo(CheckDAppResult.UNSAFE)
    }

    @Test
    fun `when status not hit then map to domain returns failed to verify`() {
        val response = DomainScanResponse(status = "miss", isMalicious = false)
        val result = mapper.mapToDomain(response)
        Truth.assertThat(result).isEqualTo(CheckDAppResult.FAILED_TO_VERIFY)
    }

    @Test
    fun `when response benign validation then returns safe validation`() {
        val spenderDetails = SpenderDetails(
            isApprovedForAll = true,
            exposure = listOf(ExposureDetail(value = "1000.0", rawValue = "0x123")),
        )
        val exposure = Exposure(
            asset = Asset(chainId = 1, logoUrl = "logo", symbol = "PEPE", decimals = 8),
            spenders = mapOf("spender" to spenderDetails),
        )
        val response = TransactionScanResponse(
            validation = ValidationResponse(status = "Success", resultType = "Benign", description = ""),
            simulation = SimulationResponse(
                status = "Success",
                accountSummary = AccountSummaryResponse(
                    assetsDiffs = emptyList(),
                    exposures = listOf(exposure),
                    traces = null,
                ),
            ),
        )

        val result = mapper.mapToDomain(response)
        Truth.assertThat(result.validation).isEqualTo(ValidationResult.SAFE)

        val simulation = result.simulation as? SimulationResult.Success
        Truth.assertThat(simulation).isNotNull()

        val approve = simulation?.data as? SimulationData.Approve
        Truth.assertThat(approve).isNotNull()
        Truth.assertThat(approve?.approvedAmounts?.size).isEqualTo(1)
        Truth.assertThat(approve?.approvedAmounts?.first()?.approvedAmount).isEqualTo(BigDecimal("1000.0"))
        Truth.assertThat(approve?.approvedAmounts?.first()?.isUnlimited).isTrue()
    }

    @Test
    fun `when response benign validation and success simulation then returns send receive result`() {
        val assetDiff = AssetDiff(
            assetType = "ERC20",
            asset = Asset(chainId = 1, logoUrl = "logo", symbol = "ETH", decimals = 8),
            inTransfer = listOf(Transfer(value = "2.0", rawValue = "0x1")),
            outTransfer = listOf(Transfer(value = "1.5", rawValue = "0x2")),
        )
        val response = TransactionScanResponse(
            validation = ValidationResponse(status = "Success", resultType = "Benign", description = ""),
            simulation = SimulationResponse(
                status = "Success",
                accountSummary = AccountSummaryResponse(
                    exposures = emptyList(),
                    assetsDiffs = listOf(assetDiff),
                    traces = null,
                ),
            ),
        )

        val result = mapper.mapToDomain(response)
        Truth.assertThat(result.validation).isEqualTo(ValidationResult.SAFE)

        val simulation = result.simulation as? SimulationResult.Success
        Truth.assertThat(simulation).isNotNull()

        val data = simulation?.data as? SimulationData.SendAndReceive
        Truth.assertThat(data).isNotNull()
        Truth.assertThat((data?.send?.first() as? AmountInfo.FungibleTokens)?.amount).isEqualTo(BigDecimal("1.5"))
        Truth.assertThat((data?.receive?.first() as? AmountInfo.FungibleTokens)?.amount).isEqualTo(BigDecimal("2.0"))
    }

    @Test
    fun `when response error validation rhen returns failed to validate`() {
        val response = TransactionScanResponse(
            validation = ValidationResponse(status = "Error", resultType = "Benign", description = ""),
            simulation = SimulationResponse(
                status = "Success",
                accountSummary = AccountSummaryResponse(emptyList(), emptyList(), null),
            ),
        )

        val result = mapper.mapToDomain(response)
        Truth.assertThat(result.validation).isEqualTo(ValidationResult.FAILED_TO_VALIDATE)
        Truth.assertThat(result.simulation is SimulationResult.FailedToSimulate).isTrue()
    }

    @Test
    fun `when response not benign then returns validation unsafe`() {
        val response = TransactionScanResponse(
            validation = ValidationResponse(status = "Success", resultType = "Phishing", description = ""),
            simulation = SimulationResponse(
                status = "Success",
                accountSummary = AccountSummaryResponse(emptyList(), emptyList(), null),
            ),
        )

        val result = mapper.mapToDomain(response)
        Truth.assertThat(result.validation).isEqualTo(ValidationResult.UNSAFE)
    }

    @Test
    fun `when response simulation not success then returns simulation failed ro simulate`() {
        val response = TransactionScanResponse(
            validation = ValidationResponse(status = "Success", resultType = "Benign", description = ""),
            simulation = SimulationResponse(
                status = "Error",
                accountSummary = AccountSummaryResponse(emptyList(), emptyList(), null),
            ),
        )

        val result = mapper.mapToDomain(response)
        Truth.assertThat(result.simulation is SimulationResult.FailedToSimulate).isTrue()
    }

    @Test
    fun `when response simulation is empty then returns failed to simulate`() {
        val txResponse = TransactionScanResponse(
            validation = ValidationResponse(status = "Success", resultType = "Benign", description = ""),
            simulation = SimulationResponse(
                status = "Success",
                accountSummary = AccountSummaryResponse(
                    assetsDiffs = emptyList(),
                    exposures = emptyList(),
                    traces = null,
                ),
            ),
        )

        val result = mapper.mapToDomain(txResponse)
        Truth.assertThat(result.simulation is SimulationResult.FailedToSimulate).isTrue()
    }
}