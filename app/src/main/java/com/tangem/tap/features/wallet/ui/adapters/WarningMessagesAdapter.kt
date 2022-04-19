package com.tangem.tap.features.wallet.ui.adapters

import android.content.res.Resources
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.ConfigurationCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.google.android.play.core.review.ReviewManagerFactory
import com.tangem.tap.common.analytics.AnalyticsEvent
import com.tangem.tap.common.extensions.*
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.configurable.warningMessage.WarningMessage
import com.tangem.tap.domain.configurable.warningMessage.WarningMessagesManager
import com.tangem.tap.features.feedback.RateCanBeBetterEmail
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.store
import com.tangem.wallet.R
import com.tangem.wallet.databinding.LayoutWarningBinding
import timber.log.Timber

class WarningMessagesAdapter : ListAdapter<WarningMessage, WarningMessageVH>(DiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WarningMessageVH {
        val binding = LayoutWarningBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return WarningMessageVH(binding)
    }

    override fun onBindViewHolder(holder: WarningMessageVH, position: Int) {
        holder.bind(currentList[position])
    }

    object DiffUtilCallback : DiffUtil.ItemCallback<WarningMessage>() {
        override fun areContentsTheSame(oldItem: WarningMessage, newItem: WarningMessage) =
            oldItem == newItem

        override fun areItemsTheSame(oldItem: WarningMessage, newItem: WarningMessage) =
            oldItem == newItem
    }
}

class WarningMessageVH(val binding: LayoutWarningBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(warning: WarningMessage) {
        setBgColor(warning.priority)
        setText(warning)
        setupControlButtons(warning)
    }

    private fun setText(warning: WarningMessage) = with(binding) {
        fun getString(resId: Int?, default: String, formatArgs: String? = null) =
            if (resId == null) default else root.getString(resId, formatArgs)

        tvTitle.text = getString(warning.titleResId, warning.title)
        tvMessage.text = getString(
            resId = warning.messageResId,
            default = warning.message,
            formatArgs = warning.messageFormatArg
        )
    }

    private fun setBgColor(priority: WarningMessage.Priority) {
        val color = when (priority) {
            WarningMessage.Priority.Info -> R.color.warning_info
            WarningMessage.Priority.Warning -> R.color.warning_warning
            WarningMessage.Priority.Critical -> R.color.warning_critical
        }
        binding.cardView.setCardBackgroundColor(binding.root.getColor(color))
    }

    private fun setupControlButtons(warning: WarningMessage) = when (warning.type) {
        WarningMessage.Type.Permanent, WarningMessage.Type.TestCard -> {
            binding.groupControlsTemporary.hide()
            binding.groupControlsRating.hide()
            binding.btnClose.hide()
        }
        WarningMessage.Type.Temporary -> {
            binding.groupControlsRating.hide()
            binding.groupControlsTemporary.show()
            binding.btnClose.hide()

            val buttonAction =
                when (warning.titleResId) {
                    R.string.warning_important_security_info -> {
                        View.OnClickListener {
                            store.dispatch(WalletAction.ShowDialog.SignedHashesMultiWalletDialog)
                        }
                    }
                    R.string.alert_funds_restoration_message -> {
                        binding.btnClose.show()
                        binding.btnClose.setOnClickListener {
                            store.dispatch(GlobalAction.HideWarningMessage(warning))
                        }
                        val locale = ConfigurationCompat
                            .getLocales(Resources.getSystem().configuration)
                            .get(0)
                        val url = WarningMessagesManager.getRestoreFundsGuideUrl(locale.language)
                        View.OnClickListener {
                            store.dispatchOpenUrl(url)
                        }
                    }
                    else -> {
                        View.OnClickListener {
                            store.dispatch(GlobalAction.HideWarningMessage(warning))
                        }
                    }
                }
            val buttonTitle = binding.root.getString(
                warning.buttonTextId ?: R.string.how_to_got_it_button
            )
            binding.btnGotIt.setOnClickListener (buttonAction)
            binding.btnGotIt.text = buttonTitle
        }
        WarningMessage.Type.AppRating -> {
            binding.groupControlsTemporary.hide()
            binding.groupControlsRating.show()
            binding.btnClose.show()
            val analyticsHandler = store.state.globalState.analyticsHandlers
            binding.btnClose.setOnClickListener {
                analyticsHandler?.triggerEvent(AnalyticsEvent.APP_RATING_DISMISS)
                store.dispatch(GlobalAction.HideWarningMessage(warning))
                store.dispatch(WalletAction.Warnings.AppRating.RemindLater)
            }
            binding.btnCanBeBetter.setOnClickListener {
                analyticsHandler?.triggerEvent(AnalyticsEvent.APP_RATING_NEGATIVE)
                store.dispatch(WalletAction.Warnings.AppRating.SetNeverToShow)
                store.dispatch(GlobalAction.HideWarningMessage(warning))
                store.dispatch(GlobalAction.SendFeedback(RateCanBeBetterEmail()))
            }
            binding.btnReallyCool.setOnClickListener {
                val activity = binding.root.context.getActivity() ?: return@setOnClickListener

                analyticsHandler?.triggerEvent(AnalyticsEvent.APP_RATING_POSITIVE)
                store.dispatch(WalletAction.Warnings.AppRating.SetNeverToShow)
                val reviewManager = ReviewManagerFactory.create(activity)
                val task = reviewManager.requestReviewFlow()
                task.addOnCompleteListener {
                    if (it.isSuccessful) {
                        val reviewFlow = reviewManager.launchReviewFlow(activity, it.result)
                        reviewFlow.addOnCompleteListener {
                            if (it.isSuccessful) {
                                // send review was succeed
                            } else {
                                // send fails
                            }
                        }
                    } else {
                        Timber.e(task.exception)
                    }
                }.addOnFailureListener {
                    Timber.e(it)
                }
                store.dispatch(GlobalAction.HideWarningMessage(warning))
            }
        }
    }
}

class SpacesItemDecoration(private val spacePx: Int) : ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        outRect.left = spacePx
        outRect.right = spacePx

        outRect.top = spacePx / 2
        outRect.top = spacePx / 2
    }
}