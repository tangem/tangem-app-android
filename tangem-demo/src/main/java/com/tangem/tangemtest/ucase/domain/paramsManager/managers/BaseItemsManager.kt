package com.tangem.tangemtest.ucase.domain.paramsManager.managers

import androidx.lifecycle.LifecycleObserver
import com.tangem.CardManager
import com.tangem.tangemtest._arch.structure.Id
import com.tangem.tangemtest._arch.structure.Payload
import com.tangem.tangemtest._arch.structure.abstraction.Item
import com.tangem.tangemtest._arch.structure.abstraction.findDataItem
import com.tangem.tangemtest.ucase.domain.actions.Action
import com.tangem.tangemtest.ucase.domain.actions.AttrForAction
import com.tangem.tangemtest.ucase.domain.paramsManager.ActionCallback
import com.tangem.tangemtest.ucase.domain.paramsManager.AffectedItemsCallback
import com.tangem.tangemtest.ucase.domain.paramsManager.ItemsManager
import com.tangem.tangemtest.ucase.domain.paramsManager.triggers.changeConsequence.ItemsChangeConsequence
import ru.dev.gbixahue.eu4d.lib.android.global.log.Log

/**
[REDACTED_AUTHOR]
 */
open class BaseItemsManager(protected val action: Action) : ItemsManager, LifecycleObserver {

    init {
        Log.d(this, "new instance created")
    }

    override val payload: MutableMap<String, Any?> = mutableMapOf()

    protected var changeConsequence: ItemsChangeConsequence? = null
    protected val itemList: MutableList<Item> = mutableListOf()

    override fun itemChanged(id: Id, value: Any?, callback: AffectedItemsCallback?) {
        if (itemList.isEmpty()) return
        val foundItem = itemList.findDataItem(id) ?: return

        foundItem.setData(value)
        applyChangesByAffectedItems(foundItem, callback)
    }

    override fun setItems(items: List<Item>) {
        itemList.clear()
        itemList.addAll(items)
    }

    override fun getItems(): List<Item> = itemList

    override fun setItemChangeConsequences(consequence: ItemsChangeConsequence?) {
        this.changeConsequence = consequence
    }

    override fun invokeMainAction(cardManager: CardManager, callback: ActionCallback) {
        action.executeMainAction(this, getAttrsForAction(cardManager), callback)
    }

    override fun getActionByTag(id: Id, cardManager: CardManager): ((ActionCallback) -> Unit)? {
        return action.getActionByTag(this, id, getAttrsForAction(cardManager))
    }

    override fun attachPayload(payload: Payload) {
        payload.forEach { this.payload[it.key] = it.value }
    }

    // Use it if current item needs to be affect any other items
    protected open fun applyChangesByAffectedItems(param: Item, callback: AffectedItemsCallback?) {
        changeConsequence?.affectChanges(this, param, itemList)?.let { callback?.invoke(it) }
    }

    protected fun getAttrsForAction(cardManager: CardManager)
            : AttrForAction = AttrForAction(cardManager, itemList, payload, changeConsequence)
}