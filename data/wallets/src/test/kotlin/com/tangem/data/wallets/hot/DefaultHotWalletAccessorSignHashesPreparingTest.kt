package com.tangem.data.wallets.hot

import com.google.common.truth.Truth.assertThat
import com.tangem.common.card.EllipticCurve
import com.tangem.domain.settings.repositories.LegacySettingsRepository
import com.tangem.domain.wallets.hot.HotWalletPasswordRequester
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.hot.sdk.TangemHotSdk
import com.tangem.hot.sdk.exception.WrongPasswordException
import com.tangem.hot.sdk.model.DataToSign
import com.tangem.hot.sdk.model.HotAuth
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.hot.sdk.model.SignedData
import com.tangem.hot.sdk.model.UnlockHotWallet
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DefaultHotWalletAccessorSignHashesPreparingTest {

    private val biometryWalletId = HotWalletId(value = "biometry", authType = HotWalletId.AuthType.Biometry)
    private val passwordWalletId = HotWalletId(value = "password", authType = HotWalletId.AuthType.Password)
    private val contextualUnlock = UnlockHotWallet(walletId = biometryWalletId, auth = HotAuth.Contextual)
    private val dataToSign = listOf(DataToSign(curve = EllipticCurve.Ed25519, hashes = listOf(byteArrayOf(1))))
    private val signedData = listOf(SignedData(curve = EllipticCurve.Ed25519, signatures = listOf(byteArrayOf(2))))

    private val events = mutableListOf<String>()
    private val tangemHotSdk: TangemHotSdk = mockk(relaxed = true)
    private val passwordRequester: HotWalletPasswordRequester = mockk(relaxed = true)
    private val walletsRepository: WalletsRepository = mockk(relaxed = true)
    private val legacySettingsRepository: LegacySettingsRepository = mockk(relaxed = true)

    private val accessor = DefaultHotWalletAccessor(
        tangemHotSdk = tangemHotSdk,
        userWalletsListRepository = mockk(relaxed = true),
        hotWalletPasswordRequester = passwordRequester,
        walletsRepository = walletsRepository,
        legacySettingsRepository = legacySettingsRepository,
        scope = mockk(relaxed = true),
    )

    @BeforeEach
    fun setUp() {
        events.clear()
        clearMocks(tangemHotSdk, passwordRequester, walletsRepository, legacySettingsRepository)
        coEvery { walletsRepository.requireAccessCode() } returns false
        coEvery { walletsRepository.useBiometricAuthentication() } returns false
        coEvery { legacySettingsRepository.canUseBiometryStrict() } returns true
        coEvery { passwordRequester.requestPassword(any()) } answers {
            events += PASSWORD_ENTERED
            HotWalletPasswordRequester.Result.EnteredPassword(HotAuth.Password("code".toCharArray()))
        }
        coEvery { tangemHotSdk.getContextUnlock(any()) } answers {
            events += UNLOCKED
            contextualUnlock
        }
        coEvery { tangemHotSdk.signHashes(any(), any()) } answers {
            events += SIGNED
            signedData
        }
    }

    @Test
    fun `GIVEN biometry wallet WHEN signHashesPreparing THEN signs the prepared data with biometry`() = runTest {
        // Act
        val result = accessor.signHashesPreparing(biometryWalletId) { prepare() }

        // Assert
        assertThat(result).isEqualTo(signedData)
        assertThat(events).containsExactly(PREPARED, SIGNED).inOrder()
        coVerify { tangemHotSdk.signHashes(UnlockHotWallet(biometryWalletId, HotAuth.Biometry), dataToSign) }
    }

    @Test
    fun `GIVEN prepare throws IllegalStateException WHEN signHashesPreparing THEN rethrows without asking a code`() =
        runTest {
            // Arrange
            val error = IllegalStateException("node is down")

            // Act
            val actual = runCatching { accessor.signHashesPreparing(biometryWalletId) { throw error } }
                .exceptionOrNull()

            // Assert
            assertThat(actual).isEqualTo(error)
            assertThat(events).isEmpty()
            coVerify(exactly = 0) { passwordRequester.requestPassword(any()) }
            coVerify { passwordRequester.dismiss() }
        }

    @Test
    fun `GIVEN password wallet WHEN signHashesPreparing THEN prepares after the code is entered`() = runTest {
        // Act
        accessor.signHashesPreparing(passwordWalletId) { prepare() }

        // Assert
        assertThat(events).containsExactly(PASSWORD_ENTERED, PREPARED, SIGNED).inOrder()
        coVerify(exactly = 0) { tangemHotSdk.getContextUnlock(any()) }
    }

    @Test
    fun `GIVEN wrong code WHEN signHashesPreparing THEN prepares again after the code is re-entered`() = runTest {
        // Arrange
        var attempt = 0
        coEvery { tangemHotSdk.signHashes(any(), any()) } answers {
            events += SIGNED
            if (attempt++ == 0) throw WrongPasswordException()
            signedData
        }

        // Act
        val result = accessor.signHashesPreparing(passwordWalletId) { prepare() }

        // Assert
        assertThat(result).isEqualTo(signedData)
        assertThat(events)
            .containsExactly(PASSWORD_ENTERED, PREPARED, SIGNED, PASSWORD_ENTERED, PREPARED, SIGNED)
            .inOrder()
    }

    @Test
    fun `GIVEN contextual unlock held WHEN signHashesPreparing THEN still asks for the wallet's auth`() = runTest {
        // Arrange
        accessor.unlockContextual(passwordWalletId, HotAuth.Password("code".toCharArray()))
        events.clear()

        // Act
        accessor.signHashesPreparing(passwordWalletId) { prepare() }

        // Assert
        assertThat(events).containsExactly(PASSWORD_ENTERED, PREPARED, SIGNED).inOrder()
        coVerify(exactly = 0) { tangemHotSdk.signHashes(contextualUnlock, any()) }
    }

    private fun prepare(): List<DataToSign> {
        events += PREPARED
        return dataToSign
    }

    private companion object {
        const val PASSWORD_ENTERED = "password entered"
        const val UNLOCKED = "unlocked"
        const val PREPARED = "prepared"
        const val SIGNED = "signed"
    }
}