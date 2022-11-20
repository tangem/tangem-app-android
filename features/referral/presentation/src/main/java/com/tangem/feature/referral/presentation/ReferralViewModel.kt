package com.tangem.feature.referral.presentation

import androidx.lifecycle.ViewModel
import com.tangem.feature.referral.domain.ReferralInteractor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ReferralViewModel @Inject constructor(referralInteractor: ReferralInteractor) : ViewModel() {

}