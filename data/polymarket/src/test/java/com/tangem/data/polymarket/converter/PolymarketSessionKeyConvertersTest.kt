package com.tangem.data.polymarket.converter

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketApprovalCall
import com.tangem.domain.polymarket.model.PolymarketSessionKey
import com.tangem.domain.polymarket.model.PolymarketSessionKeyAuthorization
import com.tangem.domain.polymarket.model.PolymarketSessionKeyRevocation
import com.tangem.domain.polymarket.model.PolymarketSessionKeyStatus
import com.tangem.domain.polymarket.model.PolymarketSessionSigner
import com.tangem.domain.polymarket.model.PolymarketWalletState
import com.tangem.domain.polymarket.model.PolymarketWalletStatus
import com.tangem.grow.datasource.polymarket.clob.models.PolymarketSessionSignerDto
import com.tangem.grow.datasource.polymarket.clob.models.PolymarketSessionSignersResponse
import com.tangem.grow.datasource.polymarket.models.PolymarketApprovalCallDto
import com.tangem.grow.datasource.polymarket.models.PolymarketAuthorizeSessionKeyRequest
import com.tangem.grow.datasource.polymarket.models.PolymarketRevokeSessionKeyRequest
import com.tangem.grow.datasource.polymarket.models.PolymarketSessionKeyDto
import com.tangem.grow.datasource.polymarket.models.PolymarketWalletStatusResponse
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.time.Instant

internal class PolymarketSessionKeyConvertersTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ToDomain {

        @Test
        fun `GIVEN an active key WHEN converted THEN every field is mapped with seconds as instants`() {
            // Act
            val actual = PolymarketSessionKeyConverter.convert(createDto())

            // Assert
            assertThat(actual).isEqualTo(
                PolymarketSessionKey(
                    address = "0x5e55000000000000000000000000000000000001",
                    deviceId = "3f2b8c1e-7a4d-4e0f-9b6a-2c1d5e8f0a17",
                    deviceName = "iPhone 15 Pro",
                    isEnabled = true,
                    status = PolymarketSessionKeyStatus.ACTIVE,
                    scopes = listOf("CLOB"),
                    validUntil = Instant.ofEpochSecond(1804982400),
                    renewAfter = Instant.ofEpochSecond(1804723200),
                    authorizedAt = Instant.ofEpochSecond(1789430400),
                    createdAt = Instant.ofEpochSecond(1789430280),
                ),
            )
        }

        @Test
        fun `GIVEN a signer known only from its revocation WHEN converted THEN the absent fields stay null`() {
            // Arrange
            val dto = createDto(
                address = "0x01D0000000000000000000000000000000000009",
                deviceId = null,
                deviceName = null,
                isEnabled = false,
                status = "REVOKING",
                scopes = emptyList(),
                validUntil = null,
                renewAfter = null,
                authorizedAt = null,
            )

            // Act
            val actual = PolymarketSessionKeyConverter.convert(dto)

            // Assert
            assertThat(actual).isEqualTo(
                PolymarketSessionKey(
                    address = "0x01d0000000000000000000000000000000000009",
                    deviceId = null,
                    deviceName = null,
                    isEnabled = false,
                    status = PolymarketSessionKeyStatus.REVOKING,
                    scopes = emptyList(),
                    validUntil = null,
                    renewAfter = null,
                    authorizedAt = null,
                    createdAt = Instant.ofEpochSecond(1789430280),
                ),
            )
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN a raw status WHEN converted THEN unknown values fall back to UNKNOWN`(model: StatusModel) {
            // Act
            val actual = PolymarketSessionKeyConverter.convert(createDto(status = model.raw)).status

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            StatusModel(raw = "PENDING", expected = PolymarketSessionKeyStatus.PENDING),
            StatusModel(raw = "ACTIVE", expected = PolymarketSessionKeyStatus.ACTIVE),
            StatusModel(raw = "REVOKING", expected = PolymarketSessionKeyStatus.REVOKING),
            StatusModel(raw = "REVOKED", expected = PolymarketSessionKeyStatus.REVOKED),
            StatusModel(raw = "EXPIRED", expected = PolymarketSessionKeyStatus.EXPIRED),
            StatusModel(raw = "FAILED", expected = PolymarketSessionKeyStatus.FAILED),
            StatusModel(raw = "SUSPENDED", expected = PolymarketSessionKeyStatus.UNKNOWN),
            StatusModel(raw = "active", expected = PolymarketSessionKeyStatus.UNKNOWN),
        )
    }

    @Test
    fun `GIVEN a wallet status with keys WHEN converted to state THEN the keys are carried in order`() {
        // Arrange
        val response = PolymarketWalletStatusResponse(
            depositWalletAddress = DW,
            status = "READY_TO_TRADE",
            sessionKeys = listOf(createDto(address = "0xnew"), createDto(address = "0xold", status = "REVOKED")),
        )

        // Act
        val actual = PolymarketWalletConverter.toState(response)

        // Assert
        assertThat(actual).isEqualTo(
            PolymarketWalletState(
                depositWalletAddress = DW,
                status = PolymarketWalletStatus.READY_TO_TRADE,
                sessionKeys = listOf(
                    PolymarketSessionKeyConverter.convert(createDto(address = "0xnew")),
                    PolymarketSessionKeyConverter.convert(createDto(address = "0xold", status = "REVOKED")),
                ),
            ),
        )
    }

    @Test
    fun `GIVEN a wallet status without session keys WHEN converted to state THEN the keys are empty`() {
        // Arrange
        val response = PolymarketWalletStatusResponse(
            depositWalletAddress = DW,
            status = "READY_TO_TRADE",
            sessionKeys = null,
        )

        // Act
        val actual = PolymarketWalletConverter.toState(response)

        // Assert
        assertThat(actual.sessionKeys).isEmpty()
    }

    @Test
    fun `GIVEN a session key with an out of range timestamp WHEN the wallet is converted THEN only it is dropped`() {
        // Arrange
        val response = PolymarketWalletStatusResponse(
            depositWalletAddress = DW,
            status = "READY_TO_TRADE",
            sessionKeys = listOf(
                createDto(address = "0xnew"),
                createDto(address = "0xbad").copy(createdAt = Long.MAX_VALUE),
            ),
        )

        // Act
        val actual = PolymarketWalletConverter.toState(response)

        // Assert
        assertThat(actual.sessionKeys)
            .containsExactly(PolymarketSessionKeyConverter.convert(createDto(address = "0xnew")))
    }

    @Test
    fun `GIVEN session signers WHEN converted THEN addresses are lowercased and missing fields default`() {
        // Arrange
        val response = PolymarketSessionSignersResponse(
            wallet = DW,
            signers = listOf(
                PolymarketSessionSignerDto(address = "0x5E55AB", scopes = listOf("CLOB"), validUntil = 1804982400),
                PolymarketSessionSignerDto(address = "0xC0FFEE", scopes = null, validUntil = null),
            ),
        )

        // Act
        val actual = PolymarketSessionSignersConverter.convert(response)

        // Assert
        assertThat(actual).containsExactly(
            PolymarketSessionSigner(
                address = "0x5e55ab",
                scopes = listOf("CLOB"),
                validUntil = Instant.ofEpochSecond(1804982400),
            ),
            PolymarketSessionSigner(address = "0xc0ffee", scopes = emptyList(), validUntil = null),
        ).inOrder()
    }

    @Test
    fun `GIVEN no signers field WHEN converted THEN the list is empty`() {
        // Act
        val actual = PolymarketSessionSignersConverter.convert(PolymarketSessionSignersResponse(wallet = DW))

        // Assert
        assertThat(actual).isEmpty()
    }

    @Test
    fun `GIVEN an authorization WHEN converted to request THEN the single call is wrapped in the envelope`() {
        // Arrange
        val authorization = PolymarketSessionKeyAuthorization(
            ownerAddress = OWNER,
            depositWalletAddress = DW,
            sessionKeyAddress = KEY,
            deviceId = "device",
            deviceName = "Pixel 9",
            validUntil = "1804982400",
            nonce = "9",
            deadline = "1789431000",
            call = PolymarketApprovalCall(target = DW, value = "0", data = "0x24017fae"),
            signature = "0xsig",
            replacesSessionKeyAddress = "0xold",
        )

        // Act
        val actual = PolymarketAuthorizeSessionKeyRequestConverter.convert(authorization)

        // Assert
        assertThat(actual).isEqualTo(
            PolymarketAuthorizeSessionKeyRequest(
                ownerAddress = OWNER,
                depositWalletAddress = DW,
                sessionKeyAddress = KEY,
                deviceId = "device",
                deviceName = "Pixel 9",
                validUntil = "1804982400",
                nonce = "9",
                deadline = "1789431000",
                calls = listOf(PolymarketApprovalCallDto(target = DW, value = "0", data = "0x24017fae")),
                signature = "0xsig",
                replacesSessionKeyAddress = "0xold",
            ),
        )
    }

    @Test
    fun `GIVEN a revocation WHEN converted to request THEN the single call is wrapped in the envelope`() {
        // Arrange
        val revocation = PolymarketSessionKeyRevocation(
            ownerAddress = OWNER,
            depositWalletAddress = DW,
            sessionKeyAddress = KEY,
            nonce = "8",
            deadline = "1789431000",
            call = PolymarketApprovalCall(target = DW, value = "0", data = "0xe63f952f"),
            signature = "0xsig",
        )

        // Act
        val actual = PolymarketRevokeSessionKeyRequestConverter.convert(revocation)

        // Assert
        assertThat(actual).isEqualTo(
            PolymarketRevokeSessionKeyRequest(
                ownerAddress = OWNER,
                depositWalletAddress = DW,
                sessionKeyAddress = KEY,
                nonce = "8",
                deadline = "1789431000",
                calls = listOf(PolymarketApprovalCallDto(target = DW, value = "0", data = "0xe63f952f")),
                signature = "0xsig",
            ),
        )
    }

    data class StatusModel(val raw: String, val expected: PolymarketSessionKeyStatus)

    private fun createDto(
        address: String = "0x5E55000000000000000000000000000000000001",
        deviceId: String? = "3f2b8c1e-7a4d-4e0f-9b6a-2c1d5e8f0a17",
        deviceName: String? = "iPhone 15 Pro",
        isEnabled: Boolean = true,
        status: String = "ACTIVE",
        scopes: List<String> = listOf("CLOB"),
        validUntil: Long? = 1804982400,
        renewAfter: Long? = 1804723200,
        authorizedAt: Long? = 1789430400,
        createdAt: Long = 1789430280,
    ) = PolymarketSessionKeyDto(
        address = address,
        deviceId = deviceId,
        deviceName = deviceName,
        isEnabled = isEnabled,
        status = status,
        scopes = scopes,
        validUntil = validUntil,
        renewAfter = renewAfter,
        authorizedAt = authorizedAt,
        createdAt = createdAt,
    )

    private companion object {
        const val OWNER = "0xabc0000000000000000000000000000000000001"
        const val DW = "0xdef0000000000000000000000000000000000002"
        const val KEY = "0x5e55000000000000000000000000000000000001"
    }
}