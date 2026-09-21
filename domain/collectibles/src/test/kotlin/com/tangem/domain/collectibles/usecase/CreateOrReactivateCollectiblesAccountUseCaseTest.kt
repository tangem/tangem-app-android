package com.tangem.domain.collectibles.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.collectibles.delegate.CollectiblesHotDelegate
import com.tangem.domain.collectibles.delegate.HotDelegatePublicKey
import com.tangem.domain.collectibles.account.derivation.CollectiblesOwnerDeriver
import com.tangem.domain.collectibles.account.flow.CollectiblesAccountStatusFetcher
import com.tangem.domain.collectibles.vault.repository.GachaVaultRepository
import com.tangem.domain.collectibles.CollectiblesAccountError
import com.tangem.domain.collectibles.account.repository.CollectiblesAccountRepository
import com.tangem.domain.collectibles.vault.GachaVaultLimits
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.CollectiblesAccountStatusValue
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.TestAppCoroutineScope
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class CreateOrReactivateCollectiblesAccountUseCaseTest {

    private val hotDelegate: CollectiblesHotDelegate = mockk()
    private val ownerDeriver: CollectiblesOwnerDeriver = mockk()
    private val gachaVaultRepository: GachaVaultRepository = mockk()
    private val accountRepository: CollectiblesAccountRepository = mockk(relaxed = true)
    private val accountStatusFetcher: CollectiblesAccountStatusFetcher = mockk(relaxed = true)

    private val userWallet: UserWallet = mockk {
        every { walletId } returns WALLET_ID
    }

    @BeforeEach
    fun resetMocks() {
        clearMocks(hotDelegate, ownerDeriver, gachaVaultRepository, accountRepository, accountStatusFetcher)
        coEvery { accountRepository.getStatusSync(any()) } returns null
        coEvery { accountRepository.saveAccount(any(), any()) } returns Unit
        coEvery { hotDelegate.provision(any()) } returns DELEGATE.right()
    }

    @Test
    fun `GIVEN the account already exists WHEN invoked THEN nothing is read or signed`() = runTest {
        // Arrange
        coEvery { accountRepository.getStatusSync(WALLET_ID) } returns ONBOARDED

        // Act
        val actual = createUseCase(testScope = this)(userWallet)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 0) {
            hotDelegate.provision(any())
            gachaVaultRepository.initOrUpdateVault(any(), any(), any())
            gachaVaultRepository.deriveOwnerAndSetUpVault(any(), any(), any())
        }
    }

    @Test
    fun `GIVEN no owner key WHEN invoked THEN one session derives it and sets up the vault`() = runTest {
        // Arrange
        coEvery { ownerDeriver.storedOwner(WALLET_ID) } returns null
        coEvery { gachaVaultRepository.deriveOwnerAndSetUpVault(any(), any(), any()) } returns Unit.right()

        // Act
        val actual = createUseCase(testScope = this)(userWallet)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 1) {
            gachaVaultRepository.deriveOwnerAndSetUpVault(userWallet, DELEGATE, GachaVaultLimits.Default)
            accountRepository.saveAccount(WALLET_ID, ONBOARDED)
        }
        // Without the owner key there is no address to read, so nothing is decided before the tap
        coVerify(exactly = 0) {
            gachaVaultRepository.initOrUpdateVault(any(), any(), any())
        }
    }

    @Test
    fun `GIVEN a wallet WHEN invoked THEN the delegate is provisioned before the vault is touched`() = runTest {
        // Arrange
        coEvery { ownerDeriver.storedOwner(WALLET_ID) } returns null
        coEvery { gachaVaultRepository.deriveOwnerAndSetUpVault(any(), any(), any()) } returns Unit.right()

        // Act
        val actual = createUseCase(testScope = this)(userWallet)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerifyOrder {
            hotDelegate.provision(ACCOUNT_ID)
            gachaVaultRepository.deriveOwnerAndSetUpVault(userWallet, DELEGATE, GachaVaultLimits.Default)
        }
    }

    @Test
    fun `GIVEN provisioning fails WHEN invoked THEN the vault is not read`() = runTest {
        // Arrange
        val error = CollectiblesAccountError.DelegateKeyCreationFailed(IllegalStateException("keystore is locked"))
        coEvery { hotDelegate.provision(any()) } returns error.left()

        // Act
        val actual = createUseCase(testScope = this)(userWallet)

        // Assert
        assertThat(actual).isEqualTo(error.left())
        coVerify(exactly = 0) { gachaVaultRepository.initOrUpdateVault(any(), any(), any()) }
    }

    @Test
    fun `GIVEN the card refuses WHEN invoked THEN the account is not recorded`() = runTest {
        // Arrange
        coEvery { ownerDeriver.storedOwner(WALLET_ID) } returns null
        coEvery { gachaVaultRepository.deriveOwnerAndSetUpVault(any(), any(), any()) } returns
            CollectiblesAccountError.CardError(code = 50002, message = "50002").left()

        // Act
        val actual = createUseCase(testScope = this)(userWallet)

        // Assert
        assertThat(actual).isEqualTo(CollectiblesAccountError.CardError(code = 50002, message = "50002").left())
        coVerify(exactly = 0) { accountRepository.saveAccount(any(), any()) }
    }

    @Test
    fun `GIVEN the account needs an update WHEN invoked THEN it is reactivated instead of skipped`() = runTest {
        // Arrange
        coEvery { accountRepository.getStatusSync(WALLET_ID) } returns UPDATE_REQUIRED
        coEvery { ownerDeriver.storedOwner(WALLET_ID) } returns OWNER
        coEvery { gachaVaultRepository.initOrUpdateVault(any(), any(), any()) } returns Unit.right()

        // Act
        val actual = createUseCase(testScope = this)(userWallet)

        // Assert
        assertThat(actual).isEqualTo(Unit.right())
        coVerify(exactly = 1) {
            gachaVaultRepository.initOrUpdateVault(userWallet, DELEGATE, GachaVaultLimits.Default)
            accountRepository.saveAccount(WALLET_ID, ONBOARDED)
        }
    }

    @Test
    fun `GIVEN the account is recorded WHEN invoked THEN the vault balance is read`() = runTest {
        // Arrange
        coEvery { ownerDeriver.storedOwner(WALLET_ID) } returns null
        coEvery { gachaVaultRepository.deriveOwnerAndSetUpVault(any(), any(), any()) } returns Unit.right()

        // Act
        createUseCase(testScope = this)(userWallet)
        advanceUntilIdle()

        // Assert — the record alone leaves the account's row shimmering
        coVerify(exactly = 1) { accountStatusFetcher(CollectiblesAccountStatusFetcher.Params(WALLET_ID)) }
    }

    @Test
    fun `GIVEN the account already exists WHEN invoked THEN the vault is not read`() = runTest {
        // Arrange
        coEvery { accountRepository.getStatusSync(WALLET_ID) } returns ONBOARDED

        // Act
        createUseCase(testScope = this)(userWallet)
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { accountStatusFetcher(any()) }
    }

    private fun createUseCase(testScope: TestScope) = CreateOrReactivateCollectiblesAccountUseCase(
        hotDelegate = hotDelegate,
        ownerDeriver = ownerDeriver,
        gachaVaultRepository = gachaVaultRepository,
        accountRepository = accountRepository,
        accountStatusFetcher = accountStatusFetcher,
        appScope = TestAppCoroutineScope(testScope),
    )

    private companion object {

        const val OWNER = "8kXHqmvJ3sYVsBLnr5aXqjy6r2ZxHhCTvJH1DPDcnUXG"

        val WALLET_ID = UserWalletId("011977")

        val ONBOARDED = CollectiblesAccountStatusValue.Onboarded(source = StatusSource.ACTUAL)
        val UPDATE_REQUIRED = CollectiblesAccountStatusValue.UpdateRequired(source = StatusSource.ACTUAL)
        val ACCOUNT_ID = AccountId.forCollectiblesAccount(userWalletId = WALLET_ID)

        val DELEGATE = HotDelegatePublicKey("CfzgiAsor8k46oAr1zLybapCbAK44892DYxBJUerNX31")
    }
}