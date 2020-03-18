package com.tangem.tangemtest._arch.mvp.view

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.tangem.tangemtest._arch.mvp.presenter.MvpPresenter
import com.tangem.tangemtest.commons.OnBackPressHandler

/**
[REDACTED_AUTHOR]
 */
abstract class BaseMvpFragment : Fragment(), MvpView, OnBackPressHandler {

    protected lateinit var mainView: View
    protected var presenter: MvpPresenter<in MvpView>? = null

    protected abstract fun getMvpPresenter(): MvpPresenter<in MvpView>

    override fun onAttach(context: Context) {
        super.onAttach(context)
        presenter = getMvpPresenter()
        presenter?.attached(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presenter?.created()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        mainView = super.onCreateView(inflater, container, savedInstanceState)!!
        presenter?.createView()
        return mainView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed()
            : Boolean = (presenter as? OnBackPressHandler)?.onBackPressed() ?: false

    override fun onPause() {
        super.onPause()
        presenter?.paused()
    }

    override fun onStop() {
        super.onStop()
        presenter?.stopped()
    }

    override fun onDestroyView() {
        presenter?.destroyView()
        super.onDestroyView()
        presenter?.viewDestroyed()
    }

    override fun onDetach() {
        super.onDetach()
        presenter?.detached()
    }
}