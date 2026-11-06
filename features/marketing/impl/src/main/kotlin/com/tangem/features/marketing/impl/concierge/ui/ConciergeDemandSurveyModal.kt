package com.tangem.features.marketing.impl.concierge.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.core.ui.ds2.modal.TopNavigation
import com.tangem.core.ui.ds2.modalstatus.TangemModalStatus
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItem
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageEffect
import com.tangem.core.ui.message.EventMessageHandler
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.marketing.impl.concierge.model.ConciergeDemandSurveyQuestion
import com.tangem.features.marketing.impl.concierge.ui.state.ConciergeDemandSurveyUM
import kotlinx.collections.immutable.toImmutableList

/**
 * The one modal of the demand survey. Its steps — the survey and, once the answers are sent, "You're all set" — are
 * the [content], so moving between them keeps the sheet on screen. The modal is shown for as long as it stays in
 * composition, so the caller opens and closes the flow by adding it to and removing it from composition.
 *
 * [Figma](https://www.figma.com/design/GhMZiR8xGeGSmaLinuE5qq/?node-id=1590-150976)
 */
@Composable
internal fun ConciergeDemandSurveyModal(
    state: ConciergeDemandSurveyUM,
    isSuccessStep: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    TangemModal<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = state.onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        onBack = state.onDismiss.takeUnless { state.isSubmitting },
        dismissOnClickOutside = !state.isSubmitting,
        scrollableContent = false,
        showDragIndicator = !isSuccessStep,
        title = {
            if (isSuccessStep) {
                TangemModal.TopNavigation(endButton = { TangemButton.Close(onClick = state.onDismiss) })
            } else {
                TangemModal.TopNavigation(
                    title = resourceReference(R.string.survey_demand_navigation_title),
                    onClose = state.onDismiss,
                )
            }
        },
        content = { content() },
    )
}

@Composable
internal fun ConciergeDemandSurveyContent(state: ConciergeDemandSurveyUM, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Column(
            modifier = Modifier
                .weight(weight = 1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 4.dp, bottom = 16.dp),
        ) {
            Header()
            state.questions.forEachIndexed { index, question ->
                Question(
                    question = question,
                    modifier = Modifier.padding(top = if (index == 0) 12.dp else 20.dp),
                )
            }
        }

        TangemFooter(
            modifier = Modifier.fillMaxWidth(),
            windowInsets = TangemFooter.ModalWindowInsets,
            extraTop = {
                Text(
                    text = stringResourceSafe(R.string.survey_demand_footer),
                    style = TangemTheme.typography3.caption.medium,
                    color = TangemTheme.colors3.text.secondary,
                    textAlign = TextAlign.Center,
                )
            },
        ) {
            TangemButton(
                modifier = Modifier.fillMaxWidth(),
                variant = TangemButton.Variant.Primary,
                size = TangemButton.Size.X12,
                text = resourceReference(R.string.survey_demand_button),
                isEnabled = state.isSubmitEnabled,
                isLoading = state.isSubmitting,
                onClick = state.onSubmitClick,
            )
        }
    }
}

@Composable
private fun Header(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            modifier = Modifier
                .padding(vertical = 12.dp)
                .semantics { heading() },
            text = stringResourceSafe(R.string.survey_demand_title),
            style = TangemTheme.typography3.heading.small,
            color = TangemTheme.colors3.text.primary,
        )
        Text(
            modifier = Modifier.padding(bottom = 12.dp),
            text = stringResourceSafe(R.string.survey_demand_subtitle),
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.secondary,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Question(question: ConciergeDemandSurveyUM.QuestionUM, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Column(
            modifier = Modifier.padding(bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    modifier = Modifier.weight(weight = 1f, fill = false),
                    text = question.title.resolveReference(),
                    style = TangemTheme.typography3.body.medium,
                    color = TangemTheme.colors3.text.primary,
                )
                if (question.isOptional) {
                    Text(
                        text = stringResourceSafe(R.string.survey_demand_optional),
                        style = TangemTheme.typography3.caption.medium,
                        color = TangemTheme.colors3.text.tertiary,
                    )
                }
            }
            question.hint?.let { hint ->
                Text(
                    text = hint.resolveReference(),
                    style = TangemTheme.typography3.subheading.medium,
                    color = TangemTheme.colors3.text.secondary,
                )
            }
        }
        FlowRow(
            modifier = Modifier
                .padding(vertical = 12.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            question.options.forEach { option ->
                TangemTabItem(
                    state = TangemTabItemUM.Content(
                        id = option.key,
                        label = option.label,
                        isSelected = option.isSelected,
                        onClick = option.onClick,
                    ),
                )
            }
        }
    }
}

@Composable
internal fun ConciergeDemandSurveySuccessContent(onDoneClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        TangemModalStatus(
            modifier = Modifier
                .weight(weight = 1f, fill = false)
                .verticalScroll(rememberScrollState()),
            variant = TangemModalStatus.Variant.Success,
            title = resourceReference(R.string.survey_demand_success_title),
            description = resourceReference(R.string.survey_demand_success_description),
        )
        TangemFooter(
            modifier = Modifier.fillMaxWidth(),
            windowInsets = TangemFooter.ModalWindowInsets,
            fadeEnabled = false,
        ) {
            TangemButton(
                modifier = Modifier.fillMaxWidth(),
                variant = TangemButton.Variant.Primary,
                size = TangemButton.Size.X12,
                text = resourceReference(R.string.common_done),
                onClick = onDoneClick,
            )
        }
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Preview(showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ConciergeDemandSurveyEmptyPreview() {
    TangemThemePreviewRedesign {
        PreviewSurveyStep(state = previewState(answers = emptyMap()))
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Preview(showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ConciergeDemandSurveyAnsweredPreview() {
    TangemThemePreviewRedesign {
        PreviewSurveyStep(state = previewState(answers = PreviewRequiredAnswers))
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Preview(showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ConciergeDemandSurveyErrorPreview() {
    TangemThemePreviewRedesign {
        val messageHandler = remember {
            EventMessageHandler().apply {
                handleMessage(
                    DialogMessage(
                        title = resourceReference(R.string.common_something_went_wrong),
                        message = resourceReference(R.string.common_unknown_error),
                    ),
                )
            }
        }
        PreviewSurveyStep(state = previewState(answers = PreviewRequiredAnswers))
        EventMessageEffect(messageHandler = messageHandler)
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Preview(showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ConciergeDemandSurveySuccessPreview() {
    TangemThemePreviewRedesign {
        ConciergeDemandSurveyModal(state = previewState(answers = PreviewRequiredAnswers), isSuccessStep = true) {
            ConciergeDemandSurveySuccessContent(onDoneClick = {})
        }
    }
}

private val PreviewRequiredAnswers = mapOf(
    ConciergeDemandSurveyQuestion.GOAL to "buy",
    ConciergeDemandSurveyQuestion.TRANSACTION_SIZE to "500k_1m",
    ConciergeDemandSurveyQuestion.FREQUENCY to "few_times_month",
    ConciergeDemandSurveyQuestion.NEEDED_CURRENCY to "usd",
)

@Composable
private fun PreviewSurveyStep(state: ConciergeDemandSurveyUM) {
    ConciergeDemandSurveyModal(state = state, isSuccessStep = false) {
        ConciergeDemandSurveyContent(state = state, modifier = Modifier.weight(weight = 1f, fill = false))
    }
}

private fun previewState(answers: Map<ConciergeDemandSurveyQuestion, String>) = ConciergeDemandSurveyUM(
    questions = ConciergeDemandSurveyQuestion.entries.map { question ->
        ConciergeDemandSurveyUM.QuestionUM(
            key = question.key,
            title = question.title,
            hint = question.hint,
            isOptional = question.isOptional,
            options = question.options.map { option ->
                ConciergeDemandSurveyUM.OptionUM(
                    key = option.key,
                    label = option.label,
                    isSelected = answers[question] == option.key,
                    onClick = {},
                )
            }.toImmutableList(),
        )
    }.toImmutableList(),
    isSubmitting = false,
    onSubmitClick = {},
    onDismiss = {},
)
// endregion