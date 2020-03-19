package com.tangem.tangemtest.card_use_cases.view_models

import androidx.annotation.UiThread
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.gson.GsonBuilder
import com.tangem.CardManager
import com.tangem.commands.Card
import com.tangem.common.tlv.TlvTag
import com.tangem.tangemtest._arch.SingleLiveEvent
import com.tangem.tangemtest.card_use_cases.domain.params_manager.IncomingParameter
import com.tangem.tangemtest.card_use_cases.domain.params_manager.ParamsManager
import com.tangem.tangemtest.commons.performAction
import com.tangem.tasks.ScanEvent
import com.tangem.tasks.TaskError
import com.tangem.tasks.TaskEvent

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
    val seIncomingParameter: MutableLiveData<IncomingParameter> = SingleLiveEvent()

    private val notifier: Notifier = Notifier(this)
    private lateinit var cardManager: CardManager

    fun setCardManager(cardManager: CardManager) {
        this.cardManager = cardManager
    }

    fun userChangedParameter(tlvTag: TlvTag, value: Any?) {
        parameterChanged(tlvTag, value)
    }

    private fun parameterChanged(tlvTag: TlvTag, value: Any?) {
        paramsManager.parameterChanged(tlvTag, value) { notifier.notifyParameterChanges(it) }
    }

    //invokes Scan, Sign etc...
    fun invokeMainAction() {
        performAction(paramsManager, cardManager) { paramsManager, cardManager ->
            paramsManager.invokeMainAction(cardManager) { response, listOfChangedParams ->
                notifier.handleActionResult(response, listOfChangedParams)
            }
        }
    }

    fun getParameterAction(tag: TlvTag): (() -> Unit)? {
        val parameterFunction = paramsManager.getActionByTag(tag, cardManager) ?: return null

        return {
            parameterFunction { response, listOfChangedParams ->
                notifier.handleActionResult(response, listOfChangedParams)
            }
        }
    }
}

internal class Notifier(private val vm: ParamsViewModel) {

    private val gsonConverter = GsonBuilder().setPrettyPrinting().create()
    private var notShowedError: TaskError? = null

    fun handleActionResult(response: TaskEvent<*>, list: List<IncomingParameter>) {
        notifyParameterChanges(list)
        handleResponse(response)
    }

    @UiThread
    fun notifyParameterChanges(list: List<IncomingParameter>) {
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
                vm.ldResponse.postValue(gsonConverter.toJson(event))
            }
            is ScanEvent.OnVerifyEvent -> {
                vm.ldIsVerified.postValue(true)
            }
            else -> vm.ldResponse.postValue(gsonConverter.toJson(event))
        }

    }

    private fun handleCompletionEvent(taskEvent: TaskEvent.Completion<*>) {
        val error: TaskError = taskEvent.error ?: return
        when (error) {
            is TaskError.UserCancelled -> {
                if (notShowedError == null) {
                    vm.seError.postValue("User was cancelled")
                } else {
                    vm.seError.postValue("Error description not implemented. Code: ${notShowedError!!.code}")
                    notShowedError = null
                }
            }
            else -> notShowedError = error
        }
    }
}