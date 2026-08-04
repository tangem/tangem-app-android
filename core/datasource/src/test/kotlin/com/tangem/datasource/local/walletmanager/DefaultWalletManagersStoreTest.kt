package com.tangem.datasource.local.walletmanager

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.WalletManager
import com.tangem.core.local.datastore.RuntimeSharedMapStore
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class DefaultWalletManagersStoreTest {

    private val store = DefaultWalletManagersStore(store = RuntimeSharedMapStore())

    private val userWalletId = mockk<UserWalletId>()

    @Test
    fun `GIVEN empty store WHEN getAllSync THEN returns empty`() = runTest {
        assertThat(store.getAllSync(userWalletId)).isEmpty()
    }

    @Test
    fun `GIVEN stored manager WHEN getAllSync THEN returns it`() = runTest {
        // Arrange
        val manager = walletManager(Blockchain.Bitcoin)
        store.store(userWalletId, manager)

        // Assert
        assertThat(store.getAllSync(userWalletId)).containsExactly(manager)
    }

    @Test
    fun `GIVEN two blockchains stored WHEN getSyncOrNull by blockchain THEN returns the matching one`() = runTest {
        // Arrange
        val bitcoin = walletManager(Blockchain.Bitcoin)
        val ethereum = walletManager(Blockchain.Ethereum)
        store.store(userWalletId, bitcoin)
        store.store(userWalletId, ethereum)

        // Act
        val actual = store.getSyncOrNull(userWalletId, blockchain = Blockchain.Ethereum, derivationPath = null)

        // Assert
        assertThat(actual).isEqualTo(ethereum)
    }

    @Test
    fun `GIVEN same blockchain stored twice WHEN getAllSync THEN it is replaced`() = runTest {
        // Arrange
        store.store(userWalletId, walletManager(Blockchain.Bitcoin))
        val latest = walletManager(Blockchain.Bitcoin)
        store.store(userWalletId, latest)

        // Assert
        assertThat(store.getAllSync(userWalletId)).containsExactly(latest)
    }

    @Test
    fun `GIVEN stored managers WHEN remove by predicate THEN matching are removed`() = runTest {
        // Arrange
        val bitcoin = walletManager(Blockchain.Bitcoin)
        val ethereum = walletManager(Blockchain.Ethereum)
        store.store(userWalletId, bitcoin)
        store.store(userWalletId, ethereum)

        // Act
        store.remove(userWalletId) { it.wallet.blockchain == Blockchain.Bitcoin }

        // Assert
        assertThat(store.getAllSync(userWalletId)).containsExactly(ethereum)
    }

    @Test
    fun `GIVEN stored managers WHEN clear THEN store is empty`() = runTest {
        // Arrange
        store.store(userWalletId, walletManager(Blockchain.Bitcoin))

        // Act
        store.clear()

        // Assert
        assertThat(store.getAllSync(userWalletId)).isEmpty()
    }

    private fun walletManager(blockchain: Blockchain) = mockk<WalletManager> {
        every { wallet.blockchain } returns blockchain
        every { wallet.publicKey.derivationPath } returns null
    }
}