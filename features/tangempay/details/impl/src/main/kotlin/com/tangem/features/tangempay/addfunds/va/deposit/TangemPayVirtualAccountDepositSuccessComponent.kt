package com.tangem.features.tangempay.addfunds.va.deposit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.button.TangemButtonGroup
import com.tangem.core.ui.ds2.statusscreen.TangemStatusIcon
import com.tangem.core.ui.ds2.statusscreen.TangemStatusScreen
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.test.TangemPayTestTags
import com.tangem.features.tangempay.details.impl.R

internal class TangemPayVirtualAccountDepositSuccessComponent(
    private val appComponentContext: AppComponentContext,
) : AppComponentContext by appComponentContext, ComposableContentComponent {

    @Composable
    override fun Content(modifier: Modifier) {
        BackHandler(onBack = ::onClose)
        TangemStatusScreen(
            modifier = modifier,
            variant = TangemStatusScreen.Variant.Success,
            title = resourceReference(R.string.tangempay_bank_transfer_success_title),
            description = resourceReference(R.string.tangempay_bank_transfer_success_subtitle),
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
                        modifier = Modifier.testTag(TangemPayTestTags.VA_DEPOSIT_SUCCESS_CLOSE_BUTTON),
                        variant = TangemButton.Variant.Primary,
                        size = TangemButton.Size.X12,
                        text = resourceReference(R.string.common_close),
                        onClick = ::onClose,
                    )
                }
            },
        )
    }

    private fun onClose() {
        router.pop()
    }
}