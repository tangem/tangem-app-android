package com.tangem.features.txhistory.model

import com.tangem.core.ui.ds.image.DeviceIconUM
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Per-page lookup context for the tx-history converter.
 *
 *  - [ownAccountByAddress] / [walletInfoById] — address-keyed lookups for resolving counterparty owners
 *    in transfer subtitles ("to / from MY account / wallet").
 *  - [isAccountsModeEnabled] — toggles whether a resolved owner is rendered as account or wallet.
 */
internal data class TxHistoryLookupContext(
    val ownAccountByAddress: Map<String, Account.CryptoPortfolio>,
    val isAccountsModeEnabled: Boolean,
    val walletInfoById: Map<UserWalletId, WalletInfo>,
)

internal data class WalletInfo(val name: String, val deviceIconUM: DeviceIconUM)