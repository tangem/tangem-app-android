package com.tangem.tangemtest._main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
[REDACTED_AUTHOR]
 */
class MainViewModel : ViewModel() {
    val ldDescriptionSwitch = MutableLiveData<Boolean>(false)

    fun switchToggled(state: Boolean) {
        ldDescriptionSwitch.postValue(state)
    }
}