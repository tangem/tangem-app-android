package com.tangem.data.blockaid

import com.domain.blockaid.models.dapp.CheckDAppResult
import com.domain.blockaid.models.transaction.SimulationResult
import com.domain.blockaid.models.transaction.ValidationResult
import com.domain.blockaid.models.transaction.simultation.SimulationData
import com.tangem.datasource.api.common.blockaid.models.response.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class BlockAidMapperTest {

    private val mapper = BlockAidMapper()

    @Test
    fun whenStatusHitAndIsMaliciousFalseThenMapToDomainReturnsSafe() {
        val response = DomainScanResponse(status = "hit", isMalicious = false)
        val result = mapper.mapToDomain(response)
        assertEquals(CheckDAppResult.SAFE, result)
    }

    @Test
    fun whenStatusHitAndIsMaliciousTrueThenMapToDomainReturnsUnsafe() {
        val response = DomainScanResponse(status = "hit", isMalicious = true)
        val result = mapper.mapToDomain(response)
        assertEquals(CheckDAppResult.UNSAFE, result)
    }

    @Test
    fun whenStatusNotHitThenMapToDomainReturnsFailedToVerify() {
        val response = DomainScanResponse(status = "miss", isMalicious = false)
        val result = mapper.mapToDomain(response)
        assertEquals(CheckDAppResult.FAILED_TO_VERIFY, result)
    }

    @Test
    fun whenResponseBenignValidationThenReturnsSafeValidation() {
        val spenderDetails = SpenderDetails(
            isApprovedForAll = true,
            exposure = listOf(ExposureDetail(value = "1000.0", rawValue = "0x123")),
        )
        val exposure = Exposure(
            asset = Asset(chainId = 1, logoUrl = "logo", symbol = "PEPE"),
            spenders = mapOf("spender" to spenderDetails),
        )
        val response = TransactionScanResponse(
            validation = ValidationResponse(status = "Success", resultType = "Benign"),
            simulation = SimulationResponse(
                status = "Success",
                accountSummary = AccountSummaryResponse(assetsDiffs = emptyList(), exposures = listOf(exposure)),
            ),
        )

        val result = mapper.mapToDomain(response)
        assertEquals(ValidationResult.SAFE, result.validation)

        val simulation = result.simulation as? SimulationResult.Success
        assertNotNull(simulation)

        val approve = simulation?.data as? SimulationData.Approve
        assertNotNull(approve)
        assertEquals(1, approve?.approvedAmounts?.size)
        assertEquals(BigDecimal("1000.0"), approve?.approvedAmounts?.first()?.approvedAmount)
        assertTrue(approve?.approvedAmounts?.first()?.isUnlimited == true)
    }

    @Test
    fun whenResponseBenignValidationAndSuccessSimulationThenReturnsSendReceiveResult() {
        val assetDiff = AssetDiff(
            assetType = "ERC20",
            asset = Asset(chainId = 1, logoUrl = "logo", symbol = "ETH"),
            inTransfer = listOf(Transfer(value = "2.0", rawValue = "0x1")),
            outTransfer = listOf(Transfer(value = "1.5", rawValue = "0x2")),
        )
        val response = TransactionScanResponse(
            validation = ValidationResponse(status = "Success", resultType = "Benign"),
            simulation = SimulationResponse(
                status = "Success",
                accountSummary = AccountSummaryResponse(exposures = emptyList(), assetsDiffs = listOf(assetDiff)),
            ),
        )

        val result = mapper.mapToDomain(response)
        assertEquals(ValidationResult.SAFE, result.validation)

        val simulation = result.simulation as? SimulationResult.Success
        assertNotNull(simulation)

        val data = simulation?.data as? SimulationData.SendAndReceive
        assertNotNull(data)
        assertEquals(BigDecimal("1.5"), data?.send?.first()?.amount)
        assertEquals(BigDecimal("2.0"), data?.receive?.first()?.amount)
    }

    @Test
    fun whenResponseErrorValidationThenReturnsFailedToValidate() {
        val response = TransactionScanResponse(
            validation = ValidationResponse(status = "Error", resultType = "Benign"),
            simulation = SimulationResponse(
                status = "Success",
                accountSummary = AccountSummaryResponse(emptyList(), emptyList()),
            ),
        )

        val result = mapper.mapToDomain(response)
        assertEquals(ValidationResult.FAILED_TO_VALIDATE, result.validation)
        assertTrue(result.simulation is SimulationResult.FailedToSimulate)
    }

    @Test
    fun whenResponseNotBenignThenReturnsValidationUnsafe() {
        val response = TransactionScanResponse(
            validation = ValidationResponse(status = "Success", resultType = "Phishing"),
            simulation = SimulationResponse(
                status = "Success",
                accountSummary = AccountSummaryResponse(emptyList(), emptyList()),
            ),
        )

        val result = mapper.mapToDomain(response)
        assertEquals(ValidationResult.UNSAFE, result.validation)
    }

    @Test
    fun whenResponseSimulationNotSuccessThenReturnsSimulationFailedToSimulate() {
        val response = TransactionScanResponse(
            validation = ValidationResponse(status = "Success", resultType = "Benign"),
            simulation = SimulationResponse(
                status = "Error",
                accountSummary = AccountSummaryResponse(emptyList(), emptyList()),
            ),
        )

        val result = mapper.mapToDomain(response)
        assertTrue(result.simulation is SimulationResult.FailedToSimulate)
    }

    @Test
    fun whenResponseSimulationIsEmptyThenReturnsFailedToSimulate() {
        val txResponse = TransactionScanResponse(
            validation = ValidationResponse(status = "Success", resultType = "Benign"),
            simulation = SimulationResponse(
                status = "Success",
                accountSummary = AccountSummaryResponse(
                    assetsDiffs = emptyList(),
                    exposures = emptyList(),
                ),
            ),
        )

        val result = mapper.mapToDomain(txResponse)
        assertTrue(result.simulation is SimulationResult.FailedToSimulate)
    }
}