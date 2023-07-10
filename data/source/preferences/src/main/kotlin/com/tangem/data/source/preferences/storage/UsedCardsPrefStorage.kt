package com.tangem.data.source.preferences.storage

import android.content.SharedPreferences
import androidx.core.content.edit
import com.tangem.common.json.MoshiJsonConverter
import com.tangem.data.source.preferences.model.DataSourceUsedCardInfo
import com.tangem.data.source.preferences.model.DataSourceUsedCardInfoOld

/**
[REDACTED_AUTHOR]
 */
@Deprecated("Create repository instead")
class UsedCardsPrefStorage internal constructor(
    private val preferences: SharedPreferences,
    private val jsonConverter: MoshiJsonConverter,
) {

    private val migrationList = mutableListOf(
        UserCardInfoToV2(this),
    )

    internal fun migrate() {
        migrationList.forEach { it.migrate() }
        migrationList.clear()
    }

    fun scanned(cardId: String) {
        val restoredList = restore()
        val foundItem = findCardInfo(cardId, restoredList)?.copy(isScanned = true)
            ?: DataSourceUsedCardInfo(cardId, true)

        save(foundItem, restoredList)
    }

    fun wasScanned(cardId: String): Boolean {
        return findCardInfo(cardId)?.isScanned ?: false
    }

    fun activationStarted(cardId: String) {
        val restoredList = restore()
        val foundItem = findCardInfo(cardId, restoredList)?.copy(isActivationStarted = true)
            ?: DataSourceUsedCardInfo(cardId, isActivationStarted = true)

        save(foundItem, restoredList)
    }

    fun activationFinished(cardId: String) {
        val restoredList = restore()
        var foundItem = findCardInfo(cardId, restoredList) ?: DataSourceUsedCardInfo(cardId)
        foundItem = foundItem.copy(
            isActivationStarted = true,
            isActivationFinished = true,
        )

        save(foundItem, restoredList)
    }

    fun isActivationStarted(cardId: String): Boolean {
        return findCardInfo(cardId)?.isActivationStarted ?: false
    }

    fun isActivationFinished(cardId: String): Boolean {
        return findCardInfo(cardId)?.isActivationFinished ?: false
    }

    fun isActivationInProgress(cardId: String): Boolean {
        val cardInfo = findCardInfo(cardId) ?: return false
        return cardInfo.isActivationStarted && !cardInfo.isActivationFinished
    }

    fun hadFinishedActivation(): Boolean {
        return restore().any { it.isActivationFinished }
    }

    private fun findCardInfo(
        cardId: String,
        list: MutableList<DataSourceUsedCardInfo>? = null,
    ): DataSourceUsedCardInfo? {
        val findInList = list ?: restore()
        return findInList.firstOrNull { it.cardId == cardId }
    }

    private fun save(usedCardInfo: DataSourceUsedCardInfo?, usedCardsInfo: MutableList<DataSourceUsedCardInfo>) {
        val info = usedCardInfo ?: return

        with(usedCardsInfo) {
            val index = indexOfFirst { it.cardId == info.cardId }
            if (index == -1) {
                add(info)
            } else {
                set(index, info)
            }
        }

        save(usedCardsInfo)
    }

    private fun save(list: MutableList<DataSourceUsedCardInfo>) {
        val json = jsonConverter.toJson(list)
        preferences.edit { putString(USED_CARDS_INFO_V2, json) }
    }

    private fun restore(): MutableList<DataSourceUsedCardInfo> {
        val json = preferences.getString(USED_CARDS_INFO_V2, null) ?: return mutableListOf()
        return try {
            jsonConverter.fromJson(json, jsonConverter.typedList(DataSourceUsedCardInfo::class.java))!!
        } catch (ex: Exception) {
            preferences.edit(true) { remove(USED_CARDS_INFO_V2) }
            mutableListOf()
        }
    }

    companion object {
        private const val USED_CARDS_INFO_V2 = "usedCardsInfo_v2"
        private const val USED_CARDS_INFO = "usedCardsInfo"
    }

    private class UserCardInfoToV2(
        private val storage: UsedCardsPrefStorage,
    ) : Migration {
        override fun migrate() {
            val restoredCardsInfo = restore()
            if (restoredCardsInfo.isEmpty()) return

            val newCardsInfo = restoredCardsInfo.map { cardInfo ->
                DataSourceUsedCardInfo(
                    cardId = cardInfo.cardId,
                    isScanned = cardInfo.isScanned,
                    isActivationStarted = true,
                    isActivationFinished = !cardInfo.isActivationStarted,
                )
            }.toMutableList()
            storage.save(newCardsInfo)
        }

        private fun restore(): MutableList<DataSourceUsedCardInfoOld> {
            val json = storage.preferences.getString(USED_CARDS_INFO, null) ?: return mutableListOf()
            return try {
                storage.jsonConverter.fromJson(
                    json,
                    storage.jsonConverter.typedList(DataSourceUsedCardInfoOld::class.java),
                )!!
            } catch (ex: Exception) {
                mutableListOf()
            } finally {
                storage.preferences.edit(true) { remove(USED_CARDS_INFO) }
            }
        }
    }
}