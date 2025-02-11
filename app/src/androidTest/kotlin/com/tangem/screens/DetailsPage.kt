package com.tangem.screens

import androidx.compose.ui.test.hasText
import com.atiurin.ultron.page.Page
import com.tangem.common.extensions.TestDataUtils.getResourceString
import com.tangem.wallet.R

object DetailsPage : Page<DetailsPage>() {
    val walletConnectButton = hasText(getResourceString(R.string.wallet_connect_title))
    val walletNameButton = hasText(getResourceString(R.string.manage_tokens_network_selector_wallet))
    val scanCardButton = hasText(getResourceString(R.string.scan_card_settings_button))
    val buyTangemButton = hasText(getResourceString(R.string.details_buy_wallet))
    val appSettingsButton = hasText(getResourceString(R.string.app_settings_title))
    val contactSupportButton = hasText(getResourceString(R.string.details_row_title_contact_to_support))
    val toSButton = hasText(getResourceString(R.string.disclaimer_title))
}