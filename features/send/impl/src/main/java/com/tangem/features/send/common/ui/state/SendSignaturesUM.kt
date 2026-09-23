package com.tangem.features.send.common.ui.state

/**
 * How many joint account signatures the operation collects before it executes
 *
 * @property isFirstOperation the Safe is not deployed yet, so this operation also deploys it
 */
internal data class SendSignaturesUM(
    val threshold: Int,
    val membersCount: Int,
    val isFirstOperation: Boolean = false,
)