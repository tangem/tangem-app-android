package com.tangem.tap.domain.walletregistration

import com.google.common.truth.Truth.assertThat
import com.tangem.common.card.Card
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.core.CardSessionRunnable
import com.tangem.operations.backup.PrimaryCard
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BackupColdWalletRegistrationTest {

    private val tangemSdkManager: TangemSdkManager = mockk()
    private val finalization: CardSessionRunnable<Card> = mockk()
    private val registeringFinalization: CardSessionRunnable<Card> = mockk()

    @BeforeEach
    fun setup() {
        clearMocks(tangemSdkManager)
        every {
            tangemSdkManager.withColdWalletRegistration(finalization, shouldForceRegistration = true)
        } returns registeringFinalization
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN primary card firmware WHEN decorating its finalization THEN re-registers only a status-attesting card`(
        model: FirmwareModel,
    ) {
        // Arrange
        val primaryCard: PrimaryCard = mockk { every { firmwareVersion } returns model.firmwareVersion }

        // Act
        val actual = tangemSdkManager.withBackupColdWalletRegistration(primaryCard, finalization)

        // Assert
        if (model.isReRegistered) {
            assertThat(actual).isSameInstanceAs(registeringFinalization)
        } else {
            assertThat(actual).isSameInstanceAs(finalization)
            verify(exactly = 0) { tangemSdkManager.withColdWalletRegistration(any<CardSessionRunnable<Card>>(), any()) }
        }
    }

    data class FirmwareModel(val firmwareVersion: FirmwareVersion?, val isReRegistered: Boolean)

    private fun provideTestModels() = listOf(
        FirmwareModel(firmwareVersion = FirmwareVersion(major = 4, minor = 52), isReRegistered = false),
        FirmwareModel(firmwareVersion = FirmwareVersion(major = 5, minor = 99), isReRegistered = false),
        FirmwareModel(firmwareVersion = FirmwareVersion(major = 6, minor = 0), isReRegistered = true),
        FirmwareModel(firmwareVersion = FirmwareVersion(major = 6, minor = 33), isReRegistered = true),
        FirmwareModel(firmwareVersion = null, isReRegistered = true),
    )
}