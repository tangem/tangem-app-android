package com.tangem.tap.features.onboarding.products.wallet.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import com.tangem.tap.common.extensions.getString
import com.tangem.wallet.R
import com.tangem.wallet.databinding.ItemBackupInfoAdapterBinding

class BackupInfoAdapter : RecyclerView.Adapter<BackupInfoViewHolder>() {

    private val data = backupInfoSnippets

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BackupInfoViewHolder =
        BackupInfoViewHolder(
            ItemBackupInfoAdapterBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: BackupInfoViewHolder, position: Int) =
        holder.binding.run {
            tvHeader.text = root.getString(data[position].header)
            tvBody.text = root.getString(data[position].body)
        }
}

private class BackupInfo(
    @StringRes val header: Int,
    @StringRes val body: Int,
)

private val backupInfoSnippets = listOf(
    BackupInfo(
        R.string.onboarding_wallet_info_title_first,
        R.string.onboarding_wallet_info_subtitle_first
    ),
    BackupInfo(
        R.string.onboarding_wallet_info_title_second,
        R.string.onboarding_wallet_info_subtitle_second
    ),
    BackupInfo(
        R.string.onboarding_wallet_info_title_third,
        R.string.onboarding_wallet_info_subtitle_third
    ),
    BackupInfo(
        R.string.onboarding_wallet_info_title_fourth,
        R.string.onboarding_wallet_info_subtitle_fourth
    ),

)

class BackupInfoViewHolder(val binding: ItemBackupInfoAdapterBinding) :
    RecyclerView.ViewHolder(binding.root)
