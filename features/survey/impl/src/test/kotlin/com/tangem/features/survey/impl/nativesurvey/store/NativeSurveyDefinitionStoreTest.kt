package com.tangem.features.survey.impl.nativesurvey.store

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import com.tangem.features.survey.impl.nativesurvey.NativeSurveyTestData.SURVEY_ID
import com.tangem.features.survey.impl.nativesurvey.NativeSurveyTestData.questionsDto
import com.tangem.test.core.datastore.createAppDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/** Real DataStore files: a new store on the same directory stands for the next launch. */
internal class NativeSurveyDefinitionStoreTest {

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
    fun `GIVEN a saved definition WHEN read in the next launch THEN returns it with its language`() = runTest {
        // Arrange
        val saved = SavedSurveyDefinition(language = "en", questions = questionsDto())
        definitionStore().save(surveyId = SURVEY_ID, askedLanguage = "ko", definition = saved)
        startNextLaunch()

        // Act
        val actual = definitionStore().get(surveyId = SURVEY_ID, askedLanguage = "ko")

        // Assert
        assertThat(actual).isEqualTo(saved)
    }

    @Test
    fun `GIVEN a definition saved for another language WHEN read THEN returns null`() = runTest {
        // Arrange
        val store = definitionStore()
        store.save(SURVEY_ID, askedLanguage = "de", SavedSurveyDefinition(language = "de", questionsDto()))

        // Act
        val actual = store.get(surveyId = SURVEY_ID, askedLanguage = "fr")

        // Assert
        assertThat(actual).isNull()
    }

    @Test
    fun `GIVEN definitions of two surveys WHEN one removed THEN only the other remains`() = runTest {
        // Arrange
        val store = definitionStore()
        val saved = SavedSurveyDefinition(language = "de", questions = questionsDto())
        store.save(SURVEY_ID, askedLanguage = "de", saved)
        store.save(OTHER_SURVEY_ID, askedLanguage = "de", saved)

        // Act
        store.remove(SURVEY_ID)

        // Assert
        assertThat(store.get(SURVEY_ID, askedLanguage = "de")).isNull()
        assertThat(store.get(OTHER_SURVEY_ID, askedLanguage = "de")).isEqualTo(saved)
    }

    /** One file allows one live DataStore, so the previous launch's store has to be gone first. */
    private suspend fun startNextLaunch() {
        dataStoreScope.coroutineContext.job.cancelAndJoin()
        dataStoreScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    private fun definitionStore() = NativeSurveyDefinitionStore(
        dataStoreFactory = createAppDataStoreFactory(filesDir = directory, appScope = dataStoreScope),
        moshi = Moshi.Builder().build(),
    )

    private companion object {
        const val OTHER_SURVEY_ID = 283576L
    }
}