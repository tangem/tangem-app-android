package com.tangem.data.account.utils

import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO

/**
 * Whether the account record describes a joint account.
 *
 * Joint records arrive in the same `/accounts` response as the wallet's own accounts, but their
 * [WalletAccountDTO.derivationIndex] is the index of the participant's owner key, which lives in its own
 * space: index 0 there means the first joint account of the wallet, never the main account.
 */
internal val WalletAccountDTO.isJoint: Boolean
    get() = WalletAccountDTO.Type.of(raw = type) == WalletAccountDTO.Type.JOINT