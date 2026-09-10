package com.tangem.features.send.common.ui.state

/** How many joint account signatures the operation collects before it executes */
internal data class SendSignaturesUM(
    val threshold: Int,
    val membersCount: Int,
)