package com.tangem.features.survey.impl.nativesurvey.store

import com.google.common.truth.Truth.assertThat
import com.tangem.features.survey.impl.nativesurvey.NativeSurveyTestData.SURVEY_ID
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.datastore.createAppDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import java.io.File
import kotlin.time.Duration.Companion.days

internal class NativeSurveyStateStoreTest {

    /** Real DataStore files: a new store on the same directory stands for the next launch. */
    @Nested
    inner class Store {

        private lateinit var dataStoreScope: CoroutineScope
        private lateinit var directory: File

        @BeforeEach
        fun setUp(@TempDir tempDir: File) {
            dataStoreScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            directory = tempDir
        }

        @AfterEach
        fun tearDown() {
            dataStoreScope.cancel()
        }

        @Test
        fun `GIVEN an answer WHEN read in the next launch THEN the record keeps its date and term`() = runTest {
            // Arrange
            stateStore().markAnswered(surveyId = SURVEY_ID, answeredAtMillis = ANSWERED_AT, repeatAfterDays = 180)
            startNextLaunch()

            // Act
            val actual = stateStore().deviceRecord(SURVEY_ID)

            // Assert
            val expected = SurveyDeviceRecord(answeredAtMillis = ANSWERED_AT, repeatAfterDays = 180)
            assertThat(actual).isEqualTo(expected)
        }

        @Test
        fun `GIVEN nothing answered WHEN read THEN there is no record`() = runTest {
            // Act
            val actual = stateStore().deviceRecord(SURVEY_ID)

            // Assert
            assertThat(actual).isNull()
        }

        /** One file allows one live DataStore, so the previous launch's store has to be gone first. */
        private suspend fun startNextLaunch() {
            dataStoreScope.coroutineContext.job.cancelAndJoin()
            dataStoreScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        }

        private fun stateStore() = NativeSurveyStateStore(
            dataStoreFactory = createAppDataStoreFactory(filesDir = directory, appScope = dataStoreScope),
        )
    }

    @Nested
    inner class FileFormat {

        @Test
        fun `GIVEN a record written without its null term WHEN read with explicit nulls THEN the record is kept`() {
            // Arrange
            val state = NativeSurveyState(mapOf(SURVEY_ID.toString() to SurveyDeviceRecord(ANSWERED_AT, null)))
            val written = Json { explicitNulls = false }.encodeToString(NativeSurveyState.serializer(), state)

            // Act
            val actual = Json.decodeFromString(NativeSurveyState.serializer(), written)

            // Assert
            assertThat(actual).isEqualTo(state)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class IsAnswerActive {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN an answer record WHEN checked at a moment THEN the answer is active within its term`(
            model: AnswerTermModel,
        ) {
            // Act
            val actual = model.record.isAnswerActive(model.nowMillis)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            AnswerTermModel("no term", SurveyDeviceRecord(ANSWERED_AT, null), ANSWERED_AT + daysMillis(1000), true),
            AnswerTermModel("within term", SurveyDeviceRecord(ANSWERED_AT, 30), ANSWERED_AT + daysMillis(30) - 1, true),
            AnswerTermModel("term ended", SurveyDeviceRecord(ANSWERED_AT, 30), ANSWERED_AT + daysMillis(30), false),
            AnswerTermModel("zero term", SurveyDeviceRecord(ANSWERED_AT, 0), ANSWERED_AT, false),
        )

        private fun daysMillis(days: Int) = days.days.inWholeMilliseconds
    }

    data class AnswerTermModel(
        val case: String,
        val record: SurveyDeviceRecord,
        val nowMillis: Long,
        val expected: Boolean,
    )

    private companion object {
        const val ANSWERED_AT = 1_794_000_000_000L
    }
}