package com.tangem.tap.persistence

import android.content.SharedPreferences
import androidx.core.content.edit
import com.tangem.common.json.MoshiJsonConverter
import com.tangem.tap.common.analytics.filters.BasicTopUpFilter
import timber.log.Timber

/**
 * Created by Anton Zhilenkov on 02.11.2022.
 */
class ToppedUpWalletStorage(
    private val preferences: SharedPreferences,
    private val jsonConverter: MoshiJsonConverter,
) {

    private val walletList: MutableSet<BasicTopUpFilter.Data> = mutableSetOf()

    init {
        walletList.addAll(restore())
    }

    fun save(userWalletInfo: BasicTopUpFilter.Data): Boolean {
        walletList.removeAll { it.walletId == userWalletInfo.walletId }
        walletList.add(userWalletInfo)
        return save(walletList)
    }

    fun restore(walletId: String): BasicTopUpFilter.Data? {
        return walletList.firstOrNull { it.walletId == walletId }
    }

    private fun save(userWallets: MutableSet<BasicTopUpFilter.Data>): Boolean {
        return try {
            val json = jsonConverter.toJson(userWallets)
            preferences.edit(true) { putString(KEY, json) }
            true
        } catch (ex: Exception) {
            Timber.e(ex)
            false
        }
    }

    private fun restore(): MutableSet<BasicTopUpFilter.Data> {
        val json = preferences.getString(KEY, null) ?: return mutableSetOf()
        return try {
            val typedList = jsonConverter.typedList(BasicTopUpFilter.Data::class.java)
            val listData = jsonConverter.fromJson<List<BasicTopUpFilter.Data>>(json, typedList)!!
            listData.toMutableSet()
        } catch (ex: Exception) {
            preferences.edit(true) { remove(KEY) }
            mutableSetOf()
        }
    }

    companion object {
        private const val KEY = "userWalletsInfo"
    }
}
