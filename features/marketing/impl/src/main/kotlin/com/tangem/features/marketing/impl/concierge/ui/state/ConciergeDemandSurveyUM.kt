package com.tangem.features.marketing.impl.concierge.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class ConciergeDemandSurveyUM(
    val questions: ImmutableList<QuestionUM>,
    val isSubmitting: Boolean,
    val onSubmitClick: () -> Unit,
    val onDismiss: () -> Unit,
) {

    val isSubmitEnabled: Boolean
        get() = questions.all { question -> question.isOptional || question.selectedOptionKey != null }

    @Immutable
    data class QuestionUM(
        val key: String,
        val title: TextReference,
        val hint: TextReference?,
        val isOptional: Boolean,
        val options: ImmutableList<OptionUM>,
    ) {
        val selectedOptionKey: String?
            get() = options.firstOrNull(OptionUM::isSelected)?.key
    }

    @Immutable
    data class OptionUM(
        val key: String,
        val label: TextReference,
        val isSelected: Boolean,
        val onClick: () -> Unit,
    )
}