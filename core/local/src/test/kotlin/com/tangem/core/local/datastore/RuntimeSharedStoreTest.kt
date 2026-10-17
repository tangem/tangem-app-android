package com.tangem.core.local.datastore

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class RuntimeSharedStoreTest {

    private val store = RuntimeSharedStore<Int>()

    @Test
    fun `GIVEN empty store WHEN getSyncOrNull THEN returns null`() = runTest {
        assertThat(store.getSyncOrNull()).isNull()
    }

    @Test
    fun `GIVEN stored value WHEN getSyncOrNull THEN returns it`() = runTest {
        store.store(value = 1)

        assertThat(store.getSyncOrNull()).isEqualTo(1)
    }

    @Test
    fun `GIVEN empty store WHEN getSyncOrDefault THEN returns default`() = runTest {
        assertThat(store.getSyncOrDefault(default = 42)).isEqualTo(42)
    }

    @Test
    fun `GIVEN stored value WHEN getSyncOrDefault THEN returns stored value`() = runTest {
        store.store(value = 1)

        assertThat(store.getSyncOrDefault(default = 42)).isEqualTo(1)
    }

    @Test
    fun `GIVEN stored value WHEN get THEN flow emits it`() = runTest {
        store.store(value = 1)

        assertThat(store.get().first()).isEqualTo(1)
    }

    @Test
    fun `GIVEN stored value stored again WHEN getSyncOrNull THEN returns the latest`() = runTest {
        store.store(value = 1)
        store.store(value = 2)

        assertThat(store.getSyncOrNull()).isEqualTo(2)
    }

    @Test
    fun `GIVEN empty store WHEN update THEN transform receives default`() = runTest {
        store.update(default = 10) { it + 1 }

        assertThat(store.getSyncOrNull()).isEqualTo(11)
    }

    @Test
    fun `GIVEN stored value WHEN update THEN transform receives current value`() = runTest {
        store.store(value = 5)

        store.update(default = 10) { it + 1 }

        assertThat(store.getSyncOrNull()).isEqualTo(6)
    }

    @Test
    fun `GIVEN concurrent updates WHEN all complete THEN none are lost`() = runTest {
        val count = 100

        (1..count).map { launch { store.update(default = 0) { it + 1 } } }.joinAll()

        assertThat(store.getSyncOrNull()).isEqualTo(count)
    }
}