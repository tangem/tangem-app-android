package com.tangem.domain.core.store

import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class StoreTests {

    @Test
    fun `when getting data from not initialized store then error should be received`() = runTest {
        // Given
        val expectedResult = TestStore.StoreError.left()

        val store = TestStore()

        // When
        val get = store.getData()

        // Then
        assertEquals(expectedResult, get)
    }

    @Test
    fun `when getting data from initialized store then data should be received`() = runTest {
        // Given
        val expectedResult = 10.right()

        val store = TestStore()
        store.update(10)

        // When
        val get = store.getData()

        // Then
        assertEquals(expectedResult, get)
    }

    @Test
    fun `when getting data after to initialized store then correct data should be received`() = runTest {
        // Given
        val expectedResult = listOf(10.right(), 20.right())

        val store = TestStore()
        store.update(10)

        val get = suspend {
            store.getData()
        }

        // When
        val get1 = get()

        store.update(data = 20)
        val get2 = get()

        // Then
        assertEquals(expectedResult, listOf(get1, get2))
    }

    @Test
    fun `when setting data after error then data should be received`() = runTest {
        // Given
        val expectedResult = listOf(TestStore.StoreError.left(), 10.right())

        val store = TestStore()
        store.update(TestStore.StoreError)

        val get = suspend {
            store.getData()
        }

        // When
        val get1 = get()

        store.update(data = 10)
        val get2 = get()

        // Then
        assertEquals(expectedResult, listOf(get1, get2))
    }

    @Test
    fun `when setting error then error should be received`() = runTest {
        // Given
        val expectedResult = listOf(10.right(), TestStore.StoreError.left())

        val store = TestStore()
        store.update(10)

        val get = suspend {
            store.getData()
        }

        // When
        val get1 = get()

        store.update(TestStore.StoreError)
        val get2 = get()

        // Then
        assertEquals(expectedResult, listOf(get1, get2))
    }

    private suspend fun TestStore.getData() = either { get().first() }
}
