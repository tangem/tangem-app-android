package com.tangem.tap.features.wallet.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.play.core.review.ReviewManagerFactory
import com.tangem.tap.common.analytics.Analytics
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.analytics.events.MainScreen
import com.tangem.tap.common.extensions.getActivity
import com.tangem.tap.common.extensions.getColor
import com.tangem.tap.common.extensions.getString
import com.tangem.tap.common.extensions.hide
import com.tangem.tap.common.extensions.show
import com.tangem.tap.common.feedback.RateCanBeBetterEmail
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.configurable.warningMessage.WarningMessage
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.store
import com.tangem.wallet.R
import com.tangem.wallet.databinding.LayoutWarningCardActionBinding
import timber.log.Timber

class WarningMessagesAdapter : ListAdapter<WarningMessage, WarningMessageVH>(DiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WarningMessageVH {
        val binding = LayoutWarningCardActionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false,
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

class WarningMessageVH(val binding: LayoutWarningCardActionBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(warning: WarningMessage) {
        setBgColor(warning.priority)
        setText(warning)
        setupControlButtons(warning)
    }

    private fun setText(warning: WarningMessage) = with(binding.warningContentContainer) {
        fun getString(resId: Int?, default: String, formatArgs: String? = null) =
            if (resId == null) default else root.getString(resId, formatArgs)

        tvTitle.text = getString(warning.titleResId, warning.title)
        tvMessage.text = getString(
            resId = warning.messageResId,
            default = warning.message,
            formatArgs = warning.messageFormatArg,
        )
    }

    private fun setBgColor(priority: WarningMessage.Priority) {
        val color = when (priority) {
            WarningMessage.Priority.Info -> R.color.warning_info
            WarningMessage.Priority.Warning -> R.color.warning_warning
            WarningMessage.Priority.Critical -> R.color.warning_critical
        }
        binding.warningCardAction.setCardBackgroundColor(binding.root.getColor(color))
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
                            store.dispatch(WalletAction.DialogAction.SignedHashesMultiWalletDialog)
                        }
                    }
                    else -> {
                        View.OnClickListener {
                            store.dispatch(GlobalAction.HideWarningMessage(warning))
                        }
                    }
                }
            val buttonTitle = binding.root.getString(
                warning.buttonTextId ?: R.string.how_to_got_it_button,
            )
            binding.btnGotIt.setOnClickListener(buttonAction)
            binding.btnGotIt.text = buttonTitle
        }
        WarningMessage.Type.AppRating -> {
            binding.groupControlsTemporary.hide()
            binding.groupControlsRating.show()
            binding.btnClose.show()
            binding.btnClose.setOnClickListener {
                Analytics.send(MainScreen.NoticeRateAppButton(AnalyticsParam.RateApp.Closed))
                store.dispatch(GlobalAction.HideWarningMessage(warning))
                store.dispatch(WalletAction.Warnings.AppRating.RemindLater)
            }
            binding.btnCanBeBetter.setOnClickListener {
                Analytics.send(MainScreen.NoticeRateAppButton(AnalyticsParam.RateApp.Disliked))
                store.dispatch(WalletAction.Warnings.AppRating.SetNeverToShow)
                store.dispatch(GlobalAction.HideWarningMessage(warning))
                store.dispatch(GlobalAction.SendEmail(RateCanBeBetterEmail()))
            }
            binding.btnReallyCool.setOnClickListener {
                val activity = binding.root.context.getActivity() ?: return@setOnClickListener

                Analytics.send(MainScreen.NoticeRateAppButton(AnalyticsParam.RateApp.Liked))
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
