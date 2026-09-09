package com.tangem.data.walletmanager

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchainsdk.models.UpdateWalletManagerResult
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.data.walletmanager.store.WalletManagersStore
import com.tangem.data.walletmanager.utils.JointPendingTransactionFactory
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.txhistory.TxHistoryFeatureToggles
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/**
 * A joint account is a Safe contract: the facade never derives a key or builds a wallet manager for its network.
 * An update attempt crashes a tester build and is refused quietly in a release one — that test follows the build
 * it runs in; a read-only lookup gets `null` in every build, so that staking or WalletConnect cannot bring the app down.
 */
internal class DefaultWalletManagersFacadeTest {

    private val walletManagersStore: WalletManagersStore = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()

    private val jointPendingTransactionFactory: JointPendingTransactionFactory = mockk {
        coEvery { create(any(), any(), any(), any()) } returns null
    }

    private val txHistoryFeatureToggles = object : TxHistoryFeatureToggles {
        override val isNewTxHistoryEnabled: Boolean = true
    }

    private val facade = DefaultWalletManagersFacade(
        walletManagersStore = walletManagersStore,
        externalAddressBalanceProvidersStore = ExternalAddressBalanceProvidersStore(),
        userWalletsListRepository = userWalletsListRepository,
        assetLoader = mockk(),
        dispatchers = TestingCoroutineDispatcherProvider(),
        gaslessTransactionRepository = mockk(),
        jointAccountSafeAddressResolver = { mockk() },
        jointPendingTransactionFactory = { jointPendingTransactionFactory },
        txHistoryFeatureToggles = txHistoryFeatureToggles,
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
    fun `GIVEN joint network WHEN getOrCreateWalletManager THEN null without a lookup in any build`() = runTest {
        // Act
        val actual = facade.getOrCreateWalletManager(userWalletId = userWalletId, network = jointNetwork)

        // Assert
        assertThat(actual).isNull()
        verify {
            userWalletsListRepository wasNot Called
            walletManagersStore wasNot Called
        }
    }
}