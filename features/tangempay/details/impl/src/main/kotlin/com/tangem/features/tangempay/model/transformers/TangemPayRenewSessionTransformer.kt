package com.tangem.features.tangempay.model.transformers

import com.tangem.core.ui.components.notifications.NotificationConfig
import com.tangem.features.tangempay.entity.TangemPayDetailsUM
import com.tangem.utils.transformer.Transformer

internal class TangemPayRenewSessionTransformer(
    private val shouldShowProgress: Boolean,
) : Transformer<TangemPayDetailsUM> {

    override fun transform(prevState: TangemPayDetailsUM): TangemPayDetailsUM {
        val config = prevState.errorNotificationConfig ?: return prevState
        val button = config.buttonsState as? NotificationConfig.ButtonsState.SecondaryButtonConfig
            ?: return prevState

        return prevState.copy(
            errorNotificationConfig = config.copy(
                buttonsState = button.copy(shouldShowProgress = shouldShowProgress),
            ),
        )
    }
}