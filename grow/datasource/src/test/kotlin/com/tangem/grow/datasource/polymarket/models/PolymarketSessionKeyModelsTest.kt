package com.tangem.grow.datasource.polymarket.models

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.tangem.core.remote.moshi.SkipMalformedElementsAdapterFactory
import org.junit.jupiter.api.Test

internal class PolymarketSessionKeyModelsTest {

    private val moshi = Moshi.Builder().add(SkipMalformedElementsAdapterFactory).build()
    private val walletAdapter = moshi.adapter(PolymarketWalletStatusResponse::class.java)
    private val problemAdapter = moshi.adapter(ProblemDetailResponse::class.java)
    private val authorizeAdapter = moshi.adapter(PolymarketAuthorizeSessionKeyRequest::class.java)

    @Test
    fun `GIVEN the openapi wallet example WHEN decoded THEN the session keys are read`() {
        // Arrange
        val json = """
            {
              "depositWalletAddress": "0xdef0000000000000000000000000000000000002",
              "status": "READY_TO_TRADE",
              "sessionKeys": [
                {
                  "address": "0x5e55000000000000000000000000000000000001",
                  "deviceId": "3f2b8c1e-7a4d-4e0f-9b6a-2c1d5e8f0a17",
                  "deviceName": "iPhone 15 Pro",
                  "enabled": true,
                  "status": "ACTIVE",
                  "scopes": ["CLOB"],
                  "validUntil": 1804982400,
                  "renewAfter": 1804723200,
                  "authorizedAt": 1789430400,
                  "createdAt": 1789430280
                },
                {
                  "address": "0x01d0000000000000000000000000000000000009",
                  "deviceId": null,
                  "deviceName": null,
                  "enabled": false,
                  "status": "REVOKING",
                  "scopes": [],
                  "validUntil": null,
                  "renewAfter": null,
                  "authorizedAt": null,
                  "createdAt": 1789430500
                }
              ]
            }
        """.trimIndent()

        // Act
        val actual = walletAdapter.fromJson(json)

        // Assert
        assertThat(actual).isEqualTo(
            PolymarketWalletStatusResponse(
                depositWalletAddress = "0xdef0000000000000000000000000000000000002",
                status = "READY_TO_TRADE",
                sessionKeys = listOf(
                    PolymarketSessionKeyDto(
                        address = "0x5e55000000000000000000000000000000000001",
                        deviceId = "3f2b8c1e-7a4d-4e0f-9b6a-2c1d5e8f0a17",
                        deviceName = "iPhone 15 Pro",
                        isEnabled = true,
                        status = "ACTIVE",
                        scopes = listOf("CLOB"),
                        validUntil = 1804982400,
                        renewAfter = 1804723200,
                        authorizedAt = 1789430400,
                        createdAt = 1789430280,
                    ),
                    PolymarketSessionKeyDto(
                        address = "0x01d0000000000000000000000000000000000009",
                        deviceId = null,
                        deviceName = null,
                        isEnabled = false,
                        status = "REVOKING",
                        scopes = emptyList(),
                        validUntil = null,
                        renewAfter = null,
                        authorizedAt = null,
                        createdAt = 1789430500,
                    ),
                ),
            ),
        )
    }

    @Test
    fun `GIVEN a backend without session keys WHEN decoded THEN the list is empty`() {
        // Act
        val actual = walletAdapter.fromJson("""{"depositWalletAddress":null,"status":"NOT_CREATED"}""")

        // Assert
        assertThat(actual).isEqualTo(
            PolymarketWalletStatusResponse(depositWalletAddress = null, status = "NOT_CREATED"),
        )
    }

    @Test
    fun `GIVEN explicit null session keys WHEN decoded THEN the wallet status is still read`() {
        // Arrange
        val json = """{"depositWalletAddress":"0xdw","status":"READY_TO_TRADE","sessionKeys":null}"""

        // Act
        val actual = walletAdapter.fromJson(json)

        // Assert
        assertThat(actual).isEqualTo(
            PolymarketWalletStatusResponse(
                depositWalletAddress = "0xdw",
                status = "READY_TO_TRADE",
                sessionKeys = null,
            ),
        )
    }

    @Test
    fun `GIVEN a malformed session key WHEN decoded THEN only that key is dropped`() {
        // Arrange
        val json = """
            {
              "depositWalletAddress": "0xdw",
              "status": "READY_TO_TRADE",
              "sessionKeys": [
                {"address": "0xbad", "enabled": true, "status": "ACTIVE", "scopes": ["CLOB"]},
                {"address": "0xgood", "enabled": true, "status": "PENDING", "scopes": ["CLOB"], "createdAt": 1}
              ]
            }
        """.trimIndent()

        // Act
        val actual = walletAdapter.fromJson(json)?.sessionKeys

        // Assert
        assertThat(actual).containsExactly(
            PolymarketSessionKeyDto(
                address = "0xgood",
                isEnabled = true,
                status = "PENDING",
                scopes = listOf("CLOB"),
                createdAt = 1,
            ),
        )
    }

    @Test
    fun `GIVEN the guide problem example WHEN decoded THEN the reason is read`() {
        // Arrange
        val json = """
            {
              "type": "about:blank",
              "title": "Conflict",
              "status": 409,
              "detail": "Device already has session key 0x01d0…0009 in status ACTIVE",
              "reason": "DEVICE_HAS_SESSION_KEY",
              "instance": "/api/v1/wallet/session-keys",
              "timestamp": "2026-10-16T12:00:00Z",
              "traceId": "6a0f3c2b9e1d4a7f"
            }
        """.trimIndent()

        // Act
        val actual = problemAdapter.fromJson(json)

        // Assert
        assertThat(actual).isEqualTo(
            ProblemDetailResponse(
                status = 409,
                title = "Conflict",
                detail = "Device already has session key 0x01d0…0009 in status ACTIVE",
                instance = "/api/v1/wallet/session-keys",
                reason = "DEVICE_HAS_SESSION_KEY",
            ),
        )
    }

    @Test
    fun `GIVEN an authorization WHEN encoded THEN the body matches the openapi field names`() {
        // Arrange
        val request = PolymarketAuthorizeSessionKeyRequest(
            ownerAddress = "0xabc0000000000000000000000000000000000001",
            depositWalletAddress = "0xdef0000000000000000000000000000000000002",
            sessionKeyAddress = "0x5e55000000000000000000000000000000000001",
            deviceId = "3f2b8c1e-7a4d-4e0f-9b6a-2c1d5e8f0a17",
            deviceName = "Pixel 9",
            validUntil = "1804982400",
            nonce = "9",
            deadline = "1789431000",
            calls = listOf(
                PolymarketApprovalCallDto(
                    target = "0xdef0000000000000000000000000000000000002",
                    value = "0",
                    data = "0x24017fae",
                ),
            ),
            signature = "0xsig",
            replacesSessionKeyAddress = "0x01d0000000000000000000000000000000000009",
        )

        // Act
        val actual = authorizeAdapter.toJson(request)

        // Assert
        assertThat(actual).isEqualTo(
            """{"ownerAddress":"0xabc0000000000000000000000000000000000001",""" +
                """"depositWalletAddress":"0xdef0000000000000000000000000000000000002",""" +
                """"sessionKeyAddress":"0x5e55000000000000000000000000000000000001",""" +
                """"deviceId":"3f2b8c1e-7a4d-4e0f-9b6a-2c1d5e8f0a17","deviceName":"Pixel 9",""" +
                """"validUntil":"1804982400","nonce":"9","deadline":"1789431000",""" +
                """"calls":[{"target":"0xdef0000000000000000000000000000000000002","value":"0","data":"0x24017fae"}],""" +
                """"signature":"0xsig","replacesSessionKeyAddress":"0x01d0000000000000000000000000000000000009"}""",
        )
    }
}