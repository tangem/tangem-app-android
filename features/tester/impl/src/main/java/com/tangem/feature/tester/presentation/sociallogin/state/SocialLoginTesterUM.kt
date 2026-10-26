package com.tangem.feature.tester.presentation.sociallogin.state

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal data class SocialLoginTesterUM(
    val onBackClick: () -> Unit = {},
    val rows: ImmutableList<StatusRow> = persistentListOf(),
    val actions: ImmutableList<Action> = persistentListOf(),
    val runningAction: String? = null,
) {

    data class StatusRow(val label: String, val value: String)

    data class Action(val label: String, val onClick: () -> Unit)
}