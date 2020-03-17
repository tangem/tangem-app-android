package com.tangem.tangemtest.card_use_cases.view_models

import androidx.annotation.UiThread
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.GsonBuilder
import com.tangem.common.tlv.TlvTag
import com.tangem.tangemtest.card_use_cases.models.CardContext
import com.tangem.tangemtest.card_use_cases.models.params.manager.IncomingParameter
import com.tangem.tangemtest.card_use_cases.models.params.manager.ParamsManager
import com.tangem.tangemtest.card_use_cases.models.params.manager.ParamsManagerFactory
import com.tangem.tangemtest.commons.Action
import com.tangem.tangemtest.commons.performAction
import com.tangem.tasks.ScanEvent
import com.tangem.tasks.TaskError
import com.tangem.tasks.TaskEvent
import ru.dev.gbixahue.eu4d.lib.android.global.threading.postUI

/**
[REDACTED_AUTHOR]
 */
class ParamsViewModel : ViewModel() {
    val ldResponse: MutableLiveData<String> = MutableLiveData()
    val ldError: MutableLiveData<String> = MutableLiveData()
    val ldIncomingParameter: MutableLiveData<IncomingParameter> = MutableLiveData()

    lateinit var ldCardContext: MutableLiveData<CardContext>
        private set

    private lateinit var ldParamsManager: LiveData<ParamsManager>
    private lateinit var notifier: Notifier

    // call before any subscriptions
    fun init(context: FragmentActivity, action: Action) {
        if (::ldCardContext.isInitialized && ::ldParamsManager.isInitialized) return

        val factory = ParamsManagerFactory.createFactory()
        val manager = factory.get(action) ?: throw NotImplementedError(
                "ParamsManager for the ${context.getString(action.resName)} not implemented yet")

        ldParamsManager = MutableLiveData(manager)
        ldCardContext = MutableLiveData(CardContext().init(context))
        notifier = Notifier(this)
    }

    fun getInitialParams(): List<IncomingParameter>? {
        return ldParamsManager.value?.getParams()
    }

    fun userChangedParameter(param: IncomingParameter) {
        parameterChanged(param.tlvTag, param.data)
    }

    private fun parameterChanged(tlvTag: TlvTag, value: Any?) {
        ldParamsManager.value?.parameterChanged(tlvTag, value) { notifier.notifyParameterChanges(it) }
    }

    //invokes Scan, Sign etc...
    fun invokeMainAction() {
        performAction(ldParamsManager.value, ldCardContext.value?.cardManager) { paramsManager, cardManager ->
            paramsManager.invokeMainAction(cardManager) { response, listOfChangedParams ->
                notifier.handleActionResult(response, listOfChangedParams)
            }
        }
    }

    fun getParameterAction(tag: TlvTag): (() -> Unit)? {
        val paramsManager = ldParamsManager.value ?: return null
        val cardManager = ldCardContext.value?.cardManager ?: return null
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
        postUI { notifyParameterChanges(list) }
        handleResponse(response)
    }

    @UiThread
    fun notifyParameterChanges(list: List<IncomingParameter>) {
        list.forEach { vm.ldIncomingParameter.value = it }
    }

    fun handleResponse(response: TaskEvent<*>) {
        val taskEvent = response as? TaskEvent<*> ?: return

        postUI {
            when (taskEvent) {
                is TaskEvent.Completion -> handleCompletionEvent(taskEvent)
                is TaskEvent.Event -> handleDataEvent(taskEvent.data)
            }
        }
    }

    @UiThread
    private fun handleDataEvent(event: Any?) {
        when (event) {
            is ScanEvent.OnReadEvent -> {
                val cardContext = vm.ldCardContext.value ?: return

                cardContext.card = event.card
                vm.ldCardContext.value = cardContext
                vm.ldResponse.value = gsonConverter.toJson(event)
            }
            is ScanEvent.OnVerifyEvent -> {
                val cardContext = vm.ldCardContext.value ?: return

                cardContext.isVerified = true
                vm.ldCardContext.value = cardContext

            }
            else -> vm.ldResponse.value = gsonConverter.toJson(event)
        }

    }

    @UiThread
    private fun handleCompletionEvent(taskEvent: TaskEvent.Completion<*>) {
        val error: TaskError = taskEvent.error ?: return
        when (error) {
            is TaskError.UserCancelled -> {
                if (notShowedError == null) {
                    vm.ldError.value = "User was cancelled"
                } else {
                    vm.ldError.value = "Error description not implemented. Code: ${notShowedError!!.code}"
                    notShowedError = null
                }
            }
            else -> notShowedError = error
        }
    }
}