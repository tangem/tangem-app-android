package com.tangem.domain.polymarket.sessionkey

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.polymarket.model.PolymarketApprovalCall
import com.tangem.domain.polymarket.model.PolymarketSessionKey
import com.tangem.domain.polymarket.model.PolymarketSessionKeyAuthorization
import com.tangem.domain.polymarket.model.PolymarketSessionKeyRevocation
import com.tangem.domain.polymarket.model.PolymarketSessionKeyStatus
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigInteger
import java.time.Instant

internal class PolymarketPendingAuthorizationStageTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class StageAt {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN the wallet nonce and the revoked key record WHEN stageAt THEN the stage left to run`(
            model: StageModel,
        ) {
            // Act
            val actual = pending().stageAt(
                sessionKeys = listOfNotNull(model.revokedRecord),
                walletNonce = model.walletNonce,
                nowMillis = model.nowMillis,
            )

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        @Test
        fun `GIVEN an extension whose old record is revoking at the next nonce WHEN stageAt THEN the revocation was sent`() {
            // Arrange
            val revokingOld = record(REVOKED_KEY, PolymarketSessionKeyStatus.REVOKING).copy(validUntil = CREATED_AT)

            // Act
            val actual = extension().stageAt(listOf(revokingOld), AUTHORIZATION_NONCE, nowMillis = NOW_MILLIS)

            // Assert
            assertThat(actual).isEqualTo(PolymarketPendingAuthorizationStage.RevocationSent(revokingOld))
        }

        @Test
        fun `GIVEN an authorization nonce that is not a number WHEN stageAt THEN the nonce moved`() {
            // Arrange
            val pending = pending(authorizationNonce = "eight")

            // Act
            val actual = pending.stageAt(emptyList(), REVOCATION_NONCE, nowMillis = NOW_MILLIS)

            // Assert
            assertThat(actual).isEqualTo(PolymarketPendingAuthorizationStage.NonceMoved)
        }

        private fun provideTestModels(): List<StageModel> {
            val revoking = record(REVOKED_KEY, PolymarketSessionKeyStatus.REVOKING)
            val revoked = record(REVOKED_KEY, PolymarketSessionKeyStatus.REVOKED)
            return listOf(
                StageModel(
                    walletNonce = AUTHORIZATION_NONCE,
                    revokedRecord = revoking,
                    expected = PolymarketPendingAuthorizationStage.RevocationSent(revoking),
                ),
                StageModel(
                    walletNonce = AUTHORIZATION_NONCE,
                    revokedRecord = revoked,
                    expected = PolymarketPendingAuthorizationStage.RevocationSent(revoked),
                ),
                StageModel(
                    walletNonce = AUTHORIZATION_NONCE,
                    revokedRecord = null,
                    expected = PolymarketPendingAuthorizationStage.RevokedRecordDropped,
                ),
                StageModel(
                    walletNonce = AUTHORIZATION_NONCE,
                    revokedRecord = record(REVOKED_KEY, PolymarketSessionKeyStatus.ACTIVE),
                    expected = PolymarketPendingAuthorizationStage.RevocationUnconfirmed(
                        record(REVOKED_KEY, PolymarketSessionKeyStatus.ACTIVE),
                    ),
                ),
                StageModel(
                    walletNonce = AUTHORIZATION_NONCE,
                    revokedRecord = record(REVOKED_KEY, PolymarketSessionKeyStatus.FAILED),
                    expected = PolymarketPendingAuthorizationStage.RevocationUnconfirmed(
                        record(REVOKED_KEY, PolymarketSessionKeyStatus.FAILED),
                    ),
                ),
                StageModel(
                    walletNonce = REVOCATION_NONCE,
                    revokedRecord = record(REVOKED_KEY, PolymarketSessionKeyStatus.ACTIVE),
                    expected = PolymarketPendingAuthorizationStage.RevocationUnsent,
                ),
                StageModel(
                    walletNonce = REVOCATION_NONCE,
                    revokedRecord = null,
                    expected = PolymarketPendingAuthorizationStage.RevocationUnsent,
                ),
                StageModel(
                    walletNonce = REVOCATION_NONCE,
                    revokedRecord = revoking,
                    expected = PolymarketPendingAuthorizationStage.RevocationSent(revoking),
                ),
                StageModel(
                    walletNonce = REVOCATION_NONCE,
                    revokedRecord = revoked,
                    expected = PolymarketPendingAuthorizationStage.RevocationSent(revoked),
                ),
                StageModel(
                    walletNonce = REVOCATION_NONCE,
                    revokedRecord = revoked,
                    nowMillis = REVOCATION_DEADLINE * MILLIS_IN_SECOND,
                    expected = PolymarketPendingAuthorizationStage.RevocationSent(revoked),
                ),
                StageModel(
                    walletNonce = REVOCATION_NONCE,
                    revokedRecord = record(REVOKED_KEY, PolymarketSessionKeyStatus.ACTIVE),
                    nowMillis = REVOCATION_DEADLINE * MILLIS_IN_SECOND,
                    expected = PolymarketPendingAuthorizationStage.Expired,
                ),
                StageModel(
                    walletNonce = REVOCATION_NONCE,
                    revokedRecord = revoking,
                    nowMillis = REVOCATION_DEADLINE * MILLIS_IN_SECOND,
                    expected = PolymarketPendingAuthorizationStage.RevocationSent(revoking),
                ),
                StageModel(
                    walletNonce = REVOCATION_NONCE,
                    revokedRecord = revoking,
                    nowMillis = AUTHORIZATION_DEADLINE * MILLIS_IN_SECOND,
                    expected = PolymarketPendingAuthorizationStage.Expired,
                ),
                StageModel(
                    walletNonce = AUTHORIZATION_NONCE,
                    revokedRecord = revoked,
                    nowMillis = AUTHORIZATION_DEADLINE * MILLIS_IN_SECOND,
                    expected = PolymarketPendingAuthorizationStage.Expired,
                ),
                StageModel(
                    walletNonce = REVOCATION_NONCE - BigInteger.ONE,
                    revokedRecord = revoking,
                    expected = PolymarketPendingAuthorizationStage.NonceBehind,
                ),
                StageModel(
                    walletNonce = REVOCATION_NONCE,
                    revokedRecord = record(REVOKED_KEY, PolymarketSessionKeyStatus.ACTIVE),
                    nowMillis = (REVOCATION_DEADLINE - MARGIN_SECONDS) * MILLIS_IN_SECOND,
                    expected = PolymarketPendingAuthorizationStage.Expired,
                ),
                StageModel(
                    walletNonce = REVOCATION_NONCE,
                    revokedRecord = record(REVOKED_KEY, PolymarketSessionKeyStatus.ACTIVE),
                    nowMillis = (REVOCATION_DEADLINE - MARGIN_SECONDS - 1) * MILLIS_IN_SECOND,
                    expected = PolymarketPendingAuthorizationStage.RevocationUnsent,
                ),
                StageModel(
                    walletNonce = AUTHORIZATION_NONCE + BigInteger.ONE,
                    revokedRecord = revoked,
                    expected = PolymarketPendingAuthorizationStage.NonceMoved,
                ),
            )
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class AcceptedIn {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN the authorized key records WHEN acceptedIn THEN only a record of this authorization is accepted`(
            model: AcceptedModel,
        ) {
            // Act
            val actual = pending().acceptedIn(model.sessionKeys)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        @Test
        fun `GIVEN an extension whose old record is revoked and the new one listed WHEN acceptedIn THEN the new record`() {
            // Arrange
            val revokedOld = record(REVOKED_KEY, PolymarketSessionKeyStatus.REVOKED).copy(
                createdAt = CREATED_AT.minusSeconds(60),
                validUntil = CREATED_AT,
            )
            val listedNew = record(REVOKED_KEY, PolymarketSessionKeyStatus.PENDING)

            // Act
            val actual = extension().acceptedIn(listOf(revokedOld, listedNew))

            // Assert
            assertThat(actual).isEqualTo(listedNew)
        }

        @Test
        fun `GIVEN an extension whose old record is still revoking WHEN acceptedIn THEN nothing is accepted`() {
            // Arrange
            val revokingOld = record(REVOKED_KEY, PolymarketSessionKeyStatus.REVOKING).copy(validUntil = CREATED_AT)

            // Act
            val actual = extension().acceptedIn(listOf(revokingOld))

            // Assert
            assertThat(actual).isNull()
        }

        private fun provideTestModels(): List<AcceptedModel> {
            val pending = record(AUTHORIZED_KEY, PolymarketSessionKeyStatus.PENDING)
            val active = record(AUTHORIZED_KEY, PolymarketSessionKeyStatus.ACTIVE)
            val olderActive = active.copy(createdAt = CREATED_AT.minusSeconds(60))
            val newerRevoked = record(AUTHORIZED_KEY, PolymarketSessionKeyStatus.REVOKED)
                .copy(createdAt = CREATED_AT.plusSeconds(60))
            return listOf(
                AcceptedModel(sessionKeys = emptyList(), expected = null),
                AcceptedModel(sessionKeys = listOf(pending), expected = pending),
                AcceptedModel(sessionKeys = listOf(active), expected = active),
                AcceptedModel(
                    sessionKeys = listOf(active.copy(address = AUTHORIZED_KEY_CHECKSUM)),
                    expected = active.copy(address = AUTHORIZED_KEY_CHECKSUM),
                ),
                AcceptedModel(
                    sessionKeys = listOf(record(AUTHORIZED_KEY, PolymarketSessionKeyStatus.REVOKING)),
                    expected = null,
                ),
                AcceptedModel(
                    sessionKeys = listOf(record(AUTHORIZED_KEY, PolymarketSessionKeyStatus.FAILED)),
                    expected = null,
                ),
                AcceptedModel(
                    sessionKeys = listOf(active.copy(validUntil = VALID_UNTIL.plusSeconds(1))),
                    expected = null,
                ),
                AcceptedModel(sessionKeys = listOf(active.copy(validUntil = null)), expected = null),
                AcceptedModel(sessionKeys = listOf(olderActive, newerRevoked), expected = null),
                AcceptedModel(
                    sessionKeys = listOf(record(REVOKED_KEY, PolymarketSessionKeyStatus.ACTIVE)),
                    expected = null,
                ),
            )
        }
    }

    data class StageModel(
        val walletNonce: BigInteger,
        val revokedRecord: PolymarketSessionKey?,
        val expected: PolymarketPendingAuthorizationStage,
        val nowMillis: Long = NOW_MILLIS,
    )

    data class AcceptedModel(
        val sessionKeys: List<PolymarketSessionKey>,
        val expected: PolymarketSessionKey?,
    )

    private companion object {
        const val OWNER = "0x1111111111111111111111111111111111111111"
        const val DEPOSIT_WALLET = "0x2222222222222222222222222222222222222222"
        const val REVOKED_KEY = "0x5e55000000000000000000000000000000000002"
        const val AUTHORIZED_KEY = "0x5e55000000000000000000000000000000000abc"
        const val AUTHORIZED_KEY_CHECKSUM = "0x5E55000000000000000000000000000000000ABC"
        const val MILLIS_IN_SECOND = 1_000L
        const val MARGIN_SECONDS = 30L
        const val NOW_SECONDS = 1_789_430_000L
        const val NOW_MILLIS = NOW_SECONDS * MILLIS_IN_SECOND
        const val REVOCATION_DEADLINE = NOW_SECONDS + 600
        const val AUTHORIZATION_DEADLINE = NOW_SECONDS + 1_200
        val REVOCATION_NONCE: BigInteger = BigInteger.valueOf(7)
        val AUTHORIZATION_NONCE: BigInteger = BigInteger.valueOf(8)
        val CREATED_AT: Instant = Instant.ofEpochSecond(NOW_SECONDS)
        val VALID_UNTIL: Instant = Instant.ofEpochSecond(NOW_SECONDS + 180L * 24 * 60 * 60)
        val CALL = PolymarketApprovalCall(target = DEPOSIT_WALLET, value = "0", data = "0x")

        fun record(address: String, status: PolymarketSessionKeyStatus) = PolymarketSessionKey(
            address = address,
            deviceId = "device",
            deviceName = "Pixel",
            isEnabled = true,
            status = status,
            scopes = listOf("CLOB"),
            validUntil = VALID_UNTIL,
            renewAfter = null,
            authorizedAt = null,
            createdAt = CREATED_AT,
        )

        fun extension() = pending().let { pending ->
            pending.copy(
                authorization = pending.authorization.copy(sessionKeyAddress = REVOKED_KEY),
                purpose = PolymarketPendingAuthorization.Purpose.EXTEND,
            )
        }

        fun pending(authorizationNonce: String = AUTHORIZATION_NONCE.toString()) = PolymarketPendingAuthorization(
            revocation = PolymarketSessionKeyRevocation(
                ownerAddress = OWNER,
                depositWalletAddress = DEPOSIT_WALLET,
                sessionKeyAddress = REVOKED_KEY,
                nonce = REVOCATION_NONCE.toString(),
                deadline = REVOCATION_DEADLINE.toString(),
                call = CALL,
                signature = "0xrevoke",
            ),
            authorization = PolymarketSessionKeyAuthorization(
                ownerAddress = OWNER,
                depositWalletAddress = DEPOSIT_WALLET,
                sessionKeyAddress = AUTHORIZED_KEY,
                deviceId = "device",
                deviceName = "Pixel",
                validUntil = VALID_UNTIL.epochSecond.toString(),
                nonce = authorizationNonce,
                deadline = AUTHORIZATION_DEADLINE.toString(),
                call = CALL,
                signature = "0xsig",
                replacesSessionKeyAddress = null,
            ),
            purpose = PolymarketPendingAuthorization.Purpose.REPLACE,
        )
    }
}