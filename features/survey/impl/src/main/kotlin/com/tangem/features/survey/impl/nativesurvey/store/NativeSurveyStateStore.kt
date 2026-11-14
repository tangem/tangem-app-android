package com.tangem.features.survey.impl.nativesurvey.store

import androidx.datastore.core.DataStore
import com.tangem.datasource.utils.AppDataStoreFactory
import com.tangem.datasource.utils.create
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.days

/**
 * Kept apart from the saved definitions: a file that fails to parse is reset as a whole, and a broken definition
 * must not bring back a survey the user has already answered.
 */
@Singleton
internal class NativeSurveyStateStore @Inject constructor(dataStoreFactory: AppDataStoreFactory) {

    private val dataStore: DataStore<NativeSurveyState> =
        dataStoreFactory.create(defaultValue = NativeSurveyState(), fileName = FILE_NAME)

    suspend fun deviceRecord(surveyId: Long): SurveyDeviceRecord? {
        return runSuspendCatching { dataStore.data.first().device[surveyId.toString()] }
            .getOrElse { error ->
                TangemLogger.e("Native survey: failed to read the survey state", error)
                null
            }
    }

    suspend fun markAnswered(surveyId: Long, answeredAtMillis: Long, repeatAfterDays: Int?) {
        runSuspendCatching {
            dataStore.updateData { state ->
                val record = SurveyDeviceRecord(answeredAtMillis = answeredAtMillis, repeatAfterDays = repeatAfterDays)
                state.copy(device = state.device + (surveyId.toString() to record))
            }
        }.onFailure { error -> TangemLogger.e("Native survey: failed to save the answer", error) }
    }

    private companion object {
        const val FILE_NAME = "native_survey_state"
    }
}

/** @property device by survey id: an answer limits the survey on the device, whichever wallet gave it */
@Serializable
internal data class NativeSurveyState(val device: Map<String, SurveyDeviceRecord> = emptyMap())

/** A null [repeatAfterDays] is left out of the file under some DataStore JSON settings, hence the default. */
@Serializable
internal data class SurveyDeviceRecord(val answeredAtMillis: Long, val repeatAfterDays: Int? = null) {

    fun isAnswerActive(nowMillis: Long): Boolean {
        val days = repeatAfterDays ?: return true
        return nowMillis < answeredAtMillis + days.days.inWholeMilliseconds
    }
}