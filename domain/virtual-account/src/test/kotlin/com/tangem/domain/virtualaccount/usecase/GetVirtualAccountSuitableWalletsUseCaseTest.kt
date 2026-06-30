package com.tangem.domain.virtualaccount.usecase

import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.domain.card.MockScanResponseFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.domain.card.configs.GenericCardConfig
import com.tangem.domain.card.configs.Wallet2CardConfig
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWallet
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.Test

internal class GetVirtualAccountSuitableWalletsUseCaseTest {

    private val userWalletsListRepository: UserWalletsListRepository = mockk()

    private val useCase = GetVirtualAccountSuitableWalletsUseCase(userWalletsListRepository = userWalletsListRepository)

    @Test
    fun `GIVEN compatible, single-currency and outdated wallets WHEN invoke THEN returns only the compatible one`() {
        // GIVEN
        val compatible = MockUserWalletFactory.create(
            MockScanResponseFactory.create(cardConfig = Wallet2CardConfig, derivedKeys = emptyMap()),
        )
        val singleCurrency = MockUserWalletFactory.createSingleWalletWithToken()
        val outdatedFirmware = MockUserWalletFactory.create(
            MockScanResponseFactory.create(cardConfig = GenericCardConfig(maxWalletCount = 2), derivedKeys = emptyMap()),
        )
        every { userWalletsListRepository.userWallets } returns
            MutableStateFlow(listOf(compatible, singleCurrency, outdatedFirmware))

        // WHEN
        val result = useCase()

        // THEN
        assertThat(result).containsExactly(compatible)
    }

    @Test
    fun `GIVEN no wallets WHEN invoke THEN returns empty list`() {
        // GIVEN
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(null)

        // WHEN
        val result = useCase()

        // THEN
        assertThat(result).isEmpty()
    }
}