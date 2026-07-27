package com.tangem.datasource.local.txhistory

import com.google.common.truth.Truth.assertThat
import com.tangem.core.local.datastore.RuntimeSharedMapStore
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.txhistory.models.Page
import com.tangem.domain.txhistory.models.PaginationWrapper
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class DefaultTxHistoryItemsStoreTest {

    private val store = DefaultTxHistoryItemsStore(store = RuntimeSharedMapStore())

    private val key = TxHistoryItemsStore.Key(userWalletId = mockk<UserWalletId>(), currency = mockk<CryptoCurrency>())

    @Test
    fun `GIVEN empty store WHEN getSyncOrNull THEN returns null`() = runTest {
        assertThat(store.getSyncOrNull(key = key, page = Page.Initial)).isNull()
    }

    @Test
    fun `GIVEN stored page WHEN getSyncOrNull by page THEN returns it`() = runTest {
        // Arrange
        val wrapper = wrapper(page = Page.Initial)
        store.store(key = key, value = wrapper)

        // Assert
        assertThat(store.getSyncOrNull(key = key, page = Page.Initial)).isEqualTo(wrapper)
        assertThat(store.getSyncOrNull(key = key, page = Page.Next(value = "2"))).isNull()
    }

    @Test
    fun `GIVEN same page stored twice WHEN getSyncOrNull THEN returns the latest`() = runTest {
        // Arrange
        val first = wrapper(page = Page.Initial, nextPage = Page.Next(value = "a"))
        val second = wrapper(page = Page.Initial, nextPage = Page.Next(value = "b"))
        store.store(key = key, value = first)
        store.store(key = key, value = second)

        // Assert
        assertThat(store.getSyncOrNull(key = key, page = Page.Initial)).isEqualTo(second)
    }

    @Test
    fun `GIVEN two pages stored WHEN getSyncOrNull THEN both retrievable`() = runTest {
        // Arrange
        val firstPage = wrapper(page = Page.Initial)
        val secondPage = wrapper(page = Page.Next(value = "2"))
        store.store(key = key, value = firstPage)
        store.store(key = key, value = secondPage)

        // Assert
        assertThat(store.getSyncOrNull(key = key, page = Page.Initial)).isEqualTo(firstPage)
        assertThat(store.getSyncOrNull(key = key, page = Page.Next(value = "2"))).isEqualTo(secondPage)
    }

    @Test
    fun `GIVEN stored value WHEN remove THEN it is gone`() = runTest {
        // Arrange
        store.store(key = key, value = wrapper(page = Page.Initial))

        // Act
        store.remove(key = key)

        // Assert
        assertThat(store.getSyncOrNull(key = key, page = Page.Initial)).isNull()
    }

    private fun wrapper(page: Page, nextPage: Page = Page.LastPage) = PaginationWrapper<TxInfo>(
        currentPage = page,
        nextPage = nextPage,
        items = emptyList(),
    )
}