package com.tangem.data.pay.store

import com.tangem.data.pay.entity.PaymentAccountRecentUsage
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.utils.getJsonOrNull
import com.tangem.datasource.local.preferences.utils.getJsonSyncOrNull
import com.tangem.datasource.local.preferences.utils.setJson
import com.tangem.domain.account.models.AccountCurrencyId
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.repository.PaymentAccountRecentsStore
import com.tangem.spend.datasource.preferences.SpendPreferencesKeys
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

internal class DefaultPaymentAccountRecentsStore(
    private val prefs: AppPreferencesStore,
    private val spendPreferencesKeys: SpendPreferencesKeys,
    private val json: Json,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) : PaymentAccountRecentsStore {

    private val usageSerializer = MapSerializer(String.serializer(), PaymentAccountRecentUsage.serializer())

    override suspend fun markUsed(userWalletId: UserWalletId, accountId: AccountId, currencyId: CryptoCurrency.ID) {
        val key = spendPreferencesKeys.paymentAccountRecents(userWalletId)
        val usage = PaymentAccountRecentUsage(accountId = accountId.value, usedAtMillis = currentTimeMillis())
        prefs.editData { preferences ->
            val current = preferences.getJsonOrNull(key, json, usageSerializer).orEmpty()

            preferences.setJson(
                key = key,
                json = json,
                serializer = usageSerializer,
                value = current + (currencyId.value to usage),
            )
        }
    }

    override suspend fun getUsage(userWalletId: UserWalletId): Map<AccountCurrencyId, Long> {
        val key = spendPreferencesKeys.paymentAccountRecents(userWalletId)

        return prefs.getJsonSyncOrNull(key, json, usageSerializer).orEmpty()
            .mapNotNull { (rawCurrencyId, usage) ->
                val accountId = AccountId
                    .forCryptoPortfolio(userWalletId = userWalletId, value = usage.accountId)
                    .getOrNull()
                    ?: return@mapNotNull null
                // An entry written by an older build, or a file edited on a rooted device, must not crash the
                // hub: CryptoCurrency.ID.fromValue throws on anything it cannot parse.
                val currencyId = runCatching { CryptoCurrency.ID.fromValue(rawCurrencyId) }.getOrNull()
                    ?: return@mapNotNull null

                accountId to currencyId to usage.usedAtMillis
            }
            .toMap()
    }
}