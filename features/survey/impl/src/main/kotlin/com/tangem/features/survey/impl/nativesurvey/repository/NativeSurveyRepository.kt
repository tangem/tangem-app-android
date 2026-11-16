package com.tangem.features.survey.impl.nativesurvey.repository

import arrow.core.Either
import com.tangem.features.survey.impl.nativesurvey.model.NativeSurvey
import com.tangem.features.survey.impl.nativesurvey.model.SurveyAnswer
import com.tangem.features.survey.impl.nativesurvey.model.SurveyEntry

internal interface NativeSurveyRepository {

    /**
     * Without a SurveySparrow token every request fails, so native surveys stay off whatever their toggles say. Kept
     * out of the toggles on purpose: removing a released toggle must not remove this check.
     */
    val isAvailable: Boolean

    /** Null when the survey cannot be shown: the failure is already reported. */
    suspend fun load(entry: SurveyEntry): LoadedNativeSurvey?

    /**
     * On success closes the survey on the device for the entry's term. Outlives the caller, so an answer sent from a
     * closed screen is still recorded.
     *
     * @param responseUuid one per opening, repeated on its retries. The vendor has no idempotency key, so a retry
     *                     that did reach it makes a second response, told apart by this value
     */
    suspend fun submit(
        entry: SurveyEntry,
        survey: NativeSurvey,
        responseUuid: String,
        answers: List<SurveyAnswer>,
    ): Either<SurveySubmitError, Unit>

    /** Whether an answer given on this device is still within its term. */
    suspend fun isAnswered(entry: SurveyEntry): Boolean
}

internal data class LoadedNativeSurvey(val survey: NativeSurvey, val isFromCache: Boolean)

/** What the error alert leads to: back to the answers to send them again, or out of the survey. */
internal enum class SurveySubmitError { RETRY, CLOSE }