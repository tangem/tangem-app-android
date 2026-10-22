package com.tangem.domain.walletconnect.model.sdkcopy

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * copy of [com.reown.android.Core.Model.AppMetaData]
 */
@Serializable
data class WcAppMetaData(
    val name: String,
    val description: String,
    val url: String,
    val icons: List<String>,
    val redirect: String?,
    val appLink: String? = null,
    @SerialName("linkMode") val isLinkMode: Boolean = false,
    val verifyUrl: String? = null,
)