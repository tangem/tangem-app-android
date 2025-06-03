package com.tangem.features.onramp.success.entity.previewdata

import com.tangem.common.ui.expressStatus.state.ExpressLinkUM
import com.tangem.common.ui.expressStatus.state.ExpressStatusItemState
import com.tangem.common.ui.expressStatus.state.ExpressStatusItemUM
import com.tangem.common.ui.expressStatus.state.ExpressStatusUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.onramp.model.OnrampStatus
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.success.entity.OnrampSuccessComponentUM
import kotlinx.collections.immutable.persistentListOf
import org.joda.time.DateTime

internal data object OnrampSuccessComponentUMPreviewData {

    val loadingState = OnrampSuccessComponentUM.Loading

    val contentState = OnrampSuccessComponentUM.Content(
        txId = "b2894851-7b63-4f56-bd75-e408f1dcba31",
        timestamp = DateTime.parse("2023-09-20T13:45:10.868Z").millis,
        providerName = stringReference("1Inch"),
        providerImageUrl = "https://s3.eu-central-1.amazonaws.com/tangem.api/express/1INCH1024.png",
        fromAmount = stringReference("100 USDT"),
        toAmount = stringReference("99.99 $"),
        currencyImageUrl = "",
        notification = null,
        activeStatus = OnrampStatus.Status.Verifying,
        statusBlock = ExpressStatusUM(
            title = resourceReference(R.string.express_exchange_status_title),
            link = ExpressLinkUM.Empty,
            statuses = persistentListOf(
                ExpressStatusItemUM(
                    text = stringReference("Deposit received"),
                    state = ExpressStatusItemState.Done,
                ),
                ExpressStatusItemUM(
                    text = stringReference("Confirmed"),
                    state = ExpressStatusItemState.Done,
                ),
                ExpressStatusItemUM(
                    text = stringReference("Buying Bitcoin..."),
                    state = ExpressStatusItemState.Active,
                ),
                ExpressStatusItemUM(
                    text = stringReference("Sending to you"),
                    state = ExpressStatusItemState.Default,
                ),
            ),
        ),
    )
}