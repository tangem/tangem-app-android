package com.tangem.data.balancehiding.converter

import com.tangem.data.balancehiding.model.BalanceHidingSettingsDTO
import com.tangem.domain.balancehiding.BalanceHidingSettings
import com.tangem.utils.converter.TwoWayConverter

internal object BalanceHidingSettingsConverter : TwoWayConverter<BalanceHidingSettings, BalanceHidingSettingsDTO> {

    override fun convert(value: BalanceHidingSettings): BalanceHidingSettingsDTO {
        return BalanceHidingSettingsDTO(
            isHidingEnabledInSettings = value.isHidingEnabledInSettings,
            isBalanceHidden = value.isBalanceHidden,
            isBalanceHidingNotificationEnabled = value.isBalanceHidingNotificationEnabled,
            isUpdateFromToast = value.isUpdateFromToast,
        )
    }

    override fun convertBack(value: BalanceHidingSettingsDTO): BalanceHidingSettings {
        return BalanceHidingSettings(
            isHidingEnabledInSettings = value.isHidingEnabledInSettings,
            isBalanceHidden = value.isBalanceHidden,
            isBalanceHidingNotificationEnabled = value.isBalanceHidingNotificationEnabled,
            isUpdateFromToast = value.isUpdateFromToast,
        )
    }
}