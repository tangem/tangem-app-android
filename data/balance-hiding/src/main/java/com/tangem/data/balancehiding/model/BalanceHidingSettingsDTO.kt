package com.tangem.data.balancehiding.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// Keys must match BalanceHidingSettings, stored by reflection before
@JsonClass(generateAdapter = true)
internal data class BalanceHidingSettingsDTO(
    @Json(name = "isHidingEnabledInSettings")
    val isHidingEnabledInSettings: Boolean,
    @Json(name = "isBalanceHidden")
    val isBalanceHidden: Boolean,
    @Json(name = "isBalanceHidingNotificationEnabled")
    val isBalanceHidingNotificationEnabled: Boolean,
    @Json(name = "isUpdateFromToast")
    val isUpdateFromToast: Boolean = false,
)