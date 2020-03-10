package com.tangem.tangemtest.card_use_cases.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.tangem.tangemtest.card_use_cases.CardContext
import com.tangem.tangemtest.commons.LayoutHolder
import com.tangem.tangemtest.commons.getDiManager
import com.tangem.tasks.TaskError
import com.tangem.tasks.TaskEvent

/**
[REDACTED_AUTHOR]
 */
abstract class BaseCardActionFragment : Fragment(), LayoutHolder {

    protected lateinit var cardContext: CardContext
    protected lateinit var mainView: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mainView = inflater.inflate(getLayoutId(), container, false)
        return mainView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cardContext = getDiManager().getCardContext()
    }

    @UiThread
    protected fun handleCompletionEvent(taskEvent: TaskEvent.Completion<*>) {
        val error: TaskError = taskEvent.error ?: return

        when (error) {
            is TaskError.UserCancelled -> showSnackbarMessage("Error: User was canceled")
            else -> showSnackbarMessage("Description not implemented. code: ${error.code}")
        }

    }

    protected fun showSnackbarMessage(message: String) {
        Snackbar.make(mainView, message, BaseTransientBottomBar.LENGTH_SHORT).show()
    }

    protected fun toJson(obj: Any): String = Gson().toJson(obj)

}