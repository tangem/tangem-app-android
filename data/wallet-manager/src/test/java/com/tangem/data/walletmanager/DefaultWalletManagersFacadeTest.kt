package com.tangem.data.walletmanager

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchainsdk.models.UpdateWalletManagerResult
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.datasource.local.walletmanager.WalletManagersStore
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.Called
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/**
 * A joint account is a Safe contract: the facade never derives a key or builds a wallet manager for its network.
 * A tester build crashes on the attempt, a release one refuses quietly — the tests follow the build they run in.
 */
internal class DefaultWalletManagersFacadeTest {

    private val walletManagersStore: WalletManagersStore = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()

    private val facade = DefaultWalletManagersFacade(
        walletManagersStore = walletManagersStore,
        userWalletsListRepository = userWalletsListRepository,
        assetLoader = mockk(),
        dispatchers = TestingCoroutineDispatcherProvider(),
        gaslessTransactionRepository = mockk(),
        blockchainSDKFactory = mockk(),
    )

    private val userWalletId = UserWalletId("011")
    private val jointNetwork = MockCryptoCurrencyFactory().ethereum.network.let { network ->
        val derivationPath = Network.DerivationPath.Joint(value = "m/44'/60'/888888'/0/2")

        network.copy(
            id = Network.ID(rawId = network.id.rawId, derivationPath = derivationPath),
            derivationPath = derivationPath,
        )
    }

    @Test
    fun `GIVEN joint network WHEN update THEN refused before any lookup`() = runTest {
        // Act
        val actual = runCatching {
            facade.update(userWalletId = userWalletId, network = jointNetwork, extraTokens = emptySet(), xpub = null)
        }

        // Assert
        if (BuildConfig.TESTER_MENU_ENABLED) {
            assertThat(actual.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
        } else {
            assertThat(actual.getOrNull()).isEqualTo(UpdateWalletManagerResult.Unreachable())
        }
        verify {
            userWalletsListRepository wasNot Called
            walletManagersStore wasNot Called
        }
    }

    @Suppress("DEPRECATION")
    @Test
    fun `GIVEN joint network WHEN getOrCreateWalletManager THEN refused before any lookup`() = runTest {
        // Act
        val actual = runCatching { facade.getOrCreateWalletManager(userWalletId = userWalletId, network = jointNetwork) }

        // Assert
        if (BuildConfig.TESTER_MENU_ENABLED) {
            assertThat(actual.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
        } else {
            assertThat(actual.getOrNull()).isNull()
        }
        verify {
            userWalletsListRepository wasNot Called
            walletManagersStore wasNot Called
        }
    }
}