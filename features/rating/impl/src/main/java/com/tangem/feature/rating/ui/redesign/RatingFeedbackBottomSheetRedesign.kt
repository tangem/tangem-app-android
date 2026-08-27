package com.tangem.feature.rating.ui.redesign

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheet
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.feature.rating.ui.RatingFeedbackBS
import kotlinx.coroutines.delay

/**
 * Redesigned provider-rating feedback bottom sheet: the selected stars, a title, a free-form feedback
 * field and the "Send feedback" CTA. Opens on top of the transaction details sheet.
 *
 * [Figma](https://www.figma.com/design/Qqm0dNTOnqtxLYEcmgc32C/Store?node-id=2011-111400)
 */
@Composable
@Suppress("MagicNumber")
internal fun RatingFeedbackBottomSheetRedesign(config: TangemBottomSheetConfig) {
    TangemModalBottomSheet<RatingFeedbackBS>(
        config = config,
        containerColor = TangemTheme.colors3.bg.secondary,
        title = { content ->
            TangemButton.Close(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(16.dp),
                onClick = content.onDismiss,
            )
        },
        content = { content ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                RedesignStarRow(
                    selectedRating = content.selectedRating,
                    isEnabled = false,
                    onRatingSelect = {},
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = stringResourceSafe(R.string.swapping_rate_feedback_title),
                    style = TangemTheme.typography3.heading.medium,
                    color = TangemTheme.colors3.text.primary,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(40.dp))
                FeedbackTextField(
                    value = content.feedbackText,
                    onValueChange = content.onFeedbackChanged,
                )
                Spacer(modifier = Modifier.height(24.dp))
                TangemButton(
                    modifier = Modifier.fillMaxWidth(),
                    variant = TangemButton.Variant.Primary,
                    text = resourceReference(R.string.swapping_rate_feedback_submit),
                    isLoading = content.isSubmitting,
                    onClick = content.onSubmit,
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        },
    )
}

@Composable
@Suppress("MagicNumber")
private fun FeedbackTextField(value: String, onValueChange: (String) -> Unit) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        delay(FOCUS_REQUEST_DELAY_MS)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 104.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(TangemTheme.colors3.bg.opaque.primary)
            .focusRequester(focusRequester),
        textStyle = TangemTheme.typography3.body.medium.copy(color = TangemTheme.colors3.text.primary),
        cursorBrush = SolidColor(TangemTheme.colors3.text.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        maxLines = FEEDBACK_MAX_LINES,
        decorationBox = { innerTextField ->
            Box(modifier = Modifier.padding(16.dp)) {
                if (value.isEmpty()) {
                    Text(
                        text = stringResourceSafe(R.string.swapping_rate_feedback_placeholder),
                        style = TangemTheme.typography3.body.medium,
                        color = TangemTheme.colors3.text.tertiary,
                    )
                }
                innerTextField()
            }
        },
    )
}

private const val FOCUS_REQUEST_DELAY_MS = 300L
private const val FEEDBACK_MAX_LINES = 3

// region Preview

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Preview(showBackground = true, widthDp = 360, heightDp = 640, uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun RatingFeedbackBottomSheetRedesignPreview() {
    TangemThemePreviewRedesign(alwaysShowBottomSheets = true) {
        Box(modifier = Modifier.background(TangemTheme.colors3.bg.primary)) {
            RatingFeedbackBottomSheetRedesign(
                config = TangemBottomSheetConfig(
                    isShown = true,
                    onDismissRequest = {},
                    content = RatingFeedbackBS(
                        selectedRating = 5,
                        feedbackText = "",
                        isSubmitting = false,
                        onFeedbackChanged = {},
                        onDismiss = {},
                        onSubmit = {},
                    ),
                ),
            )
        }
    }
}

// endregion