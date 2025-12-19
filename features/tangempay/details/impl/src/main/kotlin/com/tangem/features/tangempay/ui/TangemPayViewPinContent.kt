package com.tangem.features.tangempay.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.components.SpacerH12
import com.tangem.core.ui.components.SpacerH24
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheetV2Content
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheet
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.TangemPayViewPinUM

@Composable
internal fun TangemPayViewPinContent(state: TangemPayViewPinUM) {
    TangemModalBottomSheet<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = state.onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        onBack = state.onDismiss,
        containerColor = TangemTheme.colors.background.tertiary,
        title = {
            TangemModalBottomSheetTitle(
                title = TextReference.EMPTY,
                endIconRes = R.drawable.ic_close_24,
                onEndClick = state.onDismiss,
            )
        },
    ) {
        when (state) {
            is TangemPayViewPinUM.Content -> {
                PinSuccessContent(state)
            }
            is TangemPayViewPinUM.Error -> {
                MessageBottomSheetV2Content(state.errorMessage)
            }
            is TangemPayViewPinUM.Loading -> {
                PinLoadingContent()
            }
        }
    }
}

@Composable
private fun PinSuccessContent(state: TangemPayViewPinUM.Content, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResourceSafe(R.string.tangempay_card_details_view_pin_code_title),
            style = TangemTheme.typography.h2,
            color = TangemTheme.colors.text.primary1,
            textAlign = TextAlign.Center,
        )

        SpacerH12()

        Text(
            text = stringResourceSafe(R.string.tangempay_card_details_view_pin_code_description),
            style = TangemTheme.typography.body1,
            color = TangemTheme.colors.text.secondary,
            textAlign = TextAlign.Center,
        )

        SpacerH24()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center,
        ) {
            PinCode(
                modifier = Modifier
                    .align(Alignment.TopCenter),
                value = state.pin,
            )

            SecondaryButton(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .imePadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    .fillMaxWidth(),
                text = stringResourceSafe(R.string.tangempay_change_pin_code),
                onClick = state.onClickChangePin,
            )
        }
    }
}

@Composable
private fun PinLoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResourceSafe(R.string.tangempay_card_details_view_pin_code_title),
            style = TangemTheme.typography.h2,
            color = TangemTheme.colors.text.primary1,
            textAlign = TextAlign.Center,
        )

        SpacerH12()

        Text(
            text = stringResourceSafe(R.string.tangempay_card_details_view_pin_code_description),
            style = TangemTheme.typography.body1,
            color = TangemTheme.colors.text.secondary,
            textAlign = TextAlign.Center,
        )

        SpacerH24()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                color = TangemTheme.colors.text.disabled,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .size(TangemTheme.dimens.size24),
            )
        }
    }
}

@Composable
private fun PinCode(value: String, modifier: Modifier = Modifier, numbersCount: Int = 4) {
    BasicTextField(
        value = value,
        onValueChange = {},
        modifier = modifier,
        textStyle = TangemTheme.typography.h1.copy(color = Transparent),
        cursorBrush = SolidColor(Transparent),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(numbersCount) { index ->
                        val digit = value.getOrNull(index)?.toString()
                        PinDigitBox(
                            digit = digit,
                            backgroundColor = TangemTheme.colors.background.action,
                            borderColor = TangemTheme.colors.stroke.primary,
                            textColor = TangemTheme.colors.text.primary1,
                            textStyle = TangemTheme.typography.h1,
                        )
                    }
                }
                innerTextField()
            }
        },
    )
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayViewPinContentPreview() {
    TangemThemePreview {
        TangemPayViewPinContent(
            state = TangemPayViewPinUM.Content(
                pin = "1234",
                onClickChangePin = {},
                onDismiss = {},
            ),
        )
    }
}