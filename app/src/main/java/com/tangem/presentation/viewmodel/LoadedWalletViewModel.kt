package com.tangem.presentation.viewmodel

import android.arch.lifecycle.MutableLiveData
import com.dmmatrix.epro.core.exception.Failure

class LoadedWalletViewModel : BaseViewModel() {

    private val state: MutableLiveData<State> = MutableLiveData()

    fun getState() = state

    enum class State {
        ServerError,
        Failed,
        Success
    }

    fun connectToken(data: String, data2: String) {

    }

    private fun handleTokensSaveFailure(failure: Failure) {
        state.value = State.Success
        handleFailure(failure)
    }

    private fun handleLoginFailure(failure: Failure) {
        when (failure) {
            is Failure.ServerError -> state.value = State.ServerError
        }
        handleFailure(failure)
    }
}