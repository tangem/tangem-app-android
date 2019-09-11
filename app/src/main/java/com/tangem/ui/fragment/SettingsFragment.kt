package com.tangem.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.tangem.wallet.BuildConfig
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.fragment_settings.*

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initToolbar()
    }

    private fun initToolbar() {
        toolbar?.setTitle(R.string.settings_title)
        toolbar?.setNavigationIcon(android.R.drawable.ic_menu_close_clear_cancel)
        (activity as AppCompatActivity).setSupportActionBar(toolbar)
        toolbar?.setNavigationOnClickListener {
            NavHostFragment.findNavController(this).popBackStack()
        }
    }

    override fun onDestroyView() {
        (activity as AppCompatActivity).setSupportActionBar(toolbar)
        super.onDestroyView()
    }

}
