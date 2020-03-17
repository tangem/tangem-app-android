package com.tangem.tangemtest._arch.mvp.view.custom

import android.view.View
import com.tangem.tangemtest._arch.mvp.presenter.MvpPresenter
import com.tangem.tangemtest._arch.mvp.view.MvpView

/**
[REDACTED_AUTHOR]
 */
open class BaseMvpCustomView<in V : MvpView>(
        protected val mainView: View,
        protected val presenter: MvpPresenter<in V>
) : MvpCustomView {

    override fun onViewCreated() {
        presenter.created()
        presenter.viewCreated()
    }

    override fun onStart() {
        presenter.started()
    }

    override fun onResume() {
        presenter.resumed()
    }

    override fun onPause() {
        presenter.paused()
    }

    override fun onStop() {
        presenter.stopped()
    }

    override fun onDestroyView() {
        presenter.destroyView()
    }

    override fun onViewDestroyed() {
        presenter.viewDestroyed()
    }

    override fun onDetach() {
        presenter.detached()
    }

    protected fun <T> bindView(id: Int): T {
        return mainView.findViewById<View>(id) as T
    }
}