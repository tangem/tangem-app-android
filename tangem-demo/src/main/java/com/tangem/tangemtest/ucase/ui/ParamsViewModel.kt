package com.tangem.tangemtest.ucase.ui

import androidx.annotation.UiThread
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import com.tangem.CardManager
import com.tangem.commands.Card
import com.tangem.tangemtest._arch.SingleLiveEvent
import com.tangem.tangemtest._arch.structure.Id
import com.tangem.tangemtest._arch.structure.abstraction.Item
import com.tangem.tangemtest.commons.performAction
import com.tangem.tangemtest.ucase.domain.paramsManager.ParamsManager
import com.tangem.tangemtest.ucase.domain.responses.GsonInitializer
import com.tangem.tasks.ScanEvent
import com.tangem.tasks.TaskError
import com.tangem.tasks.TaskEvent
import ru.dev.gbixahue.eu4d.lib.android.global.log.Log

/**
[REDACTED_AUTHOR]
 */
class ActionViewModelFactory(private val manager: ParamsManager) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T = ParamsViewModel(manager) as T
}

class ParamsViewModel(val paramsManager: ParamsManager) : ViewModel() {

    val ldCard = MutableLiveData<Card>()
    val ldIsVerified = MutableLiveData<Boolean>()
    val ldResponse = MutableLiveData<String>()
    val ldParams = MutableLiveData(paramsManager.getParams())

    val seError: MutableLiveData<String> = SingleLiveEvent()
    val seIncomingParameter: MutableLiveData<Item> = SingleLiveEvent()

    private val notifier: Notifier = Notifier(this)
    private lateinit var cardManager: CardManager

    fun setCardManager(cardManager: CardManager) {
        this.cardManager = cardManager
    }

    fun userChangedParameter(id: Id, value: Any?) {
        parameterChanged(id, value)
    }

    private fun parameterChanged(id: Id, value: Any?) {
        paramsManager.parameterChanged(id, value) { notifier.notifyParameterChanges(it) }
    }

    //invokes Scan, Sign etc...
    fun invokeMainAction(payload: MutableMap<String, Any?> = mutableMapOf()) {
        paramsManager.attachPayload(payload)
        performAction(paramsManager, cardManager) { paramsManager, cardManager ->
            paramsManager.invokeMainAction(cardManager) { response, listOfChangedParams ->
                notifier.handleActionResult(response, listOfChangedParams)
            }
        }
    }

    fun getParameterAction(id: Id): (() -> Unit)? {
        val parameterFunction = paramsManager.getActionByTag(id, cardManager) ?: return null

        return {
            parameterFunction { response, listOfChangedParams ->
                notifier.handleActionResult(response, listOfChangedParams)
            }
        }
    }
}

internal class Notifier(private val vm: ParamsViewModel) {

    private var notShowedError: TaskError? = null
    private val gson: Gson = GsonInitializer().gson

    fun handleActionResult(response: TaskEvent<*>, list: List<Item>) {
        notifyParameterChanges(list)
        handleResponse(response)
    }

    @UiThread
    fun notifyParameterChanges(list: List<Item>) {
        list.forEach { vm.seIncomingParameter.postValue(it) }
    }

    fun handleResponse(response: TaskEvent<*>) {
        val taskEvent = response as? TaskEvent<*> ?: return

        when (taskEvent) {
            is TaskEvent.Completion -> handleCompletionEvent(taskEvent)
            is TaskEvent.Event -> handleDataEvent(taskEvent.data)
        }
    }

    private fun handleDataEvent(event: Any?) {
        when (event) {
            is ScanEvent.OnReadEvent -> {
                vm.ldCard.postValue(event.card)
                vm.ldResponse.postValue(gson.toJson(event))
            }
            is ScanEvent.OnVerifyEvent -> {
                vm.ldIsVerified.postValue(true)
            }
            else -> vm.ldResponse.postValue(gson.toJson(event))
        }
    }

    private fun handleCompletionEvent(taskEvent: TaskEvent.Completion<*>) {
        if (taskEvent.error == null) {
            Log.d(this, "error = null")
            if (notShowedError != null) {
                vm.seError.postValue("${notShowedError!!::class.simpleName}")
                notShowedError = null
            }
        } else {
            Log.d(this, "error = ${taskEvent.error}")
            when (taskEvent.error) {
                is TaskError.UserCancelled -> {
                    if (notShowedError == null) {
                        vm.seError.postValue("User canceled the action")
                    } else {
                        vm.seError.postValue("${notShowedError!!::class.simpleName}")
                        notShowedError = null
                    }
                }
                else -> notShowedError = taskEvent.error
            }
        }
    }
}