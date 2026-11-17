package com.tangem.features.survey.impl.nativesurvey.converter

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.tangem.features.survey.impl.nativesurvey.NativeSurveyTestData.questionsDto
import com.tangem.store.datasource.surveysparrow.models.SurveySparrowQuestionsDto
import org.junit.jupiter.api.Test

/** The NPS survey's questions exactly as the vendor returned them, read by the generated adapters. */
internal class SurveySparrowQuestionsJsonTest {

    private val adapter = Moshi.Builder().build().adapter(SurveySparrowQuestionsDto::class.java)

    @Test
    fun `GIVEN the vendor response for the NPS survey WHEN parsed THEN every read field is filled`() {
        // Act
        val actual = adapter.fromJson(resource(TEST_SURVEY_RESPONSE))

        // Assert
        assertThat(actual).isEqualTo(questionsDto())
    }

    private fun resource(path: String): String {
        return requireNotNull(javaClass.classLoader?.getResource(path)) { "Missing test resource $path" }.readText()
    }

    private companion object {
        const val TEST_SURVEY_RESPONSE = "surveysparrow/questions_283996.json"
    }
}