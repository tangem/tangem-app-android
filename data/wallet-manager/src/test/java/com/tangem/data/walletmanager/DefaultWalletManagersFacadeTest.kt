package com.tangem.data.walletmanager

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchainsdk.models.UpdateWalletManagerResult
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.data.walletmanager.store.WalletManagersStore
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/**
 * A joint network reads through the Safe wallet manager, so without the wallet behind it there is nothing to build:
 * an update is unreachable and a lookup gets `null`, in every build.
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
        jointAccountSafeAddressResolver = { mockk() },
        getCachedTokenInfoUseCase = mockk(),
        txHistoryFeatureToggles = mockk(relaxed = true),
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
    fun `GIVEN joint network of a wallet the app does not hold WHEN update THEN unreachable without a manager`() =
        runTest {
            // Arrange
            every { userWalletsListRepository.userWallets } returns MutableStateFlow(null)

            // Act
            val actual = facade.update(userWalletId = userWalletId, network = jointNetwork, extraTokens = emptySet())

            // Assert
            assertThat(actual).isEqualTo(UpdateWalletManagerResult.Unreachable())
            verify { walletManagersStore wasNot Called }
        }

    @Suppress("DEPRECATION")
    @Test
    fun `GIVEN joint network of a wallet the app does not hold WHEN getOrCreateWalletManager THEN null`() = runTest {
        // Arrange
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(null)

        // Act
        val actual = facade.getOrCreateWalletManager(userWalletId = userWalletId, network = jointNetwork)

        // Assert
        assertThat(actual).isNull()
    }
}