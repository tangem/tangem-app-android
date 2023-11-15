package com.tangem.core.ui.components.rows.states

data class ActionRowState(
    val title: String,
    val description: String,
    val onClick: () -> Unit,
)