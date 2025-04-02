package com.tangem.features.send.v2.subcomponents.fee.model.transformers

import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.common.ui.notifications.NotificationsFactory.addFeeUnreachableNotification
import com.tangem.core.ui.utils.parseToBigDecimal
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeSelectorUM
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeType
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeUM
import com.tangem.utils.extensions.isZero
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal class SendFeeNotificationsTransformer(
    private val cryptoCurrencyName: String,
    private val onFeeReload: () -> Unit,
) : Transformer<FeeUM> {

    override fun transform(prevState: FeeUM): FeeUM {
        val state = prevState as? FeeUM.Content ?: return prevState

        val notifications = state.getNotifications()
        return state.copy(
            notifications = notifications,
            isPrimaryButtonEnabled = state.isPrimaryButtonEnabled(notifications),
        )
    }

    private fun FeeUM.Content.getNotifications() = buildList {
        addFeeUnreachableNotification(
            feeError = (feeSelectorUM as? FeeSelectorUM.Error)?.error,
            tokenName = cryptoCurrencyName,
            onReload = onFeeReload,
        )
    }.toImmutableList()

    private fun FeeUM.Content.isPrimaryButtonEnabled(notifications: ImmutableList<NotificationUM>): Boolean {
        val feeSelectorState = feeSelectorUM as? FeeSelectorUM.Content ?: return false
        val customValue = feeSelectorState.customValues.firstOrNull()

        val isNotCustom = feeSelectorState.selectedType != FeeType.Custom
        val isNotEmptyCustom = if (customValue != null) {
            !customValue.value.parseToBigDecimal(customValue.decimals).isZero() && !isNotCustom
        } else {
            false
        }
        val noErrors = notifications.none { it is NotificationUM.Error }

        return noErrors && (isNotEmptyCustom || isNotCustom)
    }
}