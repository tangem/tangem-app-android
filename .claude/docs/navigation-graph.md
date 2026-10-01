# Navigation Graph

Complete navigation map of the app based on `AppRoute` sealed class and feature-internal routes.

## 1. All AppRoute Paths

58 top-level routes defined in `common/routing/src/main/kotlin/com/tangem/common/routing/AppRoute.kt`.

| # | Route | Path | Description |
|---|-------|------|-------------|
| 1 | `Initial` | `/initial` | App entry point (splash) |
| 2 | `Home` | `/home` | Stories/home screen with launch mode |
| 3 | `Welcome` | `/welcome` | Welcome screen for returning users |
| 4 | `Disclaimer` | `/disclaimer` | Terms of service / disclaimer |
| 5 | `Wallet` | `/wallet` | Main wallet portfolio screen |
| 6 | `CurrencyDetails` | `/currency_details/{walletId}/{currencyId}` | Token/coin detail screen |
| 7 | `Send` | `/send/{walletId}/{currencyId}` | Send cryptocurrency |
| 8 | `Details` | `/details/{walletId}` | Wallet details / settings hub |
| 9 | `DetailsSecurity` | `/details/security` | Security mode settings |
| 10 | `Usedesk` | `/usedesk/{walletId}` | Customer support (Usedesk) |
| 11 | `CardSettings` | `/card_settings/{walletId}` | Card-specific settings |
| 12 | `AppSettings` | `/app_settings` | Global app settings |
| 13 | `ResetToFactory` | `/reset_to_factory/{walletId}/{cardId}/...` | Factory reset flow |
| 14 | `AccessCodeRecovery` | `/access_code_recovery` | Access code recovery |
| 15 | `ManageTokens` | `{source}/manage_tokens/{accountId}` | Add/remove tokens in portfolio |
| 16 | `ChooseManagedTokens` | `/{source}/choose_managed_tokens/...` | Token chooser for send-via-swap |
| 17 | `WalletConnectSessions` | `/wallet_connect_sessions` | WalletConnect sessions list |
| 18 | `QrScanning` | `/{source}/qr_scanning` | QR code scanner |
| 19 | `ReferralProgram` | `/referral_program` | Referral program |
| 20 | `Swap` | `/swap/{fromId}/{toId}/{walletId}/...` | Token swap screen |
| 21 | `AppCurrencySelector` | `/app_currency_selector` | Fiat currency selector |
| 22 | `Staking` | `/staking/{walletId}/{currencyId}/{integrationId}` | Staking screen |
| 23 | `PushNotification` | `/push_notification` | Push notification opt-in |
| 24 | `WalletSettings` | `/wallet_settings/{walletId}` | Per-wallet settings |
| 25 | `WalletBackup` | `/wallet_backup/{walletId}/{coldOption}` | Wallet backup options |
| 26 | `WalletHardwareBackup` | `/wallet_hardware_backup/{walletId}` | Hardware wallet backup |
| 27 | `Markets` | `/markets` | Markets token list |
| 28 | `MarketsTokenDetails` | `/markets_token_details/{tokenId}/{showPortfolio}` | Market token detail |
| 29 | `Onramp` | `/onramp/{walletId}/{symbol}` | Buy crypto (onramp) |
| 30 | `OnrampSuccess` | `/onramp/success/{txId}` | Onramp success screen |
| 31 | `BuyCrypto` | `/buy_crypto/{walletId}` | Buy crypto token selector |
| 32 | `SellCrypto` | `/sell_crypto/{walletId}` | Sell crypto token selector |
| 33 | `Onboarding` | `/onboarding_v2/{mode}` | Onboarding flow (v2) |
| 34 | `Stories` | `/stories$storyId` | Stories / promotional content |
| 35 | `NFT` | `/nft/{walletId}` | NFT collection list |
| 36 | `NFTSend` | `/send/nft/{walletId}/{collection}/{assetId}` | Send NFT |
| 37 | `CreateWalletSelection` | `/create_wallet_selection` | Choose wallet creation type |
| 38 | `CreateWalletStart` | `/create_wallet_start` | Wallet creation intro (cold/hot) |
| 39 | `CreateHardwareWallet` | `/create_hardware_wallet` | Create hardware wallet flow |
| 40 | `CreateMobileWallet` | `/create_mobile_wallet` | Create mobile (hot) wallet |
| 41 | `UpgradeWallet` | `/upgrade_wallet/{walletId}` | Upgrade hot wallet to hardware |
| 42 | `AddExistingWallet` | `/add_existing_wallet` | Import existing wallet |
| 43 | `WalletActivation` | `/wallet_activation/{walletId}` | Activate wallet post-creation |
| 44 | `CreateWalletBackup` | `/create_wallet_backup/{walletId}` | Backup flow for created wallet |
| 45 | `UpdateAccessCode` | `/update_access_code/{walletId}` | Change access code |
| 46 | `ViewPhrase` | `/view_seed_phrase/{walletId}` | View recovery phrase |
| 47 | `ForgetWallet` | `/forget_wallet/{walletId}` | Remove wallet from app |
| 48 | `SendEntryPoint` | `/send_entry_point/{walletId}/{currencyId}` | Send entry with swap option |
| 49 | `CreateAccount` | `/create_account/{walletId}` | Create new account |
| 50 | `EditAccount` | `/edit_account/{accountId}` | Edit account |
| 51 | `AccountDetails` | `/account_details/{accountId}` | Account details screen |
| 52 | `ArchivedAccountList` | `/archived_account/{walletId}` | Archived accounts list |
| 53 | `TangemPayDetails` | `/tangem_pay_details/{walletId}` | Tangem Pay card details |
| 54 | `TangemPayOnboarding` | `/tangem_pay_onboarding/{mode}` | Tangem Pay onboarding |
| 55 | `Kyc` | `/kyc` | KYC verification |
| 56 | `YieldSupplyEntry` | `/yield_supply_entry/{walletId}/{symbol}` | Yield/supply entry point |
| 57 | `NewsDetails` | `/news_details/{newsId}` | News article detail |

## 2. Navigation Edges

Each entry shows: **Source route** → target routes it can navigate to (via `push` or `replaceAll`).

### Initial / Bootstrap

| Source | Target | Method | Trigger |
|--------|--------|--------|---------|
| `Initial` | `Home`, `Welcome`, `Disclaimer`, `Onboarding`, etc. | `replaceAll` | App startup (DefaultRoutingComponent) |

### Home

| Target | Method | Trigger |
|--------|--------|---------|
| `ManageTokens(STORIES)` | push | After scan, manage tokens |
| `CreateWalletStart` | push | Create wallet from home |
| `Wallet` | replaceAll | After wallet saved / already saved |

### Welcome

| Target | Method | Trigger |
|--------|--------|---------|
| `CreateWalletSelection` | push | "Add new wallet" button |
| `Home()` | replaceAll | When wallets list becomes empty |
| `Wallet` | replaceAll | After scan / wallet unlock / biometric |

### Disclaimer

| Target | Method | Trigger |
|--------|--------|---------|
| `PushNotification(Stories)` | push | After accepting TOS (stories flow) |
| `Home()` | replaceAll | After accepting TOS (non-stories flow) |

### Wallet (main portfolio)

| Target | Method | Trigger |
|--------|--------|---------|
| `Details` | push | Open wallet details |
| `ManageTokens(ACCOUNT)` | push | Manage tokens for account |
| `Onboarding` | push | Continue backup / onboarding |
| `CurrencyDetails` | push | Tap on a token |
| `Home` | push | Open stories |
| `NFT` | push | Open NFT collection |
| `TangemPayOnboarding` | push | Tangem Pay banner |
| `TangemPayDetails` | push | Tangem Pay card details |
| `YieldSupplyEntry` | push | Yield supply action |
| `QrScanning(MainScreen)` | push | QR scanner |
| `Send` | push | Send from QR / action |
| `WalletBackup` | push | Backup warning banner |

### CurrencyDetails (token details)

| Target | Method | Trigger |
|--------|--------|---------|
| `Onramp` | push | Buy action |
| `SendEntryPoint` | push | Send action |
| `Swap` | push | Swap action |
| `CurrencyDetails` | push | Navigate to related token (from staking router) |
| `Staking` | push | Open staking (from token details router) |

### Details (wallet details hub)

| Target | Method | Trigger |
|--------|--------|---------|
| `CreateWalletSelection` | push | Add new wallet |
| `WalletSettings` | push | Open wallet settings |
| `WalletConnectSessions` | push | WalletConnect item |
| `AppSettings` | push | App settings item |
| `Disclaimer(isTosAccepted=true)` | push | View TOS |
| `Usedesk` | push | Customer support |
| `TangemPayOnboarding(FromBannerInSettings)` | push | Tangem Pay banner |

### WalletSettings

| Target | Method | Trigger |
|--------|--------|---------|
| `ReferralProgram` | push | Referral program |
| `WalletHardwareBackup` | push | Hardware backup |
| `CardSettings` | push | Card settings |
| `ForgetWallet` | push | Delete/forget wallet |
| `ViewPhrase` | push | View seed phrase |
| `AccountDetails` | push | Open account details |
| `ArchivedAccountList` | push | View archived accounts |
| `CreateAccount` | push | Create new account |
| `Home()` | replaceAll | After wallet deletion completes |

### WalletBackup

| Target | Method | Trigger |
|--------|--------|---------|
| `WalletActivation` | push | Start activation (no backup) |
| `ViewPhrase` | push | View phrase option |
| `WalletHardwareBackup` | push | Hardware backup option |

### WalletHardwareBackup

| Target | Method | Trigger |
|--------|--------|---------|
| `CreateHardwareWallet` | push | Create new hardware wallet |
| `UpgradeWallet` | push | Upgrade current hot wallet |
| `CreateWalletBackup` | push | Backup existing wallet |

### CreateWalletSelection

| Target | Method | Trigger |
|--------|--------|---------|
| `CreateMobileWallet` | push | Choose mobile wallet |
| `CreateHardwareWallet` | push | Choose hardware wallet |

### CreateWalletStart

| Target | Method | Trigger |
|--------|--------|---------|
| `CreateMobileWallet` | push | Create mobile wallet |
| `Wallet` | replaceAll | After wallet creation completes |

### CreateMobileWallet

| Target | Method | Trigger |
|--------|--------|---------|
| `AddExistingWallet` | push | Import existing wallet |
| `Wallet` | replaceAll | After creation completes |

### CreateHardwareWallet

| Target | Method | Trigger |
|--------|--------|---------|
| `Wallet` | replaceAll | After hardware wallet created |

### AddExistingWallet

| Target | Method | Trigger |
|--------|--------|---------|
| `Wallet` | replaceAll | After import completes |

### UpgradeWallet

| Target | Method | Trigger |
|--------|--------|---------|
| `Onboarding(UpgradeHotWallet)` | push | Start upgrade onboarding |

### CreateWalletBackup

| Target | Method | Trigger |
|--------|--------|---------|
| `UpgradeWallet` | push | After backup, continue to upgrade |

### AccountDetails

| Target | Method | Trigger |
|--------|--------|---------|
| `EditAccount` | push | Edit account |

### ForgetWallet

| Target | Method | Trigger |
|--------|--------|---------|
| `Home()` | replaceAll | After wallet forgotten |

### WalletConnectSessions

| Target | Method | Trigger |
|--------|--------|---------|
| `QrScanning(WalletConnect)` | push | Scan WC QR code |

### Onboarding

| Target | Method | Trigger |
|--------|--------|---------|
| `Home()` | replaceAll | Onboarding completed (no wallets) |
| `Wallet` | replaceAll | Onboarding completed (has wallets) |

### PushNotification

| Target | Method | Trigger |
|--------|--------|---------|
| `Home()` | replaceAll | After push notification opt-in (via nextRoute param) |

### Staking

| Target | Method | Trigger |
|--------|--------|---------|
| `CurrencyDetails` | push | Back to token details |

### TangemPayDetails

| Target | Method | Trigger |
|--------|--------|---------|
| `Swap` | push | Top up / withdraw via swap |

### NFT

| Target | Method | Trigger |
|--------|--------|---------|
| `NFTSend` | push | Send NFT |

### Send (notifications)

| Target | Method | Trigger |
|--------|--------|---------|
| `CurrencyDetails` | push | Navigate to fee token |

### Swap / BuyCrypto / SellCrypto
| Target | Method | Trigger |
|--------|--------|---------|
| `Swap` | push | After token selection (Swap) |
| `Onramp` | push | After token selection (BuyCrypto/SellCrypto) |

### Deep Link Handlers (push to AppRoute)

| Handler | Target Route |
|---------|-------------|
| `OnrampDeepLinkHandler` | Processes onramp callback params |
| `SellRedirectDeepLinkHandler` | `Send` (with sell redirect params) |
| `BuyDeepLinkHandler` | `BuyCrypto` |
| `SellDeepLinkHandler` | `SellCrypto` |
| `SwapDeepLinkHandler` | `Swap` |
| `ReferralDeepLinkHandler` | Referral handling |
| `WalletDeepLinkHandler` | Wallet handling |
| `TokenDetailsDeepLinkHandler` | `CurrencyDetails` |
| `StakingDeepLinkHandler` | `Staking` |
| `MarketsDeepLinkHandler` | `Markets` |
| `MarketsTokenDetailDeepLinkHandler` | `MarketsTokenDetails` |
| `WalletConnectDeepLinkHandler` | WalletConnect pairing |
| `PromoDeeplinkHandler` | Promo handling |
| `OnboardVisaDeepLinkHandler` | `TangemPayOnboarding` |
| `NewsDetailsDeepLinkHandler` | `NewsDetails` |

## 3. Nested Routes (Feature-Internal Navigation)

### OnboardingRoute
**File:** `features/onboarding-v2/impl/.../routing/OnboardingRoute.kt`

| Route | Description |
|-------|-------------|
| `None` | Initial empty state |
| `Note` | Single-card onboarding note |
| `MultiWallet` | Multi-wallet onboarding (with seed phrase flow option) |
| `Visa` | Visa card onboarding |
| `Twins` | Twin cards onboarding |
| `ManageTokens` | Token management during onboarding |
| `AskBiometry` | Biometry setup prompt |
| `Done` | Onboarding completion |

### WalletRoute
**File:** `features/wallet/impl/.../navigation/WalletRoute.kt`

| Route | Description |
|-------|-------------|
| `Wallet` | Main wallet view |
| `OrganizeTokens` | Reorder tokens in portfolio |

### SendEntryRoute
**File:** `features/send-v2/api/.../entry/SendEntryRoute.kt`

| Route | Description |
|-------|-------------|
| `Send` | Direct send flow |
| `SendWithSwap` | Send with swap option |
| `ChooseToken` | Token chooser for send-via-swap |

### CommonSendRoute (Send internal)
Used internally by `SendModel` and `NFTSendModel`:
- `Amount` → `Destination` → `Confirm` → `ConfirmSuccess`
- Edit mode: `Confirm` → `Destination(edit)` or `Amount(edit)`

### FeeSelectorRoute (Send internal)
- `ChooseToken` — select fee token
- `ChooseSpeed` — select fee speed

### WcInnerRoute (WalletConnect)
**File:** `features/walletconnect/impl/.../routing/WcInnerRoute.kt`

| Route | Description |
|-------|-------------|
| `Method.Send` | WC send transaction |
| `Method.SignMessage` | WC sign message |
| `Method.AddNetwork` | WC add network |
| `Method.SwitchNetwork` | WC switch network |
| `Pair` | WC pairing request |
| `UnsupportedMethodAlert` | Unsupported method alert |
| `WcDappDisconnected` | DApp disconnected alert |
| `TangemUnsupportedNetwork` | Unsupported network alert |
| `RequiredAddNetwork` | Required network add |
| `RequiredReconnectWithNetwork` | Required network reconnect |

### TangemPayDetailsInnerRoute
**File:** `features/tangempay/details/impl/.../navigation/TangemPayDetailsInnerRoute.kt`

| Route | Description |
|-------|-------------|
| `Details` | Main details view |
| `ChangePIN` | Change PIN flow |
| `ChangePINSuccess` | PIN change success |
| `AddToWallet` | Add card to device wallet |

Transitions: `Details` → `ChangePIN` → `ChangePINSuccess`, `Details` → `AddToWallet`

### FeedEntryRoute
**File:** `features/feed/api/.../components/FeedEntryRoute.kt`

| Route | Description |
|-------|-------------|
| `MarketTokenDetails` | Market token detail view |
| `MarketTokenList` | Markets list |
| `NewsDetail` | News article detail |

### CreateWalletBackupRoute
**File:** `features/hot-wallet/impl/.../createwalletbackup/routing/CreateWalletBackupRoute.kt`

| Route | Description |
|-------|-------------|
| `RecoveryPhraseStart` | Backup intro |
| `RecoveryPhrase` | Show recovery phrase |
| `ConfirmBackup` | Confirm backup |
| `BackupCompleted` | Backup complete (with upgrade/last screen flags) |

Transitions: `RecoveryPhraseStart` → `RecoveryPhrase` → `ConfirmBackup` → `BackupCompleted`

### AddExistingWalletRoute
**File:** `features/hot-wallet/impl/.../addexistingwallet/entry/routing/AddExistingWalletRoute.kt`

| Route | Description |
|-------|-------------|
| `Import` | Seed phrase import |
| `BackupCompleted` | Backup completed |
| `SetAccessCode` | Set access code |
| `ConfirmAccessCode` | Confirm access code |
| `PushNotifications` | Push notification opt-in |
| `SetupFinished` | Setup complete |

Transitions: `Import` → `BackupCompleted` → `SetAccessCode` → `ConfirmAccessCode` → `PushNotifications` → `SetupFinished`

### UpdateAccessCodeRoute
**File:** `features/hot-wallet/impl/.../updateaccesscode/routing/UpdateAccessCodeRoute.kt`

| Route | Description |
|-------|-------------|
| `SetAccessCode` | Enter new access code |
| `ConfirmAccessCode` | Confirm new access code |
| `SetupFinished` | Update complete |

Transitions: `SetAccessCode` → `ConfirmAccessCode` → `SetupFinished`

### WalletActivationRoute
**File:** `features/hot-wallet/impl/.../walletactivation/entry/routing/WalletActivationRoute.kt`

| Route | Description |
|-------|-------------|
| `ManualBackupStart` | Backup intro |
| `ManualBackupPhrase` | Show recovery phrase |
| `ManualBackupCheck` | Verify backup |
| `ManualBackupCompleted` | Backup success |
| `SetAccessCode` | Set access code |
| `ConfirmAccessCode` | Confirm access code |
| `PushNotifications` | Push notification opt-in |
| `SetupFinished` | Activation complete |

Transitions: `ManualBackupStart` → `ManualBackupPhrase` → `ManualBackupCheck` → `ManualBackupCompleted` → `SetAccessCode` → `ConfirmAccessCode` → `PushNotifications` → `SetupFinished`

## 4. Deep Links

### URI Schemes

| Scheme | Value | Usage |
|--------|-------|-------|
| `Tangem` | `tangem://` | Primary app deep links |
| `WalletConnect` | `wc://` | WalletConnect pairing |
| `Https` | `https://` | Web links (tangem.com) |

### Tangem Scheme Routes (`tangem://{host}`)

| Host | Handler | Target |
|------|---------|--------|
| `onramp` | `OnrampDeepLinkHandler` | Onramp callback processing |
| `redirect_sell` | `SellRedirectDeepLinkHandler` | `Send` (sell redirect with tx params) |
| `redirect` | — | Buy redirect (no-op) |
| `buy` | `BuyDeepLinkHandler` | `BuyCrypto` |
| `sell` | `SellDeepLinkHandler` | `SellCrypto` |
| `swap` | `SwapDeepLinkHandler` | `Swap` |
| `referral` | `ReferralDeepLinkHandler` | Referral flow |
| `main` | `WalletDeepLinkHandler` | Wallet screen |
| `token` | `TokenDetailsDeepLinkHandler` | `CurrencyDetails` |
| `staking` | `StakingDeepLinkHandler` | `Staking` |
| `markets` | `MarketsDeepLinkHandler` | `Markets` |
| `token_chart` | `MarketsTokenDetailDeepLinkHandler` | `MarketsTokenDetails` |
| `wc` | `WalletConnectDeepLinkHandler` | WalletConnect pairing |
| `promo` | `PromoDeeplinkHandler` | Promo handling |
| `onboard-visa` | `OnboardVisaDeepLinkHandler` | `TangemPayOnboarding` |

### HTTPS Routes (`https://tangem.com/...`)

| Path prefix | Handler | Target |
|-------------|---------|--------|
| `/pay-app` | `OnboardVisaDeepLinkHandler` | `TangemPayOnboarding` |
| `/news` | `NewsDetailsDeepLinkHandler` | `NewsDetails` |

### Deep Link Readiness

Deep links are only processed when the app is on a "ready" route. These routes **block** deep link processing:
- `Initial`, `Home`, `Welcome`, `PushNotification`, `Disclaimer`, `Stories`, `Onboarding`

<!-- NAVGRAPH:BEGIN — managed by the navigation-graph skill. Edit only the CONFIG json; the rest is generated from code. -->

## Connectivity data (generated)

_Auto-generated from code by the `navigation-graph` skill. Edit only the CONFIG block below._

- **Screens (AppRoute):** 73 · screen-nav edges 138
- **Area graph:** 157 feature/domain/data areas · 1024 dependency edges · 12 inverted (domain/data→features)
- **Module graph:** 259 modules · 1508 edges

**Action needed:**
- NEW screen 'Collectibles': group defaulted to 'misc' — set it in CONFIG
- NEW screen 'Collectibles': owner defaulted to 'app' — set it in CONFIG
- NEW screen 'CreateCloudBackup': group defaulted to 'misc' — set it in CONFIG
- NEW screen 'CreateCloudBackup': owner defaulted to 'app' — set it in CONFIG
- NEW screen 'HardwareUpgradeStories': group defaulted to 'misc' — set it in CONFIG
- NEW screen 'HardwareUpgradeStories': owner defaulted to 'app' — set it in CONFIG
- NEW screen 'JointAccountCreation': group defaulted to 'misc' — set it in CONFIG
- NEW screen 'JointAccountCreation': owner defaulted to 'app' — set it in CONFIG
- NEW screen 'JointAccountJoin': group defaulted to 'misc' — set it in CONFIG
- NEW screen 'JointAccountJoin': owner defaulted to 'app' — set it in CONFIG
- NEW screen 'JointAccountMembers': group defaulted to 'misc' — set it in CONFIG
- NEW screen 'JointAccountMembers': owner defaulted to 'app' — set it in CONFIG
- NEW screen 'MoneyAccountOnboarding': group defaulted to 'misc' — set it in CONFIG
- NEW screen 'MoneyAccountOnboarding': owner defaulted to 'app' — set it in CONFIG
- NEW screen 'Polymarket': group defaulted to 'misc' — set it in CONFIG
- NEW screen 'Polymarket': owner defaulted to 'app' — set it in CONFIG
- NEW screen 'TangemPayCampaign': group defaulted to 'misc' — set it in CONFIG
- NEW screen 'TangemPayCampaign': owner defaulted to 'app' — set it in CONFIG
- NEW screen 'TangemPayEligibility': group defaulted to 'misc' — set it in CONFIG
- NEW screen 'TangemPayEligibility': owner defaulted to 'app' — set it in CONFIG

_Config has 1 screen(s) no longer in code (kept, harmless): SwapCrypto_


### Curated config (editable — preserved across refreshes)

<!-- NAVGRAPH:config:BEGIN -->
```json
{
  "hubs": [
    "domain:models",
    "domain:core",
    "domain:app-currency",
    "domain:settings",
    "domain:balance-hiding",
    "domain:feedback",
    "domain:demo"
  ],
  "groups": {
    "shell": {
      "label": "app shell",
      "color": "#5A6677",
      "anchor": {
        "x": 0.5,
        "y": 0.07
      }
    },
    "entry": {
      "label": "entry",
      "color": "#7DA2FF",
      "anchor": {
        "x": 0.18,
        "y": 0.24
      }
    },
    "onboarding": {
      "label": "onboarding",
      "color": "#34D8C4",
      "anchor": {
        "x": 0.5,
        "y": 0.24
      }
    },
    "wallet": {
      "label": "wallet",
      "color": "#E8A33D",
      "anchor": {
        "x": 0.82,
        "y": 0.24
      }
    },
    "portfolio": {
      "label": "portfolio",
      "color": "#B69CFF",
      "anchor": {
        "x": 0.18,
        "y": 0.52
      }
    },
    "tokenaction": {
      "label": "token actions",
      "color": "#FF8F6B",
      "anchor": {
        "x": 0.5,
        "y": 0.52
      }
    },
    "markets": {
      "label": "markets / news",
      "color": "#5BD08A",
      "anchor": {
        "x": 0.82,
        "y": 0.52
      }
    },
    "settings": {
      "label": "settings",
      "color": "#9AA7B8",
      "anchor": {
        "x": 0.18,
        "y": 0.8
      }
    },
    "tangempay": {
      "label": "tangem pay",
      "color": "#F2C14E",
      "anchor": {
        "x": 0.5,
        "y": 0.8
      }
    },
    "misc": {
      "label": "misc",
      "color": "#C7CFDA",
      "anchor": {
        "x": 0.82,
        "y": 0.8
      }
    }
  },
  "groupOrder": [
    "shell",
    "entry",
    "onboarding",
    "wallet",
    "portfolio",
    "tokenaction",
    "markets",
    "settings",
    "tangempay",
    "misc"
  ],
  "defaultGroup": "misc",
  "defaultOwner": "app",
  "screenGroups": {
    "Initial": "entry",
    "Home": "entry",
    "Welcome": "entry",
    "Disclaimer": "entry",
    "CreateWalletSelection": "onboarding",
    "CreateWalletStart": "onboarding",
    "CreateHardwareWallet": "onboarding",
    "CreateMobileWallet": "onboarding",
    "AddExistingWallet": "onboarding",
    "Onboarding": "onboarding",
    "CreateWalletBackup": "onboarding",
    "WalletBackup": "onboarding",
    "WalletHardwareBackup": "onboarding",
    "UpgradeWallet": "onboarding",
    "WalletActivation": "onboarding",
    "AccessCodeRecovery": "onboarding",
    "UpdateAccessCode": "onboarding",
    "ViewPhrase": "onboarding",
    "Wallet": "wallet",
    "Stories": "wallet",
    "NFT": "wallet",
    "NFTSend": "wallet",
    "PushNotification": "wallet",
    "PushNotificationSettings": "wallet",
    "CurrencyDetails": "portfolio",
    "ManageTokens": "portfolio",
    "ChooseManagedTokens": "portfolio",
    "CreateAccount": "portfolio",
    "EditAccount": "portfolio",
    "AccountDetails": "portfolio",
    "ArchivedAccountList": "portfolio",
    "Send": "tokenaction",
    "SendEntryPoint": "tokenaction",
    "Swap": "tokenaction",
    "SwapCrypto": "tokenaction",
    "Onramp": "tokenaction",
    "OnrampSuccess": "tokenaction",
    "BuyCrypto": "tokenaction",
    "SellCrypto": "tokenaction",
    "Staking": "tokenaction",
    "YieldSupplyEntry": "tokenaction",
    "Earn": "markets",
    "Markets": "markets",
    "MarketsTokenDetails": "markets",
    "News": "markets",
    "NewsDetails": "markets",
    "Details": "settings",
    "DetailsSecurity": "settings",
    "CardSettings": "settings",
    "AppSettings": "settings",
    "ResetToFactory": "settings",
    "WalletSettings": "settings",
    "ForgetWallet": "settings",
    "AppCurrencySelector": "settings",
    "ReferralProgram": "settings",
    "AddressBook": "settings",
    "WalletConnectSessions": "settings",
    "TangemPayDetails": "tangempay",
    "TangemPayHotWalletOnboarding": "tangempay",
    "TangemPayOnboarding": "tangempay",
    "Kyc": "tangempay",
    "QrScanning": "misc",
    "Usedesk": "misc",
    "Survey": "misc",
    "Collectibles": "misc",
    "CreateCloudBackup": "misc",
    "HardwareUpgradeStories": "misc",
    "JointAccountCreation": "misc",
    "JointAccountJoin": "misc",
    "JointAccountMembers": "misc",
    "MoneyAccountOnboarding": "misc",
    "Polymarket": "misc",
    "TangemPayCampaign": "misc",
    "TangemPayEligibility": "misc"
  },
  "screenOwners": {
    "Initial": "app",
    "Home": "features:home",
    "Welcome": "features:welcome",
    "Disclaimer": "features:disclaimer",
    "Wallet": "features:wallet",
    "CurrencyDetails": "features:tokendetails",
    "Send": "features:send",
    "Details": "features:details",
    "DetailsSecurity": "features:details",
    "Usedesk": "features:usedesk",
    "CardSettings": "features:details",
    "AppSettings": "features:details",
    "ResetToFactory": "features:details",
    "AccessCodeRecovery": "features:onboarding-v2",
    "ManageTokens": "features:manage-tokens",
    "ChooseManagedTokens": "features:manage-tokens",
    "WalletConnectSessions": "features:walletconnect",
    "AddressBook": "features:address-book",
    "QrScanning": "features:qr-scanning",
    "ReferralProgram": "features:referral",
    "Swap": "features:swap",
    "AppCurrencySelector": "features:wallet-settings",
    "Staking": "features:staking",
    "PushNotification": "features:push-notifications",
    "WalletSettings": "features:wallet-settings",
    "PushNotificationSettings": "features:push-notification-settings",
    "WalletBackup": "features:onboarding-v2",
    "WalletHardwareBackup": "features:onboarding-v2",
    "Markets": "features:markets",
    "MarketsTokenDetails": "features:markets",
    "Onramp": "features:onramp",
    "OnrampSuccess": "features:onramp",
    "BuyCrypto": "features:onramp",
    "SellCrypto": "features:onramp",
    "SwapCrypto": "features:swap-v2",
    "Onboarding": "features:onboarding-v2",
    "Stories": "features:stories",
    "NFT": "features:nft",
    "NFTSend": "features:nft",
    "CreateWalletSelection": "features:create-wallet-selection",
    "CreateWalletStart": "features:create-wallet-start",
    "CreateHardwareWallet": "features:onboarding-v2",
    "CreateMobileWallet": "features:hot-wallet",
    "UpgradeWallet": "features:hot-wallet",
    "AddExistingWallet": "features:onboarding-v2",
    "WalletActivation": "features:tangempay",
    "CreateWalletBackup": "features:onboarding-v2",
    "UpdateAccessCode": "features:onboarding-v2",
    "ViewPhrase": "features:onboarding-v2",
    "ForgetWallet": "features:wallet-settings",
    "SendEntryPoint": "features:send",
    "CreateAccount": "features:account",
    "EditAccount": "features:account",
    "AccountDetails": "features:account",
    "ArchivedAccountList": "features:account",
    "TangemPayDetails": "features:tangempay",
    "TangemPayHotWalletOnboarding": "features:tangempay",
    "TangemPayOnboarding": "features:tangempay",
    "Kyc": "features:kyc",
    "Survey": "features:survey",
    "YieldSupplyEntry": "features:yield-supply",
    "NewsDetails": "features:feed",
    "News": "features:feed",
    "Earn": "features:feed",
    "Collectibles": "app",
    "CreateCloudBackup": "app",
    "HardwareUpgradeStories": "app",
    "JointAccountCreation": "app",
    "JointAccountJoin": "app",
    "JointAccountMembers": "app",
    "MoneyAccountOnboarding": "app",
    "Polymarket": "app",
    "TangemPayCampaign": "app",
    "TangemPayEligibility": "app"
  },
  "teams": [
    {
      "id": "grow",
      "name": "Grow",
      "color": "#FF6FD8",
      "roots": [
        "features:approval",
        "features:onramp",
        "features:send",
        "features:staking",
        "features:swap",
        "features:swap-v2",
        "features:yield-supply",
        "domain:express",
        "domain:offramp",
        "domain:onramp",
        "domain:staking",
        "domain:swap",
        "domain:yield-supply",
        "domain:transaction",
        "data:express",
        "data:onramp",
        "data:staking",
        "data:swap",
        "data:yield-supply",
        "data:transaction"
      ],
      "screens": [
        "Send",
        "Swap",
        "Staking",
        "Onramp",
        "OnrampSuccess",
        "BuyCrypto",
        "SellCrypto",
        "SwapCrypto",
        "NFTSend",
        "SendEntryPoint",
        "YieldSupplyEntry"
      ]
    },
    {
      "id": "store",
      "name": "Store",
      "color": "#3B82F6",
      "roots": [
        "features:account",
        "features:address-book",
        "features:biometry",
        "features:create-wallet-selection",
        "features:create-wallet-start",
        "features:details",
        "features:disclaimer",
        "features:feed",
        "features:home",
        "features:hot-wallet",
        "features:manage-tokens",
        "features:markets",
        "features:nft",
        "features:onboarding-v2",
        "features:promo-banners",
        "features:push-notification-settings",
        "features:push-notifications",
        "features:qr-scanning",
        "features:rating",
        "features:referral",
        "features:stories",
        "features:survey",
        "features:tester",
        "features:usedesk",
        "features:token-recieve",
        "features:tokendetails",
        "features:txhistory",
        "features:wallet",
        "features:wallet-settings",
        "features:walletconnect",
        "features:welcome",
        "domain:account",
        "domain:address-book",
        "domain:analytics",
        "domain:app-currency",
        "domain:app-theme",
        "domain:appsflyer",
        "domain:assetsdiscovery",
        "domain:balance-hiding",
        "domain:card",
        "domain:demo",
        "domain:dynamic-addresses",
        "domain:earn",
        "domain:feedback",
        "domain:hot-wallet",
        "domain:manage-tokens",
        "domain:markets",
        "domain:networks",
        "domain:news",
        "domain:nft",
        "domain:notifications",
        "domain:onboarding",
        "domain:push-notification-preferences",
        "domain:qr-scanning",
        "domain:quotes",
        "domain:referral",
        "domain:search",
        "domain:settings",
        "domain:stories",
        "domain:tokens",
        "domain:txhistory",
        "domain:wallet-connect",
        "domain:wallet-manager",
        "domain:wallets",
        "data:account",
        "data:address-book",
        "data:analytics",
        "data:app-currency",
        "data:app-theme",
        "data:appsflyer",
        "data:assetsdiscovery",
        "data:balance-hiding",
        "data:card",
        "data:dynamic-addresses",
        "data:earn",
        "data:feedback",
        "data:hot-wallet",
        "data:manage-tokens",
        "data:markets",
        "data:networks",
        "data:news",
        "data:nft",
        "data:notifications",
        "data:onboarding",
        "data:push-notification-preferences",
        "data:qr-scanning",
        "data:quotes",
        "data:search",
        "data:settings",
        "data:stories",
        "data:tokens",
        "data:txhistory",
        "data:wallet-connect",
        "data:wallet-manager",
        "data:wallets"
      ],
      "screens": [
        "AccessCodeRecovery",
        "AccountDetails",
        "AddExistingWallet",
        "AddressBook",
        "AppCurrencySelector",
        "AppSettings",
        "ArchivedAccountList",
        "CardSettings",
        "ChooseManagedTokens",
        "CreateAccount",
        "CreateHardwareWallet",
        "CreateMobileWallet",
        "CreateWalletBackup",
        "CreateWalletSelection",
        "CreateWalletStart",
        "CurrencyDetails",
        "Details",
        "DetailsSecurity",
        "Disclaimer",
        "Earn",
        "EditAccount",
        "ForgetWallet",
        "Home",
        "ManageTokens",
        "Markets",
        "MarketsTokenDetails",
        "NFT",
        "News",
        "NewsDetails",
        "Onboarding",
        "PushNotification",
        "PushNotificationSettings",
        "QrScanning",
        "ReferralProgram",
        "ResetToFactory",
        "Stories",
        "Survey",
        "UpdateAccessCode",
        "UpgradeWallet",
        "Usedesk",
        "ViewPhrase",
        "Wallet",
        "WalletBackup",
        "WalletConnectSessions",
        "WalletHardwareBackup",
        "WalletSettings",
        "Welcome"
      ]
    },
    {
      "id": "spend",
      "name": "Spend",
      "color": "#22C55E",
      "roots": [
        "features:kyc",
        "features:payment",
        "features:tangempay",
        "features:virtual-accounts",
        "domain:payment",
        "domain:virtual-account",
        "domain:visa",
        "data:payment",
        "data:virtual-account",
        "data:visa"
      ],
      "screens": [
        "Kyc",
        "TangemPayDetails",
        "TangemPayHotWalletOnboarding",
        "TangemPayOnboarding",
        "WalletActivation"
      ]
    }
  ]
}
```
<!-- NAVGRAPH:config:END -->

### Graph data (auto — overwritten every refresh; do not hand-edit)

<!-- NAVGRAPH:areaGraph:BEGIN -->
```json
{"n":[["data:account","account","data",1,14],["data:address-book","address-book","data",0,4],["data:analytics","analytics","data",0,2],["data:app-currency","app-currency","data",0,2],["data:app-theme","app-theme","data",0,1],["data:app-update","app-update","data",0,1],["data:appsflyer","appsflyer","data",0,2],["data:assetsdiscovery","assetsdiscovery","data",0,6],["data:balance-hiding","balance-hiding","data",0,1],["data:blockaid","blockaid","data",0,2],["data:card","card","data",2,2],["data:cloud-backup","cloud-backup","data",1,1],["data:collectibles","collectibles","data",0,10],["data:common","common","data",32,11],["data:dynamic-addresses","dynamic-addresses","data",1,6],["data:earn","earn","data",0,4],["data:express","express","data",1,6],["data:feed","feed","data",0,1],["data:feedback","feedback","data",0,8],["data:hot-wallet","hot-wallet","data",0,2],["data:joint-account","joint-account","data",0,9],["data:manage-tokens","manage-tokens","data",0,7],["data:marketing","marketing","data",0,1],["data:markets","markets","data",0,5],["data:money-account","money-account","data",0,7],["data:networks","networks","data",0,8],["data:news","news","data",0,2],["data:nft","nft","data",1,6],["data:notifications","notifications","data",0,3],["data:offramp","offramp","data",0,5],["data:onboarding","onboarding","data",0,2],["data:onramp","onramp","data",0,9],["data:polymarket","polymarket","data",0,9],["data:promo","promo","data",0,3],["data:push-notification-preferences","push-notification-preferences","data",0,2],["data:qr-scanning","qr-scanning","data",0,2],["data:quotes","quotes","data",0,4],["data:search","search","data",0,4],["data:settings","settings","data",0,1],["data:social-login","social-login","data",0,2],["data:staking","staking","data",0,10],["data:stories","stories","data",0,1],["data:swap","swap","data",0,9],["data:tokens","tokens","data",0,10],["data:transaction","transaction","data",0,6],["data:txhistory","txhistory","data",0,12],["data:visa","visa","data",0,13],["data:wallet-connect","wallet-connect","data",0,12],["data:wallet-manager","wallet-manager","data",1,10],["data:wallets","wallets","data",3,9],["data:yield-supply","yield-supply","data",0,5],["domain:account","account","domain",41,21],["domain:address-book","address-book","domain",5,3],["domain:analytics","analytics","domain",3,1],["domain:app-currency","app-currency","domain",25,0],["domain:app-theme","app-theme","domain",3,0],["domain:app-update","app-update","domain",3,0],["domain:appsflyer","appsflyer","domain",13,1],["domain:assetsdiscovery","assetsdiscovery","domain",4,2],["domain:balance-hiding","balance-hiding","domain",15,0],["domain:blockaid","blockaid","domain",8,1],["domain:card","card","domain",36,4],["domain:cloud-backup","cloud-backup","domain",7,0],["domain:collectibles","collectibles","domain",5,2],["domain:common","common","domain",62,1],["domain:core","core","domain",51,0],["domain:demo","demo","domain",15,0],["domain:dynamic-addresses","dynamic-addresses","domain",5,3],["domain:earn","earn","domain",3,3],["domain:express","express","domain",13,4],["domain:feed","feed","domain",2,0],["domain:feedback","feedback","domain",18,2],["domain:hot-wallet","hot-wallet","domain",8,1],["domain:joint-account","joint-account","domain",17,3],["domain:legacy","legacy","domain",11,4],["domain:manage-tokens","manage-tokens","domain",4,4],["domain:marketing","marketing","domain",9,0],["domain:markets","markets","domain",10,5],["domain:models","models","domain",126,1],["domain:money-account","money-account","domain",8,6],["domain:networks","networks","domain",13,2],["domain:news","news","domain",2,1],["domain:nft","nft","domain",6,7],["domain:notifications","notifications","domain",14,1],["domain:offramp","offramp","domain",6,2],["domain:onboarding","onboarding","domain",2,1],["domain:onramp","onramp","domain",10,4],["domain:polymarket","polymarket","domain",6,3],["domain:promo","promo","domain",2,1],["domain:push-notification-preferences","push-notification-preferences","domain",5,1],["domain:qr-scanning","qr-scanning","domain",6,5],["domain:quotes","quotes","domain",16,2],["domain:referral","referral","domain",3,0],["domain:search","search","domain",2,4],["domain:settings","settings","domain",22,0],["domain:social-login","social-login","domain",8,1],["domain:staking","staking","domain",12,4],["domain:stories","stories","domain",7,1],["domain:swap","swap","domain",4,2],["domain:tokens","tokens","domain",28,20],["domain:transaction","transaction","domain",26,13],["domain:txhistory","txhistory","domain",13,3],["domain:visa","visa","domain",20,5],["domain:wallet-connect","wallet-connect","domain",4,5],["domain:wallet-manager","wallet-manager","domain",25,3],["domain:wallets","wallets","domain",58,13],["domain:yield-supply","yield-supply","domain",10,8],["features:account","account","features",0,5],["features:address-book","address-book","features",1,7],["features:approval","approval","features",2,4],["features:biometry","biometry","features",2,5],["features:collectibles","collectibles","features",5,5],["features:common-features","common-features","features",15,16],["features:create-wallet-selection","create-wallet-selection","features",0,2],["features:create-wallet-start","create-wallet-start","features",0,8],["features:details","details","features",0,13],["features:disclaimer","disclaimer","features",0,4],["features:feed","feed","features",1,31],["features:for-you","for-you","features",1,14],["features:force-update","force-update","features",0,2],["features:home","home","features",0,6],["features:hot-wallet","hot-wallet","features",4,15],["features:introduction","introduction","features",0,14],["features:joint-account","joint-account","features",10,10],["features:kyc","kyc","features",0,2],["features:manage-tokens","manage-tokens","features",3,11],["features:marketing","marketing","features",6,1],["features:markets","markets","features",1,3],["features:money-account","money-account","features",5,4],["features:nft","nft","features",1,9],["features:onboarding-v2","onboarding-v2","features",4,19],["features:onramp","onramp","features",0,20],["features:polymarket","polymarket","features",5,8],["features:promo-banners","promo-banners","features",4,9],["features:push-notification-settings","push-notification-settings","features",0,6],["features:push-notifications","push-notifications","features",10,6],["features:qr-scanning","qr-scanning","features",0,2],["features:rating","rating","features",2,2],["features:referral","referral","features",0,13],["features:send","send","features",5,26],["features:staking","staking","features",0,17],["features:stories","stories","features",0,1],["features:stories-v2","stories-v2","features",3,0],["features:survey","survey","features",1,2],["features:swap","swap","features",3,32],["features:swap-v2","swap-v2","features",2,18],["features:tangempay","tangempay","features",2,14],["features:tester","tester","features",0,18],["features:token-recieve","token-recieve","features",7,3],["features:tokendetails","tokendetails","features",1,33],["features:txhistory","txhistory","features",3,18],["features:usedesk","usedesk","features",0,3],["features:wallet","wallet","features",7,54],["features:wallet-settings","wallet-settings","features",3,18],["features:walletconnect","walletconnect","features",0,12],["features:welcome","welcome","features",0,7],["features:yield-supply","yield-supply","features",2,14]],"e":[["features:usedesk","domain:feedback",3,1],["features:usedesk","domain:models",1,0],["features:usedesk","domain:settings",1,1],["features:home","domain:appsflyer",1,1],["features:home","domain:card",1,1],["features:home","domain:common",1,1],["features:home","domain:settings",1,1],["features:home","domain:wallets",1,1],["features:home","domain:models",1,0],["features:create-wallet-start","features:onboarding-v2",1,1],["features:create-wallet-start","domain:card",1,1],["features:create-wallet-start","domain:appsflyer",1,0],["features:create-wallet-start","domain:common",1,1],["features:create-wallet-start","domain:hot-wallet",1,1],["features:create-wallet-start","domain:settings",1,1],["features:create-wallet-start","domain:wallets",2,1],["features:create-wallet-start","domain:models",1,0],["features:yield-supply","features:marketing",1,0],["features:yield-supply","domain:models",2,1],["features:yield-supply","domain:appsflyer",1,0],["features:yield-supply","domain:app-currency",2,0],["features:yield-supply","domain:account",2,0],["features:yield-supply","domain:wallets",2,0],["features:yield-supply","domain:tokens",3,1],["features:yield-supply","domain:transaction",2,0],["features:yield-supply","domain:yield-supply",2,0],["features:yield-supply","domain:stories",2,0],["features:yield-supply","domain:feedback",2,0],["features:yield-supply","domain:marketing",1,0],["features:yield-supply","domain:onramp",1,0],["features:yield-supply","domain:networks",1,1],["features:txhistory","features:tokendetails",1,0],["features:txhistory","features:joint-account",1,0],["features:txhistory","features:rating",1,0],["features:txhistory","domain:account",2,1],["features:txhistory","domain:balance-hiding",2,1],["features:txhistory","domain:common",1,1],["features:txhistory","domain:models",2,1],["features:txhistory","domain:staking",2,1],["features:txhistory","domain:txhistory",3,1],["features:txhistory","domain:wallets",1,1],["features:txhistory","domain:address-book",1,0],["features:txhistory","domain:app-currency",2,0],["features:txhistory","domain:joint-account",1,0],["features:txhistory","domain:express",1,0],["features:txhistory","domain:feedback",2,0],["features:txhistory","domain:onramp",1,0],["features:txhistory","domain:tokens",1,0],["features:txhistory","domain:visa",1,0],["features:collectibles","features:token-recieve",1,0],["features:collectibles","domain:collectibles",2,0],["features:collectibles","domain:common",1,0],["features:collectibles","domain:models",2,1],["features:collectibles","domain:app-currency",2,0],["features:referral","features:common-features",1,1],["features:referral","domain:account",4,1],["features:referral","domain:app-currency",2,1],["features:referral","domain:balance-hiding",1,1],["features:referral","domain:demo",1,1],["features:referral","domain:wallets",2,1],["features:referral","domain:card",1,0],["features:referral","domain:models",4,1],["features:referral","data:common",1,0],["features:referral","domain:common",1,1],["features:referral","domain:referral",1,1],["features:referral","domain:wallet-manager",1,1],["features:referral","domain:core",1,0],["features:wallet-settings","features:joint-account",1,1],["features:wallet-settings","features:wallet",1,1],["features:wallet-settings","features:hot-wallet",1,0],["features:wallet-settings","features:push-notifications",1,0],["features:wallet-settings","data:account",1,0],["features:wallet-settings","domain:account",2,1],["features:wallet-settings","domain:app-currency",2,1],["features:wallet-settings","domain:assetsdiscovery",1,1],["features:wallet-settings","domain:balance-hiding",1,1],["features:wallet-settings","domain:demo",1,1],["features:wallet-settings","domain:cloud-backup",1,0],["features:wallet-settings","domain:social-login",1,0],["features:wallet-settings","domain:nft",1,1],["features:wallet-settings","domain:notifications",2,1],["features:wallet-settings","domain:wallets",2,1],["features:wallet-settings","domain:card",1,0],["features:wallet-settings","domain:common",1,0],["features:wallet-settings","domain:models",2,1],["features:token-recieve","domain:models",2,1],["features:token-recieve","domain:transaction",1,0],["features:token-recieve","domain:tokens",2,0],["features:rating","features:swap",1,0],["features:rating","domain:models",2,1],["features:kyc","domain:visa",2,1],["features:kyc","domain:models",2,1],["features:disclaimer","domain:card",1,1],["features:disclaimer","domain:notifications",1,1],["features:disclaimer","domain:settings",1,1],["features:disclaimer","features:push-notifications",1,0],["features:nft","features:common-features",1,1],["features:nft","features:token-recieve",1,1],["features:nft","domain:account",2,1],["features:nft","domain:app-currency",2,1],["features:nft","domain:nft",3,1],["features:nft","domain:transaction",1,1],["features:nft","domain:wallets",1,1],["features:nft","domain:core",1,0],["features:nft","domain:models",2,1],["features:tokendetails","features:rating",1,0],["features:tokendetails","domain:account",2,1],["features:tokendetails","domain:app-currency",2,0],["features:tokendetails","domain:balance-hiding",2,0],["features:tokendetails","domain:card",1,0],["features:tokendetails","domain:demo",1,0],["features:tokendetails","domain:dynamic-addresses",2,0],["features:tokendetails","domain:joint-account",1,0],["features:tokendetails","domain:marketing",1,0],["features:tokendetails","domain:models",2,1],["features:tokendetails","domain:notifications",1,0],["features:tokendetails","domain:offramp",1,0],["features:tokendetails","domain:onramp",2,0],["features:tokendetails","domain:staking",3,0],["features:tokendetails","domain:tokens",3,1],["features:tokendetails","domain:transaction",2,0],["features:tokendetails","domain:txhistory",2,0],["features:tokendetails","domain:wallets",2,0],["features:tokendetails","domain:yield-supply",2,0],["features:tokendetails","features:swap",1,0],["features:tokendetails","features:wallet",1,0],["features:tokendetails","features:markets",1,0],["features:tokendetails","features:marketing",1,0],["features:tokendetails","features:push-notifications",1,0],["features:tokendetails","features:txhistory",1,0],["features:tokendetails","features:joint-account",1,0],["features:tokendetails","features:token-recieve",1,0],["features:tokendetails","features:yield-supply",1,0],["features:tokendetails","features:common-features",1,0],["features:tokendetails","domain:core",1,0],["features:tokendetails","domain:common",1,1],["features:tokendetails","domain:visa",1,1],["features:tokendetails","domain:wallet-manager",1,1],["features:introduction","features:hot-wallet",1,0],["features:introduction","features:push-notifications",1,0],["features:introduction","features:stories-v2",1,0],["features:introduction","domain:analytics",1,0],["features:introduction","domain:appsflyer",1,0],["features:introduction","domain:card",1,0],["features:introduction","domain:cloud-backup",1,0],["features:introduction","domain:common",1,0],["features:introduction","domain:feedback",2,0],["features:introduction","domain:hot-wallet",1,0],["features:introduction","domain:models",1,0],["features:introduction","domain:settings",1,0],["features:introduction","domain:social-login",1,0],["features:introduction","domain:wallets",2,0],["features:for-you","features:promo-banners",1,1],["features:for-you","features:common-features",1,0],["features:for-you","domain:account",2,1],["features:for-you","domain:app-currency",2,1],["features:for-you","domain:common",1,1],["features:for-you","domain:earn",1,1],["features:for-you","domain:markets",3,1],["features:for-you","domain:yield-supply",2,1],["features:for-you","domain:staking",2,1],["features:for-you","domain:balance-hiding",1,1],["features:for-you","domain:wallets",1,1],["features:for-you","domain:models",2,1],["features:for-you","domain:tokens",2,0],["features:for-you","data:common",1,0],["features:qr-scanning","domain:qr-scanning",3,1],["features:qr-scanning","data:card",1,0],["features:swap","features:approval",1,1],["features:swap","features:common-features",1,1],["features:swap","features:send",3,1],["features:swap","features:marketing",1,0],["features:swap","data:common",2,1],["features:swap","domain:account",4,1],["features:swap","domain:appsflyer",1,0],["features:swap","domain:app-currency",4,1],["features:swap","domain:balance-hiding",3,1],["features:swap","domain:express",3,1],["features:swap","domain:feedback",2,1],["features:swap","domain:legacy",2,1],["features:swap","domain:models",4,1],["features:swap","domain:settings",1,1],["features:swap","domain:stories",2,1],["features:swap","domain:swap",4,1],["features:swap","domain:tokens",4,1],["features:swap","domain:transaction",4,1],["features:swap","domain:txhistory",3,1],["features:swap","domain:visa",4,1],["features:swap","domain:wallets",2,1],["features:swap","domain:core",1,0],["features:swap","domain:marketing",1,0],["features:swap","domain:markets",1,0],["features:swap","domain:polymarket",1,0],["features:swap","domain:quotes",2,1],["features:swap","domain:onramp",1,0],["features:swap","domain:common",1,0],["features:swap","domain:demo",1,1],["features:swap","domain:notifications",1,1],["features:swap","domain:wallet-manager",1,1],["features:swap","domain:yield-supply",1,1],["features:details","features:onboarding-v2",1,1],["features:details","features:wallet",1,1],["features:details","features:tangempay",1,0],["features:details","features:money-account",1,0],["features:details","domain:card",1,1],["features:details","domain:feedback",2,1],["features:details","domain:settings",1,1],["features:details","domain:money-account",2,1],["features:details","domain:visa",1,1],["features:details","domain:wallet-connect",1,1],["features:details","domain:wallets",2,1],["features:details","domain:common",1,0],["features:details","domain:models",2,1],["features:create-wallet-selection","domain:hot-wallet",1,1],["features:create-wallet-selection","domain:wallets",1,1],["features:welcome","features:wallet",1,0],["features:welcome","features:onboarding-v2",1,0],["features:welcome","domain:models",1,0],["features:welcome","domain:wallets",1,0],["features:welcome","domain:card",1,0],["features:welcome","domain:settings",1,0],["features:welcome","domain:common",1,1],["features:polymarket","features:common-features",1,1],["features:polymarket","features:token-recieve",1,0],["features:polymarket","domain:feedback",2,0],["features:polymarket","domain:common",1,0],["features:polymarket","domain:models",2,1],["features:polymarket","domain:polymarket",1,0],["features:polymarket","domain:core",1,0],["features:polymarket","domain:stories",2,0],["features:common-features","features:joint-account",1,0],["features:common-features","features:token-recieve",1,1],["features:common-features","features:wallet",1,1],["features:common-features","domain:account",3,1],["features:common-features","domain:app-currency",3,1],["features:common-features","domain:balance-hiding",1,1],["features:common-features","domain:manage-tokens",1,1],["features:common-features","domain:markets",3,1],["features:common-features","domain:joint-account",1,0],["features:common-features","domain:models",2,1],["features:common-features","domain:transaction",1,1],["features:common-features","domain:wallets",1,1],["features:common-features","domain:card",1,0],["features:common-features","domain:core",1,0],["features:common-features","domain:tokens",2,0],["features:common-features","domain:yield-supply",1,0],["features:onramp","features:common-features",1,1],["features:onramp","features:marketing",1,0],["features:onramp","domain:marketing",1,0],["features:onramp","domain:quotes",1,0],["features:onramp","domain:account",2,1],["features:onramp","domain:app-currency",2,1],["features:onramp","domain:app-theme",2,1],["features:onramp","domain:balance-hiding",2,1],["features:onramp","domain:demo",1,1],["features:onramp","domain:joint-account",1,0],["features:onramp","domain:legacy",1,1],["features:onramp","domain:models",2,1],["features:onramp","domain:offramp",1,1],["features:onramp","domain:onramp",3,1],["features:onramp","domain:settings",1,1],["features:onramp","domain:tokens",2,1],["features:onramp","domain:wallets",2,1],["features:onramp","domain:core",1,0],["features:onramp","domain:transaction",1,0],["features:onramp","data:common",1,1],["features:walletconnect","features:common-features",1,0],["features:walletconnect","features:send",1,0],["features:walletconnect","domain:account",2,0],["features:walletconnect","domain:blockaid",1,0],["features:walletconnect","domain:models",2,1],["features:walletconnect","domain:qr-scanning",2,0],["features:walletconnect","domain:tokens",1,0],["features:walletconnect","domain:transaction",2,0],["features:walletconnect","domain:wallets",2,0],["features:walletconnect","domain:wallet-connect",2,0],["features:walletconnect","data:card",1,0],["features:walletconnect","domain:core",1,0],["features:stories","domain:stories",2,0],["features:onboarding-v2","features:biometry",1,1],["features:onboarding-v2","features:hot-wallet",1,1],["features:onboarding-v2","features:manage-tokens",1,1],["features:onboarding-v2","features:push-notifications",1,1],["features:onboarding-v2","domain:account",1,1],["features:onboarding-v2","domain:appsflyer",1,0],["features:onboarding-v2","domain:card",1,1],["features:onboarding-v2","domain:cloud-backup",1,0],["features:onboarding-v2","domain:common",1,1],["features:onboarding-v2","domain:feedback",2,1],["features:onboarding-v2","domain:models",2,1],["features:onboarding-v2","domain:networks",1,1],["features:onboarding-v2","domain:onboarding",1,1],["features:onboarding-v2","domain:settings",1,1],["features:onboarding-v2","domain:social-login",1,1],["features:onboarding-v2","domain:staking",1,1],["features:onboarding-v2","domain:tokens",1,1],["features:onboarding-v2","domain:visa",2,1],["features:onboarding-v2","domain:wallets",2,1],["features:promo-banners","features:common-features",1,0],["features:promo-banners","domain:common",1,1],["features:promo-banners","domain:models",1,0],["features:promo-banners","domain:app-currency",2,0],["features:promo-banners","domain:promo",2,0],["features:promo-banners","domain:markets",2,0],["features:promo-banners","data:common",1,0],["features:promo-banners","domain:wallets",2,1],["features:promo-banners","domain:account",1,1],["features:push-notifications","domain:settings",1,0],["features:push-notifications","domain:notifications",1,0],["features:push-notifications","domain:push-notification-preferences",1,0],["features:push-notifications","domain:common",1,0],["features:push-notifications","domain:models",1,0],["features:push-notifications","domain:wallets",1,0],["features:swap-v2","features:manage-tokens",1,0],["features:swap-v2","features:send",2,1],["features:swap-v2","domain:models",2,1],["features:swap-v2","domain:wallets",2,0],["features:swap-v2","domain:tokens",2,0],["features:swap-v2","domain:app-currency",2,0],["features:swap-v2","domain:express",1,0],["features:swap-v2","domain:swap",3,1],["features:swap-v2","domain:manage-tokens",3,1],["features:swap-v2","domain:transaction",2,0],["features:swap-v2","domain:legacy",1,0],["features:swap-v2","domain:balance-hiding",2,0],["features:swap-v2","domain:settings",1,0],["features:swap-v2","domain:txhistory",1,0],["features:swap-v2","domain:notifications",1,0],["features:swap-v2","domain:feedback",2,0],["features:swap-v2","domain:account",1,0],["features:swap-v2","domain:quotes",1,1],["features:force-update","domain:app-update",1,0],["features:force-update","domain:feedback",2,0],["features:manage-tokens","features:common-features",1,1],["features:manage-tokens","features:swap-v2",1,1],["features:manage-tokens","domain:account",2,1],["features:manage-tokens","domain:dynamic-addresses",1,1],["features:manage-tokens","domain:manage-tokens",2,1],["features:manage-tokens","domain:models",2,1],["features:manage-tokens","domain:notifications",1,1],["features:manage-tokens","domain:wallets",1,1],["features:manage-tokens","domain:core",1,0],["features:manage-tokens","domain:markets",1,0],["features:manage-tokens","domain:swap",1,0],["features:markets","domain:app-currency",2,1],["features:markets","domain:markets",2,1],["features:markets","domain:models",2,1],["features:feed","features:common-features",2,1],["features:feed","features:for-you",1,1],["features:feed","features:promo-banners",4,1],["features:feed","features:marketing",1,1],["features:feed","features:polymarket",1,0],["features:feed","features:collectibles",1,0],["features:feed","features:joint-account",1,0],["features:feed","features:wallet-settings",1,0],["features:feed","data:common",1,0],["features:feed","domain:account",2,1],["features:feed","domain:app-currency",9,1],["features:feed","domain:balance-hiding",4,1],["features:feed","domain:common",4,1],["features:feed","domain:earn",2,1],["features:feed","domain:feedback",2,1],["features:feed","domain:markets",8,1],["features:feed","domain:models",5,1],["features:feed","domain:news",2,1],["features:feed","domain:search",4,1],["features:feed","domain:settings",2,1],["features:feed","domain:staking",1,1],["features:feed","domain:tokens",5,1],["features:feed","domain:wallets",5,1],["features:feed","domain:yield-supply",2,1],["features:feed","domain:card",1,0],["features:feed","domain:core",1,0],["features:feed","domain:demo",1,0],["features:feed","domain:referral",1,0],["features:feed","domain:visa",1,0],["features:feed","domain:marketing",1,0],["features:feed","domain:feed",3,0],["features:staking","domain:tokens",2,0],["features:staking","domain:wallets",1,0],["features:staking","domain:staking",4,1],["features:staking","domain:balance-hiding",2,0],["features:staking","domain:app-currency",2,0],["features:staking","domain:legacy",1,0],["features:staking","domain:models",2,1],["features:staking","domain:transaction",2,0],["features:staking","domain:txhistory",2,0],["features:staking","domain:feedback",2,0],["features:staking","domain:account",1,0],["features:staking","domain:marketing",1,0],["features:staking","domain:onramp",2,0],["features:staking","features:txhistory",1,0],["features:staking","features:approval",1,0],["features:staking","features:marketing",1,0],["features:staking","domain:core",1,0],["features:money-account","domain:models",3,1],["features:money-account","domain:common",1,1],["features:money-account","domain:visa",2,1],["features:money-account","domain:money-account",1,0],["features:address-book","features:common-features",1,1],["features:address-book","domain:address-book",1,1],["features:address-book","domain:wallets",1,1],["features:address-book","domain:models",2,1],["features:address-book","domain:qr-scanning",2,0],["features:address-book","domain:common",1,1],["features:address-book","domain:transaction",1,0],["features:wallet","domain:account",2,1],["features:wallet","domain:appsflyer",1,0],["features:wallet","domain:address-book",1,1],["features:wallet","domain:analytics",1,1],["features:wallet","domain:app-currency",2,1],["features:wallet","domain:app-theme",2,1],["features:wallet","domain:app-update",1,1],["features:wallet","domain:assetsdiscovery",1,1],["features:wallet","domain:balance-hiding",2,1],["features:wallet","domain:card",1,1],["features:wallet","domain:common",1,1],["features:wallet","domain:demo",2,1],["features:wallet","domain:feedback",2,1],["features:wallet","domain:hot-wallet",1,1],["features:wallet","domain:legacy",1,1],["features:wallet","domain:models",2,1],["features:wallet","domain:money-account",1,1],["features:wallet","domain:networks",1,1],["features:wallet","domain:nft",2,1],["features:wallet","domain:notifications",1,1],["features:wallet","domain:offramp",1,1],["features:wallet","domain:onramp",2,1],["features:wallet","domain:push-notification-preferences",1,1],["features:wallet","domain:qr-scanning",2,1],["features:wallet","domain:quotes",1,1],["features:wallet","domain:settings",1,1],["features:wallet","domain:staking",2,1],["features:wallet","domain:stories",2,1],["features:wallet","domain:tokens",2,1],["features:wallet","domain:transaction",1,1],["features:wallet","domain:txhistory",2,1],["features:wallet","domain:visa",3,1],["features:wallet","domain:wallet-connect",2,1],["features:wallet","domain:wallet-manager",1,1],["features:wallet","domain:wallets",2,1],["features:wallet","domain:yield-supply",2,1],["features:wallet","domain:core",1,0],["features:wallet","domain:marketing",1,0],["features:wallet","domain:markets",1,0],["features:wallet","features:biometry",1,1],["features:wallet","features:collectibles",1,1],["features:wallet","features:common-features",1,1],["features:wallet","features:feed",1,1],["features:wallet","features:joint-account",1,1],["features:wallet","domain:joint-account",1,0],["features:wallet","features:polymarket",1,1],["features:wallet","features:promo-banners",1,1],["features:wallet","features:push-notifications",1,1],["features:wallet","features:send",1,1],["features:wallet","features:tangempay",2,1],["features:wallet","features:token-recieve",1,1],["features:wallet","features:money-account",2,1],["features:wallet","features:wallet-settings",1,1],["features:wallet","features:yield-supply",1,1],["features:marketing","domain:marketing",3,0],["features:tester","domain:account",1,1],["features:tester","domain:common",1,1],["features:tester","domain:feedback",2,1],["features:tester","domain:markets",2,1],["features:tester","domain:settings",1,1],["features:tester","domain:wallets",1,1],["features:tester","domain:app-currency",1,0],["features:tester","domain:demo",1,0],["features:tester","domain:models",1,0],["features:tester","domain:offramp",1,0],["features:tester","domain:social-login",1,0],["features:tester","domain:wallet-manager",1,0],["features:tester","data:common",1,1],["features:tester","features:stories-v2",1,1],["features:tester","features:survey",1,1],["features:tester","features:hot-wallet",1,0],["features:tester","features:push-notifications",1,0],["features:tester","domain:staking",1,0],["features:biometry","domain:card",1,1],["features:biometry","domain:common",1,1],["features:biometry","domain:settings",1,1],["features:biometry","domain:wallets",1,1],["features:biometry","domain:models",1,0],["features:account","domain:account",2,1],["features:account","domain:models",2,1],["features:account","domain:wallets",1,1],["features:account","domain:core",1,0],["features:account","features:joint-account",1,0],["features:hot-wallet","features:onboarding-v2",1,1],["features:hot-wallet","features:push-notifications",1,1],["features:hot-wallet","features:stories-v2",1,0],["features:hot-wallet","domain:assetsdiscovery",1,1],["features:hot-wallet","domain:appsflyer",1,0],["features:hot-wallet","domain:card",1,1],["features:hot-wallet","domain:common",1,1],["features:hot-wallet","domain:feedback",2,1],["features:hot-wallet","domain:hot-wallet",1,1],["features:hot-wallet","domain:settings",1,1],["features:hot-wallet","domain:social-login",2,1],["features:hot-wallet","domain:wallets",3,1],["features:hot-wallet","domain:cloud-backup",2,1],["features:hot-wallet","domain:models",2,1],["features:hot-wallet","data:cloud-backup",1,0],["features:survey","domain:models",1,0],["features:survey","domain:wallets",2,0],["features:send","features:txhistory",1,0],["features:send","features:nft",1,0],["features:send","features:swap-v2",1,0],["features:send","features:manage-tokens",1,0],["features:send","features:address-book",1,0],["features:send","data:nft",1,0],["features:send","domain:models",2,1],["features:send","domain:joint-account",1,0],["features:send","domain:legacy",1,0],["features:send","domain:offramp",1,0],["features:send","domain:tokens",2,0],["features:send","domain:wallets",3,1],["features:send","domain:app-currency",3,1],["features:send","domain:transaction",5,1],["features:send","domain:txhistory",3,0],["features:send","domain:qr-scanning",2,0],["features:send","domain:settings",1,0],["features:send","domain:feedback",2,0],["features:send","domain:balance-hiding",2,0],["features:send","domain:nft",3,1],["features:send","domain:notifications",1,0],["features:send","domain:account",2,0],["features:send","domain:address-book",1,0],["features:send","domain:core",1,0],["features:send","domain:visa",1,0],["features:send","domain:quotes",1,1],["features:joint-account","features:common-features",1,0],["features:joint-account","features:push-notifications",1,0],["features:joint-account","features:wallet",1,0],["features:joint-account","domain:account",2,0],["features:joint-account","domain:common",1,0],["features:joint-account","domain:joint-account",1,0],["features:joint-account","domain:models",2,1],["features:joint-account","domain:notifications",1,0],["features:joint-account","domain:wallets",1,0],["features:joint-account","domain:app-currency",1,0],["features:tangempay","features:token-recieve",1,1],["features:tangempay","features:promo-banners",1,1],["features:tangempay","features:money-account",1,1],["features:tangempay","domain:balance-hiding",2,1],["features:tangempay","domain:feedback",2,1],["features:tangempay","domain:models",6,1],["features:tangempay","domain:visa",7,1],["features:tangempay","domain:wallets",3,1],["features:tangempay","features:common-features",1,0],["features:tangempay","domain:money-account",2,0],["features:tangempay","features:wallet",1,1],["features:tangempay","domain:common",1,1],["features:tangempay","domain:hot-wallet",1,1],["features:tangempay","domain:appsflyer",1,0],["features:approval","features:send",1,1],["features:approval","domain:transaction",2,1],["features:approval","domain:wallets",1,1],["features:approval","domain:models",2,1],["features:push-notification-settings","features:wallet-settings",1,1],["features:push-notification-settings","features:push-notifications",1,0],["features:push-notification-settings","domain:push-notification-preferences",1,1],["features:push-notification-settings","domain:models",2,1],["features:push-notification-settings","domain:notifications",1,0],["features:push-notification-settings","domain:wallets",1,0],["data:social-login","domain:models",1,0],["data:social-login","domain:social-login",1,1],["data:transaction","data:common",1,1],["data:transaction","domain:models",1,1],["data:transaction","domain:transaction",2,1],["data:transaction","domain:wallet-manager",1,1],["data:transaction","domain:demo",2,0],["data:transaction","domain:wallets",1,1],["data:settings","domain:settings",1,1],["data:dynamic-addresses","data:common",1,1],["data:dynamic-addresses","domain:account",1,1],["data:dynamic-addresses","domain:common",1,1],["data:dynamic-addresses","domain:dynamic-addresses",2,1],["data:dynamic-addresses","domain:models",1,1],["data:dynamic-addresses","domain:wallet-manager",1,1],["data:app-theme","domain:app-theme",2,1],["data:yield-supply","domain:transaction",1,1],["data:yield-supply","domain:wallet-manager",1,1],["data:yield-supply","domain:yield-supply",2,1],["data:yield-supply","domain:legacy",1,0],["data:yield-supply","domain:models",1,0],["data:txhistory","domain:common",2,1],["data:txhistory","domain:wallet-manager",2,1],["data:txhistory","domain:models",2,1],["data:txhistory","domain:tokens",2,0],["data:txhistory","domain:txhistory",4,1],["data:txhistory","domain:express",4,1],["data:txhistory","domain:wallets",2,1],["data:txhistory","domain:onramp",4,1],["data:txhistory","domain:account",4,1],["data:txhistory","domain:visa",4,0],["data:txhistory","data:common",1,1],["data:txhistory","domain:quotes",1,0],["data:collectibles","data:common",1,0],["data:collectibles","data:wallets",1,0],["data:collectibles","domain:collectibles",2,1],["data:collectibles","domain:card",1,0],["data:collectibles","domain:wallet-manager",1,0],["data:collectibles","domain:core",1,0],["data:collectibles","domain:quotes",1,0],["data:collectibles","domain:common",1,0],["data:collectibles","domain:models",1,0],["data:collectibles","domain:wallets",1,0],["data:push-notification-preferences","domain:push-notification-preferences",1,1],["data:push-notification-preferences","domain:models",1,0],["data:card","domain:card",1,1],["data:card","domain:models",1,1],["data:nft","data:common",1,1],["data:nft","domain:common",1,1],["data:nft","domain:wallet-manager",1,1],["data:nft","domain:card",1,0],["data:nft","domain:models",1,0],["data:nft","domain:nft",2,0],["data:quotes","data:common",1,1],["data:quotes","domain:core",1,1],["data:quotes","domain:quotes",1,1],["data:quotes","domain:models",1,0],["data:wallet-manager","domain:common",1,1],["data:wallet-manager","domain:models",1,1],["data:wallet-manager","domain:transaction",2,1],["data:wallet-manager","domain:card",1,0],["data:wallet-manager","domain:joint-account",1,0],["data:wallet-manager","domain:txhistory",2,0],["data:wallet-manager","domain:wallet-manager",2,0],["data:wallet-manager","domain:wallets",1,0],["data:wallet-manager","domain:demo",1,0],["data:wallet-manager","domain:express",1,0],["data:express","data:common",1,0],["data:express","domain:common",1,1],["data:express","domain:express",2,1],["data:express","domain:txhistory",1,1],["data:express","domain:core",1,0],["data:express","domain:models",1,0],["data:qr-scanning","domain:qr-scanning",2,1],["data:qr-scanning","domain:models",1,0],["data:blockaid","domain:blockaid",2,1],["data:blockaid","domain:models",1,1],["data:app-currency","data:common",1,1],["data:app-currency","domain:app-currency",2,1],["data:swap","data:common",1,1],["data:swap","data:express",1,0],["data:swap","domain:account",1,1],["data:swap","domain:express",2,1],["data:swap","domain:models",1,1],["data:swap","domain:quotes",1,1],["data:swap","domain:swap",2,1],["data:swap","domain:core",1,0],["data:swap","domain:tokens",1,0],["data:earn","data:common",1,0],["data:earn","domain:common",1,1],["data:earn","domain:earn",1,1],["data:earn","domain:models",1,0],["data:wallet-connect","data:common",1,0],["data:wallet-connect","domain:account",2,1],["data:wallet-connect","domain:blockaid",2,1],["data:wallet-connect","domain:common",1,1],["data:wallet-connect","domain:networks",1,1],["data:wallet-connect","domain:transaction",2,1],["data:wallet-connect","domain:wallet-connect",2,1],["data:wallet-connect","domain:wallet-manager",1,1],["data:wallet-connect","domain:wallets",1,1],["data:wallet-connect","domain:core",1,0],["data:wallet-connect","domain:models",1,0],["data:wallet-connect","domain:tokens",1,0],["data:visa","data:common",1,1],["data:visa","data:wallets",1,0],["data:visa","domain:common",1,1],["data:visa","domain:core",1,1],["data:visa","domain:account",1,1],["data:visa","domain:quotes",1,1],["data:visa","domain:visa",2,1],["data:visa","domain:wallets",1,1],["data:visa","domain:card",1,0],["data:visa","domain:money-account",1,0],["data:visa","domain:models",1,0],["data:visa","domain:app-currency",1,0],["data:visa","features:swap",1,1],["data:polymarket","domain:core",1,0],["data:polymarket","data:common",1,0],["data:polymarket","domain:polymarket",1,1],["data:polymarket","domain:quotes",1,0],["data:polymarket","domain:wallets",1,0],["data:polymarket","domain:common",1,0],["data:polymarket","domain:card",1,0],["data:polymarket","data:wallets",1,0],["data:polymarket","domain:models",1,0],["data:balance-hiding","domain:balance-hiding",2,1],["data:feedback","domain:common",1,1],["data:feedback","domain:cloud-backup",2,1],["data:feedback","domain:feedback",2,1],["data:feedback","domain:card",1,0],["data:feedback","domain:models",1,0],["data:feedback","domain:social-login",1,0],["data:feedback","domain:wallet-manager",1,0],["data:feedback","domain:wallets",1,0],["data:search","domain:account",1,1],["data:search","domain:common",1,1],["data:search","domain:search",1,1],["data:search","domain:models",1,0],["data:onramp","data:common",1,1],["data:onramp","domain:common",1,1],["data:onramp","domain:models",1,1],["data:onramp","domain:onramp",2,1],["data:onramp","domain:txhistory",1,1],["data:onramp","domain:wallet-manager",1,1],["data:onramp","domain:card",1,0],["data:onramp","domain:express",1,0],["data:onramp","domain:tokens",1,0],["data:networks","data:common",1,1],["data:networks","data:dynamic-addresses",1,1],["data:networks","domain:common",1,1],["data:networks","domain:core",1,1],["data:networks","domain:models",1,1],["data:networks","domain:networks",1,1],["data:networks","domain:wallet-manager",1,1],["data:networks","domain:card",1,0],["data:common","domain:common",1,1],["data:common","domain:appsflyer",1,0],["data:common","domain:wallet-manager",1,1],["data:common","domain:wallets",2,1],["data:common","domain:card",1,0],["data:common","domain:core",1,0],["data:common","domain:joint-account",1,0],["data:common","domain:demo",1,1],["data:common","domain:express",1,1],["data:common","domain:models",1,1],["data:common","features:joint-account",1,0],["data:stories","domain:stories",2,1],["data:news","domain:news",1,1],["data:news","domain:models",1,0],["data:manage-tokens","data:common",1,1],["data:manage-tokens","domain:common",1,1],["data:manage-tokens","domain:manage-tokens",2,1],["data:manage-tokens","domain:card",1,0],["data:manage-tokens","domain:joint-account",1,0],["data:manage-tokens","domain:models",1,0],["data:manage-tokens","domain:wallets",1,0],["data:markets","data:common",1,1],["data:markets","domain:common",1,1],["data:markets","domain:markets",2,1],["data:markets","domain:joint-account",1,0],["data:markets","domain:models",1,0],["data:feed","domain:feed",1,1],["data:staking","data:common",1,0],["data:staking","domain:common",1,1],["data:staking","domain:core",1,1],["data:staking","domain:models",1,1],["data:staking","domain:staking",2,1],["data:staking","domain:wallet-manager",1,1],["data:staking","domain:wallets",1,1],["data:staking","domain:txhistory",1,1],["data:staking","domain:card",1,0],["data:staking","domain:blockaid",1,0],["data:money-account","domain:money-account",1,1],["data:money-account","domain:common",1,0],["data:money-account","domain:core",1,0],["data:money-account","domain:models",1,0],["data:money-account","domain:networks",1,0],["data:money-account","domain:visa",2,0],["data:money-account","domain:wallets",1,0],["data:offramp","domain:offramp",1,1],["data:offramp","domain:card",1,0],["data:offramp","domain:wallets",1,0],["data:offramp","domain:legacy",1,0],["data:offramp","domain:models",1,1],["data:address-book","data:common",1,1],["data:address-book","domain:address-book",1,1],["data:address-book","domain:common",1,1],["data:address-book","domain:models",1,1],["data:assetsdiscovery","data:common",1,1],["data:assetsdiscovery","data:wallet-manager",1,0],["data:assetsdiscovery","domain:assetsdiscovery",1,1],["data:assetsdiscovery","domain:common",1,1],["data:assetsdiscovery","domain:models",1,1],["data:assetsdiscovery","domain:wallets",1,0],["data:marketing","domain:marketing",2,0],["data:wallets","data:common",1,1],["data:wallets","domain:common",1,1],["data:wallets","domain:appsflyer",1,0],["data:wallets","domain:dynamic-addresses",1,1],["data:wallets","domain:models",1,1],["data:wallets","domain:settings",1,1],["data:wallets","domain:wallets",2,1],["data:wallets","domain:card",1,0],["data:wallets","domain:demo",1,0],["data:cloud-backup","domain:cloud-backup",1,0],["data:account","domain:account",1,1],["data:account","domain:common",1,1],["data:account","domain:core",1,1],["data:account","domain:models",1,1],["data:account","domain:tokens",1,1],["data:account","domain:card",1,0],["data:account","domain:joint-account",1,0],["data:account","domain:collectibles",1,0],["data:account","domain:polymarket",1,0],["data:account","data:common",1,1],["data:account","features:money-account",1,1],["data:account","features:collectibles",1,0],["data:account","features:polymarket",1,0],["data:account","features:joint-account",1,0],["data:hot-wallet","domain:hot-wallet",1,1],["data:hot-wallet","domain:models",1,0],["data:app-update","domain:app-update",1,0],["data:appsflyer","domain:appsflyer",1,1],["data:appsflyer","domain:wallets",1,0],["data:tokens","data:common",1,1],["data:tokens","domain:common",1,1],["data:tokens","domain:tokens",2,1],["data:tokens","domain:transaction",1,1],["data:tokens","domain:wallet-manager",2,1],["data:tokens","domain:core",1,0],["data:tokens","domain:joint-account",1,0],["data:tokens","domain:models",1,0],["data:tokens","features:joint-account",1,0],["data:tokens","domain:staking",1,0],["data:notifications","domain:notifications",2,1],["data:notifications","domain:appsflyer",1,0],["data:notifications","domain:models",1,0],["data:joint-account","data:common",1,0],["data:joint-account","domain:joint-account",1,1],["data:joint-account","domain:account",1,0],["data:joint-account","domain:blockaid",2,0],["data:joint-account","domain:common",1,0],["data:joint-account","domain:core",1,0],["data:joint-account","domain:models",1,0],["data:joint-account","domain:transaction",1,0],["data:joint-account","domain:wallets",1,0],["data:onboarding","domain:onboarding",1,1],["data:onboarding","domain:models",1,0],["data:promo","data:common",1,0],["data:promo","domain:promo",2,1],["data:promo","domain:models",1,0],["data:analytics","domain:analytics",1,1],["data:analytics","domain:models",1,0],["domain:social-login","domain:models",1,1],["domain:transaction","domain:account",2,1],["domain:transaction","domain:card",1,1],["domain:transaction","domain:common",1,1],["domain:transaction","domain:demo",2,1],["domain:transaction","domain:dynamic-addresses",2,1],["domain:transaction","domain:networks",1,1],["domain:transaction","domain:notifications",1,1],["domain:transaction","domain:tokens",1,1],["domain:transaction","domain:wallet-manager",1,1],["domain:transaction","domain:core",1,0],["domain:transaction","domain:legacy",1,0],["domain:transaction","domain:models",1,1],["domain:transaction","domain:wallets",1,1],["domain:dynamic-addresses","domain:models",1,1],["domain:dynamic-addresses","domain:wallet-manager",1,1],["domain:dynamic-addresses","domain:wallets",1,1],["domain:yield-supply","domain:account",2,1],["domain:yield-supply","domain:blockaid",2,1],["domain:yield-supply","domain:networks",1,1],["domain:yield-supply","domain:quotes",1,1],["domain:yield-supply","domain:transaction",2,1],["domain:yield-supply","domain:app-currency",1,1],["domain:yield-supply","domain:models",2,1],["domain:yield-supply","domain:legacy",1,0],["domain:txhistory","domain:express",1,1],["domain:txhistory","domain:models",1,1],["domain:txhistory","domain:visa",1,1],["domain:collectibles","domain:core",1,1],["domain:collectibles","domain:models",2,1],["domain:push-notification-preferences","domain:models",1,1],["domain:card","domain:core",1,0],["domain:card","domain:demo",1,0],["domain:card","domain:models",1,1],["domain:card","domain:visa",1,1],["domain:nft","domain:account",1,1],["domain:nft","domain:networks",1,1],["domain:nft","domain:quotes",1,1],["domain:nft","domain:tokens",1,1],["domain:nft","domain:wallets",1,1],["domain:nft","domain:core",1,0],["domain:nft","domain:models",2,1],["domain:quotes","domain:core",1,1],["domain:quotes","domain:models",1,1],["domain:wallet-manager","domain:models",2,1],["domain:wallet-manager","domain:transaction",1,1],["domain:wallet-manager","domain:txhistory",1,1],["domain:express","domain:core",1,1],["domain:express","domain:models",2,1],["domain:express","domain:onramp",1,1],["domain:express","domain:tokens",1,1],["domain:qr-scanning","domain:account",1,1],["domain:qr-scanning","domain:common",1,1],["domain:qr-scanning","domain:networks",1,1],["domain:qr-scanning","domain:models",2,1],["domain:qr-scanning","domain:core",1,0],["domain:blockaid","domain:models",1,1],["domain:swap","domain:express",2,1],["domain:swap","domain:models",2,1],["domain:legacy","domain:core",1,1],["domain:legacy","domain:models",1,1],["domain:legacy","domain:tokens",1,1],["domain:legacy","domain:transaction",1,1],["domain:earn","domain:account",1,1],["domain:earn","domain:common",1,1],["domain:earn","domain:models",1,1],["domain:wallet-connect","domain:core",1,0],["domain:wallet-connect","domain:transaction",3,1],["domain:wallet-connect","domain:blockaid",2,1],["domain:wallet-connect","domain:models",2,1],["domain:wallet-connect","domain:tokens",1,1],["domain:models","domain:core",1,1],["domain:visa","domain:account",1,1],["domain:visa","domain:common",1,1],["domain:visa","domain:core",1,1],["domain:visa","domain:app-currency",1,1],["domain:visa","domain:models",2,1],["domain:polymarket","domain:core",1,1],["domain:polymarket","domain:models",1,1],["domain:polymarket","domain:common",1,1],["domain:feedback","domain:models",2,1],["domain:feedback","domain:visa",2,1],["domain:search","domain:core",1,1],["domain:search","domain:models",1,1],["domain:search","domain:common",1,0],["domain:search","domain:account",2,0],["domain:onramp","domain:settings",1,1],["domain:onramp","domain:core",1,1],["domain:onramp","domain:models",2,1],["domain:onramp","domain:tokens",1,1],["domain:networks","domain:core",1,1],["domain:networks","domain:models",1,1],["domain:common","domain:models",1,1],["domain:stories","domain:settings",1,1],["domain:news","domain:models",1,1],["domain:manage-tokens","domain:account",1,1],["domain:manage-tokens","domain:joint-account",1,1],["domain:manage-tokens","domain:models",2,1],["domain:manage-tokens","domain:core",1,0],["domain:markets","domain:common",1,1],["domain:markets","domain:quotes",1,1],["domain:markets","domain:card",1,0],["domain:markets","domain:app-currency",1,1],["domain:markets","domain:models",2,1],["domain:staking","domain:core",1,1],["domain:staking","domain:models",2,1],["domain:staking","domain:wallet-manager",1,1],["domain:staking","domain:blockaid",2,0],["domain:money-account","domain:common",1,1],["domain:money-account","domain:core",1,1],["domain:money-account","domain:networks",1,1],["domain:money-account","domain:visa",2,1],["domain:money-account","domain:models",2,1],["domain:money-account","domain:card",1,0],["domain:offramp","domain:core",1,1],["domain:offramp","domain:models",1,1],["domain:address-book","domain:common",1,1],["domain:address-book","domain:transaction",1,1],["domain:address-book","domain:models",1,1],["domain:assetsdiscovery","domain:account",1,1],["domain:assetsdiscovery","domain:models",1,1],["domain:wallets","domain:account",1,1],["domain:wallets","domain:card",1,1],["domain:wallets","domain:cloud-backup",1,1],["domain:wallets","domain:common",1,1],["domain:wallets","domain:hot-wallet",1,1],["domain:wallets","domain:social-login",1,1],["domain:wallets","domain:wallet-manager",1,1],["domain:wallets","domain:core",1,1],["domain:wallets","domain:joint-account",1,0],["domain:wallets","domain:notifications",2,0],["domain:wallets","domain:push-notification-preferences",1,0],["domain:wallets","domain:models",2,1],["domain:wallets","domain:demo",1,0],["domain:account","domain:core",2,1],["domain:account","domain:models",2,1],["domain:account","domain:card",1,1],["domain:account","domain:common",1,1],["domain:account","domain:express",2,1],["domain:account","domain:quotes",1,1],["domain:account","domain:money-account",1,1],["domain:account","domain:networks",1,1],["domain:account","domain:nft",1,1],["domain:account","domain:referral",1,1],["domain:account","domain:staking",2,1],["domain:account","domain:tokens",2,1],["domain:account","domain:visa",1,1],["domain:account","domain:wallet-manager",1,1],["domain:account","domain:wallets",2,1],["domain:account","domain:yield-supply",1,1],["domain:account","domain:joint-account",1,0],["domain:account","domain:collectibles",1,0],["domain:account","domain:polymarket",1,0],["domain:account","features:collectibles",1,0],["domain:account","features:polymarket",1,0],["domain:hot-wallet","domain:models",1,1],["domain:appsflyer","domain:wallets",1,1],["domain:tokens","domain:common",1,1],["domain:tokens","domain:core",1,1],["domain:tokens","domain:express",2,1],["domain:tokens","domain:legacy",1,1],["domain:tokens","domain:money-account",1,1],["domain:tokens","domain:networks",1,1],["domain:tokens","domain:collectibles",1,1],["domain:tokens","domain:polymarket",1,1],["domain:tokens","domain:quotes",1,1],["domain:tokens","domain:staking",1,1],["domain:tokens","domain:stories",2,1],["domain:tokens","domain:visa",1,1],["domain:tokens","domain:wallet-manager",1,1],["domain:tokens","domain:card",1,0],["domain:tokens","domain:models",2,1],["domain:tokens","domain:transaction",1,1],["domain:tokens","domain:yield-supply",1,1],["domain:tokens","features:money-account",1,1],["domain:tokens","features:collectibles",1,1],["domain:tokens","features:polymarket",1,1],["domain:notifications","domain:models",1,1],["domain:joint-account","domain:core",1,1],["domain:joint-account","domain:models",1,1],["domain:joint-account","domain:account",1,0],["domain:onboarding","domain:models",1,1],["domain:promo","domain:models",1,1],["domain:analytics","domain:models",1,1]]}
```
<!-- NAVGRAPH:areaGraph:END -->

<!-- NAVGRAPH:moduleGraph:BEGIN -->
```json
{"n":[[":data:account","account","data",1,14],[":data:address-book","address-book","data",0,4],[":data:analytics","analytics","data",0,2],[":data:app-currency","app-currency","data",0,3],[":data:app-theme","app-theme","data",0,2],[":data:app-update","app-update","data",0,1],[":data:appsflyer","appsflyer","data",0,2],[":data:assetsdiscovery","assetsdiscovery","data",0,6],[":data:balance-hiding","balance-hiding","data",0,2],[":data:blockaid","blockaid","data",0,3],[":data:card","card","data",2,2],[":data:cloud-backup","cloud-backup","data",1,1],[":data:collectibles","collectibles","data",0,11],[":data:common","common","data",33,12],[":data:dynamic-addresses","dynamic-addresses","data",1,7],[":data:earn","earn","data",0,4],[":data:express","express","data",1,7],[":data:feed:search","feed:search","data",0,1],[":data:feedback","feedback","data",0,9],[":data:hot-wallet","hot-wallet","data",0,2],[":data:joint-account","joint-account","data",0,10],[":data:manage-tokens","manage-tokens","data",0,8],[":data:marketing","marketing","data",0,2],[":data:markets","markets","data",0,6],[":data:money-account","money-account","data",0,8],[":data:networks","networks","data",0,8],[":data:news","news","data",0,2],[":data:nft","nft","data",1,7],[":data:notifications","notifications","data",0,4],[":data:offramp","offramp","data",0,5],[":data:onboarding","onboarding","data",0,2],[":data:onramp","onramp","data",0,10],[":data:polymarket","polymarket","data",0,9],[":data:promo","promo","data",0,4],[":data:push-notification-preferences","push-notification-preferences","data",0,2],[":data:qr-scanning","qr-scanning","data",0,3],[":data:quotes","quotes","data",0,4],[":data:search","search","data",0,4],[":data:settings","settings","data",0,1],[":data:social-login","social-login","data",0,2],[":data:staking","staking","data",0,11],[":data:stories","stories","data",0,2],[":data:swap","swap","data",0,11],[":data:tokens","tokens","data",0,12],[":data:transaction","transaction","data",0,8],[":data:txhistory","txhistory","data",0,17],[":data:visa","visa","data",0,14],[":data:wallet-connect","wallet-connect","data",0,16],[":data:wallet-manager","wallet-manager","data",1,13],[":data:wallets","wallets","data",3,10],[":data:yield-supply","yield-supply","data",0,6],[":domain:account","account","domain",41,2],[":domain:account:status","account:status","domain",28,26],[":domain:address-book","address-book","domain",5,3],[":domain:analytics","analytics","domain",3,1],[":domain:app-currency","app-currency","domain",23,1],[":domain:app-currency:models","app-currency:models","domain",33,0],[":domain:app-theme","app-theme","domain",3,1],[":domain:app-theme:models","app-theme:models","domain",4,0],[":domain:app-update","app-update","domain",3,0],[":domain:appsflyer","appsflyer","domain",13,1],[":domain:assetsdiscovery","assetsdiscovery","domain",4,2],[":domain:balance-hiding","balance-hiding","domain",19,1],[":domain:balance-hiding:models","balance-hiding:models","domain",11,0],[":domain:blockaid","blockaid","domain",5,2],[":domain:blockaid:models","blockaid:models","domain",10,0],[":domain:card","card","domain",36,4],[":domain:cloud-backup","cloud-backup","domain",8,0],[":domain:collectibles","collectibles","domain",5,3],[":domain:collectibles:models","collectibles:models","domain",3,0],[":domain:common","common","domain",65,1],[":domain:core","core","domain",52,0],[":domain:demo","demo","domain",9,1],[":domain:demo:models","demo:models","domain",10,0],[":domain:dynamic-addresses","dynamic-addresses","domain",5,4],[":domain:dynamic-addresses:models","dynamic-addresses:models","domain",4,0],[":domain:earn","earn","domain",4,3],[":domain:express","express","domain",5,3],[":domain:express:models","express:models","domain",17,3],[":domain:feed:search","feed:search","domain",4,0],[":domain:feedback","feedback","domain",18,3],[":domain:feedback:models","feedback:models","domain",20,2],[":domain:hot-wallet","hot-wallet","domain",8,1],[":domain:joint-account","joint-account","domain",17,3],[":domain:legacy","legacy","domain",12,4],[":domain:manage-tokens","manage-tokens","domain",4,5],[":domain:manage-tokens:models","manage-tokens:models","domain",5,1],[":domain:marketing","marketing","domain",3,1],[":domain:marketing:models","marketing:models","domain",10,0],[":domain:markets","markets","domain",8,6],[":domain:markets:models","markets:models","domain",17,1],[":domain:models","models","domain",188,1],[":domain:money-account","money-account","domain",8,8],[":domain:money-account:models","money-account:models","domain",3,1],[":domain:networks","networks","domain",13,2],[":domain:news","news","domain",3,1],[":domain:nft","nft","domain",6,8],[":domain:nft:models","nft:models","domain",7,1],[":domain:notifications","notifications","domain",12,2],[":domain:notifications:models","notifications:models","domain",6,0],[":domain:offramp","offramp","domain",6,2],[":domain:onboarding","onboarding","domain",2,1],[":domain:onramp","onramp","domain",5,5],[":domain:onramp:models","onramp:models","domain",12,1],[":domain:polymarket","polymarket","domain",6,3],[":domain:promo","promo","domain",2,2],[":domain:promo:models","promo:models","domain",3,0],[":domain:push-notification-preferences","push-notification-preferences","domain",5,1],[":domain:qr-scanning","qr-scanning","domain",6,6],[":domain:qr-scanning:models","qr-scanning:models","domain",8,1],[":domain:quotes","quotes","domain",17,2],[":domain:referral","referral","domain",3,0],[":domain:search","search","domain",5,5],[":domain:settings","settings","domain",23,0],[":domain:social-login","social-login","domain",9,1],[":domain:staking","staking","domain",12,6],[":domain:staking:models","staking:models","domain",9,1],[":domain:stories","stories","domain",7,2],[":domain:stories:models","stories:models","domain",8,0],[":domain:swap","swap","domain",4,3],[":domain:swap:models","swap:models","domain",7,2],[":domain:tokens","tokens","domain",20,23],[":domain:tokens:models","tokens:models","domain",30,1],[":domain:transaction","transaction","domain",24,17],[":domain:transaction:models","transaction:models","domain",21,0],[":domain:txhistory","txhistory","domain",14,4],[":domain:txhistory:models","txhistory:models","domain",10,0],[":domain:visa","visa","domain",19,6],[":domain:visa:models","visa:models","domain",21,1],[":domain:wallet-connect","wallet-connect","domain",4,6],[":domain:wallet-connect:models","wallet-connect:models","domain",4,4],[":domain:wallet-manager","wallet-manager","domain",25,4],[":domain:wallet-manager:models","wallet-manager:models","domain",3,1],[":domain:wallets","wallets","domain",59,15],[":domain:wallets:models","wallets:models","domain",28,1],[":domain:yield-supply","yield-supply","domain",7,12],[":domain:yield-supply:models","yield-supply:models","domain",10,1],[":features:account:api","account:api","features",1,1],[":features:account:impl","account:impl","features",0,7],[":features:address-book:api","address-book:api","features",2,1],[":features:address-book:impl","address-book:impl","features",0,9],[":features:approval:api","approval:api","features",3,1],[":features:approval:impl","approval:impl","features",0,6],[":features:biometry:api","biometry:api","features",3,0],[":features:biometry:impl","biometry:impl","features",0,6],[":features:collectibles:api","collectibles:api","features",6,1],[":features:collectibles:impl","collectibles:impl","features",0,8],[":features:common-features:api","common-features:api","features",17,4],[":features:common-features:impl","common-features:impl","features",0,21],[":features:create-wallet-selection:api","create-wallet-selection:api","features",1,0],[":features:create-wallet-selection:impl","create-wallet-selection:impl","features",0,3],[":features:create-wallet-start:api","create-wallet-start:api","features",1,0],[":features:create-wallet-start:impl","create-wallet-start:impl","features",0,10],[":features:details:api","details:api","features",1,1],[":features:details:impl","details:impl","features",0,17],[":features:disclaimer:api","disclaimer:api","features",1,0],[":features:disclaimer:impl","disclaimer:impl","features",0,5],[":features:feed:api","feed:api","features",8,4],[":features:feed:crypto:api","feed:crypto:api","features",1,1],[":features:feed:crypto:impl","feed:crypto:impl","features",0,14],[":features:feed:earn:impl","feed:earn:impl","features",0,13],[":features:feed:impl","feed:impl","features",0,39],[":features:feed:rwa:impl","feed:rwa:impl","features",0,14],[":features:feed:search:api","feed:search:api","features",2,1],[":features:feed:search:impl","feed:search:impl","features",0,3],[":features:for-you:api","for-you:api","features",2,2],[":features:for-you:impl","for-you:impl","features",0,21],[":features:force-update:api","force-update:api","features",1,0],[":features:force-update:impl","force-update:impl","features",0,4],[":features:home:api","home:api","features",1,0],[":features:home:impl","home:impl","features",0,7],[":features:hot-wallet:api","hot-wallet:api","features",5,4],[":features:hot-wallet:impl","hot-wallet:impl","features",0,18],[":features:introduction:api","introduction:api","features",1,0],[":features:introduction:impl","introduction:impl","features",0,17],[":features:joint-account:api","joint-account:api","features",11,1],[":features:joint-account:impl","joint-account:impl","features",0,12],[":features:kyc:api","kyc:api","features",2,1],[":features:kyc:impl","kyc:impl","features",0,4],[":features:kyc:mock","kyc:mock","features",0,1],[":features:manage-tokens:api","manage-tokens:api","features",4,1],[":features:manage-tokens:impl","manage-tokens:impl","features",0,14],[":features:marketing:api","marketing:api","features",7,1],[":features:marketing:impl","marketing:impl","features",0,3],[":features:markets:api","markets:api","features",2,1],[":features:markets:impl","markets:impl","features",0,6],[":features:money-account:details:api","money-account:details:api","features",6,1],[":features:money-account:details:impl","money-account:details:impl","features",0,2],[":features:money-account:main:api","money-account:main:api","features",2,0],[":features:money-account:main:impl","money-account:main:impl","features",0,1],[":features:money-account:onboarding:api","money-account:onboarding:api","features",1,0],[":features:money-account:onboarding:impl","money-account:onboarding:impl","features",0,6],[":features:nft:api","nft:api","features",2,2],[":features:nft:impl","nft:impl","features",0,13],[":features:onboarding-v2:api","onboarding-v2:api","features",5,1],[":features:onboarding-v2:impl","onboarding-v2:impl","features",0,23],[":features:onramp:api","onramp:api","features",1,2],[":features:onramp:impl","onramp:impl","features",0,28],[":features:polymarket:api","polymarket:api","features",6,1],[":features:polymarket:impl","polymarket:impl","features",0,11],[":features:promo-banners:api","promo-banners:api","features",8,0],[":features:promo-banners:impl","promo-banners:impl","features",0,13],[":features:push-notification-settings:api","push-notification-settings:api","features",1,1],[":features:push-notification-settings:impl","push-notification-settings:impl","features",0,7],[":features:push-notifications:api","push-notifications:api","features",11,0],[":features:push-notifications:impl","push-notifications:impl","features",0,7],[":features:qr-scanning:api","qr-scanning:api","features",1,1],[":features:qr-scanning:impl","qr-scanning:impl","features",0,4],[":features:rating:api","rating:api","features",3,1],[":features:rating:impl","rating:impl","features",0,3],[":features:referral:api","referral:api","features",1,1],[":features:referral:data","referral:data","features",0,5],[":features:referral:domain","referral:domain","features",2,5],[":features:referral:impl","referral:impl","features",0,13],[":features:send:api","send:api","features",8,6],[":features:send:impl","send:impl","features",1,37],[":features:staking:api","staking:api","features",1,2],[":features:staking:impl","staking:impl","features",0,25],[":features:stories-v2:api","stories-v2:api","features",4,0],[":features:stories-v2:impl","stories-v2:impl","features",0,1],[":features:stories:api","stories:api","features",1,0],[":features:stories:impl","stories:impl","features",0,3],[":features:survey:api","survey:api","features",2,0],[":features:survey:impl","survey:impl","features",0,4],[":features:swap-v2:api","swap-v2:api","features",3,4],[":features:swap-v2:impl","swap-v2:impl","features",0,27],[":features:swap:api","swap:api","features",3,1],[":features:swap:data","swap:data","features",0,7],[":features:swap:domain","swap:domain","features",5,22],[":features:swap:impl","swap:impl","features",0,42],[":features:tangempay:common-ui","tangempay:common-ui","features",2,0],[":features:tangempay:details:api","tangempay:details:api","features",4,2],[":features:tangempay:details:impl","tangempay:details:impl","features",0,15],[":features:tangempay:eligibility:api","tangempay:eligibility:api","features",3,1],[":features:tangempay:eligibility:impl","tangempay:eligibility:impl","features",0,7],[":features:tangempay:main:api","tangempay:main:api","features",2,0],[":features:tangempay:main:impl","tangempay:main:impl","features",0,1],[":features:tangempay:onboarding:api","tangempay:onboarding:api","features",1,1],[":features:tangempay:onboarding:impl","tangempay:onboarding:impl","features",0,12],[":features:tester:api","tester:api","features",1,0],[":features:tester:impl","tester:impl","features",0,21],[":features:token-recieve:api","token-recieve:api","features",8,1],[":features:token-recieve:impl","token-recieve:impl","features",0,5],[":features:tokendetails:api","tokendetails:api","features",2,2],[":features:tokendetails:impl","tokendetails:impl","features",0,45],[":features:txhistory:api","txhistory:api","features",4,2],[":features:txhistory:impl","txhistory:impl","features",0,25],[":features:usedesk:api","usedesk:api","features",1,1],[":features:usedesk:impl","usedesk:impl","features",0,5],[":features:wallet-settings:api","wallet-settings:api","features",4,1],[":features:wallet-settings:impl","wallet-settings:impl","features",0,23],[":features:wallet:api","wallet:api","features",8,2],[":features:wallet:impl","wallet:impl","features",0,74],[":features:walletconnect:api","walletconnect:api","features",1,1],[":features:walletconnect:impl","walletconnect:impl","features",0,18],[":features:welcome:api","welcome:api","features",1,0],[":features:welcome:impl","welcome:impl","features",0,8],[":features:yield-supply:api","yield-supply:api","features",3,2],[":features:yield-supply:impl","yield-supply:impl","features",0,23]],"e":[[":features:usedesk:impl",":domain:feedback:models",1,0],[":features:usedesk:impl",":domain:models",1,0],[":features:usedesk:impl",":features:usedesk:api",1,1],[":features:usedesk:impl",":domain:feedback",1,1],[":features:usedesk:impl",":domain:settings",1,1],[":features:usedesk:api",":domain:feedback:models",1,1],[":features:home:impl",":features:home:api",1,1],[":features:home:impl",":domain:appsflyer",1,1],[":features:home:impl",":domain:card",1,1],[":features:home:impl",":domain:common",1,1],[":features:home:impl",":domain:settings",1,1],[":features:home:impl",":domain:wallets",1,1],[":features:home:impl",":domain:models",1,0],[":features:create-wallet-start:impl",":features:create-wallet-start:api",1,1],[":features:create-wallet-start:impl",":features:onboarding-v2:api",1,1],[":features:create-wallet-start:impl",":domain:card",1,1],[":features:create-wallet-start:impl",":domain:appsflyer",1,0],[":features:create-wallet-start:impl",":domain:common",1,1],[":features:create-wallet-start:impl",":domain:hot-wallet",1,1],[":features:create-wallet-start:impl",":domain:settings",1,1],[":features:create-wallet-start:impl",":domain:wallets",1,1],[":features:create-wallet-start:impl",":domain:models",1,0],[":features:create-wallet-start:impl",":domain:wallets:models",1,0],[":features:yield-supply:impl",":features:yield-supply:api",1,0],[":features:yield-supply:impl",":features:marketing:api",1,0],[":features:yield-supply:impl",":domain:models",1,0],[":features:yield-supply:impl",":domain:appsflyer",1,0],[":features:yield-supply:impl",":domain:app-currency:models",1,0],[":features:yield-supply:impl",":domain:app-currency",1,0],[":features:yield-supply:impl",":domain:account:status",1,0],[":features:yield-supply:impl",":domain:wallets:models",1,0],[":features:yield-supply:impl",":domain:wallets",1,0],[":features:yield-supply:impl",":domain:tokens:models",1,0],[":features:yield-supply:impl",":domain:tokens",1,0],[":features:yield-supply:impl",":domain:transaction:models",1,0],[":features:yield-supply:impl",":domain:transaction",1,0],[":features:yield-supply:impl",":domain:yield-supply:models",1,0],[":features:yield-supply:impl",":domain:yield-supply",1,0],[":features:yield-supply:impl",":domain:stories:models",1,0],[":features:yield-supply:impl",":domain:stories",1,0],[":features:yield-supply:impl",":domain:feedback:models",1,0],[":features:yield-supply:impl",":domain:feedback",1,0],[":features:yield-supply:impl",":domain:marketing:models",1,0],[":features:yield-supply:impl",":domain:account",1,0],[":features:yield-supply:impl",":domain:onramp:models",1,0],[":features:yield-supply:impl",":domain:networks",1,1],[":features:yield-supply:api",":domain:models",1,1],[":features:yield-supply:api",":domain:tokens:models",1,1],[":features:txhistory:impl",":features:txhistory:api",1,1],[":features:txhistory:impl",":features:tokendetails:api",1,0],[":features:txhistory:impl",":features:joint-account:api",1,0],[":features:txhistory:impl",":features:rating:api",1,0],[":features:txhistory:impl",":domain:account:status",1,1],[":features:txhistory:impl",":domain:balance-hiding",1,1],[":features:txhistory:impl",":domain:common",1,1],[":features:txhistory:impl",":domain:models",1,1],[":features:txhistory:impl",":domain:staking",1,1],[":features:txhistory:impl",":domain:txhistory",1,1],[":features:txhistory:impl",":domain:wallets",1,1],[":features:txhistory:impl",":domain:account",1,0],[":features:txhistory:impl",":domain:address-book",1,0],[":features:txhistory:impl",":domain:app-currency",1,0],[":features:txhistory:impl",":domain:app-currency:models",1,0],[":features:txhistory:impl",":domain:joint-account",1,0],[":features:txhistory:impl",":domain:balance-hiding:models",1,0],[":features:txhistory:impl",":domain:express:models",1,0],[":features:txhistory:impl",":domain:feedback",1,0],[":features:txhistory:impl",":domain:feedback:models",1,0],[":features:txhistory:impl",":domain:onramp:models",1,0],[":features:txhistory:impl",":domain:staking:models",1,0],[":features:txhistory:impl",":domain:tokens:models",1,0],[":features:txhistory:impl",":domain:txhistory:models",1,0],[":features:txhistory:impl",":domain:visa:models",1,0],[":features:txhistory:api",":domain:models",1,1],[":features:txhistory:api",":domain:txhistory",1,1],[":features:collectibles:impl",":features:collectibles:api",1,0],[":features:collectibles:impl",":features:token-recieve:api",1,0],[":features:collectibles:impl",":domain:collectibles",1,0],[":features:collectibles:impl",":domain:common",1,0],[":features:collectibles:impl",":domain:models",1,0],[":features:collectibles:impl",":domain:collectibles:models",1,0],[":features:collectibles:impl",":domain:app-currency",1,0],[":features:collectibles:impl",":domain:app-currency:models",1,0],[":features:collectibles:api",":domain:models",1,1],[":features:referral:impl",":features:referral:api",1,1],[":features:referral:impl",":features:common-features:api",1,1],[":features:referral:impl",":features:referral:domain",1,1],[":features:referral:impl",":domain:account:status",1,1],[":features:referral:impl",":domain:app-currency",1,1],[":features:referral:impl",":domain:balance-hiding",1,1],[":features:referral:impl",":domain:demo",1,1],[":features:referral:impl",":domain:wallets",1,1],[":features:referral:impl",":domain:account",1,0],[":features:referral:impl",":domain:app-currency:models",1,0],[":features:referral:impl",":domain:card",1,0],[":features:referral:impl",":domain:models",1,0],[":features:referral:impl",":domain:wallets:models",1,0],[":features:referral:api",":domain:models",1,1],[":features:referral:data",":data:common",1,0],[":features:referral:data",":domain:common",1,1],[":features:referral:data",":domain:referral",1,1],[":features:referral:data",":features:referral:domain",1,1],[":features:referral:data",":domain:models",1,0],[":features:referral:domain",":domain:account",1,1],[":features:referral:domain",":domain:account:status",1,1],[":features:referral:domain",":domain:models",1,1],[":features:referral:domain",":domain:wallet-manager",1,1],[":features:referral:domain",":domain:core",1,0],[":features:wallet-settings:impl",":features:joint-account:api",1,1],[":features:wallet-settings:impl",":features:wallet:api",1,1],[":features:wallet-settings:impl",":features:wallet-settings:api",1,1],[":features:wallet-settings:impl",":features:hot-wallet:api",1,0],[":features:wallet-settings:impl",":features:push-notifications:api",1,0],[":features:wallet-settings:impl",":data:account",1,0],[":features:wallet-settings:impl",":domain:account",1,1],[":features:wallet-settings:impl",":domain:account:status",1,1],[":features:wallet-settings:impl",":domain:app-currency",1,1],[":features:wallet-settings:impl",":domain:assetsdiscovery",1,1],[":features:wallet-settings:impl",":domain:balance-hiding",1,1],[":features:wallet-settings:impl",":domain:demo",1,1],[":features:wallet-settings:impl",":domain:cloud-backup",1,0],[":features:wallet-settings:impl",":domain:social-login",1,0],[":features:wallet-settings:impl",":domain:nft",1,1],[":features:wallet-settings:impl",":domain:notifications",1,1],[":features:wallet-settings:impl",":domain:wallets",1,1],[":features:wallet-settings:impl",":domain:app-currency:models",1,0],[":features:wallet-settings:impl",":domain:card",1,0],[":features:wallet-settings:impl",":domain:common",1,0],[":features:wallet-settings:impl",":domain:models",1,0],[":features:wallet-settings:impl",":domain:notifications:models",1,0],[":features:wallet-settings:impl",":domain:wallets:models",1,0],[":features:wallet-settings:api",":domain:models",1,1],[":features:token-recieve:impl",":domain:models",1,0],[":features:token-recieve:impl",":domain:transaction",1,0],[":features:token-recieve:impl",":domain:tokens",1,0],[":features:token-recieve:impl",":domain:tokens:models",1,0],[":features:token-recieve:impl",":features:token-recieve:api",1,0],[":features:token-recieve:api",":domain:models",1,1],[":features:rating:impl",":features:rating:api",1,1],[":features:rating:impl",":features:swap:domain",1,0],[":features:rating:impl",":domain:models",1,0],[":features:rating:api",":domain:models",1,1],[":features:kyc:impl",":features:kyc:api",1,1],[":features:kyc:impl",":domain:visa",1,1],[":features:kyc:impl",":domain:visa:models",1,1],[":features:kyc:impl",":domain:models",1,0],[":features:kyc:mock",":features:kyc:api",1,1],[":features:kyc:api",":domain:models",1,1],[":features:disclaimer:impl",":domain:card",1,1],[":features:disclaimer:impl",":domain:notifications",1,1],[":features:disclaimer:impl",":domain:settings",1,1],[":features:disclaimer:impl",":features:disclaimer:api",1,1],[":features:disclaimer:impl",":features:push-notifications:api",1,0],[":features:nft:impl",":features:common-features:api",1,1],[":features:nft:impl",":features:nft:api",1,1],[":features:nft:impl",":features:token-recieve:api",1,1],[":features:nft:impl",":domain:account",1,1],[":features:nft:impl",":domain:account:status",1,1],[":features:nft:impl",":domain:app-currency",1,1],[":features:nft:impl",":domain:nft",1,1],[":features:nft:impl",":domain:transaction",1,1],[":features:nft:impl",":domain:wallets",1,1],[":features:nft:impl",":domain:app-currency:models",1,0],[":features:nft:impl",":domain:core",1,0],[":features:nft:impl",":domain:models",1,0],[":features:nft:impl",":domain:nft:models",1,0],[":features:nft:api",":domain:models",1,1],[":features:nft:api",":domain:nft:models",1,1],[":features:tokendetails:impl",":features:rating:api",1,0],[":features:tokendetails:impl",":domain:account:status",1,0],[":features:tokendetails:impl",":domain:app-currency",1,0],[":features:tokendetails:impl",":domain:app-currency:models",1,0],[":features:tokendetails:impl",":domain:balance-hiding",1,0],[":features:tokendetails:impl",":domain:balance-hiding:models",1,0],[":features:tokendetails:impl",":domain:card",1,0],[":features:tokendetails:impl",":domain:demo",1,0],[":features:tokendetails:impl",":domain:dynamic-addresses",1,0],[":features:tokendetails:impl",":domain:dynamic-addresses:models",1,0],[":features:tokendetails:impl",":domain:joint-account",1,0],[":features:tokendetails:impl",":domain:marketing:models",1,0],[":features:tokendetails:impl",":domain:models",1,0],[":features:tokendetails:impl",":domain:notifications:models",1,0],[":features:tokendetails:impl",":domain:offramp",1,0],[":features:tokendetails:impl",":domain:onramp",1,0],[":features:tokendetails:impl",":domain:onramp:models",1,0],[":features:tokendetails:impl",":domain:staking",1,0],[":features:tokendetails:impl",":domain:staking:models",2,0],[":features:tokendetails:impl",":domain:tokens",1,0],[":features:tokendetails:impl",":domain:tokens:models",1,0],[":features:tokendetails:impl",":domain:transaction",1,0],[":features:tokendetails:impl",":domain:transaction:models",1,0],[":features:tokendetails:impl",":domain:txhistory",1,0],[":features:tokendetails:impl",":domain:txhistory:models",1,0],[":features:tokendetails:impl",":domain:wallets",1,0],[":features:tokendetails:impl",":domain:wallets:models",1,0],[":features:tokendetails:impl",":domain:yield-supply",1,0],[":features:tokendetails:impl",":domain:yield-supply:models",1,0],[":features:tokendetails:impl",":features:swap:domain",1,0],[":features:tokendetails:impl",":features:tokendetails:api",1,0],[":features:tokendetails:impl",":features:wallet:api",1,0],[":features:tokendetails:impl",":features:markets:api",1,0],[":features:tokendetails:impl",":features:marketing:api",1,0],[":features:tokendetails:impl",":features:push-notifications:api",1,0],[":features:tokendetails:impl",":features:txhistory:api",1,0],[":features:tokendetails:impl",":features:joint-account:api",1,0],[":features:tokendetails:impl",":features:token-recieve:api",1,0],[":features:tokendetails:impl",":features:yield-supply:api",1,0],[":features:tokendetails:impl",":features:common-features:api",1,0],[":features:tokendetails:impl",":domain:core",1,0],[":features:tokendetails:impl",":domain:account",1,1],[":features:tokendetails:impl",":domain:common",1,1],[":features:tokendetails:impl",":domain:visa",1,1],[":features:tokendetails:impl",":domain:wallet-manager",1,1],[":features:tokendetails:api",":domain:models",1,1],[":features:tokendetails:api",":domain:tokens:models",1,1],[":features:introduction:impl",":features:hot-wallet:api",1,0],[":features:introduction:impl",":features:introduction:api",1,0],[":features:introduction:impl",":features:push-notifications:api",1,0],[":features:introduction:impl",":features:stories-v2:api",1,0],[":features:introduction:impl",":domain:analytics",1,0],[":features:introduction:impl",":domain:appsflyer",1,0],[":features:introduction:impl",":domain:card",1,0],[":features:introduction:impl",":domain:cloud-backup",1,0],[":features:introduction:impl",":domain:common",1,0],[":features:introduction:impl",":domain:feedback",1,0],[":features:introduction:impl",":domain:feedback:models",1,0],[":features:introduction:impl",":domain:hot-wallet",1,0],[":features:introduction:impl",":domain:models",1,0],[":features:introduction:impl",":domain:settings",1,0],[":features:introduction:impl",":domain:social-login",1,0],[":features:introduction:impl",":domain:wallets",1,0],[":features:introduction:impl",":domain:wallets:models",1,0],[":features:for-you:impl",":features:for-you:api",1,1],[":features:for-you:impl",":features:promo-banners:api",1,1],[":features:for-you:impl",":features:common-features:api",1,0],[":features:for-you:impl",":domain:account:status",1,1],[":features:for-you:impl",":domain:app-currency",1,1],[":features:for-you:impl",":domain:common",1,1],[":features:for-you:impl",":domain:earn",1,1],[":features:for-you:impl",":domain:markets",1,1],[":features:for-you:impl",":domain:yield-supply",1,1],[":features:for-you:impl",":domain:staking",1,1],[":features:for-you:impl",":domain:account",1,0],[":features:for-you:impl",":domain:balance-hiding",1,1],[":features:for-you:impl",":domain:wallets",1,1],[":features:for-you:impl",":domain:models",1,0],[":features:for-you:impl",":domain:tokens",1,0],[":features:for-you:impl",":data:common",1,0],[":features:for-you:impl",":domain:app-currency:models",1,0],[":features:for-you:impl",":domain:markets:models",1,0],[":features:for-you:impl",":domain:staking:models",1,0],[":features:for-you:impl",":domain:tokens:models",1,0],[":features:for-you:impl",":domain:yield-supply:models",1,0],[":features:for-you:api",":domain:models",1,1],[":features:for-you:api",":domain:markets:models",1,1],[":features:qr-scanning:impl",":features:qr-scanning:api",1,0],[":features:qr-scanning:impl",":domain:qr-scanning",1,0],[":features:qr-scanning:impl",":domain:qr-scanning:models",1,0],[":features:qr-scanning:impl",":data:card",1,0],[":features:qr-scanning:api",":domain:qr-scanning:models",1,1],[":features:swap:impl",":features:approval:api",1,1],[":features:swap:impl",":features:common-features:api",1,1],[":features:swap:impl",":features:send:api",1,1],[":features:swap:impl",":features:swap:api",1,1],[":features:swap:impl",":features:swap:domain",1,1],[":features:swap:impl",":features:marketing:api",1,0],[":features:swap:impl",":features:send:impl",1,0],[":features:swap:impl",":data:common",1,0],[":features:swap:impl",":domain:account:status",1,1],[":features:swap:impl",":domain:appsflyer",1,0],[":features:swap:impl",":domain:app-currency",1,1],[":features:swap:impl",":domain:app-currency:models",1,1],[":features:swap:impl",":domain:balance-hiding",1,1],[":features:swap:impl",":domain:express:models",1,1],[":features:swap:impl",":domain:feedback",1,1],[":features:swap:impl",":domain:legacy",1,1],[":features:swap:impl",":domain:models",1,1],[":features:swap:impl",":domain:settings",1,1],[":features:swap:impl",":domain:stories",1,1],[":features:swap:impl",":domain:swap",1,1],[":features:swap:impl",":domain:swap:models",1,1],[":features:swap:impl",":domain:tokens",1,1],[":features:swap:impl",":domain:transaction",1,1],[":features:swap:impl",":domain:transaction:models",1,1],[":features:swap:impl",":domain:txhistory",1,1],[":features:swap:impl",":domain:visa",1,1],[":features:swap:impl",":domain:wallets",1,1],[":features:swap:impl",":domain:account",1,0],[":features:swap:impl",":domain:balance-hiding:models",1,0],[":features:swap:impl",":domain:core",1,0],[":features:swap:impl",":domain:feedback:models",1,0],[":features:swap:impl",":domain:marketing:models",1,0],[":features:swap:impl",":domain:markets:models",1,0],[":features:swap:impl",":domain:polymarket",1,0],[":features:swap:impl",":domain:quotes",1,0],[":features:swap:impl",":domain:stories:models",1,0],[":features:swap:impl",":domain:txhistory:models",1,0],[":features:swap:impl",":domain:visa:models",1,0],[":features:swap:impl",":domain:onramp:models",1,0],[":features:swap:impl",":domain:tokens:models",1,0],[":features:swap:impl",":domain:wallets:models",1,0],[":features:swap:impl",":domain:common",1,0],[":features:swap:api",":domain:models",1,1],[":features:swap:data",":features:swap:api",1,1],[":features:swap:data",":features:swap:domain",1,1],[":features:swap:data",":domain:express:models",1,0],[":features:swap:data",":domain:account",1,1],[":features:swap:data",":domain:models",1,1],[":features:swap:data",":domain:txhistory",1,1],[":features:swap:data",":data:common",1,1],[":features:swap:domain",":domain:account:status",1,1],[":features:swap:domain",":domain:app-currency",1,1],[":features:swap:domain",":domain:app-currency:models",1,1],[":features:swap:domain",":domain:balance-hiding",1,1],[":features:swap:domain",":domain:demo",1,1],[":features:swap:domain",":domain:express:models",1,1],[":features:swap:domain",":domain:legacy",1,1],[":features:swap:domain",":domain:models",1,1],[":features:swap:domain",":domain:notifications",1,1],[":features:swap:domain",":domain:quotes",1,1],[":features:swap:domain",":domain:swap",1,1],[":features:swap:domain",":domain:swap:models",1,1],[":features:swap:domain",":domain:tokens",1,1],[":features:swap:domain",":domain:tokens:models",1,1],[":features:swap:domain",":domain:transaction",1,1],[":features:swap:domain",":domain:transaction:models",1,1],[":features:swap:domain",":domain:visa",1,1],[":features:swap:domain",":domain:visa:models",1,1],[":features:swap:domain",":domain:wallet-manager",1,1],[":features:swap:domain",":domain:yield-supply",1,1],[":features:swap:domain",":features:swap:api",1,1],[":features:swap:domain",":features:send:api",1,0],[":features:details:impl",":features:details:api",1,1],[":features:details:impl",":features:onboarding-v2:api",1,1],[":features:details:impl",":features:wallet:api",1,1],[":features:details:impl",":features:tangempay:details:api",1,0],[":features:details:impl",":features:money-account:details:api",1,0],[":features:details:impl",":domain:card",1,1],[":features:details:impl",":domain:feedback",1,1],[":features:details:impl",":domain:settings",1,1],[":features:details:impl",":domain:money-account",1,1],[":features:details:impl",":domain:visa",1,1],[":features:details:impl",":domain:wallet-connect",1,1],[":features:details:impl",":domain:wallets",1,1],[":features:details:impl",":domain:common",1,0],[":features:details:impl",":domain:feedback:models",1,0],[":features:details:impl",":domain:models",1,0],[":features:details:impl",":domain:money-account:models",1,0],[":features:details:impl",":domain:wallets:models",1,0],[":features:details:api",":domain:models",1,1],[":features:create-wallet-selection:impl",":features:create-wallet-selection:api",1,0],[":features:create-wallet-selection:impl",":domain:hot-wallet",1,1],[":features:create-wallet-selection:impl",":domain:wallets",1,1],[":features:welcome:impl",":features:welcome:api",1,0],[":features:welcome:impl",":features:wallet:api",1,0],[":features:welcome:impl",":features:onboarding-v2:api",1,0],[":features:welcome:impl",":domain:models",1,0],[":features:welcome:impl",":domain:wallets",1,0],[":features:welcome:impl",":domain:card",1,0],[":features:welcome:impl",":domain:settings",1,0],[":features:welcome:impl",":domain:common",1,1],[":features:polymarket:impl",":features:polymarket:api",1,0],[":features:polymarket:impl",":features:common-features:api",1,1],[":features:polymarket:impl",":features:token-recieve:api",1,0],[":features:polymarket:impl",":domain:feedback",1,0],[":features:polymarket:impl",":domain:feedback:models",1,0],[":features:polymarket:impl",":domain:common",1,0],[":features:polymarket:impl",":domain:models",1,0],[":features:polymarket:impl",":domain:polymarket",1,0],[":features:polymarket:impl",":domain:core",1,0],[":features:polymarket:impl",":domain:stories",1,0],[":features:polymarket:impl",":domain:stories:models",1,0],[":features:polymarket:api",":domain:models",1,1],[":features:common-features:impl",":features:common-features:api",1,1],[":features:common-features:impl",":features:joint-account:api",1,0],[":features:common-features:impl",":features:token-recieve:api",1,1],[":features:common-features:impl",":features:wallet:api",1,1],[":features:common-features:impl",":domain:account:status",1,1],[":features:common-features:impl",":domain:app-currency",1,1],[":features:common-features:impl",":domain:app-currency:models",1,1],[":features:common-features:impl",":domain:balance-hiding",1,1],[":features:common-features:impl",":domain:manage-tokens",1,1],[":features:common-features:impl",":domain:markets",1,1],[":features:common-features:impl",":domain:joint-account",1,0],[":features:common-features:impl",":domain:markets:models",1,1],[":features:common-features:impl",":domain:models",1,1],[":features:common-features:impl",":domain:transaction",1,1],[":features:common-features:impl",":domain:wallets",1,1],[":features:common-features:impl",":domain:account",1,0],[":features:common-features:impl",":domain:card",1,0],[":features:common-features:impl",":domain:core",1,0],[":features:common-features:impl",":domain:tokens",1,0],[":features:common-features:impl",":domain:tokens:models",1,0],[":features:common-features:impl",":domain:yield-supply:models",1,0],[":features:common-features:api",":domain:account",1,1],[":features:common-features:api",":domain:app-currency:models",1,1],[":features:common-features:api",":domain:markets:models",1,1],[":features:common-features:api",":domain:models",1,1],[":features:onramp:impl",":features:common-features:api",1,1],[":features:onramp:impl",":features:onramp:api",1,1],[":features:onramp:impl",":features:marketing:api",1,0],[":features:onramp:impl",":domain:marketing:models",1,0],[":features:onramp:impl",":domain:quotes",1,0],[":features:onramp:impl",":domain:account",1,1],[":features:onramp:impl",":domain:account:status",1,1],[":features:onramp:impl",":domain:app-currency",1,1],[":features:onramp:impl",":domain:app-theme",1,1],[":features:onramp:impl",":domain:balance-hiding",1,1],[":features:onramp:impl",":domain:demo",1,1],[":features:onramp:impl",":domain:joint-account",1,0],[":features:onramp:impl",":domain:legacy",1,1],[":features:onramp:impl",":domain:models",1,1],[":features:onramp:impl",":domain:offramp",1,1],[":features:onramp:impl",":domain:onramp",1,1],[":features:onramp:impl",":domain:onramp:models",1,1],[":features:onramp:impl",":domain:settings",1,1],[":features:onramp:impl",":domain:tokens",1,1],[":features:onramp:impl",":domain:wallets",1,1],[":features:onramp:impl",":domain:app-currency:models",1,0],[":features:onramp:impl",":domain:app-theme:models",1,0],[":features:onramp:impl",":domain:balance-hiding:models",1,0],[":features:onramp:impl",":domain:core",1,0],[":features:onramp:impl",":domain:tokens:models",1,0],[":features:onramp:impl",":domain:transaction:models",1,0],[":features:onramp:impl",":domain:wallets:models",1,0],[":features:onramp:impl",":data:common",1,1],[":features:onramp:api",":domain:models",1,1],[":features:onramp:api",":domain:onramp:models",1,1],[":features:walletconnect:impl",":features:common-features:api",1,0],[":features:walletconnect:impl",":features:walletconnect:api",1,0],[":features:walletconnect:impl",":features:send:api",1,0],[":features:walletconnect:impl",":domain:account",1,0],[":features:walletconnect:impl",":domain:account:status",1,0],[":features:walletconnect:impl",":domain:blockaid:models",1,0],[":features:walletconnect:impl",":domain:models",1,0],[":features:walletconnect:impl",":domain:qr-scanning:models",1,0],[":features:walletconnect:impl",":domain:tokens:models",1,0],[":features:walletconnect:impl",":domain:transaction:models",1,0],[":features:walletconnect:impl",":domain:wallets:models",1,0],[":features:walletconnect:impl",":domain:wallet-connect",1,0],[":features:walletconnect:impl",":domain:wallet-connect:models",1,0],[":features:walletconnect:impl",":domain:qr-scanning",1,0],[":features:walletconnect:impl",":domain:transaction",1,0],[":features:walletconnect:impl",":domain:wallets",1,0],[":features:walletconnect:impl",":data:card",1,0],[":features:walletconnect:impl",":domain:core",1,0],[":features:walletconnect:api",":domain:models",1,1],[":features:stories:impl",":features:stories:api",1,0],[":features:stories:impl",":domain:stories",1,0],[":features:stories:impl",":domain:stories:models",1,0],[":features:onboarding-v2:impl",":features:biometry:api",1,1],[":features:onboarding-v2:impl",":features:hot-wallet:api",1,1],[":features:onboarding-v2:impl",":features:manage-tokens:api",1,1],[":features:onboarding-v2:impl",":features:onboarding-v2:api",1,1],[":features:onboarding-v2:impl",":features:push-notifications:api",1,1],[":features:onboarding-v2:impl",":domain:account",1,1],[":features:onboarding-v2:impl",":domain:appsflyer",1,0],[":features:onboarding-v2:impl",":domain:card",1,1],[":features:onboarding-v2:impl",":domain:cloud-backup",1,0],[":features:onboarding-v2:impl",":domain:common",1,1],[":features:onboarding-v2:impl",":domain:feedback",1,1],[":features:onboarding-v2:impl",":domain:models",1,1],[":features:onboarding-v2:impl",":domain:networks",1,1],[":features:onboarding-v2:impl",":domain:onboarding",1,1],[":features:onboarding-v2:impl",":domain:settings",1,1],[":features:onboarding-v2:impl",":domain:social-login",1,1],[":features:onboarding-v2:impl",":domain:staking",1,1],[":features:onboarding-v2:impl",":domain:tokens",1,1],[":features:onboarding-v2:impl",":domain:visa",1,1],[":features:onboarding-v2:impl",":domain:wallets",1,1],[":features:onboarding-v2:impl",":domain:feedback:models",1,0],[":features:onboarding-v2:impl",":domain:visa:models",1,0],[":features:onboarding-v2:impl",":domain:wallets:models",1,0],[":features:onboarding-v2:api",":domain:models",1,1],[":features:promo-banners:impl",":features:promo-banners:api",1,1],[":features:promo-banners:impl",":features:common-features:api",1,0],[":features:promo-banners:impl",":domain:common",1,1],[":features:promo-banners:impl",":domain:models",1,0],[":features:promo-banners:impl",":domain:app-currency",1,0],[":features:promo-banners:impl",":domain:promo",1,0],[":features:promo-banners:impl",":domain:promo:models",1,0],[":features:promo-banners:impl",":domain:markets:models",2,0],[":features:promo-banners:impl",":data:common",1,0],[":features:promo-banners:impl",":domain:wallets",1,1],[":features:promo-banners:impl",":domain:account",1,1],[":features:promo-banners:impl",":domain:app-currency:models",1,0],[":features:promo-banners:impl",":domain:wallets:models",1,0],[":features:push-notifications:impl",":domain:settings",1,0],[":features:push-notifications:impl",":domain:notifications",1,0],[":features:push-notifications:impl",":domain:push-notification-preferences",1,0],[":features:push-notifications:impl",":domain:common",1,0],[":features:push-notifications:impl",":domain:models",1,0],[":features:push-notifications:impl",":domain:wallets",1,0],[":features:push-notifications:impl",":features:push-notifications:api",1,0],[":features:swap-v2:impl",":features:swap-v2:api",1,0],[":features:swap-v2:impl",":features:manage-tokens:api",1,0],[":features:swap-v2:impl",":features:send:api",1,0],[":features:swap-v2:impl",":domain:models",1,0],[":features:swap-v2:impl",":domain:wallets:models",1,0],[":features:swap-v2:impl",":domain:wallets",1,0],[":features:swap-v2:impl",":domain:tokens:models",1,0],[":features:swap-v2:impl",":domain:tokens",1,0],[":features:swap-v2:impl",":domain:app-currency:models",1,0],[":features:swap-v2:impl",":domain:app-currency",1,0],[":features:swap-v2:impl",":domain:express:models",1,0],[":features:swap-v2:impl",":domain:swap:models",1,0],[":features:swap-v2:impl",":domain:swap",1,0],[":features:swap-v2:impl",":domain:manage-tokens:models",1,0],[":features:swap-v2:impl",":domain:manage-tokens",1,0],[":features:swap-v2:impl",":domain:transaction:models",1,0],[":features:swap-v2:impl",":domain:transaction",1,0],[":features:swap-v2:impl",":domain:legacy",1,0],[":features:swap-v2:impl",":domain:balance-hiding:models",1,0],[":features:swap-v2:impl",":domain:balance-hiding",1,0],[":features:swap-v2:impl",":domain:settings",1,0],[":features:swap-v2:impl",":domain:txhistory",1,0],[":features:swap-v2:impl",":domain:notifications",1,0],[":features:swap-v2:impl",":domain:feedback:models",1,0],[":features:swap-v2:impl",":domain:feedback",1,0],[":features:swap-v2:impl",":domain:account:status",1,0],[":features:swap-v2:impl",":domain:quotes",1,1],[":features:swap-v2:api",":features:send:api",1,1],[":features:swap-v2:api",":domain:swap:models",1,1],[":features:swap-v2:api",":domain:manage-tokens:models",1,1],[":features:swap-v2:api",":domain:models",1,1],[":features:force-update:impl",":features:force-update:api",1,0],[":features:force-update:impl",":domain:app-update",1,0],[":features:force-update:impl",":domain:feedback",1,0],[":features:force-update:impl",":domain:feedback:models",1,0],[":features:manage-tokens:impl",":features:common-features:api",1,1],[":features:manage-tokens:impl",":features:manage-tokens:api",1,1],[":features:manage-tokens:impl",":features:swap-v2:api",1,1],[":features:manage-tokens:impl",":domain:account",1,1],[":features:manage-tokens:impl",":domain:account:status",1,1],[":features:manage-tokens:impl",":domain:dynamic-addresses",1,1],[":features:manage-tokens:impl",":domain:manage-tokens",1,1],[":features:manage-tokens:impl",":domain:manage-tokens:models",1,1],[":features:manage-tokens:impl",":domain:models",1,1],[":features:manage-tokens:impl",":domain:notifications",1,1],[":features:manage-tokens:impl",":domain:wallets",1,1],[":features:manage-tokens:impl",":domain:core",1,0],[":features:manage-tokens:impl",":domain:markets:models",1,0],[":features:manage-tokens:impl",":domain:swap:models",1,0],[":features:manage-tokens:api",":domain:models",1,1],[":features:markets:impl",":features:markets:api",1,1],[":features:markets:impl",":domain:app-currency",1,1],[":features:markets:impl",":domain:markets",1,1],[":features:markets:impl",":domain:app-currency:models",1,0],[":features:markets:impl",":domain:markets:models",1,0],[":features:markets:impl",":domain:models",1,0],[":features:markets:api",":domain:models",1,1],[":features:feed:impl",":features:common-features:api",1,1],[":features:feed:impl",":features:feed:api",1,1],[":features:feed:impl",":features:feed:search:api",1,1],[":features:feed:impl",":features:for-you:api",1,1],[":features:feed:impl",":features:promo-banners:api",1,1],[":features:feed:impl",":features:marketing:api",1,1],[":features:feed:impl",":features:polymarket:api",1,0],[":features:feed:impl",":features:collectibles:api",1,0],[":features:feed:impl",":features:joint-account:api",1,0],[":features:feed:impl",":features:wallet-settings:api",1,0],[":features:feed:impl",":data:common",1,0],[":features:feed:impl",":domain:account:status",1,1],[":features:feed:impl",":domain:app-currency",1,1],[":features:feed:impl",":domain:balance-hiding",1,1],[":features:feed:impl",":domain:common",1,1],[":features:feed:impl",":domain:earn",1,1],[":features:feed:impl",":domain:feedback",1,1],[":features:feed:impl",":domain:markets",1,1],[":features:feed:impl",":domain:models",1,1],[":features:feed:impl",":domain:news",1,1],[":features:feed:impl",":domain:search",1,1],[":features:feed:impl",":domain:settings",1,1],[":features:feed:impl",":domain:staking",1,1],[":features:feed:impl",":domain:tokens",1,1],[":features:feed:impl",":domain:wallets",1,1],[":features:feed:impl",":domain:yield-supply",1,1],[":features:feed:impl",":domain:account",1,0],[":features:feed:impl",":domain:app-currency:models",1,0],[":features:feed:impl",":domain:card",1,0],[":features:feed:impl",":domain:core",1,0],[":features:feed:impl",":domain:demo",1,0],[":features:feed:impl",":domain:referral",1,0],[":features:feed:impl",":domain:feedback:models",1,0],[":features:feed:impl",":domain:tokens:models",1,0],[":features:feed:impl",":domain:visa",1,0],[":features:feed:impl",":domain:marketing:models",1,0],[":features:feed:impl",":domain:yield-supply:models",1,0],[":features:feed:impl",":domain:markets:models",1,1],[":features:feed:impl",":domain:wallets:models",1,0],[":features:feed:crypto:impl",":features:feed:api",1,0],[":features:feed:crypto:impl",":features:feed:crypto:api",1,0],[":features:feed:crypto:impl",":features:promo-banners:api",1,0],[":features:feed:crypto:impl",":domain:app-currency",1,0],[":features:feed:crypto:impl",":domain:app-currency:models",1,0],[":features:feed:crypto:impl",":domain:balance-hiding",1,0],[":features:feed:crypto:impl",":domain:common",1,0],[":features:feed:crypto:impl",":domain:feed:search",1,0],[":features:feed:crypto:impl",":domain:markets",1,0],[":features:feed:crypto:impl",":domain:markets:models",1,0],[":features:feed:crypto:impl",":domain:models",1,0],[":features:feed:crypto:impl",":domain:search",1,0],[":features:feed:crypto:impl",":domain:tokens:models",1,0],[":features:feed:crypto:impl",":domain:wallets",1,0],[":features:feed:crypto:api",":features:feed:api",1,1],[":features:feed:rwa:impl",":features:feed:api",1,0],[":features:feed:rwa:impl",":features:promo-banners:api",1,0],[":features:feed:rwa:impl",":domain:app-currency",1,0],[":features:feed:rwa:impl",":domain:app-currency:models",1,0],[":features:feed:rwa:impl",":domain:balance-hiding",1,0],[":features:feed:rwa:impl",":domain:common",1,0],[":features:feed:rwa:impl",":domain:feed:search",1,0],[":features:feed:rwa:impl",":domain:markets",1,0],[":features:feed:rwa:impl",":domain:markets:models",1,0],[":features:feed:rwa:impl",":domain:models",1,0],[":features:feed:rwa:impl",":domain:search",1,0],[":features:feed:rwa:impl",":domain:settings",1,0],[":features:feed:rwa:impl",":domain:tokens:models",1,0],[":features:feed:rwa:impl",":domain:wallets",1,0],[":features:feed:earn:impl",":features:feed:api",1,0],[":features:feed:earn:impl",":features:promo-banners:api",1,0],[":features:feed:earn:impl",":features:common-features:api",1,0],[":features:feed:earn:impl",":domain:app-currency",1,0],[":features:feed:earn:impl",":domain:app-currency:models",1,0],[":features:feed:earn:impl",":domain:balance-hiding",1,0],[":features:feed:earn:impl",":domain:common",1,0],[":features:feed:earn:impl",":domain:earn",1,0],[":features:feed:earn:impl",":domain:models",1,0],[":features:feed:earn:impl",":domain:markets:models",1,0],[":features:feed:earn:impl",":domain:search",1,0],[":features:feed:earn:impl",":domain:tokens:models",1,0],[":features:feed:earn:impl",":domain:wallets",1,0],[":features:feed:search:impl",":features:feed:api",1,0],[":features:feed:search:impl",":features:feed:search:api",1,0],[":features:feed:search:impl",":domain:feed:search",1,0],[":features:feed:search:api",":features:feed:api",1,1],[":features:feed:api",":domain:app-currency:models",1,1],[":features:feed:api",":domain:markets:models",1,1],[":features:feed:api",":domain:models",1,1],[":features:feed:api",":domain:news",1,1],[":features:staking:impl",":domain:tokens",1,0],[":features:staking:impl",":domain:tokens:models",1,0],[":features:staking:impl",":domain:wallets",1,0],[":features:staking:impl",":domain:staking",1,0],[":features:staking:impl",":domain:staking:models",2,1],[":features:staking:impl",":domain:balance-hiding",1,0],[":features:staking:impl",":domain:balance-hiding:models",1,0],[":features:staking:impl",":domain:app-currency",1,0],[":features:staking:impl",":domain:app-currency:models",1,0],[":features:staking:impl",":domain:legacy",1,0],[":features:staking:impl",":domain:models",1,0],[":features:staking:impl",":domain:transaction",1,0],[":features:staking:impl",":domain:transaction:models",1,0],[":features:staking:impl",":domain:txhistory",1,0],[":features:staking:impl",":domain:feedback",1,0],[":features:staking:impl",":domain:feedback:models",1,0],[":features:staking:impl",":domain:account:status",1,0],[":features:staking:impl",":domain:marketing:models",1,0],[":features:staking:impl",":domain:onramp:models",2,0],[":features:staking:impl",":features:staking:api",1,0],[":features:staking:impl",":features:txhistory:api",1,0],[":features:staking:impl",":features:approval:api",1,0],[":features:staking:impl",":features:marketing:api",1,0],[":features:staking:impl",":domain:core",1,0],[":features:staking:impl",":domain:txhistory:models",1,0],[":features:staking:api",":domain:models",1,1],[":features:staking:api",":domain:staking",1,1],[":features:money-account:details:impl",":domain:models",1,0],[":features:money-account:details:impl",":features:money-account:details:api",1,0],[":features:money-account:details:api",":domain:models",1,1],[":features:money-account:main:impl",":features:money-account:main:api",1,0],[":features:money-account:onboarding:impl",":features:money-account:onboarding:api",1,1],[":features:money-account:onboarding:impl",":domain:common",1,1],[":features:money-account:onboarding:impl",":domain:visa",1,1],[":features:money-account:onboarding:impl",":domain:models",1,0],[":features:money-account:onboarding:impl",":domain:money-account",1,0],[":features:money-account:onboarding:impl",":domain:visa:models",1,0],[":features:address-book:impl",":features:address-book:api",1,1],[":features:address-book:impl",":features:common-features:api",1,1],[":features:address-book:impl",":domain:address-book",1,1],[":features:address-book:impl",":domain:wallets",1,1],[":features:address-book:impl",":domain:models",1,0],[":features:address-book:impl",":domain:qr-scanning",1,0],[":features:address-book:impl",":domain:qr-scanning:models",1,0],[":features:address-book:impl",":domain:common",1,1],[":features:address-book:impl",":domain:transaction",1,0],[":features:address-book:api",":domain:models",1,1],[":features:wallet:impl",":domain:account",1,1],[":features:wallet:impl",":domain:appsflyer",1,0],[":features:wallet:impl",":domain:account:status",1,1],[":features:wallet:impl",":domain:address-book",1,1],[":features:wallet:impl",":domain:analytics",1,1],[":features:wallet:impl",":domain:app-currency",1,1],[":features:wallet:impl",":domain:app-theme",1,1],[":features:wallet:impl",":domain:app-update",1,1],[":features:wallet:impl",":domain:assetsdiscovery",1,1],[":features:wallet:impl",":domain:balance-hiding",1,1],[":features:wallet:impl",":domain:card",1,1],[":features:wallet:impl",":domain:common",1,1],[":features:wallet:impl",":domain:demo",1,1],[":features:wallet:impl",":domain:feedback",1,1],[":features:wallet:impl",":domain:hot-wallet",1,1],[":features:wallet:impl",":domain:legacy",1,1],[":features:wallet:impl",":domain:models",1,1],[":features:wallet:impl",":domain:money-account",1,1],[":features:wallet:impl",":domain:networks",1,1],[":features:wallet:impl",":domain:nft",1,1],[":features:wallet:impl",":domain:notifications",1,1],[":features:wallet:impl",":domain:offramp",1,1],[":features:wallet:impl",":domain:onramp",1,1],[":features:wallet:impl",":domain:push-notification-preferences",1,1],[":features:wallet:impl",":domain:qr-scanning",1,1],[":features:wallet:impl",":domain:quotes",1,1],[":features:wallet:impl",":domain:settings",1,1],[":features:wallet:impl",":domain:staking",1,1],[":features:wallet:impl",":domain:stories",1,1],[":features:wallet:impl",":domain:tokens",1,1],[":features:wallet:impl",":domain:tokens:models",1,1],[":features:wallet:impl",":domain:transaction",1,1],[":features:wallet:impl",":domain:txhistory",1,1],[":features:wallet:impl",":domain:visa",1,1],[":features:wallet:impl",":domain:wallet-connect",1,1],[":features:wallet:impl",":domain:wallet-manager",1,1],[":features:wallet:impl",":domain:wallets",1,1],[":features:wallet:impl",":domain:wallets:models",1,1],[":features:wallet:impl",":domain:yield-supply",1,1],[":features:wallet:impl",":domain:app-currency:models",1,0],[":features:wallet:impl",":domain:app-theme:models",1,0],[":features:wallet:impl",":domain:balance-hiding:models",1,0],[":features:wallet:impl",":domain:core",1,0],[":features:wallet:impl",":domain:demo:models",1,0],[":features:wallet:impl",":domain:feedback:models",1,0],[":features:wallet:impl",":domain:marketing",1,0],[":features:wallet:impl",":domain:markets:models",1,0],[":features:wallet:impl",":domain:nft:models",1,0],[":features:wallet:impl",":domain:onramp:models",1,0],[":features:wallet:impl",":domain:qr-scanning:models",1,0],[":features:wallet:impl",":domain:staking:models",1,0],[":features:wallet:impl",":domain:stories:models",1,0],[":features:wallet:impl",":domain:txhistory:models",1,0],[":features:wallet:impl",":domain:visa:models",1,0],[":features:wallet:impl",":domain:wallet-connect:models",1,0],[":features:wallet:impl",":domain:yield-supply:models",1,0],[":features:wallet:impl",":features:biometry:api",1,1],[":features:wallet:impl",":features:collectibles:api",1,1],[":features:wallet:impl",":features:common-features:api",1,1],[":features:wallet:impl",":features:feed:api",1,1],[":features:wallet:impl",":features:joint-account:api",1,1],[":features:wallet:impl",":domain:joint-account",1,0],[":features:wallet:impl",":features:polymarket:api",1,1],[":features:wallet:impl",":features:promo-banners:api",1,1],[":features:wallet:impl",":features:push-notifications:api",1,1],[":features:wallet:impl",":features:send:api",1,1],[":features:wallet:impl",":features:tangempay:details:api",1,1],[":features:wallet:impl",":features:tangempay:main:api",1,1],[":features:wallet:impl",":features:token-recieve:api",1,1],[":features:wallet:impl",":features:money-account:details:api",1,1],[":features:wallet:impl",":features:money-account:main:api",1,1],[":features:wallet:impl",":features:wallet:api",1,1],[":features:wallet:impl",":features:wallet-settings:api",1,1],[":features:wallet:impl",":features:yield-supply:api",1,1],[":features:wallet:api",":domain:models",1,1],[":features:wallet:api",":domain:visa:models",1,1],[":features:stories-v2:impl",":features:stories-v2:api",1,1],[":features:marketing:impl",":features:marketing:api",1,0],[":features:marketing:impl",":domain:marketing",1,0],[":features:marketing:impl",":domain:marketing:models",1,0],[":features:marketing:api",":domain:marketing:models",1,0],[":features:tester:impl",":domain:account",1,1],[":features:tester:impl",":domain:common",1,1],[":features:tester:impl",":domain:feedback",1,1],[":features:tester:impl",":domain:markets",1,1],[":features:tester:impl",":domain:settings",1,1],[":features:tester:impl",":domain:wallets",1,1],[":features:tester:impl",":domain:app-currency:models",1,0],[":features:tester:impl",":domain:demo:models",1,0],[":features:tester:impl",":domain:feedback:models",1,0],[":features:tester:impl",":domain:markets:models",1,0],[":features:tester:impl",":domain:models",1,0],[":features:tester:impl",":domain:offramp",1,0],[":features:tester:impl",":domain:social-login",1,0],[":features:tester:impl",":domain:wallet-manager",1,0],[":features:tester:impl",":data:common",1,1],[":features:tester:impl",":features:stories-v2:api",1,1],[":features:tester:impl",":features:survey:api",1,1],[":features:tester:impl",":features:tester:api",1,1],[":features:tester:impl",":features:hot-wallet:api",1,0],[":features:tester:impl",":features:push-notifications:api",1,0],[":features:tester:impl",":domain:staking",1,0],[":features:biometry:impl",":features:biometry:api",1,1],[":features:biometry:impl",":domain:card",1,1],[":features:biometry:impl",":domain:common",1,1],[":features:biometry:impl",":domain:settings",1,1],[":features:biometry:impl",":domain:wallets",1,1],[":features:biometry:impl",":domain:models",1,0],[":features:account:impl",":features:account:api",1,1],[":features:account:impl",":domain:account",1,1],[":features:account:impl",":domain:account:status",1,1],[":features:account:impl",":domain:models",1,1],[":features:account:impl",":domain:wallets",1,1],[":features:account:impl",":domain:core",1,0],[":features:account:impl",":features:joint-account:api",1,0],[":features:account:api",":domain:models",1,1],[":features:hot-wallet:impl",":features:hot-wallet:api",1,1],[":features:hot-wallet:impl",":features:onboarding-v2:api",1,1],[":features:hot-wallet:impl",":features:push-notifications:api",1,1],[":features:hot-wallet:impl",":features:stories-v2:api",1,0],[":features:hot-wallet:impl",":domain:assetsdiscovery",1,1],[":features:hot-wallet:impl",":domain:appsflyer",1,0],[":features:hot-wallet:impl",":domain:card",1,1],[":features:hot-wallet:impl",":domain:common",1,1],[":features:hot-wallet:impl",":domain:feedback",1,1],[":features:hot-wallet:impl",":domain:hot-wallet",1,1],[":features:hot-wallet:impl",":domain:settings",1,1],[":features:hot-wallet:impl",":domain:social-login",1,1],[":features:hot-wallet:impl",":domain:wallets",1,1],[":features:hot-wallet:impl",":domain:cloud-backup",1,0],[":features:hot-wallet:impl",":domain:feedback:models",1,0],[":features:hot-wallet:impl",":domain:models",1,0],[":features:hot-wallet:impl",":domain:wallets:models",1,0],[":features:hot-wallet:impl",":data:cloud-backup",1,0],[":features:hot-wallet:api",":domain:cloud-backup",1,1],[":features:hot-wallet:api",":domain:models",1,1],[":features:hot-wallet:api",":domain:social-login",1,1],[":features:hot-wallet:api",":domain:wallets",1,1],[":features:survey:impl",":features:survey:api",1,0],[":features:survey:impl",":domain:models",1,0],[":features:survey:impl",":domain:wallets",1,0],[":features:survey:impl",":domain:wallets:models",1,0],[":features:send:impl",":features:send:api",1,0],[":features:send:impl",":features:txhistory:api",1,0],[":features:send:impl",":features:nft:api",1,0],[":features:send:impl",":features:swap-v2:api",1,0],[":features:send:impl",":features:manage-tokens:api",1,0],[":features:send:impl",":features:address-book:api",1,0],[":features:send:impl",":data:nft",1,0],[":features:send:impl",":domain:models",1,0],[":features:send:impl",":domain:joint-account",1,0],[":features:send:impl",":domain:legacy",1,0],[":features:send:impl",":domain:offramp",1,0],[":features:send:impl",":domain:tokens:models",1,0],[":features:send:impl",":domain:tokens",1,0],[":features:send:impl",":domain:wallets:models",1,0],[":features:send:impl",":domain:wallets",1,0],[":features:send:impl",":domain:app-currency:models",1,0],[":features:send:impl",":domain:app-currency",1,0],[":features:send:impl",":domain:transaction:models",1,0],[":features:send:impl",":domain:transaction",2,0],[":features:send:impl",":domain:txhistory:models",1,0],[":features:send:impl",":domain:txhistory",2,0],[":features:send:impl",":domain:qr-scanning:models",1,0],[":features:send:impl",":domain:qr-scanning",1,0],[":features:send:impl",":domain:settings",1,0],[":features:send:impl",":domain:feedback",1,0],[":features:send:impl",":domain:feedback:models",1,0],[":features:send:impl",":domain:balance-hiding:models",1,0],[":features:send:impl",":domain:balance-hiding",1,0],[":features:send:impl",":domain:nft:models",1,0],[":features:send:impl",":domain:nft",1,0],[":features:send:impl",":domain:notifications",1,0],[":features:send:impl",":domain:account",1,0],[":features:send:impl",":domain:account:status",1,0],[":features:send:impl",":domain:address-book",1,0],[":features:send:impl",":domain:core",1,0],[":features:send:impl",":domain:visa",1,0],[":features:send:impl",":domain:quotes",1,1],[":features:send:api",":domain:transaction",1,1],[":features:send:api",":domain:app-currency:models",1,1],[":features:send:api",":domain:models",1,1],[":features:send:api",":domain:nft:models",1,1],[":features:send:api",":domain:transaction:models",1,1],[":features:send:api",":domain:wallets:models",1,1],[":features:joint-account:impl",":features:joint-account:api",1,1],[":features:joint-account:impl",":features:common-features:api",1,0],[":features:joint-account:impl",":features:push-notifications:api",1,0],[":features:joint-account:impl",":features:wallet:api",1,0],[":features:joint-account:impl",":domain:account",1,0],[":features:joint-account:impl",":domain:account:status",1,0],[":features:joint-account:impl",":domain:common",1,0],[":features:joint-account:impl",":domain:joint-account",1,0],[":features:joint-account:impl",":domain:models",1,0],[":features:joint-account:impl",":domain:notifications:models",1,0],[":features:joint-account:impl",":domain:wallets",1,0],[":features:joint-account:impl",":domain:app-currency:models",1,0],[":features:joint-account:api",":domain:models",1,1],[":features:tangempay:details:impl",":features:tangempay:details:api",1,1],[":features:tangempay:details:impl",":features:tangempay:common-ui",1,0],[":features:tangempay:details:impl",":features:tangempay:eligibility:api",1,0],[":features:tangempay:details:impl",":features:token-recieve:api",1,1],[":features:tangempay:details:impl",":features:promo-banners:api",1,1],[":features:tangempay:details:impl",":features:money-account:details:api",1,1],[":features:tangempay:details:impl",":domain:balance-hiding",1,1],[":features:tangempay:details:impl",":domain:feedback",1,1],[":features:tangempay:details:impl",":domain:models",1,1],[":features:tangempay:details:impl",":domain:visa",1,1],[":features:tangempay:details:impl",":domain:balance-hiding:models",1,0],[":features:tangempay:details:impl",":domain:feedback:models",1,0],[":features:tangempay:details:impl",":domain:visa:models",1,0],[":features:tangempay:details:impl",":domain:wallets",1,0],[":features:tangempay:details:impl",":domain:wallets:models",1,0],[":features:tangempay:details:api",":domain:models",1,1],[":features:tangempay:details:api",":domain:visa:models",1,1],[":features:tangempay:eligibility:impl",":features:tangempay:eligibility:api",1,1],[":features:tangempay:eligibility:impl",":features:common-features:api",1,0],[":features:tangempay:eligibility:impl",":domain:models",1,0],[":features:tangempay:eligibility:impl",":domain:money-account",1,0],[":features:tangempay:eligibility:impl",":domain:money-account:models",1,0],[":features:tangempay:eligibility:impl",":domain:visa",1,0],[":features:tangempay:eligibility:impl",":domain:visa:models",1,0],[":features:tangempay:eligibility:api",":domain:models",1,1],[":features:tangempay:main:impl",":features:tangempay:main:api",1,0],[":features:tangempay:onboarding:impl",":features:tangempay:onboarding:api",1,1],[":features:tangempay:onboarding:impl",":features:wallet:api",1,1],[":features:tangempay:onboarding:impl",":features:tangempay:common-ui",1,0],[":features:tangempay:onboarding:impl",":features:tangempay:details:api",1,0],[":features:tangempay:onboarding:impl",":features:tangempay:eligibility:api",1,0],[":features:tangempay:onboarding:impl",":domain:common",1,1],[":features:tangempay:onboarding:impl",":domain:hot-wallet",1,1],[":features:tangempay:onboarding:impl",":domain:appsflyer",1,0],[":features:tangempay:onboarding:impl",":domain:visa",1,1],[":features:tangempay:onboarding:impl",":domain:visa:models",1,1],[":features:tangempay:onboarding:impl",":domain:wallets",1,1],[":features:tangempay:onboarding:impl",":domain:models",1,0],[":features:tangempay:onboarding:api",":domain:models",1,1],[":features:approval:impl",":features:approval:api",1,1],[":features:approval:impl",":features:send:api",1,1],[":features:approval:impl",":domain:transaction",1,1],[":features:approval:impl",":domain:wallets",1,1],[":features:approval:impl",":domain:models",1,0],[":features:approval:impl",":domain:transaction:models",1,0],[":features:approval:api",":domain:models",1,1],[":features:push-notification-settings:impl",":features:push-notification-settings:api",1,1],[":features:push-notification-settings:impl",":features:wallet-settings:api",1,1],[":features:push-notification-settings:impl",":features:push-notifications:api",1,0],[":features:push-notification-settings:impl",":domain:push-notification-preferences",1,1],[":features:push-notification-settings:impl",":domain:models",1,0],[":features:push-notification-settings:impl",":domain:notifications",1,0],[":features:push-notification-settings:impl",":domain:wallets",1,0],[":features:push-notification-settings:api",":domain:models",1,1],[":data:social-login",":domain:models",1,0],[":data:social-login",":domain:social-login",1,1],[":data:transaction",":data:common",1,1],[":data:transaction",":domain:models",1,1],[":data:transaction",":domain:transaction",1,1],[":data:transaction",":domain:wallet-manager",1,1],[":data:transaction",":domain:demo",1,0],[":data:transaction",":domain:wallets:models",1,1],[":data:transaction",":domain:demo:models",1,0],[":data:transaction",":domain:transaction:models",1,0],[":data:settings",":domain:settings",1,1],[":data:dynamic-addresses",":data:common",1,1],[":data:dynamic-addresses",":domain:account",1,1],[":data:dynamic-addresses",":domain:common",1,1],[":data:dynamic-addresses",":domain:dynamic-addresses",1,1],[":data:dynamic-addresses",":domain:models",1,1],[":data:dynamic-addresses",":domain:wallet-manager",1,1],[":data:dynamic-addresses",":domain:dynamic-addresses:models",1,0],[":data:app-theme",":domain:app-theme",1,1],[":data:app-theme",":domain:app-theme:models",1,0],[":data:yield-supply",":domain:transaction",1,1],[":data:yield-supply",":domain:wallet-manager",1,1],[":data:yield-supply",":domain:yield-supply",1,1],[":data:yield-supply",":domain:legacy",1,0],[":data:yield-supply",":domain:models",1,0],[":data:yield-supply",":domain:yield-supply:models",1,0],[":data:txhistory",":domain:common",2,1],[":data:txhistory",":domain:wallet-manager",2,1],[":data:txhistory",":domain:models",2,1],[":data:txhistory",":domain:tokens:models",2,0],[":data:txhistory",":domain:txhistory",2,1],[":data:txhistory",":domain:txhistory:models",2,0],[":data:txhistory",":domain:express",2,1],[":data:txhistory",":domain:express:models",2,0],[":data:txhistory",":domain:wallets",2,1],[":data:txhistory",":domain:onramp",2,1],[":data:txhistory",":domain:onramp:models",2,0],[":data:txhistory",":domain:account",2,1],[":data:txhistory",":domain:account:status",2,0],[":data:txhistory",":domain:visa",2,0],[":data:txhistory",":domain:visa:models",2,0],[":data:txhistory",":data:common",1,1],[":data:txhistory",":domain:quotes",1,0],[":data:collectibles",":data:common",1,0],[":data:collectibles",":data:wallets",1,0],[":data:collectibles",":domain:collectibles",1,1],[":data:collectibles",":domain:card",1,0],[":data:collectibles",":domain:wallet-manager",1,0],[":data:collectibles",":domain:collectibles:models",1,0],[":data:collectibles",":domain:core",1,0],[":data:collectibles",":domain:quotes",1,0],[":data:collectibles",":domain:common",1,0],[":data:collectibles",":domain:models",1,0],[":data:collectibles",":domain:wallets",1,0],[":data:push-notification-preferences",":domain:push-notification-preferences",1,1],[":data:push-notification-preferences",":domain:models",1,0],[":data:card",":domain:card",1,1],[":data:card",":domain:models",1,1],[":data:nft",":data:common",1,1],[":data:nft",":domain:common",1,1],[":data:nft",":domain:wallet-manager",1,1],[":data:nft",":domain:card",1,0],[":data:nft",":domain:models",1,0],[":data:nft",":domain:nft",1,0],[":data:nft",":domain:nft:models",1,0],[":data:quotes",":data:common",1,1],[":data:quotes",":domain:core",1,1],[":data:quotes",":domain:quotes",1,1],[":data:quotes",":domain:models",1,0],[":data:wallet-manager",":domain:common",1,1],[":data:wallet-manager",":domain:models",1,1],[":data:wallet-manager",":domain:transaction",1,1],[":data:wallet-manager",":domain:card",1,0],[":data:wallet-manager",":domain:joint-account",1,0],[":data:wallet-manager",":domain:txhistory",1,0],[":data:wallet-manager",":domain:wallet-manager",1,0],[":data:wallet-manager",":domain:wallets",1,0],[":data:wallet-manager",":domain:demo:models",1,0],[":data:wallet-manager",":domain:express:models",1,0],[":data:wallet-manager",":domain:transaction:models",1,0],[":data:wallet-manager",":domain:txhistory:models",1,0],[":data:wallet-manager",":domain:wallet-manager:models",1,0],[":data:express",":data:common",1,0],[":data:express",":domain:common",1,1],[":data:express",":domain:express",1,1],[":data:express",":domain:express:models",1,1],[":data:express",":domain:txhistory",1,1],[":data:express",":domain:core",1,0],[":data:express",":domain:models",1,0],[":data:qr-scanning",":domain:qr-scanning",1,1],[":data:qr-scanning",":domain:models",1,0],[":data:qr-scanning",":domain:qr-scanning:models",1,0],[":data:blockaid",":domain:blockaid",1,1],[":data:blockaid",":domain:blockaid:models",1,1],[":data:blockaid",":domain:models",1,1],[":data:app-currency",":data:common",1,1],[":data:app-currency",":domain:app-currency",1,1],[":data:app-currency",":domain:app-currency:models",1,0],[":data:swap",":data:common",1,1],[":data:swap",":data:express",1,0],[":data:swap",":domain:account",1,1],[":data:swap",":domain:express",1,1],[":data:swap",":domain:models",1,1],[":data:swap",":domain:quotes",1,1],[":data:swap",":domain:swap",1,1],[":data:swap",":domain:core",1,0],[":data:swap",":domain:tokens",1,0],[":data:swap",":domain:swap:models",1,1],[":data:swap",":domain:express:models",1,0],[":data:earn",":data:common",1,0],[":data:earn",":domain:common",1,1],[":data:earn",":domain:earn",1,1],[":data:earn",":domain:models",1,0],[":data:wallet-connect",":data:common",1,0],[":data:wallet-connect",":domain:account",1,1],[":data:wallet-connect",":domain:account:status",1,1],[":data:wallet-connect",":domain:blockaid",1,1],[":data:wallet-connect",":domain:common",1,1],[":data:wallet-connect",":domain:networks",1,1],[":data:wallet-connect",":domain:transaction",1,1],[":data:wallet-connect",":domain:wallet-connect",1,1],[":data:wallet-connect",":domain:wallet-connect:models",1,1],[":data:wallet-connect",":domain:wallet-manager",1,1],[":data:wallet-connect",":domain:wallets",1,1],[":data:wallet-connect",":domain:core",1,0],[":data:wallet-connect",":domain:models",1,0],[":data:wallet-connect",":domain:blockaid:models",1,0],[":data:wallet-connect",":domain:tokens:models",1,0],[":data:wallet-connect",":domain:transaction:models",1,0],[":data:visa",":data:common",1,1],[":data:visa",":data:wallets",1,0],[":data:visa",":domain:common",1,1],[":data:visa",":domain:core",1,1],[":data:visa",":domain:account",1,1],[":data:visa",":domain:quotes",1,1],[":data:visa",":domain:visa",1,1],[":data:visa",":domain:wallets",1,1],[":data:visa",":domain:card",1,0],[":data:visa",":domain:money-account",1,0],[":data:visa",":domain:models",1,0],[":data:visa",":domain:visa:models",1,1],[":data:visa",":domain:app-currency:models",1,0],[":data:visa",":features:swap:domain",1,1],[":data:polymarket",":domain:core",1,0],[":data:polymarket",":data:common",1,0],[":data:polymarket",":domain:polymarket",1,1],[":data:polymarket",":domain:quotes",1,0],[":data:polymarket",":domain:wallets",1,0],[":data:polymarket",":domain:common",1,0],[":data:polymarket",":domain:card",1,0],[":data:polymarket",":data:wallets",1,0],[":data:polymarket",":domain:models",1,0],[":data:balance-hiding",":domain:balance-hiding",1,1],[":data:balance-hiding",":domain:balance-hiding:models",1,0],[":data:feedback",":domain:common",1,1],[":data:feedback",":domain:cloud-backup",2,1],[":data:feedback",":domain:feedback",1,1],[":data:feedback",":domain:card",1,0],[":data:feedback",":domain:feedback:models",1,0],[":data:feedback",":domain:models",1,0],[":data:feedback",":domain:social-login",1,0],[":data:feedback",":domain:wallet-manager",1,0],[":data:feedback",":domain:wallets",1,0],[":data:search",":domain:account:status",1,1],[":data:search",":domain:common",1,1],[":data:search",":domain:search",1,1],[":data:search",":domain:models",1,0],[":data:onramp",":data:common",1,1],[":data:onramp",":domain:common",1,1],[":data:onramp",":domain:models",1,1],[":data:onramp",":domain:onramp",1,1],[":data:onramp",":domain:txhistory",1,1],[":data:onramp",":domain:wallet-manager",1,1],[":data:onramp",":domain:card",1,0],[":data:onramp",":domain:express:models",1,0],[":data:onramp",":domain:onramp:models",1,0],[":data:onramp",":domain:tokens:models",1,0],[":data:networks",":data:common",1,1],[":data:networks",":data:dynamic-addresses",1,1],[":data:networks",":domain:common",1,1],[":data:networks",":domain:core",1,1],[":data:networks",":domain:models",1,1],[":data:networks",":domain:networks",1,1],[":data:networks",":domain:wallet-manager",1,1],[":data:networks",":domain:card",1,0],[":data:common",":domain:common",1,1],[":data:common",":domain:appsflyer",1,0],[":data:common",":domain:wallet-manager",1,1],[":data:common",":domain:wallets",1,1],[":data:common",":domain:card",1,0],[":data:common",":domain:core",1,0],[":data:common",":domain:joint-account",1,0],[":data:common",":domain:demo:models",1,1],[":data:common",":domain:express:models",1,1],[":data:common",":domain:models",1,1],[":data:common",":domain:wallets:models",1,0],[":data:common",":features:joint-account:api",1,0],[":data:stories",":domain:stories",1,1],[":data:stories",":domain:stories:models",1,0],[":data:news",":domain:news",1,1],[":data:news",":domain:models",1,0],[":data:manage-tokens",":data:common",1,1],[":data:manage-tokens",":domain:common",1,1],[":data:manage-tokens",":domain:manage-tokens",1,1],[":data:manage-tokens",":domain:card",1,0],[":data:manage-tokens",":domain:joint-account",1,0],[":data:manage-tokens",":domain:models",1,0],[":data:manage-tokens",":domain:wallets",1,0],[":data:manage-tokens",":domain:manage-tokens:models",1,0],[":data:markets",":data:common",1,1],[":data:markets",":domain:common",1,1],[":data:markets",":domain:markets",1,1],[":data:markets",":domain:joint-account",1,0],[":data:markets",":domain:models",1,0],[":data:markets",":domain:markets:models",1,0],[":data:feed:search",":domain:feed:search",1,1],[":data:staking",":data:common",1,0],[":data:staking",":domain:common",1,1],[":data:staking",":domain:core",1,1],[":data:staking",":domain:models",1,1],[":data:staking",":domain:staking",1,1],[":data:staking",":domain:wallet-manager",1,1],[":data:staking",":domain:wallets",1,1],[":data:staking",":domain:txhistory",1,1],[":data:staking",":domain:card",1,0],[":data:staking",":domain:staking:models",1,1],[":data:staking",":domain:blockaid:models",1,0],[":data:money-account",":domain:money-account",1,1],[":data:money-account",":domain:common",1,0],[":data:money-account",":domain:core",1,0],[":data:money-account",":domain:models",1,0],[":data:money-account",":domain:networks",1,0],[":data:money-account",":domain:visa",1,0],[":data:money-account",":domain:wallets",1,0],[":data:money-account",":domain:visa:models",1,0],[":data:offramp",":domain:offramp",1,1],[":data:offramp",":domain:card",1,0],[":data:offramp",":domain:wallets",1,0],[":data:offramp",":domain:legacy",1,0],[":data:offramp",":domain:models",1,1],[":data:address-book",":data:common",1,1],[":data:address-book",":domain:address-book",1,1],[":data:address-book",":domain:common",1,1],[":data:address-book",":domain:models",1,1],[":data:assetsdiscovery",":data:common",1,1],[":data:assetsdiscovery",":data:wallet-manager",1,0],[":data:assetsdiscovery",":domain:assetsdiscovery",1,1],[":data:assetsdiscovery",":domain:common",1,1],[":data:assetsdiscovery",":domain:models",1,1],[":data:assetsdiscovery",":domain:wallets",1,0],[":data:marketing",":domain:marketing",1,0],[":data:marketing",":domain:marketing:models",1,0],[":data:wallets",":data:common",1,1],[":data:wallets",":domain:common",1,1],[":data:wallets",":domain:appsflyer",1,0],[":data:wallets",":domain:dynamic-addresses",1,1],[":data:wallets",":domain:models",1,1],[":data:wallets",":domain:settings",1,1],[":data:wallets",":domain:wallets",1,1],[":data:wallets",":domain:card",1,0],[":data:wallets",":domain:wallets:models",1,0],[":data:wallets",":domain:demo:models",1,0],[":data:cloud-backup",":domain:cloud-backup",1,0],[":data:account",":domain:account",1,1],[":data:account",":domain:common",1,1],[":data:account",":domain:core",1,1],[":data:account",":domain:models",1,1],[":data:account",":domain:tokens",1,1],[":data:account",":domain:card",1,0],[":data:account",":domain:joint-account",1,0],[":data:account",":domain:collectibles",1,0],[":data:account",":domain:polymarket",1,0],[":data:account",":data:common",1,1],[":data:account",":features:money-account:details:api",1,1],[":data:account",":features:collectibles:api",1,0],[":data:account",":features:polymarket:api",1,0],[":data:account",":features:joint-account:api",1,0],[":data:hot-wallet",":domain:hot-wallet",1,1],[":data:hot-wallet",":domain:models",1,0],[":data:app-update",":domain:app-update",1,0],[":data:appsflyer",":domain:appsflyer",1,1],[":data:appsflyer",":domain:wallets:models",1,0],[":data:tokens",":data:common",1,1],[":data:tokens",":domain:common",1,1],[":data:tokens",":domain:tokens",1,1],[":data:tokens",":domain:transaction",1,1],[":data:tokens",":domain:wallet-manager",1,1],[":data:tokens",":domain:core",1,0],[":data:tokens",":domain:joint-account",1,0],[":data:tokens",":domain:models",1,0],[":data:tokens",":features:joint-account:api",1,0],[":data:tokens",":domain:tokens:models",1,0],[":data:tokens",":domain:wallet-manager:models",1,0],[":data:tokens",":domain:staking:models",1,0],[":data:notifications",":domain:notifications",1,1],[":data:notifications",":domain:appsflyer",1,0],[":data:notifications",":domain:models",1,0],[":data:notifications",":domain:notifications:models",1,0],[":data:joint-account",":data:common",1,0],[":data:joint-account",":domain:joint-account",1,1],[":data:joint-account",":domain:account",1,0],[":data:joint-account",":domain:blockaid",1,0],[":data:joint-account",":domain:blockaid:models",1,0],[":data:joint-account",":domain:common",1,0],[":data:joint-account",":domain:core",1,0],[":data:joint-account",":domain:models",1,0],[":data:joint-account",":domain:transaction",1,0],[":data:joint-account",":domain:wallets",1,0],[":data:onboarding",":domain:onboarding",1,1],[":data:onboarding",":domain:models",1,0],[":data:promo",":data:common",1,0],[":data:promo",":domain:promo",1,1],[":data:promo",":domain:models",1,0],[":data:promo",":domain:promo:models",1,0],[":data:analytics",":domain:analytics",1,1],[":data:analytics",":domain:models",1,0],[":domain:demo",":domain:demo:models",1,1],[":domain:social-login",":domain:models",1,1],[":domain:transaction",":domain:account",1,1],[":domain:transaction",":domain:account:status",1,1],[":domain:transaction",":domain:card",1,1],[":domain:transaction",":domain:common",1,1],[":domain:transaction",":domain:demo",1,1],[":domain:transaction",":domain:dynamic-addresses",1,1],[":domain:transaction",":domain:networks",1,1],[":domain:transaction",":domain:notifications",1,1],[":domain:transaction",":domain:tokens",1,1],[":domain:transaction",":domain:wallet-manager",1,1],[":domain:transaction",":domain:core",1,0],[":domain:transaction",":domain:dynamic-addresses:models",1,0],[":domain:transaction",":domain:legacy",1,0],[":domain:transaction",":domain:demo:models",1,1],[":domain:transaction",":domain:models",1,1],[":domain:transaction",":domain:transaction:models",1,1],[":domain:transaction",":domain:wallets:models",1,1],[":domain:dynamic-addresses",":domain:models",1,1],[":domain:dynamic-addresses",":domain:wallet-manager",1,1],[":domain:dynamic-addresses",":domain:wallets",1,1],[":domain:dynamic-addresses",":domain:dynamic-addresses:models",1,1],[":domain:app-theme",":domain:app-theme:models",1,1],[":domain:yield-supply",":domain:account",1,1],[":domain:yield-supply",":domain:blockaid",1,1],[":domain:yield-supply",":domain:networks",1,1],[":domain:yield-supply",":domain:quotes",1,1],[":domain:yield-supply",":domain:transaction",1,1],[":domain:yield-supply",":domain:account:status",1,0],[":domain:yield-supply",":domain:app-currency:models",1,1],[":domain:yield-supply",":domain:models",1,1],[":domain:yield-supply",":domain:transaction:models",1,1],[":domain:yield-supply",":domain:yield-supply:models",1,1],[":domain:yield-supply",":domain:blockaid:models",1,0],[":domain:yield-supply",":domain:legacy",1,0],[":domain:yield-supply:models",":domain:models",1,1],[":domain:txhistory",":domain:express:models",1,1],[":domain:txhistory",":domain:models",1,1],[":domain:txhistory",":domain:txhistory:models",1,1],[":domain:txhistory",":domain:visa:models",1,1],[":domain:collectibles",":domain:core",1,1],[":domain:collectibles",":domain:models",2,1],[":domain:collectibles",":domain:collectibles:models",1,1],[":domain:push-notification-preferences",":domain:models",1,1],[":domain:card",":domain:core",1,0],[":domain:card",":domain:demo:models",1,0],[":domain:card",":domain:models",1,1],[":domain:card",":domain:visa:models",1,1],[":domain:nft",":domain:account",1,1],[":domain:nft",":domain:networks",1,1],[":domain:nft",":domain:quotes",1,1],[":domain:nft",":domain:tokens",1,1],[":domain:nft",":domain:wallets",1,1],[":domain:nft",":domain:core",1,0],[":domain:nft",":domain:models",1,1],[":domain:nft",":domain:nft:models",1,1],[":domain:nft:models",":domain:models",1,1],[":domain:quotes",":domain:core",1,1],[":domain:quotes",":domain:models",1,1],[":domain:wallet-manager",":domain:models",1,1],[":domain:wallet-manager",":domain:transaction:models",1,1],[":domain:wallet-manager",":domain:txhistory:models",1,1],[":domain:wallet-manager",":domain:wallet-manager:models",1,1],[":domain:wallet-manager:models",":domain:models",1,1],[":domain:express",":domain:core",1,1],[":domain:express",":domain:models",1,1],[":domain:express",":domain:express:models",1,1],[":domain:express:models",":domain:models",1,1],[":domain:express:models",":domain:onramp:models",1,1],[":domain:express:models",":domain:tokens:models",1,1],[":domain:qr-scanning",":domain:account",1,1],[":domain:qr-scanning",":domain:common",1,1],[":domain:qr-scanning",":domain:networks",1,1],[":domain:qr-scanning",":domain:models",1,1],[":domain:qr-scanning",":domain:qr-scanning:models",1,1],[":domain:qr-scanning",":domain:core",1,0],[":domain:qr-scanning:models",":domain:models",1,1],[":domain:blockaid",":domain:models",1,1],[":domain:blockaid",":domain:blockaid:models",1,1],[":domain:app-currency",":domain:app-currency:models",1,1],[":domain:swap",":domain:express:models",1,1],[":domain:swap",":domain:models",1,1],[":domain:swap",":domain:swap:models",1,1],[":domain:swap:models",":domain:express:models",1,1],[":domain:swap:models",":domain:models",1,1],[":domain:legacy",":domain:core",1,1],[":domain:legacy",":domain:models",1,1],[":domain:legacy",":domain:tokens:models",1,1],[":domain:legacy",":domain:transaction:models",1,1],[":domain:earn",":domain:account",1,1],[":domain:earn",":domain:common",1,1],[":domain:earn",":domain:models",1,1],[":domain:wallet-connect",":domain:core",1,0],[":domain:wallet-connect",":domain:transaction",1,0],[":domain:wallet-connect",":domain:blockaid:models",1,1],[":domain:wallet-connect",":domain:models",1,1],[":domain:wallet-connect",":domain:transaction:models",1,1],[":domain:wallet-connect",":domain:wallet-connect:models",1,1],[":domain:wallet-connect:models",":domain:blockaid:models",1,1],[":domain:wallet-connect:models",":domain:models",1,1],[":domain:wallet-connect:models",":domain:tokens:models",1,1],[":domain:wallet-connect:models",":domain:transaction:models",1,1],[":domain:models",":domain:core",1,1],[":domain:visa",":domain:account",1,1],[":domain:visa",":domain:common",1,1],[":domain:visa",":domain:core",1,1],[":domain:visa",":domain:app-currency:models",1,1],[":domain:visa",":domain:models",1,1],[":domain:visa",":domain:visa:models",1,1],[":domain:visa:models",":domain:models",1,0],[":domain:polymarket",":domain:core",1,1],[":domain:polymarket",":domain:models",1,1],[":domain:polymarket",":domain:common",1,1],[":domain:balance-hiding",":domain:balance-hiding:models",1,1],[":domain:feedback",":domain:models",1,1],[":domain:feedback",":domain:visa:models",1,0],[":domain:feedback",":domain:feedback:models",1,1],[":domain:feedback:models",":domain:models",1,1],[":domain:feedback:models",":domain:visa:models",1,1],[":domain:search",":domain:core",1,1],[":domain:search",":domain:models",1,1],[":domain:search",":domain:common",1,0],[":domain:search",":domain:account",1,0],[":domain:search",":domain:account:status",1,0],[":domain:onramp",":domain:settings",1,1],[":domain:onramp",":domain:core",1,1],[":domain:onramp",":domain:models",1,1],[":domain:onramp",":domain:onramp:models",1,1],[":domain:onramp",":domain:tokens:models",1,1],[":domain:onramp:models",":domain:models",1,1],[":domain:networks",":domain:core",1,1],[":domain:networks",":domain:models",1,1],[":domain:common",":domain:models",1,1],[":domain:stories",":domain:settings",1,1],[":domain:stories",":domain:stories:models",1,1],[":domain:news",":domain:models",1,1],[":domain:manage-tokens",":domain:account",1,1],[":domain:manage-tokens",":domain:joint-account",1,1],[":domain:manage-tokens",":domain:models",1,1],[":domain:manage-tokens",":domain:core",1,0],[":domain:manage-tokens",":domain:manage-tokens:models",1,1],[":domain:manage-tokens:models",":domain:models",1,1],[":domain:markets",":domain:common",1,1],[":domain:markets",":domain:quotes",1,1],[":domain:markets",":domain:card",1,0],[":domain:markets",":domain:app-currency:models",1,1],[":domain:markets",":domain:markets:models",1,1],[":domain:markets",":domain:models",1,1],[":domain:markets:models",":domain:models",1,1],[":domain:staking",":domain:core",1,1],[":domain:staking",":domain:models",1,1],[":domain:staking",":domain:wallet-manager",1,1],[":domain:staking",":domain:blockaid",1,0],[":domain:staking",":domain:blockaid:models",1,0],[":domain:staking",":domain:staking:models",1,1],[":domain:staking:models",":domain:models",1,1],[":domain:money-account",":domain:common",1,1],[":domain:money-account",":domain:core",1,1],[":domain:money-account",":domain:networks",1,1],[":domain:money-account",":domain:visa",1,1],[":domain:money-account",":domain:models",1,1],[":domain:money-account",":domain:money-account:models",1,1],[":domain:money-account",":domain:visa:models",1,1],[":domain:money-account",":domain:card",1,0],[":domain:money-account:models",":domain:models",1,1],[":domain:offramp",":domain:core",1,1],[":domain:offramp",":domain:models",1,1],[":domain:address-book",":domain:common",1,1],[":domain:address-book",":domain:transaction",1,1],[":domain:address-book",":domain:models",1,1],[":domain:assetsdiscovery",":domain:account:status",1,1],[":domain:assetsdiscovery",":domain:models",1,1],[":domain:marketing",":domain:marketing:models",1,0],[":domain:wallets",":domain:account",1,1],[":domain:wallets",":domain:card",1,1],[":domain:wallets",":domain:cloud-backup",1,1],[":domain:wallets",":domain:common",1,1],[":domain:wallets",":domain:hot-wallet",1,1],[":domain:wallets",":domain:social-login",1,1],[":domain:wallets",":domain:wallet-manager",1,1],[":domain:wallets",":domain:core",1,1],[":domain:wallets",":domain:joint-account",1,0],[":domain:wallets",":domain:notifications",1,0],[":domain:wallets",":domain:push-notification-preferences",1,0],[":domain:wallets",":domain:models",1,1],[":domain:wallets",":domain:wallets:models",1,1],[":domain:wallets",":domain:demo:models",1,0],[":domain:wallets",":domain:notifications:models",1,0],[":domain:wallets:models",":domain:models",1,1],[":domain:account",":domain:core",1,1],[":domain:account",":domain:models",1,1],[":domain:account:status",":domain:account",1,1],[":domain:account:status",":domain:card",1,1],[":domain:account:status",":domain:core",1,1],[":domain:account:status",":domain:common",1,1],[":domain:account:status",":domain:express",1,1],[":domain:account:status",":domain:quotes",1,1],[":domain:account:status",":domain:money-account",1,1],[":domain:account:status",":domain:models",1,1],[":domain:account:status",":domain:networks",1,1],[":domain:account:status",":domain:nft",1,1],[":domain:account:status",":domain:referral",1,1],[":domain:account:status",":domain:staking",1,1],[":domain:account:status",":domain:tokens",1,1],[":domain:account:status",":domain:tokens:models",1,1],[":domain:account:status",":domain:visa",1,1],[":domain:account:status",":domain:wallet-manager",1,1],[":domain:account:status",":domain:wallets",1,1],[":domain:account:status",":domain:yield-supply:models",1,1],[":domain:account:status",":domain:express:models",1,0],[":domain:account:status",":domain:joint-account",1,0],[":domain:account:status",":domain:collectibles",1,0],[":domain:account:status",":domain:polymarket",1,0],[":domain:account:status",":features:collectibles:api",1,0],[":domain:account:status",":features:polymarket:api",1,0],[":domain:account:status",":domain:wallets:models",1,0],[":domain:account:status",":domain:staking:models",1,0],[":domain:hot-wallet",":domain:models",1,1],[":domain:appsflyer",":domain:wallets:models",1,1],[":domain:tokens",":domain:common",1,1],[":domain:tokens",":domain:core",1,1],[":domain:tokens",":domain:express",1,1],[":domain:tokens",":domain:legacy",1,1],[":domain:tokens",":domain:money-account",1,1],[":domain:tokens",":domain:networks",1,1],[":domain:tokens",":domain:collectibles",1,1],[":domain:tokens",":domain:polymarket",1,1],[":domain:tokens",":domain:quotes",1,1],[":domain:tokens",":domain:staking",1,1],[":domain:tokens",":domain:stories",1,1],[":domain:tokens",":domain:visa",1,1],[":domain:tokens",":domain:wallet-manager",1,1],[":domain:tokens",":domain:card",1,0],[":domain:tokens",":domain:models",1,1],[":domain:tokens",":domain:tokens:models",1,1],[":domain:tokens",":domain:transaction:models",1,1],[":domain:tokens",":domain:yield-supply:models",1,1],[":domain:tokens",":domain:express:models",1,0],[":domain:tokens",":domain:stories:models",1,0],[":domain:tokens",":features:money-account:details:api",1,1],[":domain:tokens",":features:collectibles:api",1,1],[":domain:tokens",":features:polymarket:api",1,1],[":domain:tokens:models",":domain:models",1,1],[":domain:notifications",":domain:models",1,1],[":domain:notifications",":domain:notifications:models",1,1],[":domain:joint-account",":domain:core",1,1],[":domain:joint-account",":domain:models",1,1],[":domain:joint-account",":domain:account",1,0],[":domain:onboarding",":domain:models",1,1],[":domain:promo",":domain:models",1,1],[":domain:promo",":domain:promo:models",1,1],[":domain:analytics",":domain:models",1,1]]}
```
<!-- NAVGRAPH:moduleGraph:END -->

<!-- NAVGRAPH:screensGraph:BEGIN -->
```json
{"n":[["AccessCodeRecovery","AccessCodeRecovery","onboarding",1,0],["AccountDetails","AccountDetails","portfolio",1,0],["AddExistingWallet","AddExistingWallet","onboarding",2,0],["AddressBook","AddressBook","settings",3,1],["AppCurrencySelector","AppCurrencySelector","settings",1,0],["AppSettings","AppSettings","settings",2,0],["ArchivedAccountList","ArchivedAccountList","portfolio",1,0],["BuyCrypto","BuyCrypto","tokenaction",1,0],["CardSettings","CardSettings","settings",1,0],["ChooseManagedTokens","ChooseManagedTokens","portfolio",1,0],["Collectibles","Collectibles","misc",2,0],["CreateAccount","CreateAccount","portfolio",2,2],["CreateCloudBackup","CreateCloudBackup","misc",1,0],["CreateHardwareWallet","CreateHardwareWallet","onboarding",2,0],["CreateMobileWallet","CreateMobileWallet","onboarding",2,0],["CreateWalletBackup","CreateWalletBackup","onboarding",1,0],["CreateWalletSelection","CreateWalletSelection","onboarding",2,2],["CreateWalletStart","CreateWalletStart","onboarding",2,3],["CurrencyDetails","CurrencyDetails","portfolio",5,5],["Details","Details","settings",2,11],["DetailsSecurity","DetailsSecurity","settings",1,0],["Disclaimer","Disclaimer","entry",4,1],["Earn","Earn","markets",0,13],["EditAccount","EditAccount","portfolio",1,0],["ForgetWallet","ForgetWallet","settings",1,0],["HardwareUpgradeStories","HardwareUpgradeStories","misc",0,13],["Home","Home","entry",6,2],["Initial","Initial","entry",0,0],["JointAccountCreation","JointAccountCreation","misc",2,0],["JointAccountJoin","JointAccountJoin","misc",1,0],["JointAccountMembers","JointAccountMembers","misc",3,0],["Kyc","Kyc","tangempay",0,0],["ManageTokens","ManageTokens","portfolio",1,1],["Markets","Markets","markets",1,1],["MarketsTokenDetails","MarketsTokenDetails","markets",3,0],["MoneyAccountOnboarding","MoneyAccountOnboarding","misc",2,0],["NFT","NFT","wallet",1,1],["NFTSend","NFTSend","wallet",1,0],["News","News","markets",1,0],["NewsDetails","NewsDetails","markets",1,0],["Onboarding","Onboarding","onboarding",6,3],["Onramp","Onramp","tokenaction",4,2],["OnrampSuccess","OnrampSuccess","tokenaction",0,0],["Polymarket","Polymarket","misc",2,0],["PushNotification","PushNotification","wallet",2,0],["PushNotificationSettings","PushNotificationSettings","wallet",1,0],["QrScanning","QrScanning","misc",4,0],["ReferralProgram","ReferralProgram","settings",2,0],["ResetToFactory","ResetToFactory","settings",1,0],["SellCrypto","SellCrypto","tokenaction",1,0],["Send","Send","tokenaction",1,2],["SendEntryPoint","SendEntryPoint","tokenaction",2,0],["Staking","Staking","tokenaction",3,1],["Stories","Stories","wallet",3,0],["Survey","Survey","misc",0,0],["Swap","Swap","tokenaction",5,2],["TangemPayCampaign","TangemPayCampaign","misc",1,0],["TangemPayDetails","TangemPayDetails","tangempay",2,0],["TangemPayEligibility","TangemPayEligibility","misc",2,0],["TangemPayHotWalletOnboarding","TangemPayHotWalletOnboarding","tangempay",2,0],["TangemPayOnboarding","TangemPayOnboarding","tangempay",4,7],["UpdateAccessCode","UpdateAccessCode","onboarding",2,0],["UpgradeWallet","UpgradeWallet","onboarding",0,13],["Usedesk","Usedesk","misc",1,0],["ViewPhrase","ViewPhrase","onboarding",2,0],["Wallet","Wallet","wallet",11,16],["WalletActivation","WalletActivation","onboarding",1,0],["WalletBackup","WalletBackup","onboarding",1,0],["WalletConnectSessions","WalletConnectSessions","settings",1,1],["WalletHardwareBackup","WalletHardwareBackup","onboarding",1,0],["WalletSettings","WalletSettings","settings",1,14],["Welcome","Welcome","entry",1,3],["YieldSupplyEntry","YieldSupplyEntry","tokenaction",3,2],["AppShell","App shell","shell",0,16]],"e":[["HardwareUpgradeStories","Home",2,0],["HardwareUpgradeStories","Wallet",5,0],["HardwareUpgradeStories","TangemPayOnboarding",3,0],["HardwareUpgradeStories","TangemPayHotWalletOnboarding",4,0],["HardwareUpgradeStories","CreateWalletStart",2,0],["HardwareUpgradeStories","TangemPayCampaign",5,0],["HardwareUpgradeStories","AppSettings",1,0],["HardwareUpgradeStories","Welcome",2,0],["HardwareUpgradeStories","DetailsSecurity",1,0],["HardwareUpgradeStories","AccessCodeRecovery",1,0],["HardwareUpgradeStories","Onboarding",2,0],["HardwareUpgradeStories","ResetToFactory",1,0],["HardwareUpgradeStories","AppCurrencySelector",1,0],["Home","CreateWalletStart",5,0],["Home","Wallet",2,0],["CreateWalletStart","CreateMobileWallet",2,0],["CreateWalletStart","Wallet",3,0],["CreateWalletStart","Onboarding",1,0],["YieldSupplyEntry","Stories",3,0],["YieldSupplyEntry","CurrencyDetails",2,0],["AppShell","AddressBook",2,0],["AppShell","Swap",8,0],["WalletSettings","AccountDetails",1,0],["WalletSettings","ArchivedAccountList",1,0],["WalletSettings","ManageTokens",1,0],["WalletSettings","PushNotificationSettings",1,0],["WalletSettings","Home",1,0],["WalletSettings","Onboarding",1,0],["WalletSettings","ReferralProgram",1,0],["WalletSettings","UpdateAccessCode",1,0],["WalletSettings","WalletBackup",4,0],["WalletSettings","CardSettings",1,0],["WalletSettings","JointAccountCreation",1,0],["WalletSettings","CreateAccount",1,0],["WalletSettings","ForgetWallet",5,0],["WalletSettings","ViewPhrase",2,0],["Disclaimer","PushNotification",1,0],["NFT","NFTSend",1,0],["CurrencyDetails","Wallet",8,0],["CurrencyDetails","Onramp",3,0],["CurrencyDetails","SendEntryPoint",1,0],["CurrencyDetails","Swap",1,0],["CurrencyDetails","Staking",1,0],["AppShell","Wallet",25,0],["AppShell","PushNotification",1,0],["AppShell","AddExistingWallet",5,0],["AppShell","CurrencyDetails",4,0],["AppShell","Staking",2,0],["Swap","Stories",1,0],["Swap","CurrencyDetails",1,0],["Details","WalletConnectSessions",2,0],["Details","AddressBook",2,0],["Details","Wallet",1,0],["Details","Onboarding",2,0],["Details","TangemPayOnboarding",4,0],["Details","Usedesk",3,0],["Details","CreateWalletSelection",2,0],["Details","AppSettings",1,0],["Details","Disclaimer",1,0],["Details","MoneyAccountOnboarding",1,0],["Details","WalletSettings",2,0],["CreateWalletSelection","CreateMobileWallet",1,0],["CreateWalletSelection","CreateHardwareWallet",1,0],["Welcome","Home",1,0],["Welcome","Wallet",6,0],["Welcome","CreateWalletSelection",1,0],["AppShell","Stories",3,0],["Onramp","SellCrypto",1,0],["Onramp","BuyCrypto",1,0],["WalletConnectSessions","QrScanning",1,0],["Onboarding","Wallet",7,0],["Onboarding","Home",1,0],["Onboarding","Disclaimer",1,0],["AppShell","ChooseManagedTokens",1,0],["ManageTokens","Swap",1,0],["Markets","MarketsTokenDetails",1,0],["Earn","CreateAccount",4,0],["Earn","JointAccountCreation",2,0],["Earn","ReferralProgram",3,0],["Earn","Collectibles",6,0],["Earn","YieldSupplyEntry",2,0],["Earn","News",5,0],["Earn","NewsDetails",4,0],["Earn","Wallet",3,0],["Earn","TangemPayOnboarding",1,0],["Earn","Polymarket",1,0],["Earn","CurrencyDetails",11,0],["Earn","Markets",4,0],["Earn","MarketsTokenDetails",2,0],["Staking","Onramp",1,0],["AppShell","TangemPayEligibility",1,0],["AppShell","MoneyAccountOnboarding",1,0],["AddressBook","QrScanning",2,0],["Wallet","Details",1,0],["Wallet","JointAccountMembers",2,0],["Wallet","CurrencyDetails",2,0],["Wallet","Onboarding",1,0],["Wallet","Polymarket",1,0],["Wallet","Collectibles",1,0],["Wallet","NFT",1,0],["Wallet","TangemPayOnboarding",1,0],["Wallet","TangemPayDetails",1,0],["Wallet","YieldSupplyEntry",1,0],["Wallet","QrScanning",1,0],["Wallet","Send",1,0],["Wallet","Onramp",1,0],["Wallet","MarketsTokenDetails",1,0],["Wallet","Staking",1,0],["Wallet","Swap",1,0],["CreateAccount","JointAccountMembers",2,0],["CreateAccount","EditAccount",1,0],["UpgradeWallet","UpdateAccessCode",2,0],["UpgradeWallet","ViewPhrase",3,0],["UpgradeWallet","WalletActivation",2,0],["UpgradeWallet","WalletHardwareBackup",3,0],["UpgradeWallet","CreateCloudBackup",7,0],["UpgradeWallet","Disclaimer",1,0],["UpgradeWallet","AddExistingWallet",3,0],["UpgradeWallet","Wallet",4,0],["UpgradeWallet","CreateWalletBackup",1,0],["UpgradeWallet","CreateHardwareWallet",1,0],["UpgradeWallet","Onboarding",1,0],["UpgradeWallet","Details",1,0],["UpgradeWallet","Home",1,0],["Send","AddressBook",3,0],["Send","QrScanning",1,0],["AppShell","JointAccountMembers",2,0],["AppShell","JointAccountJoin",2,0],["TangemPayOnboarding","Wallet",36,0],["TangemPayOnboarding","Swap",9,0],["TangemPayOnboarding","Home",7,0],["TangemPayOnboarding","TangemPayHotWalletOnboarding",4,0],["TangemPayOnboarding","TangemPayEligibility",2,0],["TangemPayOnboarding","Disclaimer",4,0],["TangemPayOnboarding","TangemPayDetails",7,0],["AppShell","Onramp",1,0],["AppShell","SendEntryPoint",2,0],["AppShell","YieldSupplyEntry",1,0]]}
```
<!-- NAVGRAPH:screensGraph:END -->

<!-- NAVGRAPH:screensMeta:BEGIN -->
```json
{"AccessCodeRecovery":{"path":"/access_code_recovery","owner":"features:onboarding-v2","group":"onboarding","total":2,"refs":[["app",2]]},"AccountDetails":{"path":"/account_details/${account.accountId.value}","owner":"features:account","group":"portfolio","total":2,"refs":[["app",1],["features:wallet-settings",1]]},"AddExistingWallet":{"path":"/add_existing_wallet","owner":"features:onboarding-v2","group":"onboarding","total":20,"refs":[["features:introduction",10],["features:hot-wallet",9],["app",1]]},"AddressBook":{"path":"/address_book/${addressBookOpenMode.address}-${addressBookOpenMode.networkId}","owner":"features:address-book","group":"settings","total":10,"refs":[["features:send",3],["app",2],["features:txhistory",2],["features:details",2],["common:routing",1]]},"AppCurrencySelector":{"path":"/app_currency_selector","owner":"features:wallet-settings","group":"settings","total":2,"refs":[["app",2]]},"AppSettings":{"path":"/app_settings","owner":"features:details","group":"settings","total":9,"refs":[["app",7],["features:details",1],["features:feed",1]]},"ArchivedAccountList":{"path":"/archived_account/${userWalletId.stringValue}","owner":"features:account","group":"portfolio","total":2,"refs":[["app",1],["features:wallet-settings",1]]},"BuyCrypto":{"path":"/buy_crypto/${userWalletId.stringValue}","owner":"features:onramp","group":"tokenaction","total":3,"refs":[["app",1],["features:onramp",1],["features:wallet",1]]},"CardSettings":{"path":"/card_settings/${userWalletId.stringValue}","owner":"features:details","group":"settings","total":2,"refs":[["app",1],["features:wallet-settings",1]]},"ChooseManagedTokens":{"path":"/$source/choose_managed_tokens/$userWalletId/${initialCurrency.id.value}","owner":"features:manage-tokens","group":"portfolio","total":3,"refs":[["features:swap-v2",2],["app",1]]},"Collectibles":{"path":"/collectibles","owner":"app","group":"misc","total":9,"refs":[["features:feed",6],["app",1],["features:wallet",1],["common:routing",1]]},"CreateAccount":{"path":"/create_account/${userWalletId.stringValue}","owner":"features:account","group":"portfolio","total":6,"refs":[["features:feed",4],["app",1],["features:wallet-settings",1]]},"CreateCloudBackup":{"path":"/create_cloud_backup/${userWalletId.stringValue}","owner":"app","group":"misc","total":8,"refs":[["features:hot-wallet",7],["app",1]]},"CreateHardwareWallet":{"path":"/create_hardware_wallet","owner":"features:onboarding-v2","group":"onboarding","total":3,"refs":[["app",1],["features:create-wallet-selection",1],["features:hot-wallet",1]]},"CreateMobileWallet":{"path":"/create_mobile_wallet","owner":"features:hot-wallet","group":"onboarding","total":4,"refs":[["features:create-wallet-start",2],["app",1],["features:create-wallet-selection",1]]},"CreateWalletBackup":{"path":"/create_wallet_backup/${userWalletId.stringValue}","owner":"features:onboarding-v2","group":"onboarding","total":2,"refs":[["app",1],["features:hot-wallet",1]]},"CreateWalletSelection":{"path":"/create_wallet_selection","owner":"features:create-wallet-selection","group":"onboarding","total":4,"refs":[["features:details",2],["app",1],["features:welcome",1]]},"CreateWalletStart":{"path":"/create_wallet_start","owner":"features:create-wallet-start","group":"onboarding","total":27,"refs":[["app",17],["features:home",10]]},"CurrencyDetails":{"path":"/currency_details/${userWalletId.stringValue}/${currency.id.value}","owner":"features:tokendetails","group":"portfolio","total":40,"refs":[["features:feed",11],["features:tokendetails",8],["features:yield-supply",5],["features:for-you",3],["features:swap",3],["app",2],["features:wallet",2],["common:routing",2],["features:txhistory",1],["features:common-features",1],["features:staking",1],["features:send",1]]},"Details":{"path":"/details/${userWalletId.stringValue}","owner":"features:details","group":"settings","total":5,"refs":[["app",2],["features:wallet",1],["features:hot-wallet",1],["features:joint-account",1]]},"DetailsSecurity":{"path":"/details/security","owner":"features:details","group":"settings","total":2,"refs":[["app",2]]},"Disclaimer":{"path":"/disclaimer${if (isTosAccepted) \"/tos_accepted\" else \"\"}","owner":"features:disclaimer","group":"entry","total":13,"refs":[["app",5],["features:tangempay",4],["features:details",1],["features:walletconnect",1],["features:onboarding-v2",1],["features:hot-wallet",1]]},"Earn":{"path":"/earn","owner":"features:feed","group":"markets","total":19,"refs":[["features:feed",18],["app",1]]},"EditAccount":{"path":"/edit_account/${account.accountId.value}","owner":"features:account","group":"portfolio","total":2,"refs":[["app",1],["features:account",1]]},"ForgetWallet":{"path":"/forget_wallet/${userWalletId.stringValue}","owner":"features:wallet-settings","group":"settings","total":6,"refs":[["features:wallet-settings",5],["app",1]]},"HardwareUpgradeStories":{"path":"/hardware_upgrade_stories/${userWalletId.stringValue}","owner":"app","group":"misc","total":12,"refs":[["features:hot-wallet",10],["app",1],["features:wallet-settings",1]]},"Home":{"path":"/home","owner":"features:home","group":"entry","total":61,"refs":[["app",28],["features:introduction",17],["features:tangempay",7],["features:wallet-settings",2],["features:welcome",2],["features:hot-wallet",2],["features:disclaimer",1],["features:onboarding-v2",1],["features:push-notifications",1]]},"Initial":{"path":"/initial","owner":"app","group":"entry","total":10,"refs":[["app",9],["features:walletconnect",1]]},"JointAccountCreation":{"path":"/joint_account_creation/${userWalletId.stringValue}","owner":"app","group":"misc","total":4,"refs":[["features:feed",2],["app",1],["features:wallet-settings",1]]},"JointAccountJoin":{"path":"/joint_account_join/$inviteId","owner":"app","group":"misc","total":6,"refs":[["features:joint-account",5],["app",1]]},"JointAccountMembers":{"path":"/joint_account_members/${userWalletId.stringValue}","owner":"app","group":"misc","total":7,"refs":[["features:joint-account",3],["features:account",2],["app",1],["features:wallet",1]]},"Kyc":{"path":"/kyc","owner":"features:kyc","group":"tangempay","total":12,"refs":[["features:tangempay",9],["app",3]]},"ManageTokens":{"path":"${source.name.lowercase()}/manage_tokens/${accountId?.value}","owner":"features:manage-tokens","group":"portfolio","total":16,"refs":[["app",5],["features:wallet",5],["features:account",4],["features:wallet-settings",2]]},"Markets":{"path":"/markets","owner":"features:markets","group":"markets","total":6,"refs":[["features:feed",4],["app",2]]},"MarketsTokenDetails":{"path":"/markets_token_details/${token.id}/$shouldShowPortfolio","owner":"features:markets","group":"markets","total":7,"refs":[["features:markets",2],["features:feed",2],["features:wallet",2],["app",1]]},"MoneyAccountOnboarding":{"path":"/money_account_onboarding/$mode","owner":"app","group":"misc","total":8,"refs":[["app",4],["features:details",2],["features:money-account",2]]},"NFT":{"path":"/nft/${userWalletId.stringValue}","owner":"features:nft","group":"wallet","total":2,"refs":[["app",1],["features:wallet",1]]},"NFTSend":{"path":"/send/nft/${userWalletId.stringValue}/$nftCollectionName/${nftAsset.id}","owner":"features:nft","group":"wallet","total":2,"refs":[["app",1],["features:nft",1]]},"News":{"path":"/news","owner":"features:feed","group":"markets","total":6,"refs":[["features:feed",5],["app",1]]},"NewsDetails":{"path":"/news_details/$newsId","owner":"features:feed","group":"markets","total":5,"refs":[["features:feed",4],["app",1]]},"Onboarding":{"path":"/onboarding_v2/$mode","owner":"features:onboarding-v2","group":"onboarding","total":40,"refs":[["app",23],["features:create-wallet-start",4],["features:details",4],["features:wallet",3],["features:wallet-settings",2],["features:welcome",2],["features:hot-wallet",2]]},"Onramp":{"path":"/onramp/${userWalletId.stringValue}/${currency.symbol}","owner":"features:onramp","group":"tokenaction","total":11,"refs":[["features:tokendetails",3],["features:onramp",3],["app",1],["features:staking",1],["features:wallet",1],["common:ui-markets",1],["common:routing",1]]},"OnrampSuccess":{"path":"/onramp/success/$txId","owner":"features:onramp","group":"tokenaction","total":4,"refs":[["features:onramp",3],["app",1]]},"Polymarket":{"path":"/polymarket","owner":"app","group":"misc","total":3,"refs":[["app",1],["features:feed",1],["features:wallet",1]]},"PushNotification":{"path":"/push_notification","owner":"features:push-notifications","group":"wallet","total":42,"refs":[["features:push-notifications",24],["app",7],["features:introduction",4],["features:disclaimer",2],["features:onboarding-v2",2],["features:hot-wallet",2],["features:wallet",1]]},"PushNotificationSettings":{"path":"/push_notification_settings/${userWalletId.stringValue}","owner":"features:push-notification-settings","group":"wallet","total":2,"refs":[["app",1],["features:wallet-settings",1]]},"QrScanning":{"path":"/$source/qr_scanning${source.path}","owner":"features:qr-scanning","group":"misc","total":16,"refs":[["app",6],["features:address-book",4],["features:walletconnect",2],["features:wallet",2],["features:send",2]]},"ReferralProgram":{"path":"/referral_program","owner":"features:referral","group":"settings","total":6,"refs":[["features:feed",3],["app",1],["features:referral",1],["features:wallet-settings",1]]},"ResetToFactory":{"path":"/reset_to_factory","owner":"features:details","group":"settings","total":2,"refs":[["app",2]]},"SellCrypto":{"path":"/sell_crypto/${userWalletId.stringValue}","owner":"features:onramp","group":"tokenaction","total":3,"refs":[["app",1],["features:onramp",1],["features:wallet",1]]},"Send":{"path":"/send/${userWalletId.stringValue}/${currency.id.value}?","owner":"features:send","group":"tokenaction","total":13,"refs":[["features:wallet",7],["app",4],["features:send",1],["common:routing",1]]},"SendEntryPoint":{"path":"/send_entry_point/${userWalletId.stringValue}/${currency.id.value}?","owner":"features:send","group":"tokenaction","total":5,"refs":[["features:tokendetails",2],["common:ui-markets",2],["app",1]]},"Staking":{"path":"/staking/${userWalletId.stringValue}/${cryptoCurrency.id.value}/${integrationId.value}","owner":"features:staking","group":"tokenaction","total":8,"refs":[["features:for-you",3],["app",1],["features:tokendetails",1],["features:staking",1],["features:wallet",1],["common:ui-markets",1]]},"Stories":{"path":"/stories$storyId","owner":"features:stories","group":"wallet","total":17,"refs":[["app",8],["features:yield-supply",3],["features:polymarket",3],["features:swap",1],["features:walletconnect",1],["features:wallet",1]]},"Survey":{"path":"/survey","owner":"features:survey","group":"misc","total":4,"refs":[["features:survey",3],["app",1]]},"Swap":{"path":"/swap","owner":"features:swap","group":"tokenaction","total":109,"refs":[["features:swap",35],["features:tangempay",20],["features:polymarket",19],["common:ui-markets",15],["app",4],["features:collectibles",4],["features:tokendetails",4],["features:for-you",2],["features:manage-tokens",2],["features:wallet",2],["common:routing",2]]},"TangemPayCampaign":{"path":"/tangem_pay_campaign","owner":"app","group":"misc","total":9,"refs":[["app",9]]},"TangemPayDetails":{"path":"/tangem_pay_details/${status.account}","owner":"features:tangempay","group":"tangempay","total":16,"refs":[["features:tangempay",13],["features:wallet",2],["app",1]]},"TangemPayEligibility":{"path":"/tangem_pay_eligibility","owner":"app","group":"misc","total":7,"refs":[["features:tangempay",5],["app",1],["features:money-account",1]]},"TangemPayHotWalletOnboarding":{"path":"/tangem_pay_hot_wallet_onboarding","owner":"features:tangempay","group":"tangempay","total":13,"refs":[["app",9],["features:tangempay",4]]},"TangemPayOnboarding":{"path":"/tangem_pay_onboarding/$mode","owner":"features:tangempay","group":"tangempay","total":41,"refs":[["app",18],["features:details",8],["features:wallet",7],["features:tangempay",6],["features:feed",2]]},"UpdateAccessCode":{"path":"/update_access_code/${userWalletId.stringValue}","owner":"features:onboarding-v2","group":"onboarding","total":4,"refs":[["features:hot-wallet",2],["app",1],["features:wallet-settings",1]]},"UpgradeWallet":{"path":"/upgrade_wallet/${userWalletId.stringValue}","owner":"features:hot-wallet","group":"onboarding","total":4,"refs":[["features:hot-wallet",3],["app",1]]},"Usedesk":{"path":"/usedesk/${walletMetaInfo.userWalletId}","owner":"features:usedesk","group":"misc","total":4,"refs":[["features:details",3],["app",1]]},"ViewPhrase":{"path":"/view_seed_phrase/${userWalletId.stringValue}","owner":"features:onboarding-v2","group":"onboarding","total":6,"refs":[["features:hot-wallet",3],["features:wallet-settings",2],["app",1]]},"Wallet":{"path":"/wallet","owner":"features:wallet","group":"wallet","total":157,"refs":[["app",49],["features:tangempay",36],["features:joint-account",15],["features:introduction",13],["features:tokendetails",7],["features:welcome",7],["features:onboarding-v2",7],["features:feed",5],["features:create-wallet-start",4],["features:swap",4],["features:hot-wallet",4],["features:home",2],["features:details",2],["features:wallet",1],["common:routing",1]]},"WalletActivation":{"path":"/wallet_activation/${userWalletId.stringValue}","owner":"features:tangempay","group":"onboarding","total":3,"refs":[["features:hot-wallet",2],["app",1]]},"WalletBackup":{"path":"/wallet_backup/${userWalletId.stringValue}/$isColdWalletOptionShown","owner":"features:onboarding-v2","group":"onboarding","total":6,"refs":[["features:wallet-settings",4],["app",1],["features:wallet",1]]},"WalletConnectSessions":{"path":"/wallet_connect_sessions","owner":"features:walletconnect","group":"settings","total":3,"refs":[["features:details",2],["app",1]]},"WalletHardwareBackup":{"path":"/wallet_hardware_backup/${userWalletId.stringValue}","owner":"features:onboarding-v2","group":"onboarding","total":6,"refs":[["features:hot-wallet",5],["app",1]]},"WalletSettings":{"path":"/wallet_settings/${userWalletId.stringValue}","owner":"features:wallet-settings","group":"settings","total":4,"refs":[["features:details",2],["app",1],["features:hot-wallet",1]]},"Welcome":{"path":"/welcome","owner":"features:welcome","group":"entry","total":9,"refs":[["app",8],["features:walletconnect",1]]},"YieldSupplyEntry":{"path":"/yield_supply_entry/${userWalletId.stringValue}/${cryptoCurrency.symbol}","owner":"features:yield-supply","group":"tokenaction","total":15,"refs":[["features:yield-supply",5],["features:feed",4],["features:for-you",3],["app",1],["features:wallet",1],["common:ui-markets",1]]}}
```
<!-- NAVGRAPH:screensMeta:END -->

<!-- NAVGRAPH:END -->