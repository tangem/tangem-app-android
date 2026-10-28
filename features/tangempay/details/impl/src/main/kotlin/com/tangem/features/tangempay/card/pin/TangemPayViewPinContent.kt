package com.tangem.features.tangempay.card.pin

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetType
import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheetContent
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds.topbar.TangemTopBarType
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.loader.TangemLoader
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.security.SensitiveContentEffect
import com.tangem.core.ui.test.TangemPayTestTags
import com.tangem.features.tangempay.details.impl.R

@Composable
internal fun TangemPayViewPinContent(state: TangemPayViewPinUM) {
    TangemBottomSheet<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = state.onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        onBack = state.onDismiss,
        type = TangemBottomSheetType.Modal,
        containerColor = TangemTheme.colors3.bg.secondary,
        title = {
            TangemTopBar(
                type = TangemTopBarType.BottomSheet,
                endContent = {
                    TangemButton(
                        iconStart = TangemIconUM.Icon(iconRes = R.drawable.ic_close_24),
                        onClick = state.onDismiss,
                        size = TangemButton.Size.X11,
                        variant = TangemButton.Variant.Material,
                    )
                },
            )
        },
        content = {
            SensitiveContentEffect()
            AnimatedContent(
                targetState = state,
                contentKey = { um -> um::class.java },
                transitionSpec = { fadeIn() togetherWith fadeOut() },
            ) { animatedState ->
                when (animatedState) {
                    is TangemPayViewPinUM.Content -> {
                        PinSuccessContent(animatedState)
                    }
                    is TangemPayViewPinUM.Error -> {
                        MessageBottomSheetContent(animatedState.errorMessage)
                    }
                    is TangemPayViewPinUM.Loading -> {
                        PinLoadingContent()
                    }
                }
            }
        },
    )
}

@Composable
private fun PinSuccessContent(state: TangemPayViewPinUM.Content, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .padding(top = 48.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            modifier = Modifier.testTag(TangemPayTestTags.VIEW_PIN_TITLE),
            text = stringResourceSafe(R.string.tangempay_card_details_view_pin_code_title),
            style = TangemTheme.typography3.heading.small,
            color = TangemTheme.colors3.text.primary,
            textAlign = TextAlign.Center,
        )

        SpacerH(8.dp)

        Text(
            text = stringResourceSafe(R.string.tangempay_card_details_view_pin_code_description),
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.secondary,
            textAlign = TextAlign.Center,
        )

        SpacerH(32.dp)

        PinCode(value = state.pin)

        SpacerH(48.dp)

        TangemButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag(TangemPayTestTags.VIEW_PIN_CHANGE_BUTTON),
            size = TangemButton.Size.X12,
            text = resourceReference(R.string.tangempay_change_pin_code),
            onClick = state.onClickChangePin,
        )
    }
}

@Composable
private fun PinLoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .padding(top = 48.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            modifier = Modifier.testTag(TangemPayTestTags.VIEW_PIN_TITLE),
            text = stringResourceSafe(R.string.tangempay_card_details_view_pin_code_title),
            style = TangemTheme.typography3.heading.small,
            color = TangemTheme.colors3.text.primary,
            textAlign = TextAlign.Center,
        )

        SpacerH(8.dp)

        Text(
            text = stringResourceSafe(R.string.tangempay_card_details_view_pin_code_description),
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.secondary,
            textAlign = TextAlign.Center,
        )

        SpacerH(32.dp)

        TangemLoader(
            modifier = Modifier
                .padding(vertical = 20.dp)
                .testTag(TangemPayTestTags.VIEW_PIN_LOADER),
        )

        SpacerH(48.dp)

        TangemButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag(TangemPayTestTags.VIEW_PIN_CHANGE_BUTTON),
            size = TangemButton.Size.X12,
            text = resourceReference(R.string.tangempay_change_pin_code),
            onClick = {},
            isEnabled = false,
        )
    }
}

@Composable
private fun PinCode(value: String, modifier: Modifier = Modifier, numbersCount: Int = 4) {
    BasicTextField(
        enabled = false,
        value = value,
        onValueChange = {},
        modifier = modifier.testTag(TangemPayTestTags.VIEW_PIN_VALUE),
        textStyle = TangemTheme.typography3.heading.medium.copy(color = Transparent),
        cursorBrush = SolidColor(Transparent),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(numbersCount) { index ->
                        val digit = value.getOrNull(index)?.toString()
                        PinDigitBox(
                            modifier = Modifier.size(
                                width = 56.dp,
                                height = 64.dp,
                            ),
                            digit = digit,
                            backgroundColor = TangemTheme.colors3.bg.transparent.primary,
                            borderColor = TangemTheme.colors3.border.secondary,
                            textColor = TangemTheme.colors3.text.primary,
                            textStyle = TangemTheme.typography3.heading.medium,
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
    TangemThemePreviewRedesign {
        TangemPayViewPinContent(
            state = TangemPayViewPinUM.Content(
                pin = "1234",
                onClickChangePin = {},
                onDismiss = {},
            ),
        )
    }
}