package com.tangem.tangemtest._arch.mvp.dialog

import com.tangem.tangemtest.commons.OnBackPressHandler
import io.reactivex.disposables.CompositeDisposable
import ru.dev.gbixahue.eu4d.lib.android.global.log.Log


/**
[REDACTED_AUTHOR]
 */
open class BaseMvpBottomSheetPresenter<View : MvpDlgView> : MvpDlgPresenter<View>, OnBackPressHandler {

    protected var view: View? = null

    protected val stopDisposable = CompositeDisposable()
    protected val dismissDisposable = CompositeDisposable()
    protected val cancelDisposable = CompositeDisposable()

    override fun attached(view: View) {
        Log.d(this, "mvpView attached")
        this.view = view
    }

    override fun contentViewSet() {
        Log.d(this, "contentViewSet")
    }

    override fun created() {
        Log.d(this, "created")
    }

    override fun started() {
        Log.d(this, "started")
    }

    override fun showed() {
        Log.d(this, "showed")
    }

    override fun stopped() {
        Log.d(this, "stoped")
        stopDisposable.clear()
    }

    override fun dismissed() {
        Log.d(this, "dismissed")
        dismissDisposable.clear()
    }

    override fun cancelled() {
        Log.d(this, "cancelled")
        cancelDisposable.clear()
    }

    override fun detached() {
        Log.d(this, "detached")
        view = null
    }

    override fun onBackPressed(): Boolean {
        Log.d(this, "onBackPressed")
        return true
    }
}