package com.tangem.ui.fragment

import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.tangem.wallet.BuildConfig
import com.tangem.wallet.R

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_main)

        val categoryCommon = findPreference<PreferenceCategory>(getString(R.string.pref_category_common))
        val selectNodes = findPreference<Preference>(getString(R.string.pref_select_nodes))
        val encryptionModes = findPreference<ListPreference>(getString(R.string.pref_encryption_modes))

        if (!BuildConfig.DEBUG) {
            categoryCommon?.removePreference(selectNodes)
            categoryCommon?.removePreference(encryptionModes)
        }
    }

}
