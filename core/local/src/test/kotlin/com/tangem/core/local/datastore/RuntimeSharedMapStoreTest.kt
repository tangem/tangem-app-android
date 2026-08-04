package com.tangem.core.local.datastore

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
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

    @Test
    fun `GIVEN absent key WHEN update THEN transform receives default`() = runTest {
        store.update(key = "a", default = 10) { it + 1 }

        assertThat(store.getSyncOrNull(key = "a")).isEqualTo(11)
    }

    @Test
    fun `GIVEN stored key WHEN update THEN transform receives current value`() = runTest {
        store.store(key = "a", value = 5)

        store.update(key = "a", default = 10) { it + 1 }

        assertThat(store.getSyncOrNull(key = "a")).isEqualTo(6)
    }

    @Test
    fun `GIVEN concurrent updates of same key WHEN all complete THEN none are lost`() = runTest {
        val count = 100

        (1..count).map { launch { store.update(key = "a", default = 0) { it + 1 } } }.joinAll()

        assertThat(store.getSyncOrNull(key = "a")).isEqualTo(count)
    }

    @Test
    fun `GIVEN absent key WHEN updateIfPresent THEN it is a no-op and store stays uninitialized`() = runTest {
        store.updateIfPresent(key = "a") { it + 1 }

        assertThat(store.getSyncOrNull(key = "a")).isNull()
        assertThat(store.contains(key = "a")).isFalse()
        assertThat(store.getAllSyncOrNull()).isNull()
    }

    @Test
    fun `GIVEN stored key WHEN updateIfPresent THEN value is transformed`() = runTest {
        store.store(key = "a", value = 5)

        store.updateIfPresent(key = "a") { it + 1 }

        assertThat(store.getSyncOrNull(key = "a")).isEqualTo(6)
    }

    @Test
    fun `GIVEN initialized store WHEN updateIfPresent on absent key THEN no-op and key not created`() = runTest {
        store.store(key = "a", value = 1)

        store.updateIfPresent(key = "b") { it + 1 }

        assertThat(store.contains(key = "b")).isFalse()
        assertThat(store.getSyncOrNull(key = "a")).isEqualTo(1)
    }
}