package com.tangem.tangemtest._main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tangem.commands.CommandResponse
import ru.dev.gbixahue.eu4d.lib.android.global.log.Log

/**
[REDACTED_AUTHOR]
 */
class MainViewModel : ViewModel() {
    val ldDescriptionSwitch = MutableLiveData<Boolean>(false)

    var commandResponse: CommandResponse? = null

    fun switchToggled(state: Boolean) {
        ldDescriptionSwitch.postValue(state)
    }

    fun changeResponseEvent(commandResponse: CommandResponse?) {
        Log.d(this, "changeResponseEvent")
        val response = commandResponse ?: return

        this.commandResponse = response
    }
}