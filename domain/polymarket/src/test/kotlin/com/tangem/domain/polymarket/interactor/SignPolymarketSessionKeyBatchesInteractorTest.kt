package com.tangem.domain.polymarket.interactor

import arrow.core.Either
import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketApiCredentials
import com.tangem.domain.polymarket.model.PolymarketOnboardingError
import com.tangem.domain.polymarket.model.PolymarketSessionKeyAuthorization
import com.tangem.domain.polymarket.model.PolymarketSessionKeyBatch
import com.tangem.domain.polymarket.model.PolymarketSessionKeyError
import com.tangem.domain.polymarket.model.PolymarketSessionKeyRevocation
import com.tangem.domain.polymarket.model.PolymarketSignedOnboarding
import com.tangem.domain.polymarket.model.PolymarketSignedSessionKeyBatches
import com.tangem.domain.polymarket.model.PolymarketSigningError
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyCalls
import com.tangem.domain.polymarket.signing.PolymarketApprovalsPayload
import com.tangem.domain.polymarket.signing.PolymarketClobAuthData
import com.tangem.domain.polymarket.signing.PolymarketSignRequest
import com.tangem.domain.polymarket.signing.PolymarketSigned
import com.tangem.domain.polymarket.signing.PolymarketSigningSession
import com.tangem.domain.polymarket.signing.PolymarketTypedPayload
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketApiCredentialsUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketRelayerNonceUseCase
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigInteger

internal class SignPolymarketSessionKeyBatchesInteractorTest {

    private val deriveAddresses: DerivePolymarketAddressesUseCase = mockk()
    private val getRelayerNonce: GetPolymarketRelayerNonceUseCase = mockk()
    private val getApiCredentials: GetPolymarketApiCredentialsUseCase = mockk()

    private val session = FakeSigningSession()
    private val interactor = SignPolymarketSessionKeyBatchesInteractor(
        signingSession = session,
        deriveAddresses = deriveAddresses,
        getRelayerNonce = getRelayerNonce,
        getApiCredentials = getApiCredentials,
        nowMillis = { NOW_MILLIS },
    )

    @BeforeEach
    fun setup() {
        clearMocks(deriveAddresses, getRelayerNonce, getApiCredentials)
        session.reset()
        every { deriveAddresses.fromOwner(USER_WALLET_ID, OWNER) } returns ADDRESSES.right()
        coEvery { getRelayerNonce(ADDRESSES) } returns NONCE.right()
        coEvery { getApiCredentials(USER_WALLET_ID) } returns mockk<PolymarketApiCredentials>()
    }

    @Test
    fun `GIVEN a revocation and an authorization WHEN invoke THEN one session signs both on consecutive nonces`() =
        runTest {
            // Act
            val actual = interactor(USER_WALLET_ID, nonEmptyListOf(REVOKE, AUTHORIZE))

            // Assert
            val expectedPayloads = listOf(
                PolymarketTypedPayload.RelayedBatch(revokePayload(nonce = "41", deadline = DEADLINE)),
                PolymarketTypedPayload.RelayedBatch(authorizePayload(nonce = "42", deadline = FOLLOW_UP_DEADLINE)),
            )
            assertThat(session.runs).isEqualTo(1)
            assertThat(session.signedPayloads).containsExactly(expectedPayloads)
            val expected = PolymarketSignedSessionKeyBatches(
                ownerClobAuth = null,
                batches = listOf(
                    PolymarketSignedSessionKeyBatches.Signed.Revocation(
                        revocation(nonce = "41", deadline = DEADLINE, signature = "0x0"),
                    ),
                    PolymarketSignedSessionKeyBatches.Signed.Authorization(
                        authorization(nonce = "42", deadline = FOLLOW_UP_DEADLINE, signature = "0x1"),
                    ),
                ),
            )
            assertThat(actual).isEqualTo(expected.right())
        }

    @Test
    fun `GIVEN no owner credentials WHEN invoke THEN the owner ClobAuth is signed first in the same session`() =
        runTest {
            // Arrange
            coEvery { getApiCredentials(USER_WALLET_ID) } returns null

            // Act
            val actual = interactor(USER_WALLET_ID, nonEmptyListOf(AUTHORIZE))

            // Assert
            val expectedPayloads = listOf(
                PolymarketTypedPayload.ClobAuth(PolymarketClobAuthData(timestamp = NOW_SECONDS.toString())),
                PolymarketTypedPayload.RelayedBatch(authorizePayload(nonce = "41", deadline = DEADLINE)),
            )
            assertThat(session.runs).isEqualTo(1)
            assertThat(session.signedPayloads).containsExactly(expectedPayloads)
            val expected = PolymarketSignedSessionKeyBatches(
                ownerClobAuth = PolymarketSignedOnboarding.SignedClobAuth(
                    l1Signature = "0x0",
                    timestamp = NOW_SECONDS.toString(),
                ),
                batches = listOf(
                    PolymarketSignedSessionKeyBatches.Signed.Authorization(
                        authorization(nonce = "41", deadline = DEADLINE, signature = "0x1"),
                    ),
                ),
            )
            assertThat(actual).isEqualTo(expected.right())
        }

    @Test
    fun `GIVEN the nonce is unreadable WHEN invoke THEN PreparationFailed and nothing is signed`() = runTest {
        // Arrange
        coEvery { getRelayerNonce(ADDRESSES) } returns PolymarketOnboardingError.Network.left()

        // Act
        val actual = interactor(USER_WALLET_ID, nonEmptyListOf(AUTHORIZE))

        // Assert
        assertThat(actual).isEqualTo(
            PolymarketSessionKeyError.PreparationFailed(PolymarketOnboardingError.Network).left(),
        )
        assertThat(session.signedPayloads).isEmpty()
    }

    @Test
    fun `GIVEN a malformed session key WHEN invoke THEN InvalidAddress and nothing is signed`() = runTest {
        // Act
        val actual = interactor(USER_WALLET_ID, nonEmptyListOf(PolymarketSessionKeyBatch.Revoke("0x1234")))

        // Assert
        assertThat(actual).isEqualTo(PolymarketSessionKeyError.InvalidAddress("0x1234").left())
        assertThat(session.signedPayloads).isEmpty()
    }

    @Test
    fun `GIVEN the user cancels WHEN invoke THEN SigningFailed with the cancellation`() = runTest {
        // Arrange
        session.signing = { PolymarketSigningError.UserCancelled.left() }

        // Act
        val actual = interactor(USER_WALLET_ID, nonEmptyListOf(AUTHORIZE))

        // Assert
        assertThat(
            actual,
        ).isEqualTo(PolymarketSessionKeyError.SigningFailed(PolymarketSigningError.UserCancelled).left())
    }

    @Test
    fun `GIVEN fewer signatures than payloads WHEN invoke THEN SigningFailed Unknown`() = runTest {
        // Arrange
        session.signing = { listOf("0x0").right() }

        // Act
        val actual = interactor(USER_WALLET_ID, nonEmptyListOf(REVOKE, AUTHORIZE))

        // Assert
        assertThat(actual).isEqualTo(PolymarketSessionKeyError.SigningFailed(PolymarketSigningError.Unknown).left())
    }

    private fun authorizePayload(nonce: String, deadline: String) = PolymarketApprovalsPayload(
        depositWalletAddress = DEPOSIT_WALLET,
        nonce = nonce,
        deadline = deadline,
        calls = listOf(authorizeCall()),
    )

    private fun revokePayload(nonce: String, deadline: String) = PolymarketApprovalsPayload(
        depositWalletAddress = DEPOSIT_WALLET,
        nonce = nonce,
        deadline = deadline,
        calls = listOf(revokeCall()),
    )

    private fun authorizeCall() = PolymarketSessionKeyCalls.authorize(
        sessionKeyAddress = SESSION_KEY,
        validUntil = VALID_UNTIL,
        depositWalletAddress = DEPOSIT_WALLET,
    ).getOrNull()!!

    private fun revokeCall() = PolymarketSessionKeyCalls.revoke(
        sessionKeyAddress = OLD_SESSION_KEY,
        depositWalletAddress = DEPOSIT_WALLET,
    ).getOrNull()!!

    private fun authorization(nonce: String, deadline: String, signature: String) = PolymarketSessionKeyAuthorization(
        ownerAddress = OWNER,
        depositWalletAddress = DEPOSIT_WALLET,
        sessionKeyAddress = SESSION_KEY,
        deviceId = DEVICE_ID,
        deviceName = DEVICE_NAME,
        validUntil = VALID_UNTIL.toString(),
        nonce = nonce,
        deadline = deadline,
        call = authorizeCall(),
        signature = signature,
        replacesSessionKeyAddress = OLD_SESSION_KEY,
    )

    private fun revocation(nonce: String, deadline: String, signature: String) = PolymarketSessionKeyRevocation(
        ownerAddress = OWNER,
        depositWalletAddress = DEPOSIT_WALLET,
        sessionKeyAddress = OLD_SESSION_KEY,
        nonce = nonce,
        deadline = deadline,
        call = revokeCall(),
        signature = signature,
    )

    private class FakeSigningSession : PolymarketSigningSession {

        var signing: (List<PolymarketTypedPayload>) -> Either<PolymarketSigningError, List<String>> =
            DEFAULT_SIGNING
        val signedPayloads = mutableListOf<List<PolymarketTypedPayload>>()
        var runs = 0

        fun reset() {
            signing = DEFAULT_SIGNING
            signedPayloads.clear()
            runs = 0
        }

        override suspend fun <T> run(
            userWalletId: UserWalletId,
            prepare: suspend (ownerAddress: String) -> PolymarketSignRequest<T>,
        ): Either<PolymarketSigningError, PolymarketSigned<T>> {
            runs++
            val request = prepare(OWNER)
            if (request.payloads.isEmpty()) return PolymarketSigned(request.value, emptyList()).right()

            signedPayloads += request.payloads
            return signing(request.payloads).map { signatures -> PolymarketSigned(request.value, signatures) }
        }

        private companion object {
            val DEFAULT_SIGNING: (List<PolymarketTypedPayload>) -> Either<PolymarketSigningError, List<String>> =
                { payloads -> payloads.indices.map { index -> "0x$index" }.right() }
        }
    }

    private companion object {
        const val OWNER = "0x1111111111111111111111111111111111111111"
        const val DEPOSIT_WALLET = "0xfAeA0f08159fcF2f573fE24E9E989B0d48f7651B"
        const val SESSION_KEY = "0x5E55000000000000000000000000000000000001"
        const val OLD_SESSION_KEY = "0x5E55000000000000000000000000000000000002"
        const val DEVICE_ID = "device-1"
        const val DEVICE_NAME = "Pixel 9"
        const val NOW_MILLIS = 1_789_430_400_750L
        const val NOW_SECONDS = 1_789_430_400L
        const val VALID_UNTIL = NOW_SECONDS + 15_552_000L
        const val DEADLINE = "1789431000"
        const val FOLLOW_UP_DEADLINE = "1789431600"
        val NONCE: BigInteger = BigInteger.valueOf(41)
        val USER_WALLET_ID = UserWalletId("011")
        val ADDRESSES = PolymarketAddresses(
            ownerAddress = OWNER,
            depositWalletAddress = DEPOSIT_WALLET,
            userWalletId = USER_WALLET_ID,
        )
        val AUTHORIZE = PolymarketSessionKeyBatch.Authorize(
            sessionKeyAddress = SESSION_KEY,
            deviceId = DEVICE_ID,
            deviceName = DEVICE_NAME,
            replacesSessionKeyAddress = OLD_SESSION_KEY,
        )
        val REVOKE = PolymarketSessionKeyBatch.Revoke(sessionKeyAddress = OLD_SESSION_KEY)
    }
}