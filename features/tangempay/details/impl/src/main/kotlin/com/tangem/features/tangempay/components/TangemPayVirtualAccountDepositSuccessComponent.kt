package com.tangem.features.tangempay.components

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.ui.components.TangemPaySuccessScreenWrapper

/**

 * "Preparing your banking details". Close pops back to the previous screen.
 */
internal class TangemPayVirtualAccountDepositSuccessComponent(
    private val appComponentContext: AppComponentContext,
) : AppComponentContext by appComponentContext, ComposableContentComponent {

    @Composable
    override fun Content(modifier: Modifier) {
        BackHandler(onBack = ::onClose)
        TangemPaySuccessScreenWrapper(
            modifier = modifier,
            title = resourceReference(R.string.tangempay_bank_transfer_success_title),
            subtitle = resourceReference(R.string.tangempay_bank_transfer_success_subtitle),
            buttonText = resourceReference(R.string.common_close),
            onButtonClick = ::onClose,
        )
    }

    private fun onClose() {
        router.pop()
    }
}