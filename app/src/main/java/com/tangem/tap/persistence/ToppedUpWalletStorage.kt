package com.tangem.tap.persistence

import android.content.SharedPreferences
import androidx.core.content.edit
import com.tangem.common.json.MoshiJsonConverter
import com.tangem.tap.common.analytics.events.AnalyticsParam
import timber.log.Timber

/**
[REDACTED_AUTHOR]
 */
class ToppedUpWalletStorage(
    private val preferences: SharedPreferences,
    private val jsonConverter: MoshiJsonConverter,
) {

    private val walletList: MutableSet<TopupInfo> = mutableSetOf()

    init {
        walletList.addAll(restore())
    }

    fun save(userWalletInfo: TopupInfo): Boolean {
        walletList.removeAll { it.walletId == userWalletInfo.walletId }
        walletList.add(userWalletInfo)
        return save(walletList)
    }

    fun restore(walletId: String): TopupInfo? {
        return walletList.firstOrNull { it.walletId == walletId }
    }

    private fun save(userWallets: MutableSet<TopupInfo>): Boolean {
        return try {
            val json = jsonConverter.toJson(userWallets)
            preferences.edit(true) { putString(KEY, json) }
            true
        } catch (ex: Exception) {
            Timber.e(ex)
            false
        }
    }

    private fun restore(): MutableSet<TopupInfo> {
        val json = preferences.getString(KEY, null) ?: return mutableSetOf()
        return try {
            val typedList = jsonConverter.typedList(TopupInfo::class.java)
            val listData = jsonConverter.fromJson<List<TopupInfo>>(json, typedList)!!
            listData.toMutableSet()
        } catch (ex: Exception) {
            preferences.edit(true) { remove(KEY) }
            mutableSetOf()
        }
    }

    data class TopupInfo(
        val walletId: String,
        val cardBalanceState: AnalyticsParam.CardBalanceState,
    ) {
        val isToppedUp: Boolean = cardBalanceState == AnalyticsParam.CardBalanceState.Full
    }

    companion object {
        private const val KEY = "userWalletsInfo"
    }
}