package com.tangem.tangemtest._arch.mvp.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.tangem.tangemtest._arch.mvp.presenter.MvpPresenter
import ru.dev.gbixahue.eu4d.lib.kotlin.common.LayoutHolder

/**
[REDACTED_AUTHOR]
 */
abstract class BaseMvpActivity : AppCompatActivity(), MvpView, LayoutHolder {

    protected var presenter: MvpPresenter<in MvpView>? = null

    protected abstract fun initComponent()
    protected abstract fun getMvpPresenter(): MvpPresenter<in MvpView>?

    override fun onCreate(savedInstanceState: Bundle?) {
        initComponent()
        presenter = getMvpPresenter()
        presenter?.attached(this)
        super.onCreate(savedInstanceState)
        presenter?.created()
        setContentView(getLayoutId())
        presenter?.createView()
        presenter?.viewCreated()
    }

    override fun onStart() {
        super.onStart()
        presenter?.started()
    }

    override fun onResume() {
        super.onResume()
        presenter?.resumed()
    }

    override fun onPause() {
        super.onPause()
        presenter?.paused()
    }

    override fun onStop() {
        super.onStop()
        presenter?.stopped()
    }

    override fun onDestroy() {
        presenter?.destroyView()
        super.onDestroy()
        presenter?.viewDestroyed()
        presenter?.detached()
    }
}