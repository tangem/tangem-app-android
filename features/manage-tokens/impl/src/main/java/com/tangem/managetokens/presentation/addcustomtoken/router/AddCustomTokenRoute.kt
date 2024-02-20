package com.tangem.managetokens.presentation.addcustomtoken.router

/**
 *  Add Custom Tokens screens
 *  @property route route string representation
 */
internal sealed class AddCustomTokenRoute(val route: String) {
    object Main : AddCustomTokenRoute("$BASE_ROUTE/main")
    object ChooseNetwork : AddCustomTokenRoute("$BASE_ROUTE/choose_network")
    object ChooseWallet : AddCustomTokenRoute("$BASE_ROUTE/choose_wallet")
    object ChooseDerivation : AddCustomTokenRoute("$BASE_ROUTE/choose_derivation")
}

private const val BASE_ROUTE = "manage_tokens/add_custom_token"
