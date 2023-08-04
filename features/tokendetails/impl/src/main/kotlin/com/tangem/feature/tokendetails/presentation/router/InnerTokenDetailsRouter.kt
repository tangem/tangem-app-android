package com.tangem.feature.tokendetails.presentation.router

import com.tangem.features.tokendetails.navigation.TokenDetailsRouter

internal interface InnerTokenDetailsRouter : TokenDetailsRouter {

    fun popBackStack()
}
