package com.tangem.features.survey.impl.deeplink

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.deeplink.DeepLinkRequest
import com.tangem.features.survey.impl.nativesurvey.opener.NativeSurveyOpener
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coJustRun
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class NativeSurveyDeepLinkHandlerTest {

    private val opener: NativeSurveyOpener = mockk()
    private val router: Router = mockk()
    private val handler = NativeSurveyDeepLinkHandler(opener)

    private val requests = mutableListOf<NativeSurveyOpener.Request>()

    @BeforeEach
    fun setUp() {
        clearMocks(opener, router)
        requests.clear()
        coJustRun { opener.open(capture(requests), router) }
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN link parameters WHEN handled THEN the survey is opened with the valid ones only`(model: LinkModel) =
        runTest {
            // Act
            handler.handle(deepLinkRequest(model.queryParams))
            advanceUntilIdle()

            // Assert
            assertThat(requests).isEqualTo(listOfNotNull(model.expected))
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `GIVEN the opener availability WHEN the handler is checked THEN it is enabled the same way`(
        isAvailable: Boolean,
    ) {
        // Arrange
        every { opener.isAvailable } returns isAvailable

        // Act
        val actual = handler.isEnabled

        // Assert
        assertThat(actual).isEqualTo(isAvailable)
    }

    private fun TestScope.deepLinkRequest(queryParams: Map<String, String>) = DeepLinkRequest(
        uri = mockk<Uri>(),
        queryParams = queryParams,
        scope = this,
        isFromOnNewIntent = false,
        router = router,
    )

    private fun provideTestModels() = listOf(
        LinkModel(case = "no survey id", queryParams = emptyMap(), expected = null),
        LinkModel(case = "zero survey id", queryParams = mapOf("survey_id" to "0"), expected = null),
        LinkModel(case = "negative survey id", queryParams = mapOf("survey_id" to "-1"), expected = null),
        LinkModel(case = "survey id not a number", queryParams = mapOf("survey_id" to "abc"), expected = null),
        LinkModel(
            case = "too long survey id",
            queryParams = mapOf("survey_id" to "99999999999999999999"),
            expected = null,
        ),
        LinkModel(case = "survey id only", queryParams = mapOf("survey_id" to "283996"), expected = request()),
        LinkModel(
            case = "every parameter",
            queryParams = mapOf(
                "survey_id" to "283996",
                "stage" to "funded_30d",
                "repeat_after_days" to "180",
                "channel" to "push",
            ),
            expected = request(source = AppRoute.NativeSurvey.Source.PUSH, stage = "funded_30d", repeatAfterDays = 180),
        ),
        LinkModel(
            case = "banner channel",
            queryParams = mapOf("survey_id" to "283996", "channel" to "banner"),
            expected = request(source = AppRoute.NativeSurvey.Source.BANNER),
        ),
        LinkModel(
            case = "unknown channel",
            queryParams = mapOf("survey_id" to "283996", "channel" to "email"),
            expected = request(),
        ),
        LinkModel(
            case = "upper-case channel",
            queryParams = mapOf("survey_id" to "283996", "channel" to "PUSH"),
            expected = request(),
        ),
        LinkModel(
            case = "zero repeat days",
            queryParams = mapOf("survey_id" to "283996", "repeat_after_days" to "0"),
            expected = request(repeatAfterDays = 0),
        ),
        LinkModel(
            case = "negative repeat days",
            queryParams = mapOf("survey_id" to "283996", "repeat_after_days" to "-1"),
            expected = request(),
        ),
        LinkModel(
            case = "fractional repeat days",
            queryParams = mapOf("survey_id" to "283996", "repeat_after_days" to "1.5"),
            expected = request(),
        ),
    )

    private fun request(
        source: AppRoute.NativeSurvey.Source = AppRoute.NativeSurvey.Source.LINK,
        stage: String = "",
        repeatAfterDays: Int? = null,
    ) = NativeSurveyOpener.Request(
        surveyId = 283996L,
        source = source,
        stage = stage,
        repeatAfterDays = repeatAfterDays,
    )

    data class LinkModel(
        val case: String,
        val queryParams: Map<String, String>,
        val expected: NativeSurveyOpener.Request?,
    ) {
        override fun toString() = case
    }
}