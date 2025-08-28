package com.tangem.data.account.store

import com.google.common.truth.Truth
import com.tangem.domain.models.wallet.UserWalletId
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArchivedAccountsStoreFactoryTest {

    private val factory = ArchivedAccountsStoreFactory()

    @AfterEach
    fun tearDownEach() {
        factory.clearStores()
    }

    @Test
    fun `creates new store for unique userWalletId`() {
        // Arrange
        val userWalletId = UserWalletId("001")
        val createdStore = factory.create(userWalletId)

        // Act
        val actual = factory.getAllStores()

        // Assert
        Truth.assertThat(actual).containsExactly(userWalletId, createdStore)
    }

    @Test
    fun `reuses existing data store for same userWalletId`() {
        val userWalletId = UserWalletId("011")

        // Arrange (first creation)
        val firstStore = factory.create(userWalletId = userWalletId)

        // Act (first creation)
        val actual1 = factory.getAllStores()

        // Assert (first creation)
        Truth.assertThat(actual1).containsExactly(userWalletId, firstStore)

        // Arrange (second creation)
        val secondStore = factory.create(userWalletId = userWalletId)

        // Act (second creation)
        val actual2 = factory.getAllStores()

        // Assert (second creation)
        Truth.assertThat(actual2).containsExactly(userWalletId, secondStore)
        Truth.assertThat(firstStore).isSameInstanceAs(secondStore)
    }

    @Test
    fun `creates separate data stores for different userWalletIds`() {
        // Arrange (first creation)
        val firstWalletId = UserWalletId("011")
        val firstStore = factory.create(userWalletId = firstWalletId)

        // Act (first creation)
        val actual1 = factory.getAllStores()

        // Assert (first creation)
        Truth.assertThat(actual1).containsExactly(firstWalletId, firstStore)

        // Arrange (second creation)
        val secondWalletId = UserWalletId("011")
        val secondStore = factory.create(userWalletId = secondWalletId)

        // Act (second creation)
        val actual2 = factory.getAllStores()

        // Assert (second creation)
        val expected = mapOf(firstWalletId to firstStore, secondWalletId to secondStore)
        Truth.assertThat(actual2).containsExactlyEntriesIn(expected)
    }
}