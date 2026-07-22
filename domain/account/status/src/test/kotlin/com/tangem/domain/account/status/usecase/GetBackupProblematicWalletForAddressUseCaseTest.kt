package com.tangem.domain.account.status.usecase

import arrow.core.left
import arrow.core.none
import arrow.core.right
import arrow.core.some
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.account.status.model.AccountCryptoCurrency
import com.tangem.domain.card.IsWalletBackupProblematicUseCase
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.models.errors.GetUserWalletError
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetBackupProblematicWalletForAddressUseCaseTest {

    private val getAccountCurrencyByAddressUseCase: GetAccountCurrencyByAddressUseCase = mockk()
    private val getUserWalletUseCase: GetUserWalletUseCase = mockk()
    private val isWalletBackupProblematicUseCase: IsWalletBackupProblematicUseCase = mockk()

    private val useCase = GetBackupProblematicWalletForAddressUseCase(
        getAccountCurrencyByAddressUseCase = getAccountCurrencyByAddressUseCase,
        getUserWalletUseCase = getUserWalletUseCase,
        isWalletBackupProblematicUseCase = isWalletBackupProblematicUseCase,
    )

    @AfterEach
    fun tearDown() {
        clearMocks(getAccountCurrencyByAddressUseCase, getUserWalletUseCase, isWalletBackupProblematicUseCase)
    }

    @Test
    fun `returns null when address is not resolved to an account`() = runTest {
        coEvery { getAccountCurrencyByAddressUseCase(ADDRESS) } returns none()

        assertThat(useCase(ADDRESS)).isNull()
    }

    @Test
    fun `returns null when destination wallet is not found`() = runTest {
        coEvery { getAccountCurrencyByAddressUseCase(ADDRESS) } returns accountCurrency.some()
        every { getUserWalletUseCase(walletId) } returns GetUserWalletError.UserWalletNotFound.left()

        assertThat(useCase(ADDRESS)).isNull()
    }

    @Test
    fun `returns null when destination wallet is not problematic`() = runTest {
        coEvery { getAccountCurrencyByAddressUseCase(ADDRESS) } returns accountCurrency.some()
        every { getUserWalletUseCase(walletId) } returns userWallet.right()
        every { isWalletBackupProblematicUseCase(userWallet) } returns false

        assertThat(useCase(ADDRESS)).isNull()
    }

    @Test
    fun `returns wallet id when destination wallet is problematic`() = runTest {
        coEvery { getAccountCurrencyByAddressUseCase(ADDRESS) } returns accountCurrency.some()
        every { getUserWalletUseCase(walletId) } returns userWallet.right()
        every { isWalletBackupProblematicUseCase(userWallet) } returns true

        assertThat(useCase(ADDRESS)).isEqualTo(walletId)
    }

    private companion object {
        const val ADDRESS = "0x1234567890abcdef"
        val walletId = UserWalletId("011")
        val userWallet = mockk<UserWallet>()
        val accountCurrency = mockk<AccountCryptoCurrency> {
            every { account } returns mockk { every { userWalletId } returns walletId }
        }
    }
}