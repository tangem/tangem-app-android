package com.tangem.features.survey.impl.nativesurvey.screen.ui.state

import androidx.compose.runtime.Immutable

@Immutable
internal sealed interface NativeSurveyUM {

    val onCloseClick: () -> Unit

    data class Loading(override val onCloseClick: () -> Unit) : NativeSurveyUM

    /** @property comment null until a score is chosen, and for a survey without an open question */
    data class Form(
        val body: BodyUM,
        val comment: CommentUM?,
        val isSubmitting: Boolean,
        val onSubmitClick: () -> Unit,
        override val onCloseClick: () -> Unit,
    ) : NativeSurveyUM {

        val isSubmitEnabled: Boolean
            get() = when (body) {
                is BodyUM.Score -> body.selectedScore != null
            }
    }

    data class Done(override val onCloseClick: () -> Unit) : NativeSurveyUM

    @Immutable
    sealed interface BodyUM {

        data class Score(val question: String, val selectedScore: Int?, val onScoreClick: (Int) -> Unit) : BodyUM
    }

    @Immutable
    data class CommentUM(val question: String, val value: String, val onValueChange: (String) -> Unit)
}