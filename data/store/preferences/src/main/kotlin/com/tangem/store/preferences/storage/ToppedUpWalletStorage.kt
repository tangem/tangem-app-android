package com.tangem.store.preferences.storage

import android.content.SharedPreferences
import androidx.core.content.edit
import com.tangem.common.json.MoshiJsonConverter
import com.tangem.store.preferences.model.TopupInfoDM

/**
 * Created by Anton Zhilenkov on 02.11.2022.
 */
@Deprecated("Create repository instead")
class ToppedUpWalletStorage internal constructor(
    private val preferences: SharedPreferences,
    private val jsonConverter: MoshiJsonConverter,
) {

    private val walletList: MutableSet<TopupInfoDM> = mutableSetOf()

    init {
        walletList.addAll(restore())
    }

    fun save(userWalletInfo: TopupInfoDM): Boolean {
        walletList.removeAll { it.walletId == userWalletInfo.walletId }
        walletList.add(userWalletInfo)
        return save(walletList)
    }

    fun restore(walletId: String): TopupInfoDM? {
        return walletList.firstOrNull { it.walletId == walletId }
    }

    private fun save(userWallets: MutableSet<TopupInfoDM>): Boolean {
        return try {
            val json = jsonConverter.toJson(userWallets)
            preferences.edit(true) { putString(KEY, json) }
            true
        } catch (ex: Exception) {
            false
        }
    }

    private fun restore(): MutableSet<TopupInfoDM> {
        val json = preferences.getString(KEY, null) ?: return mutableSetOf()
        return try {
            val typedList = jsonConverter.typedList(TopupInfoDM::class.java)
            val listData = jsonConverter.fromJson<List<TopupInfoDM>>(json, typedList)!!
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
