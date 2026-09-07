package com.tangem.datasource.local.stories

import com.google.common.truth.Truth.assertThat
import com.tangem.core.local.datastore.RuntimeSharedMapStore
import com.tangem.datasource.api.stories.models.StoryContentResponse
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class DefaultStoriesStoreTest {

    private val store = DefaultStoriesStore(store = RuntimeSharedMapStore())

    @Test
    fun `GIVEN empty store WHEN getSyncOrNull THEN returns null`() = runTest {
        // Act
        val actual = store.getSyncOrNull(storyId = "s1")

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN stored story WHEN getSyncOrNull THEN returns it`() = runTest {
        // Arrange
        val item = mockk<StoryContentResponse>()
        store.store(storyId = "s1", item = item)

        // Act
        val actual = store.getSyncOrNull(storyId = "s1")

        // Assert
        assertThat(actual).isEqualTo(item)
    }

    @Test
    fun `GIVEN stored story WHEN get THEN flow emits it`() = runTest {
        // Arrange
        val item = mockk<StoryContentResponse>()
        store.store(storyId = "s1", item = item)

        // Act
        val actual = store.get(storyId = "s1").first()

        // Assert
        assertThat(actual).isEqualTo(item)
    }

    @Test
    fun `GIVEN story stored twice WHEN getSyncOrNull THEN returns the latest`() = runTest {
        // Arrange
        val first = mockk<StoryContentResponse>()
        val second = mockk<StoryContentResponse>()
        store.store(storyId = "s1", item = first)
        store.store(storyId = "s1", item = second)

        // Act
        val actual = store.getSyncOrNull(storyId = "s1")

        // Assert
        assertThat(actual).isEqualTo(second)
    }
}