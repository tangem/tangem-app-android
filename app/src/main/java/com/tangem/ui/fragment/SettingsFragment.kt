package com.tangem.ui.fragment

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.navigation.fragment.NavHostFragment
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.google.firebase.analytics.FirebaseAnalytics
import com.tangem.util.Analytics
import com.tangem.util.AnalyticsEvent
import com.tangem.wallet.BuildConfig
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.fragment_settings.*

class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
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

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == getString(R.string.pref_analytics)) {
            activity?.let {activity ->
                val analytics = FirebaseAnalytics.getInstance(activity)
                if (!Analytics.isEnabled()) {
                    analytics.logEvent(AnalyticsEvent.ANALYTICS_TURNED_OFF.event, bundleOf())
                }
                Analytics.setFirebaseEnabled(activity)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences
                .registerOnSharedPreferenceChangeListener(this);
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences
                .unregisterOnSharedPreferenceChangeListener(this);
    }
}