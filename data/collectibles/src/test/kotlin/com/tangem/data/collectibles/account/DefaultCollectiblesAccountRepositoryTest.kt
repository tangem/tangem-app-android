package com.tangem.data.collectibles.account

import com.google.common.truth.Truth.assertThat
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.data.collectibles.account.converter.CollectiblesAccountStatusValueDTOConverter
import com.tangem.data.collectibles.account.store.CollectiblesAccountStore
import com.tangem.data.collectibles.account.store.WalletIdWithCollectiblesAccountDTO
import com.tangem.domain.collectibles.account.CollectiblesVaultCurrencyFactory
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.test.core.datastore.MockStateDataStore
import com.tangem.test.mock.collectibles.MockCollectibles
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

internal class DefaultCollectiblesAccountRepositoryTest {

    private val timeSource = TestTimeSource()

    @Test
    fun `GIVEN a relink just sent WHEN isRelinkPending THEN it is pending for this wallet only`() = runTest {
        // Arrange
        val repository = createRepository(testScope = this)

        // Act
        repository.markRelinkSent(WALLET_A)

        // Assert
        assertThat(repository.isRelinkPending(WALLET_A)).isTrue()
        assertThat(repository.isRelinkPending(WALLET_B)).isFalse()
    }

    @Test
    fun `GIVEN a relink sent two minutes ago WHEN isRelinkPending THEN it can no longer land`() = runTest {
        // Arrange
        val repository = createRepository(testScope = this)
        repository.markRelinkSent(WALLET_A)

        // Act
        timeSource += 120.seconds

        // Assert
        assertThat(repository.isRelinkPending(WALLET_A)).isFalse()
    }

    @Test
    fun `GIVEN a relink sent WHEN clearAccount THEN it is no longer pending`() = runTest {
        // Arrange
        val repository = createRepository(testScope = this)
        repository.markRelinkSent(WALLET_A)

        // Act
        repository.clearAccount(WALLET_A)

        // Assert
        assertThat(repository.isRelinkPending(WALLET_A)).isFalse()
    }

    private fun createRepository(testScope: TestScope) = DefaultCollectiblesAccountRepository(
        collectiblesAccountStore = CollectiblesAccountStore(
            runtimeStore = RuntimeSharedStore(),
            persistenceDataStore = MockStateDataStore<WalletIdWithCollectiblesAccountDTO>(default = emptyMap()),
            converter = CollectiblesAccountStatusValueDTOConverter(
                currencyFactory = CollectiblesVaultCurrencyFactory(MockCollectibles::usdc),
            ),
            scope = TestAppCoroutineScope(testScope),
        ),
        timeSource = timeSource,
    )

    private companion object {
        val WALLET_A = UserWalletId("011")
        val WALLET_B = UserWalletId("022")
    }
}