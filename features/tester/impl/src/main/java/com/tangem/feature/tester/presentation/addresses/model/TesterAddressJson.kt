package com.tangem.feature.tester.presentation.addresses.model

import kotlinx.serialization.Serializable

@Serializable
data class TesterAddressJson(
    val addresses: List<String>,
    val blockchain: String,
    val derivationPath: String,
    val token: String? = null,
)