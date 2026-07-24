package com.tangem.core.local.datastore

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class RuntimeSharedMapStoreTest {

    private val store = RuntimeSharedMapStore<String, Int>()

    @Test
    fun `GIVEN empty store WHEN getSyncOrNull THEN returns null`() = runTest {
        assertThat(store.getSyncOrNull(key = "a")).isNull()
    }

    @Test
    fun `GIVEN stored value WHEN getSyncOrNull THEN returns it`() = runTest {
        store.store(key = "a", value = 1)

        assertThat(store.getSyncOrNull(key = "a")).isEqualTo(1)
    }

    @Test
    fun `GIVEN stored value WHEN get THEN flow emits it`() = runTest {
        store.store(key = "a", value = 1)

        assertThat(store.get(key = "a").first()).isEqualTo(1)
    }

    @Test
    fun `GIVEN several values WHEN getAllSyncOrNull THEN returns all`() = runTest {
        store.store(key = "a", value = 1)
        store.store(key = "b", value = 2)

        assertThat(store.getAllSyncOrNull()).containsExactly(1, 2)
    }

    @Test
    fun `GIVEN empty store WHEN getAllSyncOrNull THEN returns null`() = runTest {
        assertThat(store.getAllSyncOrNull()).isNull()
    }

    @Test
    fun `GIVEN stored map WHEN getSyncOrNull THEN each key resolves`() = runTest {
        store.store(values = mapOf("a" to 1, "b" to 2))

        assertThat(store.getSyncOrNull(key = "a")).isEqualTo(1)
        assertThat(store.getSyncOrNull(key = "b")).isEqualTo(2)
    }

    @Test
    fun `GIVEN stored value WHEN contains THEN reflects presence`() = runTest {
        store.store(key = "a", value = 1)

        assertThat(store.contains(key = "a")).isTrue()
        assertThat(store.contains(key = "b")).isFalse()
    }

    @Test
    fun `GIVEN several keys WHEN remove single THEN only it is removed`() = runTest {
        store.store(key = "a", value = 1)
        store.store(key = "b", value = 2)

        store.remove(key = "a")

        assertThat(store.getSyncOrNull(key = "a")).isNull()
        assertThat(store.getSyncOrNull(key = "b")).isEqualTo(2)
    }

    @Test
    fun `GIVEN several keys WHEN remove collection THEN all listed are removed`() = runTest {
        store.store(values = mapOf("a" to 1, "b" to 2, "c" to 3))

        store.remove(keys = listOf("a", "b"))

        assertThat(store.getSyncOrNull(key = "a")).isNull()
        assertThat(store.getSyncOrNull(key = "b")).isNull()
        assertThat(store.getSyncOrNull(key = "c")).isEqualTo(3)
    }

    @Test
    fun `GIVEN stored key stored again WHEN getSyncOrNull THEN returns the latest`() = runTest {
        store.store(key = "a", value = 1)
        store.store(key = "a", value = 2)

        assertThat(store.getSyncOrNull(key = "a")).isEqualTo(2)
    }

    @Test
    fun `GIVEN stored values WHEN clear THEN store is empty`() = runTest {
        store.store(values = mapOf("a" to 1, "b" to 2))

        store.clear()

        assertThat(store.getSyncOrNull(key = "a")).isNull()
        assertThat(store.getAllSyncOrNull()).isEmpty()
    }
}