package com.tangem.features.tangempay.ui

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
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
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.TangemPayViewPinUM

@Composable
internal fun TangemPayViewPinContentV2(state: TangemPayViewPinUM) {
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
            .padding(horizontal = TangemTheme.dimens2.x4)
            .padding(top = TangemTheme.dimens2.x12)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResourceSafe(R.string.tangempay_card_details_view_pin_code_title),
            style = TangemTheme.typography3.heading.small,
            color = TangemTheme.colors3.text.primary,
            textAlign = TextAlign.Center,
        )

        SpacerH(TangemTheme.dimens2.x2)

        Text(
            text = stringResourceSafe(R.string.tangempay_card_details_view_pin_code_description),
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.secondary,
            textAlign = TextAlign.Center,
        )

        SpacerH(TangemTheme.dimens2.x8)

        PinCode(value = state.pin)

        SpacerH(TangemTheme.dimens2.x12)

        TangemButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = TangemTheme.dimens2.x4),
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
            .padding(horizontal = TangemTheme.dimens2.x4)
            .padding(top = TangemTheme.dimens2.x12)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResourceSafe(R.string.tangempay_card_details_view_pin_code_title),
            style = TangemTheme.typography3.heading.small,
            color = TangemTheme.colors3.text.primary,
            textAlign = TextAlign.Center,
        )

        SpacerH(TangemTheme.dimens2.x2)

        Text(
            text = stringResourceSafe(R.string.tangempay_card_details_view_pin_code_description),
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.secondary,
            textAlign = TextAlign.Center,
        )

        SpacerH(TangemTheme.dimens2.x8)

        TangemLoader(
            modifier = Modifier.padding(vertical = TangemTheme.dimens2.x5),
        )

        SpacerH(TangemTheme.dimens2.x12)

        TangemButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = TangemTheme.dimens2.x4),
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
        modifier = modifier,
        textStyle = TangemTheme.typography3.heading.medium.copy(color = Transparent),
        cursorBrush = SolidColor(Transparent),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x2),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(numbersCount) { index ->
                        val digit = value.getOrNull(index)?.toString()
                        PinDigitBox(
                            modifier = Modifier.size(
                                width = TangemTheme.dimens2.x14,
                                height = TangemTheme.dimens2.x16,
                            ),
                            digit = digit,
                            backgroundColor = TangemTheme.colors3.bg.opaque.primary,
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
        TangemPayViewPinContentV2(
            state = TangemPayViewPinUM.Content(
                pin = "1234",
                onClickChangePin = {},
                onDismiss = {},
            ),
        )
    }
}