package com.tangem.features.storiesv2.impl.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.features.storiesv2.StoriesV2Component
import com.tangem.features.storiesv2.StoriesV2Result
import com.tangem.features.storiesv2.StoryV2Source
import com.tangem.features.storiesv2.StoryV2Type
import com.tangem.features.storiesv2.impl.content.StoryV2ContentLoad
import com.tangem.features.storiesv2.impl.content.StoryV2ContentRepository
import com.tangem.features.storiesv2.impl.ui.state.StoriesV2ScreenUM
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class StoriesV2ModelTest {

    private val contentRepository: StoryV2ContentRepository = mockk()

    @BeforeEach
    fun resetMocks() {
        clearMocks(contentRepository)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN no showable story WHEN the player opens THEN the host continues without it`(model: LoadModel) =
        runTest {
            // Arrange
            coEvery { contentRepository.load(StoryV2Type.HARDWARE_UPGRADE) } returns model.load
            val results = mutableListOf<StoriesV2Result>()

            // Act
            val storiesModel = createModel(onResult = results::add, shouldSkipOnDeliveryFailure = true)
            advanceUntilIdle()

            // Assert
            assertThat(results).containsExactly(model.expected)
            storiesModel.onDestroy()
        }

    @Test
    fun `GIVEN host keeps the retry WHEN delivery fails THEN the error screen is shown and the story does not end`() =
        runTest {
            // Arrange
            coEvery { contentRepository.load(StoryV2Type.HARDWARE_UPGRADE) } returns StoryV2ContentLoad.Failed
            val results = mutableListOf<StoriesV2Result>()

            // Act
            val storiesModel = createModel(onResult = results::add, shouldSkipOnDeliveryFailure = false)
            advanceUntilIdle()

            // Assert
            assertThat(storiesModel.uiState.value).isInstanceOf(StoriesV2ScreenUM.Error::class.java)
            assertThat(results).isEmpty()
            storiesModel.onDestroy()
        }

    private fun provideTestModels() = listOf(
        LoadModel(
            load = StoryV2ContentLoad.Failed,
            expected = StoriesV2Result.Unavailable(StoriesV2Result.Unavailable.Reason.DELIVERY_FAILED),
        ),
        LoadModel(
            load = StoryV2ContentLoad.Unavailable,
            expected = StoriesV2Result.Unavailable(StoriesV2Result.Unavailable.Reason.NO_CONTENT),
        ),
        LoadModel(
            load = StoryV2ContentLoad.AlreadyShown,
            expected = StoriesV2Result.Unavailable(StoriesV2Result.Unavailable.Reason.NO_CONTENT),
        ),
    )

    internal data class LoadModel(val load: StoryV2ContentLoad, val expected: StoriesV2Result)

    private fun TestScope.createModel(onResult: (StoriesV2Result) -> Unit, shouldSkipOnDeliveryFailure: Boolean) =
        StoriesV2Model(
            paramsContainer = MutableParamsContainer(
                value = StoriesV2Component.Params(
                    type = StoryV2Type.HARDWARE_UPGRADE,
                    source = StoryV2Source.MAIN,
                    onAction = {},
                    onResult = onResult,
                    shouldSkipOnDeliveryFailure = shouldSkipOnDeliveryFailure,
                ),
            ),
            dispatchers = createTestingCoroutineDispatcherProvider(),
            context = mockk(),
            contentRepository = contentRepository,
            analytics = mockk(relaxUnitFun = true),
            hapticManager = mockk(),
        )
}