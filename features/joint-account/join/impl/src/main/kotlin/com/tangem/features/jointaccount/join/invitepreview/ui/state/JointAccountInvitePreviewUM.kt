package com.tangem.features.jointaccount.join.invitepreview.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.account.AccountIconUM

@Immutable
internal data class JointAccountInvitePreviewUM(
    val accountName: String,
    val accountIcon: AccountIconUM.CryptoPortfolio,
    val requiredToSign: Int,
    val totalMembers: Int,
    val creatorName: String,
    val wallet: WalletUM?,
    val onCreatorInfoClick: () -> Unit,
    val onContinueClick: () -> Unit,
    val onCloseClick: () -> Unit,
) {

    data class WalletUM(
        val name: String,
        val onClick: () -> Unit,
    )
}