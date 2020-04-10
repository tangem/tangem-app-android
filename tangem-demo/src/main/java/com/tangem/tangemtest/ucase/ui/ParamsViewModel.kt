package com.tangem.tangemtest.ucase.ui

import androidx.annotation.UiThread
import androidx.lifecycle.*
import com.google.gson.Gson
import com.tangem.SessionError
import com.tangem.TangemSdk
import com.tangem.commands.Card
import com.tangem.commands.CommandResponse
import com.tangem.common.CompletionResult
import com.tangem.tangemtest._arch.SingleLiveEvent
import com.tangem.tangemtest._arch.structure.Id
import com.tangem.tangemtest._arch.structure.Payload
import com.tangem.tangemtest._arch.structure.abstraction.Item
import com.tangem.tangemtest.commons.performAction
import com.tangem.tangemtest.ucase.domain.paramsManager.ItemsManager
import com.tangem.tangemtest.ucase.domain.responses.GsonInitializer
import com.tangem.tangemtest.ucase.tunnel.ViewScreen
import ru.dev.gbixahue.eu4d.lib.android.global.log.Log

/**
[REDACTED_AUTHOR]
 */
class ActionViewModelFactory(private val manager: ItemsManager) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T = ParamsViewModel(manager) as T
}

class ParamsViewModel(private val itemsManager: ItemsManager) : ViewModel(), LifecycleObserver {

    val ldCard = MutableLiveData<Card>()
    val ldIsVerified = MutableLiveData<Boolean>()
    val ldResponse = MutableLiveData<String>()
    val ldReadResponse = MutableLiveData<String>()
    val ldParams = MutableLiveData(itemsManager.getItems())

    val seError: MutableLiveData<String> = SingleLiveEvent()
    val seChangedItems: MutableLiveData<List<Item>> = SingleLiveEvent()

    private val notifier: Notifier = Notifier(this)
    private lateinit var tangemSdk: TangemSdk

    fun setCardManager(tangemSdk: TangemSdk) {
        this.tangemSdk = tangemSdk
    }

    fun userChangedItem(id: Id, value: Any?) {
        itemChanged(id, value)
    }

    private fun itemChanged(id: Id, value: Any?) {
        itemsManager.itemChanged(id, value) { notifier.notifyItemsChanged(it) }
    }

    //invokes Scan, Sign etc...
    fun invokeMainAction() {
        performAction(itemsManager, tangemSdk) { paramsManager, cardManager ->
            paramsManager.invokeMainAction(cardManager) { response, listOfChangedParams ->
                notifier.handleActionResult(response, listOfChangedParams)
            }
        }
    }

    fun getItemAction(id: Id): (() -> Unit)? {
        val itemFunction = itemsManager.getActionByTag(id, tangemSdk) ?: return null

        return {
            itemFunction { response, listOfChangedParams ->
                notifier.handleActionResult(response, listOfChangedParams)
            }
        }
    }

    fun attachToPayload(payload: Payload) {
        itemsManager.attachPayload(payload)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun viewOnDestroy() {
        val keyList = mutableListOf<String>()
        itemsManager.payload.filterValues { it is ViewScreen }.forEach { keyList.add(it.key) }
        keyList.forEach { itemsManager.payload.remove(it) }
    }
}

internal class Notifier(private val vm: ParamsViewModel) {

    private var notShowedError: SessionError? = null
    private val gson: Gson = GsonInitializer().gson

    fun handleActionResult(response: CompletionResult<*>, list: List<Item>) {
        if (list.isNotEmpty()) notifyItemsChanged(list)
        handleResponse(response)
    }

    @UiThread
    fun notifyItemsChanged(list: List<Item>) {
        vm.seChangedItems.postValue(list)
    }

    fun handleResponse(response: CompletionResult<*>) {
        val commandResponse = response as? CompletionResult<CommandResponse> ?: return

        when (commandResponse) {
            is CompletionResult.Failure -> handleError(commandResponse.error)
            is CompletionResult.Success -> handleDataEvent(commandResponse.data)
        }
    }

    private fun handleDataEvent(data: CommandResponse?) {
        when (data) {
            is Card -> {
                vm.ldCard.postValue(data)
                vm.ldReadResponse.postValue(gson.toJson(data))
            }
            else -> vm.ldResponse.postValue(gson.toJson(data))
        }
    }

    private fun handleError(error: SessionError) {
        Log.d(this, "error = ${error}")
        when (error) {
            is SessionError.UserCancelled -> {
                if (notShowedError == null) {
                    vm.seError.postValue("User canceled the action")
                } else {
                    vm.seError.postValue("${notShowedError!!::class.simpleName}")
                    notShowedError = null
                }
            }
            else -> notShowedError = error
        }
    }
}