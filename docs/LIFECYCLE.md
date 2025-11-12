# Android Application Lifecycle Documentation

## Overview

This document describes the lifecycle management of the ft_hangouts_42 Android application, including Activity lifecycle callbacks, state preservation, and background/foreground transitions.

## Activity Lifecycle

The application uses a single `MainActivity` that extends `ComponentActivity` and implements Jetpack Compose for UI rendering. The lifecycle is managed through standard Android Activity callbacks.

### Lifecycle Callbacks

#### onCreate()

**Location**: `MainActivity.onCreate()`

**Purpose**: Initializes the application when the Activity is first created.

**Key Operations**:
1. **Permission Request**: Requests SMS receiving permission via `requestSmsPermission()`
2. **Repository Initialization**: 
   - Creates `ContactRepository` instance
   - Creates `MessageRepository` instance
3. **Permission Launcher Setup**: Registers `ActivityResultLauncher` for phone call permissions
4. **Compose UI Setup**: Sets up the Compose UI with:
   - Language management and localization
   - Image picker launchers
   - Permission handling
   - Phone call functionality
   - Main screen composition

**State Initialization**:
- Loads saved language preference
- Initializes image selection state
- Sets up permission launchers
- Configures locale-aware context

**Code Flow**:
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    requestSmsPermission()
    contactRepo = ContactRepository(this)
    messageRepo = MessageRepository(this)
    
    callPermissionLauncher = registerForActivityResult(...)
    
    setContent {
        // Compose UI setup
    }
}
```

#### onStart()

**Location**: `MainActivity.onStart()`

**Purpose**: Called when the Activity becomes visible to the user. Handles background tracking and first launch detection.

**Key Operations**:
1. **SharedPreferences Access**: 
   - Accesses `PREFS_NAME` for background tracking
   - Accesses `APP_PREFS_NAME` for app-specific settings
2. **First Launch Detection**: Checks if this is the first app start
3. **Background Time Display**: Shows toast with last background timestamp if:
   - App was previously stopped (`wasStopped == true`)
   - Not the first start
   - Background timestamp exists
   - Language was not changed
4. **State Reset**: Clears flags after displaying toast

**State Management**:
- `KEY_WAS_STOPPED`: Boolean flag indicating if app was stopped
- `KEY_LAST_BACKGROUND_TS`: Timestamp of last background transition
- `KEY_IS_FIRST_START`: Boolean flag for first launch detection
- `KEY_LANGUAGE_CHANGED`: Boolean flag for language change detection

**Code Flow**:
```kotlin
override fun onStart() {
    super.onStart()
    val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val appPrefs = getSharedPreferences(APP_PREFS_NAME, Context.MODE_PRIVATE)
    
    // Check first start
    val isFirstStart = !appPrefs.contains(KEY_IS_FIRST_START)
    
    // Show background time if applicable
    if (wasStopped && !isFirstStart && lastBackgroundTime != 0L && !wasLanguageChanged) {
        // Display toast
    }
    
    // Reset flags
}
```

#### onStop()

**Location**: `MainActivity.onStop()`

**Purpose**: Called when the Activity is no longer visible to the user. Saves background transition timestamp.

**Key Operations**:
1. **Configuration Check**: Uses `isChangingConfigurations` to distinguish between:
   - Configuration changes (screen rotation) - no save needed
   - Actual background transition - save timestamp
2. **Timestamp Storage**: Saves current time as background timestamp
3. **State Flag**: Sets `wasStopped` flag to `true`

**Important Note**: The `isChangingConfigurations` check prevents saving state during configuration changes (like screen rotation), which would incorrectly trigger the background toast.

**Code Flow**:
```kotlin
override fun onStop() {
    super.onStop()
    if (!isChangingConfigurations) {
        val backgroundTime = System.currentTimeMillis()
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putLong(KEY_LAST_BACKGROUND_TS, backgroundTime)
            .putBoolean(KEY_WAS_STOPPED, true)
            .apply()
    }
}
```

## State Preservation

### SharedPreferences Usage

The application uses two separate SharedPreferences files for different purposes:

#### 1. `prefs` (PREFS_NAME)
**Purpose**: Activity lifecycle state tracking

**Keys**:
- `KEY_LAST_BACKGROUND_TS`: Long - Timestamp when app went to background
- `KEY_WAS_STOPPED`: Boolean - Flag indicating if app was stopped

**Usage**: Tracks when the app transitions to background to display this information when returning to foreground.

#### 2. `app_prefs` (APP_PREFS_NAME)
**Purpose**: Application-wide settings and preferences

**Keys**:
- `KEY_IS_FIRST_START`: Boolean - First launch detection
- `KEY_LANGUAGE_CHANGED`: Boolean - Language change flag
- `top_bar_color`: Int - Top bar color preference (ARGB format)
- `language`: String - Saved language preference ("en" or "fr")

**Usage**: Persists user preferences and app configuration across app restarts.

### Compose State Preservation

#### rememberSaveable
Used for state that should survive configuration changes:
- `showEdit`: Boolean - Edit screen visibility
- `contactToEdit`: ContactEntity? - Contact being edited
- `showConversation`: Boolean - Conversation screen visibility
- `selectedContact`: ContactEntity? - Selected contact for conversation
- Form fields in `ContactEditScreen`

#### remember
Used for state that doesn't need to survive configuration changes:
- `contacts`: List<ContactEntity> - Contact list (refreshed from DB)
- `expandedContactId`: Long? - Currently expanded contact menu
- `topBarColor`: Color - Top bar color (synced with SharedPreferences)
- `cardCoordinates`: Offset? - Menu positioning coordinates

## Background/Foreground Transitions

### Background Transition Flow

1. User leaves app (home button, recent apps, etc.)
2. `onStop()` is called
3. `isChangingConfigurations` is checked
4. If false (actual background):
   - Current timestamp is saved to `KEY_LAST_BACKGROUND_TS`
   - `KEY_WAS_STOPPED` is set to `true`
5. Activity is stopped

### Foreground Transition Flow

1. User returns to app
2. `onStart()` is called
3. SharedPreferences are read
4. Conditions are checked:
   - `wasStopped == true`
   - `!isFirstStart`
   - `lastBackgroundTime != 0L`
   - `!wasLanguageChanged`
5. If all conditions met:
   - Toast is displayed with formatted timestamp
   - `wasStopped` flag is reset
6. Activity becomes visible

### Language Change Handling

When language is changed:
1. `onLanguageChange` callback is triggered
2. Language is saved to SharedPreferences
3. `KEY_LANGUAGE_CHANGED` flag is set to `true`
4. Activity is recreated via `recreate()`
5. On next `onStart()`, the flag prevents background toast display
6. Flag is reset after handling

## Permission Lifecycle

### SMS Permission

**Request Location**: `onCreate()` → `requestSmsPermission()`

**Method**: Uses deprecated `ActivityCompat.requestPermissions()` (for compatibility)

**Callback**: `onRequestPermissionsResult()`

**Purpose**: Required for `SMSReceiver` to receive incoming SMS messages

### Phone Call Permission

**Request Location**: Dynamic permission request via `ActivityResultLauncher`

**Method**: Modern `ActivityResultContracts.RequestPermission()`

**Trigger**: When user attempts to make a phone call

**Purpose**: Required to initiate phone calls via `Intent.ACTION_CALL`

### Storage Permission

**Request Location**: Dynamic permission request via `ActivityResultLauncher`

**Method**: Modern `ActivityResultContracts.RequestPermission()`

**Trigger**: When user attempts to select an avatar image

**Purpose**: Required to read images from external storage

## BroadcastReceiver Lifecycle

### SMSReceiver

**Type**: `BroadcastReceiver` registered in AndroidManifest.xml

**Lifecycle**: System-managed, independent of Activity lifecycle

**Trigger**: System broadcasts `android.provider.Telephony.SMS_RECEIVED`

**Operations**:
1. Receives SMS intent
2. Extracts message data (address, body)
3. Creates coroutine scope for database operations
4. Checks if contact exists by phone number
5. Creates contact if not found (auto-creation)
6. Saves message to database

**Important**: Receiver operates independently and can receive SMS even when app is in background or closed.

## Compose Lifecycle Effects

### LaunchedEffect(Unit)

**Location**: `MainScreen` composable

**Purpose**: Initializes and maintains periodic contact list updates

**Behavior**:
- Runs once when composable is first created
- Starts infinite loop with 2-second delay
- Fetches contacts from database every 2 seconds
- Updates UI state with new contact list

**Code**:
```kotlin
LaunchedEffect(Unit) {
    contacts = contactRepo.getAllContacts()
    while (true) {
        kotlinx.coroutines.delay(2000)
        contacts = contactRepo.getAllContacts()
    }
}
```

### LaunchedEffect with Keys

**Examples**:
- `LaunchedEffect(topBarColor)`: Saves color preference when changed
- `LaunchedEffect(currentLang)`: Resets expanded menu when language changes
- `LaunchedEffect(pickedImageUri)`: Processes selected image URI

## Configuration Changes

### Screen Rotation Handling

When device rotates:
1. `onStop()` is called with `isChangingConfigurations == true`
2. No state is saved (prevents false background detection)
3. Activity is destroyed and recreated
4. `onCreate()` is called with saved instance state
5. Compose state with `rememberSaveable` is automatically restored
6. `onStart()` is called, but background toast is not shown

### State Restoration

**Automatic Restoration**:
- Compose state with `rememberSaveable` is automatically restored
- SharedPreferences persist across configuration changes

**Manual Restoration**:
- Contact list is reloaded from database
- UI state is recomposed with saved values

## Best Practices Implemented

1. **Configuration Change Detection**: Uses `isChangingConfigurations` to avoid false background detection
2. **State Separation**: Uses separate SharedPreferences for different concerns
3. **Modern Permission API**: Uses `ActivityResultLauncher` for runtime permissions
4. **State Preservation**: Uses `rememberSaveable` for important UI state
5. **Background Tracking**: Tracks actual background transitions, not configuration changes
6. **First Launch Detection**: Prevents showing background toast on first start
7. **Language Change Handling**: Prevents background toast when language changes

## Lifecycle Diagram

```
App Launch
    ↓
onCreate()
    ↓
onStart()
    ↓
[App Running]
    ↓
User Leaves App
    ↓
onStop() → Save timestamp if !isChangingConfigurations
    ↓
[App in Background]
    ↓
User Returns
    ↓
onStart() → Show toast if conditions met
    ↓
[App Running]
```

## Notes

- The application uses polling (2-second intervals) for contact updates instead of reactive database observers (Flow/StateFlow)
- Background timestamp is only saved when app actually goes to background, not during configuration changes
- First launch is detected by checking if `KEY_IS_FIRST_START` exists in SharedPreferences
- Language changes trigger Activity recreation, which resets all Compose state except `rememberSaveable` values
