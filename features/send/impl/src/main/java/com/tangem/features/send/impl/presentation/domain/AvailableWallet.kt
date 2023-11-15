package com.tangem.features.send.impl.presentation.domain

import androidx.compose.runtime.Immutable

/**
 * Available wallet to send
 *
 * @property name wallet name
 * @property address blockchain address
 */
@Immutable
data class AvailableWallet(
    val name: String,
    val address: String,
)
