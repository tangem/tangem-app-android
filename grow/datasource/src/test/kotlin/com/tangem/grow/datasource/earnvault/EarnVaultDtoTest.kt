package com.tangem.grow.datasource.earnvault

import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.json.NetworkJsonFactory
import com.tangem.grow.datasource.earnvault.models.EarnVaultApyHistoryResponse
import com.tangem.grow.datasource.earnvault.models.EarnVaultApyPointDto
import com.tangem.grow.datasource.earnvault.models.EarnVaultChainFamilyDto
import com.tangem.grow.datasource.earnvault.models.EarnVaultCuratorDto
import com.tangem.grow.datasource.earnvault.models.EarnVaultErrorCodeDto
import com.tangem.grow.datasource.earnvault.models.EarnVaultErrorResponse
import com.tangem.grow.datasource.earnvault.models.EarnVaultFeeModeDto
import com.tangem.grow.datasource.earnvault.models.EarnVaultMarketDto
import com.tangem.grow.datasource.earnvault.models.EarnVaultMarketsResponse
import com.tangem.grow.datasource.earnvault.models.EarnVaultModeDto
import com.tangem.grow.datasource.earnvault.models.EarnVaultMoneyDto
import com.tangem.grow.datasource.earnvault.models.EarnVaultPositionDto
import com.tangem.grow.datasource.earnvault.models.EarnVaultPositionsResponse
import com.tangem.grow.datasource.earnvault.models.EarnVaultPrepareResponse
import com.tangem.grow.datasource.earnvault.models.EarnVaultRegisterTransactionsBody
import com.tangem.grow.datasource.earnvault.models.EarnVaultRiskDto
import com.tangem.grow.datasource.earnvault.models.EarnVaultSendResultDto
import com.tangem.grow.datasource.earnvault.models.EarnVaultSignedTransactionDto
import com.tangem.grow.datasource.earnvault.models.EarnVaultStepDto
import com.tangem.grow.datasource.earnvault.models.EarnVaultStepTypeDto
import com.tangem.grow.datasource.earnvault.models.EarnVaultTokenDto
import com.tangem.grow.datasource.earnvault.models.EarnVaultTransactionDto
import com.tangem.grow.datasource.earnvault.models.EarnVaultTransactionErrorCodeDto
import com.tangem.grow.datasource.earnvault.models.EarnVaultTransactionStatusDto
import com.tangem.grow.datasource.earnvault.models.EarnVaultTransactionTypeDto
import com.tangem.grow.datasource.earnvault.models.EarnVaultTransactionsResponse
import com.tangem.grow.datasource.earnvault.models.EarnVaultUpdateTransactionBody
import com.tangem.grow.datasource.earnvault.models.EarnVaultWithdrawPrepareBody
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/** The JSON samples are the examples of the Earn vaults API contract (2026-11-12) */
internal class EarnVaultDtoTest {

    private val json = NetworkJsonFactory.create()

    @Nested
    inner class Responses {

        @Test
        fun `GIVEN contract markets json WHEN parsed THEN every field is read`() {
            // Arrange
            val body = """{"items":[$ETH_MARKET_JSON]}"""

            // Act
            val actual = json.decodeFromString<EarnVaultMarketsResponse>(body)

            // Assert
            assertThat(actual).isEqualTo(EarnVaultMarketsResponse(items = listOf(createEthMarket())))
        }

        @Test
        fun `GIVEN market without wrapNative WHEN parsed THEN wrap fields are empty`() {
            // Arrange
            val body = marketJson(
                "wrapNative" to JsonPrimitive(false),
                "wrappedToken" to JsonNull,
                "wrapGasLimit" to null,
                "unwrapGasLimit" to null,
                "token" to json.parseToJsonElement("""{"symbol":"USDC","address":"0xa0b8","decimals":6}"""),
            )

            // Act
            val actual = json.decodeFromString<EarnVaultMarketDto>(body)

            // Assert
            assertThat(actual).isEqualTo(
                createEthMarket().copy(
                    token = EarnVaultTokenDto(symbol = "USDC", address = "0xa0b8", decimals = 6),
                    isWrapNative = false,
                    wrappedToken = null,
                    wrapGasLimit = null,
                    unwrapGasLimit = null,
                ),
            )
        }

        @Test
        fun `GIVEN values this app does not know WHEN market parsed THEN they fall back to UNKNOWN`() {
            // Arrange
            val body = marketJson(
                "risk" to JsonPrimitive("extreme"),
                "chainFamily" to JsonPrimitive("solana"),
            )

            // Act
            val actual = json.decodeFromString<EarnVaultMarketDto>(body)

            // Assert
            assertThat(actual).isEqualTo(
                createEthMarket().copy(risk = EarnVaultRiskDto.UNKNOWN, chainFamily = EarnVaultChainFamilyDto.UNKNOWN),
            )
        }

        @Test
        fun `GIVEN contract apy history json WHEN parsed THEN points keep their order`() {
            // Arrange
            val body = """
                {"points":[{"date":"2026-11-11T00:00:00.000Z","apy":0.0342},
                {"date":"2026-11-12T00:00:00.000Z","apy":0.0351}],
                "averageApy":0.03465,"asOf":"2026-11-12T01:00:00.000Z","stale":false}
            """.trimIndent()

            // Act
            val actual = json.decodeFromString<EarnVaultApyHistoryResponse>(body)

            // Assert
            assertThat(actual).isEqualTo(
                EarnVaultApyHistoryResponse(
                    points = listOf(
                        EarnVaultApyPointDto(date = "2026-11-11T00:00:00.000Z", apy = BigDecimal("0.0342")),
                        EarnVaultApyPointDto(date = "2026-11-12T00:00:00.000Z", apy = BigDecimal("0.0351")),
                    ),
                    averageApy = BigDecimal("0.03465"),
                    asOf = "2026-11-12T01:00:00.000Z",
                    isStale = false,
                ),
            )
        }

        @Test
        fun `GIVEN contract positions json WHEN parsed THEN amounts stay in smallest units`() {
            // Arrange
            val body = """
                {"items":[{"vaultId":"eth-enhanced-yield","network":"ethereum",
                "balance":{"amount":"1000000000000000000","usd":"3250.12"},
                "pendingWithdraw":{"amount":"0","usd":"0"},
                "earned":{"amount":"12000000000000000","usd":"39.00"},
                "wrappedOwed":{"amount":"0","usd":"0"},
                "unitsLockedUntil":"2026-11-12T11:00:00.000Z","asOf":"2026-11-12T10:00:00.000Z","stale":false}]}
            """.trimIndent()

            // Act
            val actual = json.decodeFromString<EarnVaultPositionsResponse>(body)

            // Assert
            val zero = EarnVaultMoneyDto(amount = "0", usd = "0")
            assertThat(actual).isEqualTo(
                EarnVaultPositionsResponse(
                    items = listOf(
                        EarnVaultPositionDto(
                            vaultId = "eth-enhanced-yield",
                            network = "ethereum",
                            balance = EarnVaultMoneyDto(amount = "1000000000000000000", usd = "3250.12"),
                            pendingWithdraw = zero,
                            earned = EarnVaultMoneyDto(amount = "12000000000000000", usd = "39.00"),
                            wrappedOwed = zero,
                            unitsLockedUntil = "2026-11-12T11:00:00.000Z",
                            asOf = "2026-11-12T10:00:00.000Z",
                            isStale = false,
                        ),
                    ),
                ),
            )
        }

        @Test
        fun `GIVEN contract withdraw prepare json WHEN parsed THEN amounts and steps are read`() {
            // Arrange
            val body = """
                {"batchId":"3f7d1d0a-7b2c-4e0a-9f0e-2d1c8b7a6e5f","mode":"instant",
                "steps":[{"index":0,"chainFamily":"evm","type":"redeem","to":"0x2222","data":"0xba087652",
                "value":"0","gasLimit":"220000","chainId":1}],
                "expectedAmount":"499000000000000000","minAmount":"495000000000000000",
                "unwrapAmount":"500000000000000000"}
            """.trimIndent()

            // Act
            val actual = json.decodeFromString<EarnVaultPrepareResponse>(body)

            // Assert
            assertThat(actual).isEqualTo(
                EarnVaultPrepareResponse(
                    batchId = "3f7d1d0a-7b2c-4e0a-9f0e-2d1c8b7a6e5f",
                    mode = EarnVaultModeDto.INSTANT,
                    steps = listOf(
                        EarnVaultStepDto(
                            index = 0,
                            chainFamily = EarnVaultChainFamilyDto.EVM,
                            type = EarnVaultStepTypeDto.REDEEM,
                            to = "0x2222",
                            data = "0xba087652",
                            value = "0",
                            gasLimit = "220000",
                            chainId = 1,
                        ),
                    ),
                    expectedAmount = "499000000000000000",
                    minAmount = "495000000000000000",
                    unwrapAmount = "500000000000000000",
                ),
            )
        }

        @Test
        fun `GIVEN queued prepare json WHEN parsed THEN withdrawal amounts are read without unwrap`() {
            // Arrange
            val body = """
                {"batchId":"b1","mode":"queued","steps":[{"index":0,"chainFamily":"evm","type":"requestRedeem",
                "to":"0x3333","data":"0x","value":"0","gasLimit":"180000","chainId":1}],
                "expectedAmount":"10","minAmount":"9"}
            """.trimIndent()

            // Act
            val actual = json.decodeFromString<EarnVaultPrepareResponse>(body)

            // Assert
            assertThat(actual).isEqualTo(
                EarnVaultPrepareResponse(
                    batchId = "b1",
                    mode = EarnVaultModeDto.QUEUED,
                    steps = listOf(
                        EarnVaultStepDto(
                            index = 0,
                            chainFamily = EarnVaultChainFamilyDto.EVM,
                            type = EarnVaultStepTypeDto.REQUEST_REDEEM,
                            to = "0x3333",
                            data = "0x",
                            value = "0",
                            gasLimit = "180000",
                            chainId = 1,
                        ),
                    ),
                    expectedAmount = "10",
                    minAmount = "9",
                    unwrapAmount = null,
                ),
            )
        }

        @Test
        fun `GIVEN contract transactions json WHEN parsed THEN statuses and cursor are read`() {
            // Arrange
            val body = """
                {"items":[{"id":"9b2e4c1a-5d3f-4a7b-8c6e-1f2d3a4b5c6d","vaultId":"eth-enhanced-yield",
                "network":"ethereum","type":"withdraw","amount":"500000000000000000",
                "receivedAmount":"499000000000000000","status":"completed","txHash":"0x1111",
                "submittedAt":"2026-11-12T10:00:00.000Z","confirmedAt":"2026-11-12T10:01:00.000Z",
                "completedAt":"2026-11-12T12:00:00.000Z","expectedBy":"2026-11-14T10:01:00.000Z","errorCode":null},
                {"id":"t2","vaultId":"eth-enhanced-yield","network":"ethereum","type":"deposit","amount":"1",
                "receivedAmount":null,"status":"failed","txHash":"0x2222","submittedAt":"2026-11-12T09:00:00.000Z",
                "confirmedAt":null,"completedAt":null,"expectedBy":null,"errorCode":"DROPPED"}],
                "nextCursor":"eyJpZCI6Ii4uLiJ9"}
            """.trimIndent()

            // Act
            val actual = json.decodeFromString<EarnVaultTransactionsResponse>(body)

            // Assert
            assertThat(actual).isEqualTo(
                EarnVaultTransactionsResponse(
                    items = listOf(
                        createTransaction(
                            id = "9b2e4c1a-5d3f-4a7b-8c6e-1f2d3a4b5c6d",
                            type = EarnVaultTransactionTypeDto.WITHDRAW,
                            amount = "500000000000000000",
                            receivedAmount = "499000000000000000",
                            status = EarnVaultTransactionStatusDto.COMPLETED,
                            txHash = "0x1111",
                            submittedAt = "2026-11-12T10:00:00.000Z",
                            confirmedAt = "2026-11-12T10:01:00.000Z",
                            completedAt = "2026-11-12T12:00:00.000Z",
                            expectedBy = "2026-11-14T10:01:00.000Z",
                        ),
                        createTransaction(
                            id = "t2",
                            type = EarnVaultTransactionTypeDto.DEPOSIT,
                            amount = "1",
                            status = EarnVaultTransactionStatusDto.FAILED,
                            txHash = "0x2222",
                            submittedAt = "2026-11-12T09:00:00.000Z",
                            errorCode = EarnVaultTransactionErrorCodeDto.DROPPED,
                        ),
                    ),
                    nextCursor = "eyJpZCI6Ii4uLiJ9",
                ),
            )
        }

        @Test
        fun `GIVEN status and type this app does not know WHEN transaction parsed THEN they fall back to UNKNOWN`() {
            // Arrange
            val body = """
                {"id":"t3","vaultId":"eth-enhanced-yield","network":"ethereum","type":"bridge","amount":"1",
                "receivedAmount":null,"status":"bridging","txHash":null,"submittedAt":"2026-11-12T09:00:00.000Z",
                "confirmedAt":null,"completedAt":null,"expectedBy":null,"errorCode":null}
            """.trimIndent()

            // Act
            val actual = json.decodeFromString<EarnVaultTransactionDto>(body)

            // Assert
            assertThat(actual).isEqualTo(
                createTransaction(
                    id = "t3",
                    type = EarnVaultTransactionTypeDto.UNKNOWN,
                    amount = "1",
                    status = EarnVaultTransactionStatusDto.UNKNOWN,
                    txHash = null,
                    submittedAt = "2026-11-12T09:00:00.000Z",
                ),
            )
        }
    }

    @Nested
    inner class Errors {

        @Test
        fun `GIVEN units locked error WHEN parsed THEN code and lock time are read`() {
            // Arrange
            val body = """{"code":"UNITS_LOCKED","message":"m","details":{"lockedUntil":"2026-11-12T11:00:00.000Z"}}"""

            // Act
            val actual = json.decodeFromString<EarnVaultErrorResponse>(body)

            // Assert
            assertThat(actual).isEqualTo(
                EarnVaultErrorResponse(
                    code = EarnVaultErrorCodeDto.UNITS_LOCKED,
                    message = "m",
                    details = EarnVaultErrorResponse.Details(lockedUntil = "2026-11-12T11:00:00.000Z"),
                ),
            )
        }

        @Test
        fun `GIVEN validation failure without a code WHEN parsed THEN code is UNKNOWN`() {
            // Arrange
            val body = """{"statusCode":400,"message":"Validation failed","errors":["amount must be a string"]}"""

            // Act
            val actual = json.decodeFromString<EarnVaultErrorResponse>(body)

            // Assert
            assertThat(actual).isEqualTo(
                EarnVaultErrorResponse(code = EarnVaultErrorCodeDto.UNKNOWN, message = "Validation failed"),
            )
        }

        @Test
        fun `GIVEN code this app does not know WHEN parsed THEN code is UNKNOWN`() {
            // Arrange
            val body = """{"code":"A_CODE_ADDED_LATER","message":"m","details":{}}"""

            // Act
            val actual = json.decodeFromString<EarnVaultErrorResponse>(body)

            // Assert
            assertThat(actual).isEqualTo(
                EarnVaultErrorResponse(
                    code = EarnVaultErrorCodeDto.UNKNOWN,
                    message = "m",
                    details = EarnVaultErrorResponse.Details(),
                ),
            )
        }
    }

    @Nested
    inner class Requests {

        @Test
        fun `GIVEN partial withdrawal WHEN serialized THEN entire is left out`() {
            // Arrange
            val body = EarnVaultWithdrawPrepareBody.partial(
                vaultId = "eth-enhanced-yield",
                network = "ethereum",
                address = "0xabc",
                amount = "500",
            )

            // Act
            val actual = json.encodeToString(body)

            // Assert
            assertThat(actual).isEqualTo(
                """{"vaultId":"eth-enhanced-yield","network":"ethereum","address":"0xabc","amount":"500"}""",
            )
        }

        @Test
        fun `GIVEN entire withdrawal WHEN serialized THEN amount is left out`() {
            // Arrange
            val body = EarnVaultWithdrawPrepareBody.entire(
                vaultId = "eth-enhanced-yield",
                network = "ethereum",
                address = "0xabc",
            )

            // Act
            val actual = json.encodeToString(body)

            // Assert
            assertThat(actual).isEqualTo(
                """{"vaultId":"eth-enhanced-yield","network":"ethereum","address":"0xabc","entire":true}""",
            )
        }

        @Test
        fun `GIVEN signed eth batch WHEN serialized THEN it matches the contract shape`() {
            // Arrange
            val body = EarnVaultRegisterTransactionsBody(
                vaultId = "eth-enhanced-yield",
                network = "ethereum",
                address = "0xabc",
                batchId = "b1",
                feeMode = EarnVaultFeeModeDto.ETH,
                transactions = listOf(
                    EarnVaultSignedTransactionDto(
                        stepType = EarnVaultStepTypeDto.WRAP,
                        txHash = "0x01",
                        rawTransaction = "0x02f8",
                        nonce = 12,
                    ),
                    EarnVaultSignedTransactionDto(
                        stepType = EarnVaultStepTypeDto.REQUEST_REDEEM,
                        txHash = "0x02",
                        rawTransaction = "0x02f9",
                        nonce = 13,
                        replacesTxHash = "0x00",
                    ),
                ),
            )

            // Act
            val actual = json.encodeToString(body)

            // Assert
            assertThat(actual).isEqualTo(
                """{"vaultId":"eth-enhanced-yield","network":"ethereum","address":"0xabc","batchId":"b1",""" +
                    """"feeMode":"eth","transactions":[""" +
                    """{"stepType":"wrap","txHash":"0x01","rawTransaction":"0x02f8","nonce":12},""" +
                    """{"stepType":"requestRedeem","txHash":"0x02","rawTransaction":"0x02f9","nonce":13,""" +
                    """"replacesTxHash":"0x00"}]}""",
            )
        }

        @Test
        fun `GIVEN failed send WHEN serialized THEN the error text is sent`() {
            // Arrange
            val body = EarnVaultUpdateTransactionBody(
                network = "ethereum",
                sendResult = EarnVaultSendResultDto.FAILED,
                error = "nonce too low",
            )

            // Act
            val actual = json.encodeToString(body)

            // Assert
            assertThat(actual).isEqualTo("""{"network":"ethereum","sendResult":"failed","error":"nonce too low"}""")
        }
    }

    /** The contract's ETH market with [overrides] put over its fields; a `null` value removes the field */
    private fun marketJson(vararg overrides: Pair<String, JsonElement?>): String {
        val fields = json.parseToJsonElement(ETH_MARKET_JSON).jsonObject.toMutableMap()
        overrides.forEach { (key, value) -> if (value == null) fields.remove(key) else fields[key] = value }
        return JsonObject(fields).toString()
    }

    private fun createEthMarket() = EarnVaultMarketDto(
        vaultId = "eth-enhanced-yield",
        name = "ETH Enhanced Yield",
        risk = EarnVaultRiskDto.MEDIUM,
        network = "ethereum",
        chainFamily = EarnVaultChainFamilyDto.EVM,
        chainId = 1,
        token = EarnVaultTokenDto(symbol = "ETH", address = null, decimals = 18),
        vaultAddress = "0x1111111111111111111111111111111111111111",
        isWrapNative = true,
        wrappedToken = "0xc02aaa39b223fe8d0a0e5c4f27ead9083c756cc2",
        wrapGasLimit = "60000",
        unwrapGasLimit = "60000",
        isGaslessDeposits = false,
        isApprovalResetRequired = false,
        currentApy = BigDecimal("0.0509"),
        projectionApy = BigDecimal("0.0509"),
        performanceFee = BigDecimal("0.15"),
        managementFee = BigDecimal("0"),
        curator = EarnVaultCuratorDto(name = "Gauntlet", url = "https://www.gauntlet.xyz/"),
        description = "A Gauntlet-curated ETH vault on Ethereum",
        auditReportUrl = "https://example.com/audit.pdf",
        withdrawalHours = 48,
        unitLockSeconds = 3600,
        isDepositsEnabled = true,
        isWithdrawalsEnabled = true,
        asOf = "2026-11-12T10:00:00.000Z",
        isStale = false,
    )

    @Suppress("LongParameterList")
    private fun createTransaction(
        id: String,
        type: EarnVaultTransactionTypeDto,
        amount: String,
        status: EarnVaultTransactionStatusDto,
        txHash: String?,
        submittedAt: String,
        receivedAmount: String? = null,
        confirmedAt: String? = null,
        completedAt: String? = null,
        expectedBy: String? = null,
        errorCode: EarnVaultTransactionErrorCodeDto? = null,
    ) = EarnVaultTransactionDto(
        id = id,
        vaultId = "eth-enhanced-yield",
        network = "ethereum",
        type = type,
        amount = amount,
        receivedAmount = receivedAmount,
        status = status,
        txHash = txHash,
        submittedAt = submittedAt,
        confirmedAt = confirmedAt,
        completedAt = completedAt,
        expectedBy = expectedBy,
        errorCode = errorCode,
    )

    private companion object {
        const val ETH_MARKET_JSON = """
            {"vaultId":"eth-enhanced-yield","name":"ETH Enhanced Yield","risk":"medium","network":"ethereum",
            "chainFamily":"evm","chainId":1,"token":{"symbol":"ETH","address":null,"decimals":18},
            "vaultAddress":"0x1111111111111111111111111111111111111111","wrapNative":true,
            "wrappedToken":"0xc02aaa39b223fe8d0a0e5c4f27ead9083c756cc2","wrapGasLimit":"60000",
            "unwrapGasLimit":"60000","gaslessDeposits":false,"approvalResetRequired":false,
            "currentApy":0.0509,"projectionApy":0.0509,"performanceFee":0.15,"managementFee":0,
            "curator":{"name":"Gauntlet","url":"https://www.gauntlet.xyz/"},
            "description":"A Gauntlet-curated ETH vault on Ethereum",
            "auditReportUrl":"https://example.com/audit.pdf","withdrawalHours":48,"unitLockSeconds":3600,
            "depositsEnabled":true,"withdrawalsEnabled":true,"asOf":"2026-11-12T10:00:00.000Z","stale":false}
        """
    }
}