package com.tangem.tangemtest._arch.mvp.view.custom

import com.tangem.tangemtest._arch.mvp.view.MvpView

/**
[REDACTED_AUTHOR]
 */
interface MvpCustomView : MvpView {

    fun onViewCreated()
    fun onStart()
    fun onResume()
    fun onPause()
    fun onStop()
    fun onDestroyView()
    fun onViewDestroyed()
    fun onDetach()
}