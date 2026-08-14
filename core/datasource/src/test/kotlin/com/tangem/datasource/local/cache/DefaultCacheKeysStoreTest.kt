package com.tangem.datasource.local.cache

import com.google.common.truth.Truth.assertThat
import com.tangem.core.local.datastore.RuntimeSharedMapStore
import com.tangem.datasource.local.cache.model.CacheKey
import kotlinx.coroutines.test.runTest
import org.joda.time.Duration
import org.joda.time.LocalDateTime
import org.junit.jupiter.api.Test

internal class DefaultCacheKeysStoreTest {

    private val store = DefaultCacheKeysStore(store = RuntimeSharedMapStore())

    @Test
    fun `GIVEN empty store WHEN getSyncOrNull THEN returns null`() = runTest {
        // Act
        val actual = store.getSyncOrNull(key = "a")

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN stored key WHEN getSyncOrNull THEN returns it`() = runTest {
        // Arrange
        val key = cacheKey(id = "a")
        store.store(key = key)

        // Act
        val actual = store.getSyncOrNull(key = "a")

        // Assert
        assertThat(actual).isEqualTo(key)
    }

    @Test
    fun `GIVEN several keys WHEN remove single THEN only it is removed`() = runTest {
        // Arrange
        store.store(key = cacheKey(id = "a"))
        val keyB = cacheKey(id = "b")
        store.store(key = keyB)

        // Act
        store.remove(key = "a")

        // Assert
        assertThat(store.getSyncOrNull(key = "a")).isNull()
        assertThat(store.getSyncOrNull(key = "b")).isEqualTo(keyB)
    }

    @Test
    fun `GIVEN several keys WHEN remove collection THEN all listed are removed`() = runTest {
        // Arrange
        store.store(key = cacheKey(id = "a"))
        store.store(key = cacheKey(id = "b"))
        val keyC = cacheKey(id = "c")
        store.store(key = keyC)

        // Act
        store.remove(keys = listOf("a", "b"))

        // Assert
        assertThat(store.getSyncOrNull(key = "a")).isNull()
        assertThat(store.getSyncOrNull(key = "b")).isNull()
        assertThat(store.getSyncOrNull(key = "c")).isEqualTo(keyC)
    }

    @Test
    fun `GIVEN stored keys WHEN clear THEN store is empty`() = runTest {
        // Arrange
        store.store(key = cacheKey(id = "a"))
        store.store(key = cacheKey(id = "b"))

        // Act
        store.clear()

        // Assert
        assertThat(store.getSyncOrNull(key = "a")).isNull()
        assertThat(store.getSyncOrNull(key = "b")).isNull()
    }

    private fun cacheKey(id: String) = CacheKey(
        id = id,
        updatedAt = LocalDateTime(2024, 1, 1, 0, 0),
        expiresIn = Duration.standardMinutes(1),
    )
}