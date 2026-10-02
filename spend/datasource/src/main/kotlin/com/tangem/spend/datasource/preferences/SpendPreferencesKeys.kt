package com.tangem.spend.datasource.preferences

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.tangem.domain.models.wallet.UserWalletId
import javax.inject.Inject

/**
 * DataStore preference keys owned by the Spend stream (Tangem Pay).
 *

 * and can be garbage-collected, instead of being pinned in memory for the whole process lifetime.
 *
 * The persisted key names must stay identical to the previous core:datasource definitions so existing
 * user data survives the move.
 */
class SpendPreferencesKeys @Inject constructor() {

    val withdrawOrdersKey: Preferences.Key<String> = stringPreferencesKey(name = "tangemPayWithdrawOrdersKey")

    val activeWithdrawOrdersKey: Preferences.Key<String> =
        stringPreferencesKey(name = "tangemPayActiveWithdrawOrdersKey")

    val eligibilityKey: Preferences.Key<Set<String>> = stringSetPreferencesKey(name = "tangemPayEligibilityListV3")

    fun paymentAccountRecents(userWalletId: UserWalletId): Preferences.Key<String> =
        stringPreferencesKey("tangem_pay_account_recents_${userWalletId.stringValue}")

    fun getAddToWalletKey(customerWalletAddress: String): Preferences.Key<Boolean> =
        booleanPreferencesKey("tangem_pay_add_to_wallet_done_key_$customerWalletAddress")

    fun getCashbackDeactivationDismissedKey(customerWalletAddress: String): Preferences.Key<Boolean> =
        booleanPreferencesKey("tangem_pay_cashback_deactivation_dismissed_key_$customerWalletAddress")

    fun getOrderIdKey(customerWalletAddress: String): Preferences.Key<String> =
        stringPreferencesKey("tangem_pay_order_id_key_$customerWalletAddress")

    fun getCampaignOrderKey(customerWalletAddress: String): Preferences.Key<Boolean> =
        booleanPreferencesKey("tangem_pay_campaign_order_key_$customerWalletAddress")

    fun getVirtualAccountOrderIdKey(customerWalletAddress: String): Preferences.Key<String> =
        stringPreferencesKey("tangem_pay_va_order_id_key_$customerWalletAddress")

    fun getCustomerWalletAddressKey(userWalletId: UserWalletId): Preferences.Key<String> =
        stringPreferencesKey("tangem_pay_customer_wallet_address_key_${userWalletId.stringValue}")

    fun getCheckCustomerByWalletId(userWalletId: UserWalletId): Preferences.Key<Boolean> =
        booleanPreferencesKey("tangem_pay_check_customer_by_wallet_id_$userWalletId")

    fun getHideOnboardingKey(userWalletId: UserWalletId): Preferences.Key<Boolean> =
        booleanPreferencesKey("tangem_pay_hide_onboarding_key_$userWalletId")

    fun getDeactivatedKey(userWalletId: UserWalletId): Preferences.Key<Boolean> =
        booleanPreferencesKey("tangem_pay_deactivated_$userWalletId")
}