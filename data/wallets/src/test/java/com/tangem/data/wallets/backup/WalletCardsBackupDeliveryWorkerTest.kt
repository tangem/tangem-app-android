package com.tangem.data.wallets.backup

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.wallets.models.errors.WalletCardsBackupError
import com.tangem.domain.wallets.usecase.SendPendingWalletCardsBackupUseCase
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class WalletCardsBackupDeliveryWorkerTest {

    private val sendPendingWalletCardsBackupUseCase: SendPendingWalletCardsBackupUseCase = mockk()

    private val worker = WalletCardsBackupDeliveryWorker(
        context = mockk<Context>(relaxed = true),
        params = mockk<WorkerParameters>(relaxed = true),
        sendPendingWalletCardsBackupUseCase = sendPendingWalletCardsBackupUseCase,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(sendPendingWalletCardsBackupUseCase)
    }

    @Test
    fun `GIVEN every report delivered WHEN doWork THEN succeeds`() = runTest {
        // Arrange
        coEvery { sendPendingWalletCardsBackupUseCase() } returns Unit.right()

        // Act
        val actual = worker.doWork()

        // Assert
        assertThat(actual).isEqualTo(ListenableWorker.Result.success())
    }

    @Test
    fun `GIVEN reports deferred WHEN doWork THEN retries`() = runTest {
        // Arrange
        coEvery { sendPendingWalletCardsBackupUseCase() } returns WalletCardsBackupError.NoInternetConnection.left()

        // Act
        val actual = worker.doWork()

        // Assert
        assertThat(actual).isEqualTo(ListenableWorker.Result.retry())
    }
}