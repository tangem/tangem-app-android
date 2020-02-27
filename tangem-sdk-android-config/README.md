
[![Release](https://jitpack.io/v/Tangem/tangem-sdk-android.svg)]

# Welcome to Tangem

The Tangem card is a self-custodial hardware wallet for blockchain assets. The main functions of Tangem cards are to securely create and store a private key from a blockchain wallet and sign blockchain transactions. The Tangem card does not allow users to import/export, backup/restore private keys, thereby guaranteeing that the wallet is unique and unclonable. 

- [Getting Started](#getting-started)
	- [Requirements](#requirements)
	- [Installation](#installation)
- [Usage](#usage)
	- [Initialization](#initialization)
	- [Card interaction](#card-interaction)
		- [Scan card](#scan-card)
		- [Sign](#sign)
- [Customization](#customization)
	- [UI](#ui)
	- [Tasks](#tasks)
	- [Localization](#localization)


## Getting Started

### Requirements
Android with minimal SDK version of 21 and a device with NFC support

### Installation

1) Add Tangem library to the project:

Add to a project build.gradle file:

```gradle
allprojects {
    repositories {
        maven { url "https://jitpack.io" }
    }
}
```

And add Tangem library to the dependencies (in an app or module build.gradle file):

```gradle 
dependencies {
    implementation "com.github.tangem.tangem-sdk-android:tangem-core:$latestVersionOfCore"
    implementation "com.github.tangem.tangem-sdk-android:tangem-sdk:$latestVersionOfSdk"
}
```
Tangem Core is a JVM library (without Android dependencies) that provides core functionality of interacting with Tangem cards.
Tangem Sdk is an Android library that implements NFC interaction between Android devices and Tangem cards and graphical interface for this  interaction. 

2) Add res file ‘tech_filter’ with the following:

```xml
<resources>
   <tech-list>
       <tech>android.nfc.tech.IsoDep</tech>
       <tech>android.nfc.tech.Ndef</tech>
       <tech>android.nfc.tech.NfcV</tech>
   </tech-list>
</resources>
```

3) Add to Manifest:
```xml
<intent-filter>
               <action android:name=“android.nfc.action.TECH_DISCOVERED” />
</intent-filter>
<meta-data
               android:name=“android.nfc.action.TECH_DISCOVERED”
               android:resource=“@xml/tech_filter” />
```

## Usage

Tangem SDK is a self-sufficient solution that implements a card abstraction model, methods of interaction with the card and interactions with the user via UI.

### Initialization
To get started, you need to create an instance of the `CardManager` class. It provides the simple way of interacting with the card. 
Our default implementation of `CardManager` requires `NfcReader` and `DefaultCardManagerDelegate`.


```kotlin
private val nfcManager = NfcManager()
private val cardManagerDelegate: DefaultCardManagerDelegate = DefaultCardManagerDelegate(nfcManager.reader)
private val cardManager = CardManager(nfcManager.reader, cardManagerDelegate)
```

NfcManager requires a reference to activity in order to use Android API for interacting with NFC.  DefaultCardManagerDelegate requires a reference to activity in order to render views. 

```kotlin
nfcManager.setCurrentActivity(this)
cardManagerDelegate.activity = this
```

```kotlin
lifecycle.addObserver(NfcLifecycleObserver(nfcManager))
```

Default implementation of `NfcManager`, `CardReader` and `CardManagerDelegate` allows you to start using CardManager in your application without any additional setup.

You can also provide your implementations of `CardReader` and `CardManagerDelegate`.
You can read more about this in [Customization](#сustomization).

### Tasks

#### Scan card 
To start using any card, you first need to read it using the `scanCard()` method. This method launches an NFC session, and once it’s connected with the card, it obtains the card data. Optionally, if the card contains a wallet (private and public key pair), it proves that the wallet owns a private key that corresponds to a public one.

Example:

```kotlin
cardManager.scanCard { taskEvent ->
    when (taskEvent) {
        is TaskEvent.Event<ScanEvent> -> {
            when (taskEvent.data) {
                is ScanEvent.OnReadEvent -> {
                    // Handle returned card data
                    cardId = (taskEvent.data as ScanEvent.OnReadEvent).card.cardId
                    // Switch to UI thread to show results in UI
                    runOnUiThread {}
                }
                is ScanEvent.OnVerifyEvent -> {
                    //Handle card verification
                    val isGenuine = (taskEvent.data as ScanEvent.OnVerifyEvent).isGenuine
                }
            }
    }
        is TaskEvent.Completion<ScanEvent> -> {
            if (taskEvent.error != null) {
                if (taskEvent.error is TaskError.UserCancelledError) {
                    // Handle case when user cancelled manually
                }
                // Handle other errors
            }
            // Handle completion
        }
    }
}

```

Communication with the card is an asynchronous operation. In order to get a result for the method, you need to subscribe to the task callback. In order to render the callback results on UI, you need to switch to the main thread.

Every task can invoke callback several times with different events:

`Completion<T>(val error: TaskError? = null)` – this event is triggered only once when task is completely finished. It means that it's the final callback. If error is not nil, then something went wrong during the operation.

`Event<T>(val data: T)` –  this event is triggered when one of operations inside the task is completed. 

**Possible events of the Scan card task:**

`OnReadEvent(val result: Card)` – this event is triggered after the card has been successfully read. In addition, the obtained card object is contained inside the enum. At this stage, the authenticity of the card is ***NOT*** verified.

`OnVerifyEvent(val isGenuine: Boolean)` – this event is triggered when the card’s authenticity has been verified. If the card is authentic, isGenuine will be set to true, otherwise, it will be set to false.

#### Sign
This method allows you to sign one or multiple hashes. Simultaneous signing of array of hashes in a single SIGN command is required to support Bitcoin-type multi-input blockchains (UTXO). The SIGN command will return a corresponding array of signatures.

```kotlin
cardManager.sign(
        hashes = arrayOf(hash1, hash2),
        cardId = card.cardId) { taskEvent ->
    when (taskEvent) {
        is TaskEvent.Event -> {
        // Handle sign response data
        val signResponse = taskEvent.data
        }
        is TaskEvent.Completion<ScanEvent> -> {
            if (taskEvent.error != null) {
                if (taskEvent.error is TaskError.UserCancelledError) {
                    // Handle case when user cancelled manually
                }
                // Handle other errors
            }
            // Handle completion
        }
    }
}
```

## Customization
### UI
If the interaction with user is required, the SDK performs the entire cycle of this interaction. In order to change the appearance or behavior of the user UI, you can provide you own implementation of the `CardManagerDelegate` inteface. After this, initialize the `CardManager` class with your delegate class.

```kolin
val myCardManagerDelegate = MyCardManagerDelegate()
val cardManager = CardManager(cardManagerDelegate = myCardManagerDelegate)
```

> If you pass null instead of `cardManagerDelegate`, the SDK won’t be able to process errors that require user intervention and return them to `.failure(let error)`.

### Tasks
`CardManager` only covers general tasks. If you want to trigger card commands in a certain order, you need to create your own task.

To do this, you need to create a subclass of the `Task` class, and override the `onRun(..)` method.

Then call the `runTask(..)` method of the `CardManager` class with you task.

```kotlin
val task = YourTask()
cardManager.runTask(task) { taskEvent ->
    // Handle your events
}
```
> For example, you want to read the card and immediately sign a transaction on it. In such a case, you need to inherit from the `Task` class and override the `onRun(..)` method, in which you implement the required behavior.

It’s possible to run just one command without the need to create a separate task by using the `runCommand(..)` method.
> For example, if you need to read the card details, but don’t need to check the authenticity. 
```kotlin
// Create command
val readCommand = ReadCardCommand()
// Run command with the callback
cardManager.runCommand(readCommand) { taskEvent ->
    when (taskEvent) {
        is TaskEvent.Event -> {
            // Handle returned card data
            val card: Card = taskEvent.data
        }
        is TaskEvent.Completion -> {
            if (taskEvent.error != null) {
                if (taskEvent.error is TaskError.UserCancelledError) {
                    // Handle case when user cancelled manually
                }
                // Handle other errors
            }
            // Handle completion
        }
    }
}
```