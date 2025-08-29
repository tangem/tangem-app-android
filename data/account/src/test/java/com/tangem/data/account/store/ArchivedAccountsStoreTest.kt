package com.tangem.data.account.store

import com.google.common.truth.Truth
import com.tangem.common.test.utils.getEmittedValues
import com.tangem.datasource.local.datastore.RuntimeStateStore
import com.tangem.domain.account.models.ArchivedAccount
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountName
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArchivedAccountsStoreTest {

    private val runtimeStore: RuntimeStateStore<List<ArchivedAccount>?> = RuntimeStateStore(defaultValue = null)
    private val archivedAccountsStore: ArchivedAccountsStore = ArchivedAccountsStore(runtimeStore = runtimeStore)

    @AfterEach
    fun tearDown() {
        archivedAccountsStore.clear()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Get {

        @Test
        fun `get returns empty flow`() = runTest {
            // Act
            val actual = getEmittedValues(archivedAccountsStore.get())

            // Assert
            Truth.assertThat(actual).isEmpty() // nothing emmited
        }

        @Test
        fun `get returns flow with not expired data`() = runTest {
            // Arrange
            val archivedAccount = createArchivedAccount()
            archivedAccountsStore.store(value = listOf(archivedAccount))

            // Act
            val actual = getEmittedValues(archivedAccountsStore.get())

            // Assert
            val expected = listOf(archivedAccount)
            Truth.assertThat(actual).containsExactly(expected)
        }

        @Test
        fun `get returns flow with expired data`() = runTest {
            // Arrange
            archivedAccountsStore.store(value = listOf(createArchivedAccount()))
            archivedAccountsStore.setTimestamp(time = System.currentTimeMillis() - 120.seconds.inWholeMicroseconds)

            // Act
            val actual = getEmittedValues(archivedAccountsStore.get())

            // Assert
            Truth.assertThat(actual).isEmpty() // nothing emmited
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class GetSyncOrnNull {

        @Test
        fun `getSyncOrNull returns null`() = runTest {
            // Act
            val actual = archivedAccountsStore.getSyncOrNull()

            // Assert
            Truth.assertThat(actual).isNull()
        }

        @Test
        fun `get returns flow with not expired data`() = runTest {
            // Arrange
            val archivedAccount = createArchivedAccount()
            archivedAccountsStore.store(value = listOf(archivedAccount))

            // Act
            val actual = archivedAccountsStore.getSyncOrNull()

            // Assert
            val expected = archivedAccount
            Truth.assertThat(actual).containsExactly(expected)
        }

        @Test
        fun `get returns flow with expired data`() = runTest {
            // Arrange
            archivedAccountsStore.store(value = listOf(createArchivedAccount()))
            archivedAccountsStore.setTimestamp(time = System.currentTimeMillis() - 120.seconds.inWholeMicroseconds)

            // Act
            val actual = archivedAccountsStore.getSyncOrNull()

            // Assert
            Truth.assertThat(actual).isNull()
        }
    }

    @Test
    fun store() = runTest {
        // Arrange
        val archivedAccount = createArchivedAccount()

        // Act
        archivedAccountsStore.store(value = listOf(archivedAccount))
        val actual = runtimeStore.getSyncOrNull()

        // Assert
        val expected = archivedAccount
        Truth.assertThat(actual).containsExactly(expected)
    }

    private fun createArchivedAccount(): ArchivedAccount {
        return ArchivedAccount(
            accountId = AccountId.forCryptoPortfolio(
                userWalletId = UserWalletId("011"),
                derivationIndex = DerivationIndex.Main,
            ),
            name = AccountName("Archived Account").getOrNull()!!,
            icon = CryptoPortfolioIcon.ofDefaultCustomAccount(),
            derivationIndex = DerivationIndex.Main,
            tokensCount = 2,
            networksCount = 1,
        )
    }
}