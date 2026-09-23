package com.tangem.common.ui.cosign

import androidx.compose.runtime.Immutable

/** How many of the required cosign signatures have been collected. */
@Immutable
data class SignatureProgressUM(val collected: Int, val total: Int)

/** Signature-progress rendering (ring, avatar stack) is not specified in Figma beyond this many signers. */
internal const val MAX_COSIGN_SIGNERS = 5