package com.tangem.features.tangempay.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.test.TangemPayTestTags
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.ui.components.TangemPaySuccessScreenWrapper

@Composable
internal fun TangemPayChangePinCodeSuccessScreenV2(onClose: () -> Unit, modifier: Modifier = Modifier) {
    TangemPaySuccessScreenWrapper(
        modifier = modifier,
        title = resourceReference(R.string.tangempay_card_details_change_pin_success_title),
        subtitle = resourceReference(R.string.tangempay_card_details_change_pin_success_description),
        buttonText = resourceReference(R.string.common_close),
        onButtonClick = onClose,
        titleTestTag = TangemPayTestTags.PIN_SUCCESS_TITLE,
        subtitleTestTag = TangemPayTestTags.PIN_SUCCESS_DESCRIPTION,
        buttonTestTag = TangemPayTestTags.PIN_DONE_BUTTON,
    )
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    TangemThemePreviewRedesign {
        TangemPayChangePinCodeSuccessScreenV2(onClose = {})
    }
}