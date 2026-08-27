package com.tangem.data.blockaid

import com.domain.blockaid.models.dapp.CheckDAppResult
import com.domain.blockaid.models.transaction.SimulationResult
import com.domain.blockaid.models.transaction.TransactionData
import com.domain.blockaid.models.transaction.TransactionParams
import com.domain.blockaid.models.transaction.ValidationResult
import com.domain.blockaid.models.transaction.simultation.AmountInfo
import com.domain.blockaid.models.transaction.simultation.ApproveInfo
import com.domain.blockaid.models.transaction.simultation.SimulationData
import com.google.common.truth.Truth
import com.tangem.datasource.api.common.blockaid.models.response.*
import org.junit.jupiter.api.Test
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
            assetType = "native",
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
        Truth.assertThat(approve?.items?.size).isEqualTo(1)
        Truth.assertThat((approve?.items?.first() as? ApproveInfo.Amount)?.approvedAmount)
            .isEqualTo(BigDecimal("1000.0"))
        Truth.assertThat((approve?.items?.first() as? ApproveInfo.Amount)?.isUnlimited).isTrue()
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
        Truth.assertThat(result.simulation is SimulationResult.Success).isTrue()
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
        Truth.assertThat(result.simulation is SimulationResult.Success).isTrue()
    }

    @Test
    fun `GIVEN eth_signTypedData_v4 Permit WHEN mapToEvmRequest THEN params preserved and spender exposed`() {
        // Arrange — signTypedData params are [address, typedData], not an array of tx objects.
        val attacker = "0x00000000000000000000000000000000DeaDBeef"
        val address = "0xC3E41b10Adb2b96421f103520c8C866618D9B030"
        val rawParams = """
            ["$address",{"types":{"Permit":[{"name":"owner","type":"address"}]},"primaryType":"Permit",
            "domain":{"name":"USD Coin","chainId":1},
            "message":{"owner":"$address","spender":"$attacker","value":"123","nonce":"0","deadline":"1999999999"}}]
        """.trimIndent()
        val data = TransactionData(
            chain = "ethereum",
            accountAddress = address,
            method = "eth_signTypedData_v4",
            domainUrl = "https://example.org",
            params = TransactionParams.Evm(rawParams),
        )

        // Act — previously this threw (getJSONObject on the address string) -> FAILED_TO_VALIDATE.
        val request = mapper.mapToEvmRequest(data)

        // Assert — the array shape is preserved and the dangerous spender survives the mapping.
        Truth.assertThat(request.data.method).isEqualTo("eth_signTypedData_v4")
        Truth.assertThat(request.data.params).hasSize(2)
        Truth.assertThat(request.data.params[0]).isEqualTo(address)
        @Suppress("UNCHECKED_CAST")
        val typedData = request.data.params[1] as Map<String, Any>
        val message = typedData["message"] as Map<String, Any>
        Truth.assertThat(message["spender"]).isEqualTo(attacker)
        Truth.assertThat(message["value"]).isEqualTo("123")
        // JSON primitive types are preserved (numeric chainId stays a number, not "1").
        @Suppress("UNCHECKED_CAST")
        val domain = typedData["domain"] as Map<String, Any>
        Truth.assertThat(domain["chainId"]).isEqualTo(1)
    }

    @Test
    fun `GIVEN eth_sendTransaction WHEN mapToEvmRequest THEN tx object params still mapped`() {
        // Arrange — the existing send path: params is [ {tx} ].
        val rawParams = """[{"from":"0xFROM","to":"0xTO","data":"0xdeadbeef","value":"0x0"}]"""
        val data = TransactionData(
            chain = "ethereum",
            accountAddress = "0xFROM",
            method = "eth_sendTransaction",
            domainUrl = "https://example.org",
            params = TransactionParams.Evm(rawParams),
        )

        // Act
        val request = mapper.mapToEvmRequest(data)

        // Assert — unchanged behaviour: one tx object with its string fields.
        Truth.assertThat(request.data.params).hasSize(1)
        @Suppress("UNCHECKED_CAST")
        val tx = request.data.params[0] as Map<String, Any>
        Truth.assertThat(tx["to"]).isEqualTo("0xTO")
        Truth.assertThat(tx["data"]).isEqualTo("0xdeadbeef")
    }
}