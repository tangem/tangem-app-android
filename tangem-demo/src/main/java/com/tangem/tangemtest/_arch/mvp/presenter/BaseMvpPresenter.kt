package com.tangem.tangemtest._arch.mvp.presenter

import com.tangem.tangemtest._arch.mvp.view.MvpView
import com.tangem.tangemtest.commons.OnBackPressHandler
import io.reactivex.disposables.CompositeDisposable
import ru.dev.gbixahue.eu4d.lib.android.global.log.Log

/**
[REDACTED_AUTHOR]
 */

abstract class BaseMvpPresenter<View : MvpView> : MvpPresenter<View>, OnBackPressHandler, DisposableHolder {

    protected var view: View? = null

    protected val pauseDisposable = CompositeDisposable()
    protected val stopDisposable = CompositeDisposable()
    protected val destroyDisposable = CompositeDisposable()

    override fun attached(view: View) {
        Log.d(this, "attached")
        this.view = view
    }

    override fun created() {
        Log.d(this, "created")
    }

    override fun createView() {
        Log.d(this, "createView")
    }

    override fun viewCreated() {
        Log.d(this, "viewCreated")
    }

    override fun started() {
        Log.d(this, "started")
    }

    override fun resumed() {
        Log.d(this, "resumed")
    }

    override fun paused() {
        Log.d(this, "paused")
        pauseDisposable.clear()
    }

    override fun stopped() {
        Log.d(this, "stopped")
        stopDisposable.clear()
    }

    override fun destroyView() {
        Log.d(this, "destroyView")
        destroyDisposable.clear()
    }

    override fun viewDestroyed() {
        Log.d(this, "viewDestroyed")
    }

    override fun detached() {
        Log.d(this, "detached")
        this.view = null
    }

    override fun disposeAll() {
        pauseDisposable.clear()
        stopDisposable.clear()
        destroyDisposable.clear()
    }

    override fun onBackPressed(): Boolean = true
}