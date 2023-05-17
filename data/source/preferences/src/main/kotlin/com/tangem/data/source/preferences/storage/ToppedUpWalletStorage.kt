package com.tangem.data.source.preferences.storage

import android.content.SharedPreferences
import androidx.core.content.edit
import com.tangem.common.json.MoshiJsonConverter
import com.tangem.data.source.preferences.model.DataSourceTopupInfo

/**
[REDACTED_AUTHOR]
 */
@Deprecated("Create repository instead")
class ToppedUpWalletStorage internal constructor(
    private val preferences: SharedPreferences,
    private val jsonConverter: MoshiJsonConverter,
) {

    private val walletList: MutableSet<DataSourceTopupInfo> = mutableSetOf()

    init {
        walletList.addAll(restore())
    }

    fun save(userWalletInfo: DataSourceTopupInfo): Boolean {
        walletList.removeAll { it.walletId == userWalletInfo.walletId }
        walletList.add(userWalletInfo)
        return save(walletList)
    }

    fun restore(walletId: String): DataSourceTopupInfo? {
        return walletList.firstOrNull { it.walletId == walletId }
    }

    private fun save(userWallets: MutableSet<DataSourceTopupInfo>): Boolean {
        return try {
            val json = jsonConverter.toJson(userWallets)
            preferences.edit(true) { putString(KEY, json) }
            true
        } catch (ex: Exception) {
            false
        }
    }

    private fun restore(): MutableSet<DataSourceTopupInfo> {
        val json = preferences.getString(KEY, null) ?: return mutableSetOf()
        return try {
            val typedList = jsonConverter.typedList(DataSourceTopupInfo::class.java)
            val listData = jsonConverter.fromJson<List<DataSourceTopupInfo>>(json, typedList)!!
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