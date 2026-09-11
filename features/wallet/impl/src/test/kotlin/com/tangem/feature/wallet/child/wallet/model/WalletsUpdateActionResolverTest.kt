package com.tangem.feature.wallet.child.wallet.model

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.domain.wallets.usecase.IsWalletBackedUpUseCase
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletAdditionalInfo
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletCardState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletNotification
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletScreenState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletType
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class WalletsUpdateActionResolverTest {

    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase = mockk()
    private val isWalletBackedUpUseCase: IsWalletBackedUpUseCase = mockk()

    private val resolver = WalletsUpdateActionResolver(
        getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
        isWalletBackedUpUseCase = isWalletBackedUpUseCase,
    )

    private val hotUserWalletId = UserWalletId(stringValue = "0011")
    private val coldUserWalletId = UserWalletId(stringValue = "0022")

    private val hotWallet: UserWallet.Hot = mockk(relaxed = true) {
        every { walletId } returns hotUserWalletId
        every { name } returns HOT_WALLET_NAME
    }
    private val coldWallet: UserWallet.Cold = mockk(relaxed = true) {
        every { walletId } returns coldUserWalletId
        every { name } returns COLD_WALLET_NAME
    }

    @BeforeEach
    fun resetMocks() {
        clearMocks(getSelectedWalletSyncUseCase, isWalletBackedUpUseCase)
    }

    @Test
    fun `GIVEN backed up hot wallet with activation banner WHEN cold wallet added THEN add wallet action`() = runTest {
        // Arrange
        val state = screenState(hotWalletState(isHotBackedUp = true, bannerIsBackupExists = true))
        every { getSelectedWalletSyncUseCase() } returns coldWallet.right()
        coEvery { isWalletBackedUpUseCase(hotWallet) } returns true

        // Act
        val action = resolver.resolve(wallets = listOf(hotWallet, coldWallet), currentState = state)

        // Assert
        assertThat(action).isEqualTo(
            WalletsUpdateActionResolver.Action.AddWallet(
                prevWalletIndex = 0,
                selectedWalletIndex = 1,
                selectedWallet = coldWallet,
            ),
        )
    }

    @Test
    fun `GIVEN not backed up hot wallet in state WHEN cold wallet added and hot backed up THEN add wallet action`() =
        runTest {
            // Arrange
            val state = screenState(hotWalletState(isHotBackedUp = false, bannerIsBackupExists = false))
            every { getSelectedWalletSyncUseCase() } returns coldWallet.right()
            coEvery { isWalletBackedUpUseCase(hotWallet) } returns true

            // Act
            val action = resolver.resolve(wallets = listOf(hotWallet, coldWallet), currentState = state)

            // Assert
            assertThat(action).isEqualTo(
                WalletsUpdateActionResolver.Action.AddWallet(
                    prevWalletIndex = 0,
                    selectedWalletIndex = 1,
                    selectedWallet = coldWallet,
                ),
            )
        }

    @Test
    fun `GIVEN not backed up hot wallet in state WHEN hot wallet becomes backed up THEN reload wallets action`() =
        runTest {
            // Arrange
            val state = screenState(hotWalletState(isHotBackedUp = false, bannerIsBackupExists = false))
            every { getSelectedWalletSyncUseCase() } returns hotWallet.right()
            coEvery { isWalletBackedUpUseCase(hotWallet) } returns true

            // Act
            val action = resolver.resolve(wallets = listOf(hotWallet), currentState = state)

            // Assert
            assertThat(action).isEqualTo(WalletsUpdateActionResolver.Action.ReloadWallets(listOf(hotWallet)))
        }

    @Test
    fun `GIVEN backed up hot wallet with access code banner WHEN nothing changed THEN no reload action`() = runTest {
        // Arrange
        val state = screenState(hotWalletState(isHotBackedUp = true, bannerIsBackupExists = true))
        every { getSelectedWalletSyncUseCase() } returns hotWallet.right()
        coEvery { isWalletBackedUpUseCase(hotWallet) } returns true

        // Act
        val action = resolver.resolve(wallets = listOf(hotWallet), currentState = state)

        // Assert
        assertThat(action).isEqualTo(WalletsUpdateActionResolver.Action.Unknown)
    }

    private fun screenState(vararg walletStates: WalletState): WalletScreenState = mockk {
        every { selectedWalletIndex } returns 0
        every { wallets } returns persistentListOf(*walletStates)
    }

    private fun hotWalletState(isHotBackedUp: Boolean, bannerIsBackupExists: Boolean?): WalletState {
        val cardState: WalletCardState.Content = mockk {
            every { id } returns hotUserWalletId
            every { title } returns HOT_WALLET_NAME
            every { additionalInfo } returns WalletAdditionalInfo(
                hideable = true,
                content = WalletAdditionalInfo.Content.Text(text = mockk()),
                isHotBackedUp = isHotBackedUp,
            )
        }
        val warnings = if (bannerIsBackupExists == null) {
            persistentListOf()
        } else {
            val banner: WalletNotification.FinishWalletActivation = mockk {
                every { isBackupExists } returns bannerIsBackupExists
            }
            persistentListOf<WalletNotification>(banner)
        }

        return mockk<WalletState.MultiCurrency.Content> {
            every { walletCardState } returns cardState
            every { this@mockk.warnings } returns warnings
            every { type } returns WalletType.Hot
        }
    }

    private companion object {
        const val HOT_WALLET_NAME = "Mobile Wallet"
        const val COLD_WALLET_NAME = "Wallet"
    }
}