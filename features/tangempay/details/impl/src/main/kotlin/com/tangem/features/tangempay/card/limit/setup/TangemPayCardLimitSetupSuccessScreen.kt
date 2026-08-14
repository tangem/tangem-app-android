package com.tangem.features.tangempay.card.limit.setup

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.test.TangemPayTestTags
import com.tangem.features.tangempay.common.TangemPaySuccessScreenWrapper
import com.tangem.features.tangempay.details.impl.R

@Composable
internal fun TangemPayCardLimitSetupSuccessScreen(onDoneClick: () -> Unit, modifier: Modifier = Modifier) {
    TangemPaySuccessScreenWrapper(
        modifier = modifier,
        title = resourceReference(R.string.tangempay_card_page_daily_limit_success_title),
        subtitle = resourceReference(R.string.tangempay_card_page_daily_limit_success_description),
        buttonText = resourceReference(R.string.common_done),
        onButtonClick = onDoneClick,
        titleTestTag = TangemPayTestTags.DAILY_LIMIT_SUCCESS_TITLE,
        buttonTestTag = TangemPayTestTags.DAILY_LIMIT_DONE_BUTTON,
    )
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    TangemThemePreviewRedesign {
        TangemPayCardLimitSetupSuccessScreen(onDoneClick = {})
    }
}