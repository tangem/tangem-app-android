package com.tangem.feature.rating.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay
import com.tangem.core.ui.R
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.SpacerH12
import com.tangem.core.ui.components.SpacerH16
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.sheet.TangemBottomSheet
import com.tangem.core.ui.components.buttons.small.TangemIconButton
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme

@Composable
@Suppress("LongMethod")
internal fun RatingFeedbackBottomSheet(config: TangemBottomSheetConfig) {
    TangemBottomSheet<RatingFeedbackBS>(
        config = config,
        containerColor = TangemTheme.colors.background.secondary,
        addBottomInsets = false,
        title = { content ->
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = TangemTheme.dimens.spacing16,
                            vertical = TangemTheme.dimens.spacing8,
                        ),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TangemIconButton(
                        iconRes = R.drawable.ic_close_24,
                        onClick = content.onDismiss,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(TangemTheme.dimens.size56)
                        .clip(CircleShape)
                        .background(TangemTheme.colors.icon.attention.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_rating_star_24),
                        contentDescription = null,
                        tint = TangemTheme.colors.icon.attention,
                        modifier = Modifier.size(TangemTheme.dimens.size32),
                    )
                }
                SpacerH12()
                Text(
                    text = stringResourceSafe(R.string.swapping_rate_feedback_title),
                    style = TangemTheme.typography.h2,
                    color = TangemTheme.colors.text.primary1,
                    textAlign = TextAlign.Center,
                )
                SpacerH16()
            }
        },
        content = { content ->
            Column(
                modifier = Modifier
                    .padding(horizontal = TangemTheme.dimens.spacing16)
                    .padding(top = TangemTheme.dimens.spacing16)
                    .navigationBarsPadding(),
            ) {
                FeedbackTextField(
                    value = content.feedbackText,
                    onValueChange = content.onFeedbackChanged,
                )
                SpacerH16()
                PrimaryButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResourceSafe(R.string.swapping_rate_feedback_submit),
                    onClick = content.onSubmit,
                    showProgress = content.isSubmitting,
                )
                SpacerH16()
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedbackTextField(value: String, onValueChange: (String) -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        delay(FOCUS_REQUEST_DELAY_MS)
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    val fieldShape = RoundedCornerShape(TangemTheme.dimens.radius14)
    val colors = TextFieldDefaults.colors().copy(
        focusedContainerColor = TangemTheme.colors.field.focused,
        unfocusedContainerColor = TangemTheme.colors.field.focused,
        focusedTextColor = TangemTheme.colors.text.primary1,
        unfocusedTextColor = TangemTheme.colors.text.primary1,
        cursorColor = TangemTheme.colors.icon.primary1,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
    )

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = TangemTheme.dimens.size48)
            .focusRequester(focusRequester),
        textStyle = TangemTheme.typography.body1.copy(color = TangemTheme.colors.text.primary1),
        cursorBrush = SolidColor(TangemTheme.colors.icon.primary1),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        maxLines = 3,
        singleLine = false,
        minLines = 3,
        interactionSource = interactionSource,
        decorationBox = { innerTextField ->
            TextFieldDefaults.DecorationBox(
                value = value,
                innerTextField = innerTextField,
                enabled = true,
                singleLine = false,
                visualTransformation = VisualTransformation.None,
                interactionSource = interactionSource,
                shape = fieldShape,
                colors = colors,
                contentPadding = PaddingValues(
                    horizontal = TangemTheme.dimens.spacing16,
                    vertical = TangemTheme.dimens.spacing12,
                ),
                placeholder = {
                    Text(
                        text = stringResourceSafe(R.string.swapping_rate_feedback_placeholder),
                        style = TangemTheme.typography.body1,
                        color = TangemTheme.colors.text.tertiary,
                    )
                },
            )
        },
    )
}

private const val FOCUS_REQUEST_DELAY_MS = 300L