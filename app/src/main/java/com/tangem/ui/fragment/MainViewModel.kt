package com.tangem.ui.fragment

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {
    private val responseVersionName = MutableLiveData<String>()

    fun getVersionName(): LiveData<String> {

        return responseVersionName
    }
}