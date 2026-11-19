package com.tangem.features.survey.impl.nativesurvey.store

import androidx.datastore.core.DataStore
import com.squareup.moshi.Moshi
import com.tangem.core.remote.moshi.NetworkMoshi
import com.tangem.datasource.utils.AppDataStoreFactory
import com.tangem.datasource.utils.create
import com.tangem.store.datasource.surveysparrow.models.SurveySparrowQuestionsDto
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class NativeSurveyDefinitionStore @Inject constructor(
    dataStoreFactory: AppDataStoreFactory,
    @NetworkMoshi moshi: Moshi,
) {

    private val dataStore: DataStore<NativeSurveyDefinitions> =
        dataStoreFactory.create(defaultValue = NativeSurveyDefinitions(), fileName = FILE_NAME)

    private val questionsAdapter = moshi.adapter(SurveySparrowQuestionsDto::class.java)

    suspend fun get(surveyId: Long, askedLanguage: String): SavedSurveyDefinition? {
        val saved = runSuspendCatching { dataStore.data.first().definitions[key(surveyId, askedLanguage)] }
            .getOrElse { error ->
                TangemLogger.e("Native survey: failed to read saved definitions", error)
                null
            } ?: return null

        val questions = runCatching { questionsAdapter.fromJson(saved.json) }
            .onFailure { error -> TangemLogger.e("Native survey: saved definition is unreadable", error) }
            .getOrNull()
        return questions?.let { SavedSurveyDefinition(language = saved.language, questions = it) }
    }

    suspend fun save(surveyId: Long, askedLanguage: String, definition: SavedSurveyDefinition) {
        val saved = NativeSurveyDefinitions.Definition(
            language = definition.language,
            json = questionsAdapter.toJson(definition.questions),
        )
        runSuspendCatching {
            dataStore.updateData { it.copy(definitions = it.definitions + (key(surveyId, askedLanguage) to saved)) }
        }.onFailure { error -> TangemLogger.e("Native survey: failed to save the definition", error) }
    }

    suspend fun remove(surveyId: Long) {
        runSuspendCatching {
            dataStore.updateData { stored ->
                stored.copy(definitions = stored.definitions.filterKeys { !it.startsWith(keyPrefix(surveyId)) })
            }
        }.onFailure { error -> TangemLogger.e("Native survey: failed to remove the definition", error) }
    }

    private fun key(surveyId: Long, askedLanguage: String) = keyPrefix(surveyId) + askedLanguage

    private fun keyPrefix(surveyId: Long) = "$surveyId:"

    private companion object {
        const val FILE_NAME = "native_survey_definitions"
    }
}

internal data class SavedSurveyDefinition(val language: String, val questions: SurveySparrowQuestionsDto)

/** The vendor's questions as the DTO reads them, converted again on every read. */
@Serializable
internal data class NativeSurveyDefinitions(val definitions: Map<String, Definition> = emptyMap()) {

    @Serializable
    data class Definition(val language: String, val json: String)
}