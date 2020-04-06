package com.tangem.ui.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment.findNavController
import com.tangem.ui.activity.MainActivity
import com.tangem.ui.navigation.NavigationResult
import com.tangem.ui.navigation.NavigationResultListener

abstract class BaseFragment : Fragment() {

    protected val requestCode: String
        get() = arguments?.getString(ARGUMENT_NAVIGATION_REQUEST_CODE, REQUEST_CODE_NOT_SET)
                ?: REQUEST_CODE_NOT_SET
    protected val navigatedBack: Boolean
        get() = (activity as? MainActivity)?.viewModel?.navigationResult != null

    protected abstract val layoutId: Int

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("LIFECYCLE", "onCreate: ${this::class.java.simpleName}")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(layoutId, container, false)
    }

    override fun onStart() {
        super.onStart()
        Log.d("LIFECYCLE", "onStart: ${this::class.java.simpleName}")
        if (this is NavigationResultListener) {
            val navigationResult = (activity as? MainActivity)?.viewModel?.navigationResult
            navigationResult?.run {
                onNavigationResult(this.requestCode, this.resultCode, this.data)
                (activity as? MainActivity)?.viewModel?.navigationResult = null
            }
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d("LIFECYCLE", "onPause: ${this::class.java.simpleName}")
    }

    override fun onStop() {
        super.onStop()
        Log.d("LIFECYCLE", "onStop: ${this::class.java.simpleName}")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("LIFECYCLE", "onDestroy: ${this::class.java.simpleName}")
    }

    protected fun navigateUp(@IdRes destination: Int = DESTINATION_NOT_SET) {
        try {
            if (destination == DESTINATION_NOT_SET) {
                findNavController(this).popBackStack()
            } else {
                findNavController(this).popBackStack(destination, false)
            }
        } catch (e: IllegalArgumentException) {
            Log.w(this::class.java.simpleName, e.message)
        } catch (e: IllegalStateException) {
            Log.w(this::class.java.simpleName, e.message)
        }
    }

    protected fun navigateForResult(requestCode: String, @IdRes destination: Int, data: Bundle? = null) {
        val argumentsWithRequestCode = (data ?: Bundle()).apply {
            putString(ARGUMENT_NAVIGATION_REQUEST_CODE, requestCode)
        }
        navigateToDestination(destination, argumentsWithRequestCode)
    }

    protected fun navigateToDestination(@IdRes destination: Int, data: Bundle? = null) {
        try {
            findNavController(this).navigate(destination, data)
        } catch (e: IllegalArgumentException) {
            Log.w(this::class.java.simpleName, e.message)
        } catch (e: IllegalStateException) {
            Log.w(this::class.java.simpleName, e.message)
        }
    }

    protected fun navigateBackWithResult(resultCode: Int, data: Bundle? = null,
                                         @IdRes destination: Int = DESTINATION_NOT_SET) {
        (requireActivity() as MainActivity).viewModel.navigationResult =
                NavigationResult(requestCode, resultCode, data)
        return navigateUp(destination)
    }

    companion object {
        private const val ARGUMENT_NAVIGATION_REQUEST_CODE = "NAVIGATION_REQUEST_CODE"
        private const val DESTINATION_NOT_SET = -1
        private const val REQUEST_CODE_NOT_SET = "REQUEST_CODE_NOT_SET"
    }
}