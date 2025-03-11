package com.tangem.screens

import androidx.compose.ui.test.hasText
import com.atiurin.ultron.page.Page
import com.tangem.common.extensions.TestDataUtils.getResourceString
import com.tangem.wallet.R

object WalletSettingsPage : Page<WalletSettingsPage>() {
    val linkMoreCardsButton = hasText(getResourceString(R.string.details_row_title_create_backup))
    val cardSettingsButton = hasText(getResourceString(R.string.card_settings_title))
    val referralProgramButton = hasText(getResourceString(R.string.details_referral_title))
    val forgetWalletButton = hasText(getResourceString(R.string.settings_forget_wallet))
}