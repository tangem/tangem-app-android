package com.tangem.domain.stories

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.settings.repositories.SettingsRepository
import com.tangem.domain.settings.usercountry.models.UserCountry
import com.tangem.domain.stories.models.StoryContent
import com.tangem.domain.stories.models.StoryContentIds
import com.tangem.test.core.getEmittedValues
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.Locale

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetStoryContentUseCaseTest {

    private val storiesRepository: StoriesRepository = mockk()
    private val settingsRepository: SettingsRepository = mockk()
    private val story: StoryContent = mockk()

    private val userCountryFlow = MutableStateFlow<UserCountry?>(value = null)

    private val useCase = GetStoryContentUseCase(
        storiesRepository = storiesRepository,
        settingsRepository = settingsRepository,
    )

    private val defaultLocale: Locale = Locale.getDefault()

    @BeforeEach
    fun setup() {
        clearMocks(storiesRepository, settingsRepository)
        userCountryFlow.value = null
        every { settingsRepository.getUserCountryCode() } returns userCountryFlow
        every { storiesRepository.getStoryById(FCA_STORY_ID) } returns MutableStateFlow(story)
        coEvery { storiesRepository.getStoryByIdSync(FCA_STORY_ID, any()) } returns story
    }

    @AfterEach
    fun tearDown() {
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun `GIVEN gb locale AND country not resolved WHEN invokeSync THEN story is restricted`() = runTest {
        // Arrange
        Locale.setDefault(Locale.UK)

        // Act
        val actual = useCase.invokeSync(id = FCA_STORY_ID)

        // Assert
        assertThat(actual.getOrNull()).isNull()
    }

    @Test
    fun `GIVEN gb locale AND geo resolved to other country WHEN invokeSync THEN story is allowed`() = runTest {
        // Arrange
        Locale.setDefault(Locale.UK)
        userCountryFlow.value = UserCountry.Other(code = "de")

        // Act
        val actual = useCase.invokeSync(id = FCA_STORY_ID)

        // Assert
        assertThat(actual.getOrNull()).isEqualTo(story)
    }

    @Test
    fun `GIVEN geo resolved to gb WHEN invokeSync THEN story is restricted`() = runTest {
        // Arrange
        Locale.setDefault(Locale.GERMANY)
        userCountryFlow.value = UserCountry.Other(code = "gb")

        // Act
        val actual = useCase.invokeSync(id = FCA_STORY_ID)

        // Assert
        assertThat(actual.getOrNull()).isNull()
    }

    @Test
    fun `GIVEN gb locale AND country not resolved WHEN invoke THEN emits restricted story`() = runTest {
        // Arrange
        Locale.setDefault(Locale.UK)

        // Act
        val emitted = getEmittedValues(useCase.invoke(id = FCA_STORY_ID))

        // Assert
        assertThat(emitted.map { it.getOrNull() }).containsExactly(null)
    }

    @Test
    fun `GIVEN gb locale WHEN geo resolves to other country THEN story becomes allowed`() = runTest {
        // Arrange
        Locale.setDefault(Locale.UK)

        // Act
        val emitted = getEmittedValues(useCase.invoke(id = FCA_STORY_ID))
        advanceUntilIdle()
        userCountryFlow.value = UserCountry.Other(code = "de")
        advanceUntilIdle()

        // Assert
        assertThat(emitted.map { it.getOrNull() }).containsExactly(null, story).inOrder()
    }

    @Test
    fun `GIVEN allowed story WHEN geo resolves to gb THEN story becomes restricted`() = runTest {
        // Arrange
        Locale.setDefault(Locale.GERMANY)

        // Act
        val emitted = getEmittedValues(useCase.invoke(id = FCA_STORY_ID))
        advanceUntilIdle()
        userCountryFlow.value = UserCountry.Other(code = "gb")
        advanceUntilIdle()

        // Assert
        assertThat(emitted.map { it.getOrNull() }).containsExactly(story, null).inOrder()
    }

    @Test
    fun `GIVEN non gb locale AND country not resolved WHEN invokeSync THEN story is allowed`() = runTest {
        // Arrange
        Locale.setDefault(Locale.GERMANY)

        // Act
        val actual = useCase.invokeSync(id = FCA_STORY_ID)

        // Assert
        assertThat(actual.getOrNull()).isEqualTo(story)
    }

    @Test
    fun `GIVEN gb locale AND non FCA story WHEN invokeSync THEN story is allowed`() = runTest {
        // Arrange
        Locale.setDefault(Locale.UK)
        coEvery { storiesRepository.getStoryByIdSync(OTHER_STORY_ID, any()) } returns story

        // Act
        val actual = useCase.invokeSync(id = OTHER_STORY_ID)

        // Assert
        assertThat(actual.getOrNull()).isEqualTo(story)
    }

    private companion object {
        val FCA_STORY_ID = StoryContentIds.STORY_FIRST_TIME_SWAP.id
        val OTHER_STORY_ID = StoryContentIds.STORY_FIRST_TIME_YIELD_PROMO.id
    }
}