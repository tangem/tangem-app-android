package com.tangem.features.tangempay.limit.setup

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.ui.components.TangemPaySuccessScreenWrapper

@Composable
internal fun TangemPayCardLimitSetupSuccessScreenV2(onDoneClick: () -> Unit, modifier: Modifier = Modifier) {
    TangemPaySuccessScreenWrapper(
        modifier = modifier,
        title = resourceReference(R.string.tangempay_card_page_daily_limit_success_title),
        subtitle = resourceReference(R.string.tangempay_card_page_daily_limit_success_description),
        buttonText = resourceReference(R.string.common_done),
        onButtonClick = onDoneClick,
    )
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    TangemThemePreviewRedesign {
        TangemPayCardLimitSetupSuccessScreenV2(onDoneClick = {})
    }
}