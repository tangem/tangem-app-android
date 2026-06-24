package com.tangem.features.addressbook.list.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.account.AccountIconUM

@Immutable
internal data class ContactUM(
    val id: String,
    val name: String,
    val icon: AccountIconUM.CryptoPortfolio,
    val networkAddressCount: Int,
    val onClick: () -> Unit,
)