# **How to build application**

# Project Build Documentation

## 1. Required Tools
To build the project, you will need to have **Android Studio** version 2024.1.1 or higher. The embedded version of **Java** should be version **17** or higher.

### Steps:
1. Install **Android Studio**
   - You can download the latest version from the [official Android Studio website](https://developer.android.com/studio).
2. Setup JDK
   - In the Android Studio go to *Android Studio* option in top menu bar -> *Preferences* -> *Build, Execution, 
      Deployment* -> *Build Tools* -> *Gradle*. 
   - In *Gradle JDK* field check if you have JDK 17. If not, choose *Download JDK* in the drop down menu and choose **Amazon Coretto 17.0.14**
   (or any other version starts with 17). 
   - Apply changes, press **Ok** and restart the Android Studio.

## 2. Required Dependencies
To be able to use key dependencies, you need to be able to pull them from **GitHub Package Registry**.

### Steps:
1. Go to [GitHub Settings → Developer settings → Personal access tokens](https://github.com/settings/tokens).
2. Click *Generate new token* -> *Generate new token (classic)* -> type any name for the token -> don’t change anything, just check the box `read:packages` -> press “Generate token” -> copy the generated token
3. Open the `local.properties` file in the root project directory (or you can press **Command+Shift+O** and type “local.
   properties” to find the file) and add the following lines:
  ```properties
  gpr.user=YOUR_GITHUB_USERNAME
  gpr.key=YOUR_GENERATED_TOKEN
  ```
   - `YOUR_GITHUB_USERNAME` — your GitHub username
   - `YOUR_GENERATED_TOKEN` — the token you generated in the previous step.

## 3. Sync Gradle and Build the Project
Once all the previous steps are completed, you need to sync your Gradle files and run the build.

### Steps:
1. Checkout the **master** branch.
2. Select **Build Variant** for the project as **googleExternal**.
3. In Android Studio, go to the **File** menu and select **Sync Project with Gradle Files**.
4. Once the sync is complete, you can build the project by selecting **Build → Make Project** or pressing **Ctrl+F9** (Windows/Linux) or **Cmd+F9** (macOS) or **Build → Build App Bundle(s) → Build APK(s)**.
5. Ensure there are no errors in the **Build Output** and that the project is successfully built.

**Alternative:**  
Instead of steps 2–5, you can build the project from the command line:
`./gradlew assembleGoogleExternal`