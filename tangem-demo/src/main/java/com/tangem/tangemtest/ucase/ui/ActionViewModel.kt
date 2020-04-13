package com.tangem.tangemtest.ucase.ui

import android.view.View
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
import com.tangem.tangemtest._arch.structure.abstraction.iterate
import com.tangem.tangemtest.ucase.domain.paramsManager.ItemsManager
import com.tangem.tangemtest.ucase.domain.responses.ResponseJsonConverter
import com.tangem.tangemtest.ucase.resources.ActionType
import com.tangem.tangemtest.ucase.tunnel.ViewScreen
import com.tangem.tangemtest.ucase.variants.personalize.converter.ItemTypes
import ru.dev.gbixahue.eu4d.lib.android.global.log.Log

/**
[REDACTED_AUTHOR]
 */
class ActionViewModelFactory(private val manager: ItemsManager) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T = ActionViewModel(manager) as T
}

class ActionViewModel(private val itemsManager: ItemsManager) : ViewModel(), LifecycleObserver {

    val seResponse = SingleLiveEvent<CommandResponse>()
    val seResponseData = SingleLiveEvent<CommandResponse>()
    val seResponseCardData = SingleLiveEvent<Card>()

    val ldItemList = MutableLiveData(itemsManager.getItems())
    val seError: MutableLiveData<String> = SingleLiveEvent()
    val seChangedItems: MutableLiveData<List<Item>> = SingleLiveEvent()

    private val notifier: Notifier = Notifier(this)
    private lateinit var tangemSdk: TangemSdk

    fun setCardManager(tangemSdk: TangemSdk) {
        this.tangemSdk = tangemSdk
    }

    @Deprecated("Events must be send directly from the Widget")
    fun userChangedItem(id: Id, value: Any?) {
        itemChanged(id, value)
    }

    private fun itemChanged(id: Id, value: Any?) {
        itemsManager.itemChanged(id, value) { notifier.notifyItemsChanged(it) }
    }

    //invokes Scan, Sign etc...
    fun invokeMainAction() {
        if (!::tangemSdk.isInitialized) {
            Log.e(this, "TangemSdk isn't initialized")
            return
        }
        itemsManager.invokeMainAction(tangemSdk) { response, listOfChangedParams ->
            notifier.handleActionResult(response, listOfChangedParams)
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

    fun toggleDescriptionVisibility(state: Boolean) {
        ldItemList.value?.iterate {
            it.viewModel.viewState.descriptionVisibility.value = if (state) View.VISIBLE else View.GONE
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

    fun showFields(type: ActionType) {
        toggleFieldsVisibility(type, true)
    }

    fun hideFields(type: ActionType) {
        toggleFieldsVisibility(type, false)
    }

    private fun toggleFieldsVisibility(type: ActionType, show: Boolean) {
        val oftenUsed = getItemsForTogglingVisibilityState(type)
        val hidden = getItemIdsWhichWontShows(type)
        itemsManager.getItems().iterate {
            if (!hidden.contains(it.id)) {
                if (!oftenUsed.contains(it.id)) {
                    it.viewModel.viewState.isVisibleState.value = show
                }
            }

        }
    }

    private fun getItemsForTogglingVisibilityState(type: ActionType): List<Id> {
        return when (type) {
            ActionType.Personalize -> ItemTypes().oftenUsedList
            else -> emptyList()
        }
    }

    private fun getItemIdsWhichWontShows(type: ActionType): List<Id> {
        return when (type) {
            ActionType.Personalize -> ItemTypes().hiddenList
            else -> emptyList()
        }
    }
}

internal class Notifier(private val vm: ActionViewModel) {

    private var notShowedError: SessionError? = null
    private val gson: Gson = ResponseJsonConverter().gson

    fun handleActionResult(result: CompletionResult<*>, list: List<Item>) {
        if (list.isNotEmpty()) notifyItemsChanged(list)
        handleCompletionResult(result)
    }

    @UiThread
    fun notifyItemsChanged(list: List<Item>) {
        vm.seChangedItems.postValue(list)
    }

    fun handleCompletionResult(result: CompletionResult<*>) {
        val commandResponse = result as? CompletionResult<CommandResponse> ?: return

        when (commandResponse) {
            is CompletionResult.Success -> handleData(commandResponse.data)
            is CompletionResult.Failure -> handleError(commandResponse.error)
        }
    }

    private fun handleData(event: CommandResponse?) {
        vm.seResponse.postValue(event)
        when (event) {
            is Card -> vm.seResponseCardData.postValue(event)
            else -> vm.seResponseData.postValue(event)
        }
    }

    private fun handleError(error: SessionError) {
        Log.d(this, "error = $error")
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