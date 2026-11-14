package com.tangem.features.survey.impl.nativesurvey.screen.ui

import android.content.res.Configuration
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.fields.SimpleTextField
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.ds2.loader.TangemLoader
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.scorescale.TangemScoreScale
import com.tangem.core.ui.ds2.statusscreen.TangemStatusIcon
import com.tangem.core.ui.ds2.statusscreen.TangemStatusScreen
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.survey.impl.nativesurvey.screen.ui.state.NativeSurveyUM
import com.tangem.features.survey.impl.nativesurvey.screen.ui.state.NativeSurveyUM.BodyUM

@Composable
internal fun NativeSurveyScreen(state: NativeSurveyUM, modifier: Modifier = Modifier) {
    when (state) {
        is NativeSurveyUM.Done -> DoneScreen(onCloseClick = state.onCloseClick, modifier = modifier)
        is NativeSurveyUM.Loading,
        is NativeSurveyUM.Form,
        -> FormScreen(state = state, modifier = modifier)
    }
}

@Composable
private fun FormScreen(state: NativeSurveyUM, modifier: Modifier = Modifier) {
    val form = state as? NativeSurveyUM.Form
    var footerHeightPx by remember { mutableIntStateOf(0) }
    TangemTopBarScaffold(
        modifier = modifier,
        topBar = { TangemTopNavigation(endButton = { TangemButton.Close(onClick = state.onCloseClick) }) },
        overlay = {
            if (form != null) {
                FormFooter(
                    form = form,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .imePadding()
                        .onSizeChanged { size -> footerHeightPx = size.height },
                )
            }
        },
    ) { contentPadding ->
        if (form == null) {
            TangemLoader(modifier = Modifier.align(Alignment.Center))
        } else {
            val footerHeight = with(LocalDensity.current) { footerHeightPx.toDp() }
            FormContent(
                form = form,
                topPadding = contentPadding.calculateTopPadding(),
                // Not a sum: the footer already includes the navigation bar inset
                bottomPadding = maxOf(contentPadding.calculateBottomPadding(), footerHeight),
            )
        }
    }
}

@Composable
private fun FormContent(form: NativeSurveyUM.Form, topPadding: Dp, bottomPadding: Dp) {
    val focusManager = LocalFocusManager.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = topPadding + 12.dp, bottom = bottomPadding + 12.dp),
        verticalArrangement = Arrangement.spacedBy(52.dp),
    ) {
        when (val body = form.body) {
            is BodyUM.Score -> ScoreBody(body = body)
        }
        form.comment?.let { comment -> CommentField(comment = comment, isReadOnly = form.isSubmitting) }
    }
}

@Composable
private fun ScoreBody(body: BodyUM.Score) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = body.question,
            style = TangemTheme.typography3.heading.medium,
            color = TangemTheme.colors3.text.primary,
        )
        Text(
            text = stringResourceSafe(R.string.native_survey_nps_scale_hint),
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.secondary,
        )
        TangemScoreScale(
            selectedScore = body.selectedScore,
            onScoreClick = body.onScoreClick,
            modifier = Modifier.padding(top = 32.dp),
        )
    }
}

@Composable
private fun CommentField(comment: NativeSurveyUM.CommentUM, isReadOnly: Boolean) {
    val optional = stringResourceSafe(R.string.native_survey_optional)
    val optionalColor = TangemTheme.colors3.text.tertiary
    var isFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    // Otherwise a failed send lifts readOnly on a focused field and the keyboard reopens over the alert
    LaunchedEffect(isReadOnly) {
        if (isReadOnly) focusManager.clearFocus()
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = buildAnnotatedString {
                append(comment.question)
                withStyle(SpanStyle(color = optionalColor)) { append(" • $optional") }
            },
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
        )
        SimpleTextField(
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState -> isFocused = focusState.isFocused },
            value = comment.value,
            onValueChange = comment.onValueChange,
            placeholder = resourceReference(R.string.native_survey_comment_placeholder),
            textStyle = TangemTheme.typography3.body.medium,
            readOnly = isReadOnly,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )
        HorizontalDivider(
            modifier = Modifier.padding(top = 8.dp),
            color = if (isFocused) TangemTheme.colors3.border.brand else TangemTheme.colors3.border.tertiary,
        )
    }
}

@Composable
private fun FormFooter(form: NativeSurveyUM.Form, modifier: Modifier = Modifier) {
    TangemFooter(modifier = modifier) {
        TangemButton(
            modifier = Modifier.fillMaxWidth(),
            variant = TangemButton.Variant.Primary,
            size = TangemButton.Size.X12,
            text = resourceReference(R.string.native_survey_send_feedback),
            isEnabled = form.isSubmitEnabled,
            isLoading = form.isSubmitting,
            onClick = form.onSubmitClick,
        )
    }
}

@Composable
private fun DoneScreen(onCloseClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        TangemStatusScreen(
            variant = TangemStatusScreen.Variant.Success,
            title = resourceReference(R.string.native_survey_done_title),
            description = resourceReference(R.string.native_survey_done_description),
            asset = { TangemStatusIcon(kind = TangemStatusIcon.Kind.Checkmark, modifier = Modifier.size(40.dp)) },
            footer = {
                TangemButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    variant = TangemButton.Variant.Primary,
                    size = TangemButton.Size.X12,
                    text = resourceReference(R.string.common_done),
                    onClick = onCloseClick,
                )
            },
        )
        TangemTopNavigation(
            modifier = Modifier.align(Alignment.TopCenter),
            endButton = { TangemButton.Close(onClick = onCloseClick) },
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Preview(showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(showBackground = true, widthDp = 320, heightDp = 800, fontScale = 1.5f)
@Composable
private fun Preview_NativeSurveyScreen(@PreviewParameter(NativeSurveyStateProvider::class) state: NativeSurveyUM) {
    TangemThemePreviewRedesign {
        NativeSurveyScreen(state = state)
    }
}

private class NativeSurveyStateProvider : CollectionPreviewParameterProvider<NativeSurveyUM>(
    collection = listOf(
        previewForm(selectedScore = null, comment = null),
        previewForm(selectedScore = 8, comment = "Add more networks"),
        previewForm(selectedScore = 8, comment = "Add more networks", isSubmitting = true),
        NativeSurveyUM.Done(onCloseClick = {}),
    ),
)

private fun previewForm(selectedScore: Int?, comment: String?, isSubmitting: Boolean = false) = NativeSurveyUM.Form(
    body = BodyUM.Score(
        question = "How likely are you to recommend Tangem?",
        selectedScore = selectedScore,
        onScoreClick = {},
    ),
    comment = comment?.let {
        NativeSurveyUM.CommentUM(question = "What should we improve?", value = it, onValueChange = {})
    },
    isSubmitting = isSubmitting,
    onSubmitClick = {},
    onCloseClick = {},
)