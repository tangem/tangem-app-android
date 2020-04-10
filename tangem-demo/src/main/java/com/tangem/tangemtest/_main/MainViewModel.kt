package com.tangem.tangemtest._main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tangem.tasks.TaskEvent
import ru.dev.gbixahue.eu4d.lib.android.global.log.Log

/**
[REDACTED_AUTHOR]
 */
class MainViewModel : ViewModel() {
    val ldDescriptionSwitch = MutableLiveData<Boolean>(false)

    var responseEvent: TaskEvent<*>? = null

    fun switchToggled(state: Boolean) {
        ldDescriptionSwitch.postValue(state)
    }

    fun changeResponseEvent(event: TaskEvent<*>?) {
        Log.d(this, "changeResponseEvent")
        val reqEvent = event ?: return

        responseEvent = reqEvent
    }
}