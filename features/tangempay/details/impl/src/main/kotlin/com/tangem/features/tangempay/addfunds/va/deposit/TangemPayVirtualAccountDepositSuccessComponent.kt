package com.tangem.features.tangempay.addfunds.va.deposit

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.test.TangemPayTestTags
import com.tangem.features.tangempay.common.TangemPayResultScreen
import com.tangem.features.tangempay.details.impl.R

/**

 * "Preparing your banking details". Close pops back to the previous screen.
 */
internal class TangemPayVirtualAccountDepositSuccessComponent(
    private val appComponentContext: AppComponentContext,
) : AppComponentContext by appComponentContext, ComposableContentComponent {

    @Composable
    override fun Content(modifier: Modifier) {
        BackHandler(onBack = ::onClose)
        TangemPayResultScreen(
            modifier = modifier,
            variant = TangemPayResultScreen.Variant.Success,
            title = resourceReference(R.string.tangempay_bank_transfer_success_title),
            subtitle = resourceReference(R.string.tangempay_bank_transfer_success_subtitle),
            buttonText = resourceReference(R.string.common_close),
            onButtonClick = ::onClose,
            titleTestTag = TangemPayTestTags.VA_DEPOSIT_SUCCESS_TITLE,
            buttonTestTag = TangemPayTestTags.VA_DEPOSIT_SUCCESS_CLOSE_BUTTON,
        )
    }

    private fun onClose() {
        router.pop()
    }
}