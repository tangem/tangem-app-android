package com.tangem.domain.wallets.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.cloudbackup.repository.CloudBackupRepository
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.getEmittedValues
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class IsWalletBackedUpUseCaseTest {

    private val cloudBackupRepository: CloudBackupRepository = mockk()

    private val useCase = IsWalletBackedUpUseCase(cloudBackupRepository)

    // region invoke
    @Test
    fun `GIVEN hot wallet with seed backup WHEN invoke THEN true without querying cloud`() = runTest {
        // GIVEN
        val wallet = hotWallet(backedUp = true)

        // WHEN
        val result = useCase(wallet)

        // THEN
        assertThat(result).isTrue()
        coVerify(exactly = 0) { cloudBackupRepository.isBackedUp(any()) }
    }

    @Test
    fun `GIVEN hot wallet without seed backup AND cloud backup exists WHEN invoke THEN true`() = runTest {
        // GIVEN
        val wallet = hotWallet(backedUp = false)
        coEvery { cloudBackupRepository.isBackedUp(WALLET_ID) } returns true

        // WHEN
        val result = useCase(wallet)

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `GIVEN hot wallet without seed backup AND no cloud backup WHEN invoke THEN false`() = runTest {
        // GIVEN
        val wallet = hotWallet(backedUp = false)
        coEvery { cloudBackupRepository.isBackedUp(WALLET_ID) } returns false

        // WHEN
        val result = useCase(wallet)

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `GIVEN cold wallet with active card backup WHEN invoke THEN true`() = runTest {
        assertThat(useCase(coldWallet(CardDTO.BackupStatus.Active(cardCount = 1)))).isTrue()
    }

    @Test
    fun `GIVEN cold wallet without active card backup WHEN invoke THEN false`() = runTest {
        assertThat(useCase(coldWallet(CardDTO.BackupStatus.NoBackup))).isFalse()
        assertThat(useCase(coldWallet(status = null))).isFalse()
    }
    // endregion

    // region flow
    @Test
    fun `GIVEN hot wallet with seed backup WHEN flow THEN emits true without subscribing to cloud`() = runTest {
        // GIVEN
        val wallet = hotWallet(backedUp = true)

        // WHEN
        val values = getEmittedValues(useCase.flow(wallet))

        // THEN
        assertThat(values).containsExactly(true)
        verify(exactly = 0) { cloudBackupRepository.isBackedUpFlow(any()) }
    }

    @Test
    fun `GIVEN hot wallet without seed backup WHEN cloud backup changes THEN flow emits each change`() = runTest {
        // GIVEN
        val wallet = hotWallet(backedUp = false)
        every { cloudBackupRepository.isBackedUpFlow(WALLET_ID) } returns flowOf(false, true)

        // WHEN
        val values = getEmittedValues(useCase.flow(wallet))

        // THEN
        assertThat(values).containsExactly(false, true).inOrder()
    }

    @Test
    fun `GIVEN hot wallet without seed backup WHEN cloud flow re-emits same value THEN flow deduplicates`() = runTest {
        // GIVEN
        val wallet = hotWallet(backedUp = false)
        every { cloudBackupRepository.isBackedUpFlow(WALLET_ID) } returns flowOf(false, false, true, true)

        // WHEN
        val values = getEmittedValues(useCase.flow(wallet))

        // THEN
        assertThat(values).containsExactly(false, true).inOrder()
    }

    @Test
    fun `GIVEN cold wallet with active card backup WHEN flow THEN emits single true`() = runTest {
        // WHEN
        val values = getEmittedValues(useCase.flow(coldWallet(CardDTO.BackupStatus.Active(cardCount = 1))))

        // THEN
        assertThat(values).containsExactly(true)
    }
    // endregion

    private fun hotWallet(id: String = WALLET_ID, backedUp: Boolean): UserWallet.Hot = UserWallet.Hot(
        name = "Hot",
        walletId = UserWalletId(id),
        hotWalletId = mockk(relaxed = true),
        wallets = null,
        backedUp = backedUp,
    )

    private fun coldWallet(status: CardDTO.BackupStatus?): UserWallet.Cold = mockk {
        every { scanResponse } returns mockk {
            every { card } returns mockk {
                every { backupStatus } returns status
            }
        }
    }

    private companion object {
        const val WALLET_ID = "0A0B0C0D"
    }
}