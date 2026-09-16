package com.tangem.features.tangempay.card.limit.setup

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.button.TangemButtonGroup
import com.tangem.core.ui.ds2.statusscreen.TangemStatusIcon
import com.tangem.core.ui.ds2.statusscreen.TangemStatusScreen
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.test.TangemPayTestTags
import com.tangem.features.tangempay.details.impl.R

@Composable
internal fun TangemPayCardLimitSetupSuccessScreen(onDoneClick: () -> Unit, modifier: Modifier = Modifier) {
    TangemStatusScreen(
        modifier = modifier,
        variant = TangemStatusScreen.Variant.Success,
        title = resourceReference(R.string.tangempay_card_page_daily_limit_success_title),
        description = resourceReference(R.string.tangempay_card_page_daily_limit_success_description),
        asset = {
            TangemStatusIcon(
                kind = TangemStatusIcon.Kind.Checkmark,
                modifier = Modifier.size(40.dp),
            )
        },
        footer = {
            TangemButtonGroup(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                TangemButton(
                    modifier = Modifier.testTag(TangemPayTestTags.DAILY_LIMIT_DONE_BUTTON),
                    variant = TangemButton.Variant.Primary,
                    size = TangemButton.Size.X12,
                    text = resourceReference(R.string.common_close),
                    onClick = onDoneClick,
                )
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun Preview() {
    TangemThemePreviewRedesign {
        TangemPayCardLimitSetupSuccessScreen(onDoneClick = {})
    }
}