package com.tangem.features.onramp.swap.availablepairs.entity.transformers

import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.feature.swap.domain.models.ExpressException
import com.tangem.features.onramp.tokenlist.entity.TokenListUM
import com.tangem.features.onramp.tokenlist.entity.TokenListUMTransformer
import kotlinx.collections.immutable.persistentListOf

/**
[REDACTED_AUTHOR]
 */
internal class SetErrorWarningTransformer(
    private val cause: Throwable,
    private val onRefresh: () -> Unit,
) : TokenListUMTransformer {

    override fun transform(prevState: TokenListUM): TokenListUM {
        return prevState.copy(
            availableItems = persistentListOf(),
            unavailableItems = persistentListOf(),
            warning = NotificationUM.Warning.OnrampErrorNotification(
                errorCode = (cause as? ExpressException)?.expressDataError?.code?.toString(),
                onRefresh = onRefresh,
            ),
        )
    }
}