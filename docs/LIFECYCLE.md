# Android Application Lifecycle Documentation

## Overview

This document describes the lifecycle management of the ft_hangouts_42 Android application, including Activity lifecycle callbacks, state preservation, and background/foreground transitions.

## General Android Lifecycle Theory

### Introduction to Android Lifecycle

Android applications have a complex lifecycle system that manages how components (Activities, Services, Fragments, etc.) are created, started, stopped, and destroyed. Understanding the lifecycle is crucial for:
- Proper resource management
- State preservation across configuration changes
- Handling background/foreground transitions
- Preventing memory leaks
- Ensuring good user experience

### Activity Lifecycle Overview

An Activity is a single, focused thing a user can do. The Activity lifecycle consists of a series of callback methods that are called by the system as the Activity transitions between different states.

#### Activity States

An Activity can exist in one of several states:

1. **Created**: Activity is being created (`onCreate()`)
2. **Started**: Activity is visible but not in foreground (`onStart()`)
3. **Resumed**: Activity is in foreground and interactive (`onResume()`)
4. **Paused**: Activity is partially visible (another Activity is on top) (`onPause()`)
5. **Stopped**: Activity is completely hidden (`onStop()`)
6. **Destroyed**: Activity is being destroyed (`onDestroy()`)

#### Complete Activity Lifecycle Callbacks

```
onCreate()
    ↓
onStart()
    ↓
onResume()
    ↓
[Activity Running - User Interacting]
    ↓
onPause() ← Activity partially visible or losing focus
    ↓
onStop() ← Activity no longer visible
    ↓
onDestroy() ← Activity being destroyed
```

**Detailed Callback Methods**:

##### onCreate(savedInstanceState: Bundle?)

**When Called**: 
- First time Activity is created
- After Activity is destroyed and recreated (configuration change, process death)

**Purpose**: 
- Initialize essential components
- Set up UI
- Initialize variables
- Set up listeners

**Bundle Parameter**:
- `null` on first creation
- Contains saved state on recreation

**Common Operations**:
- Inflate layout
- Initialize views
- Set up data sources
- Initialize ViewModels
- Request permissions

**Example**:
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    
    if (savedInstanceState == null) {
        // First creation
    } else {
        // Recreation - restore state
    }
}
```

##### onStart()

**When Called**: 
- After `onCreate()` when Activity becomes visible
- After `onRestart()` when Activity returns from stopped state

**Purpose**: 
- Prepare Activity to become visible
- Start animations
- Register broadcast receivers
- Refresh UI data

**Note**: Activity is visible but may not be in foreground (another Activity may be on top).

##### onResume()

**When Called**: 
- After `onStart()` when Activity comes to foreground
- After `onPause()` when Activity returns to foreground

**Purpose**: 
- Activity is now interactive
- Resume animations
- Start camera preview
- Resume sensors
- Start location updates

**Best Practice**: Keep this method lightweight - heavy operations can cause UI lag.

##### onPause()

**When Called**: 
- When another Activity comes to foreground
- When system is about to show a dialog
- When device screen turns off
- Before `onStop()` in most cases

**Purpose**: 
- Pause operations that shouldn't continue when Activity is not in foreground
- Save critical data
- Release resources that shouldn't be held while paused
- Stop animations
- Pause camera preview

**Important**: 
- Must complete quickly (system waits for this to complete)
- Don't perform heavy operations
- Don't save to database here (use `onStop()`)

##### onStop()

**When Called**: 
- When Activity is no longer visible
- Before `onDestroy()` (unless Activity is being destroyed immediately)

**Purpose**: 
- Save data that should persist
- Release resources
- Unregister broadcast receivers
- Stop location updates

**Note**: Activity may still be in memory and can be restarted without `onCreate()`.

##### onRestart()

**When Called**: 
- After `onStop()` when Activity is being restarted
- Before `onStart()`

**Purpose**: 
- Restore Activity state before it becomes visible again
- Re-initialize components that were released in `onStop()`

**Note**: Only called if Activity was stopped, not destroyed.

##### onDestroy()

**When Called**: 
- Final cleanup before Activity is destroyed
- May be called after `onStop()` or directly after `onPause()`

**Purpose**: 
- Final cleanup
- Release all resources
- Close database connections
- Unregister all listeners

**Note**: 
- May not be called if system kills process
- Don't rely on this for critical data saving (use `onPause()` or `onStop()`)

#### Lifecycle State Diagram

```
                    [Activity Created]
                           ↓
                      onCreate()
                           ↓
                    [Activity Started]
                           ↓
                      onStart()
                           ↓
                    [Activity Resumed]
                           ↓
                      onResume()
                           ↓
              ┌────────────┴──────────-──┐
              ↓                          ↓
         [User Interacts]        [Another Activity]
              ↓                    comes to front
              ↓                          ↓
              ↓                      onPause()
              ↓                          ↓
              ↓                    [Activity Paused]
              ↓                          ↓
              ↓                      onStop()
              ↓                          ↓
              ↓                    [Activity Stopped]
              ↓                          ↓
              └────────────┬─────────-───┘
                           ↓
                    [Activity Destroyed]
                           ↓
                      onDestroy()
```

#### Configuration Changes

**What are Configuration Changes?**
- Screen rotation
- Language change
- Keyboard availability
- Screen size changes
- Night mode changes

**What Happens**:
1. Activity is destroyed (`onPause()` → `onStop()` → `onDestroy()`)
2. `onSaveInstanceState()` is called (saves state to Bundle)
3. New Activity instance is created
4. `onCreate()` is called with saved Bundle
5. `onStart()` → `onResume()` are called

**Key Point**: `isChangingConfigurations` flag indicates if destruction is due to configuration change.

#### Process Lifecycle

Android manages processes based on their importance:

**Process States**:

1. **Foreground Process**: 
   - Activity with user interaction (`onResume()`)
   - Bound Service with foreground Activity
   - System kills only as last resort

2. **Visible Process**: 
   - Activity visible but not in foreground (`onPause()`)
   - Bound Service with visible Activity
   - System kills only if needed for foreground process

3. **Service Process**: 
   - Started Service
   - System kills if memory needed

4. **Background Process**: 
   - Activity stopped (`onStop()`)
   - System may kill to free memory

5. **Empty Process**: 
   - No active components
   - Kept for caching
   - First to be killed

**Process Death**:
- System may kill process when memory is low
- `onSaveInstanceState()` is called before process death
- State is restored in `onCreate()` when user returns

#### Application Lifecycle

The `Application` class has its own lifecycle:

##### onCreate()

**When Called**: Before any Activity, Service, or ContentProvider is created.

**Purpose**: 
- Initialize app-wide components
- Set up dependency injection
- Initialize analytics
- Set up crash reporting

**Example**:
```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize app-wide components
    }
}
```

##### onTerminate()

**When Called**: Only in emulator/testing - never in production.

**Note**: Don't rely on this method - it's not guaranteed to be called.

#### Fragment Lifecycle

Fragments have their own lifecycle that's tied to the Activity lifecycle:

**Key Methods**:
- `onAttach()`: Fragment attached to Activity
- `onCreate()`: Fragment created
- `onCreateView()`: Create fragment's view
- `onViewCreated()`: View created
- `onStart()`: Fragment visible
- `onResume()`: Fragment interactive
- `onPause()`: Fragment no longer interactive
- `onStop()`: Fragment not visible
- `onDestroyView()`: View destroyed
- `onDestroy()`: Fragment destroyed
- `onDetach()`: Fragment detached from Activity

**Note**: Fragment lifecycle is more complex and depends on Activity lifecycle.

### State Preservation

#### onSaveInstanceState(outState: Bundle)

**When Called**: 
- Before configuration changes
- Before process death (if possible)
- Before Activity is destroyed (in some cases)

**Purpose**: Save temporary UI state that should survive configuration changes.

**What to Save**:
- User input in forms
- Scroll position
- Selected items
- UI state flags

**What NOT to Save**:
- Large objects (use database)
- Parcelable objects (can be slow)
- Data already persisted elsewhere

**Example**:
```kotlin
override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putString("userInput", editText.text.toString())
    outState.putInt("scrollPosition", recyclerView.computeVerticalScrollOffset())
}
```

#### onRestoreInstanceState(savedInstanceState: Bundle)

**When Called**: After `onStart()`, before `onResume()`.

**Purpose**: Restore state saved in `onSaveInstanceState()`.

**Alternative**: Can restore in `onCreate()` by checking if Bundle is not null.

### Lifecycle Best Practices

#### 1. Keep Lifecycle Methods Lightweight

**Problem**: Heavy operations in lifecycle methods cause UI lag.

**Solution**: 
- Perform heavy operations in background threads
- Use coroutines or background services
- Defer non-critical initialization

```kotlin
// ❌ Bad
override fun onResume() {
    super.onResume()
    loadLargeDataset() // Blocks UI thread
}

// ✅ Good
override fun onResume() {
    super.onResume()
    viewModelScope.launch {
        loadLargeDataset() // Background thread
    }
}
```

#### 2. Save State Appropriately

**Problem**: Losing user data on configuration changes.

**Solution**: 
- Use `onSaveInstanceState()` for temporary UI state
- Use database/SharedPreferences for persistent data
- Use ViewModel for complex state

#### 3. Release Resources Properly

**Problem**: Memory leaks from not releasing resources.

**Solution**: 
- Release in `onPause()` or `onStop()`
- Unregister listeners
- Close connections
- Cancel coroutines

```kotlin
override fun onPause() {
    super.onPause()
    locationManager.removeUpdates(locationListener)
    camera?.release()
}
```

#### 4. Handle Configuration Changes

**Problem**: Losing state on screen rotation.

**Solution**: 
- Use `onSaveInstanceState()` for simple state
- Use ViewModel (survives configuration changes)
- Use `android:configChanges` (not recommended)

#### 5. Don't Perform Long Operations

**Problem**: System may kill process if lifecycle methods take too long.

**Solution**: 
- Keep lifecycle methods fast
- Use background threads
- Use Services for long operations

#### 6. Handle Process Death

**Problem**: Process may be killed, losing in-memory state.

**Solution**: 
- Save critical data in `onPause()` or `onStop()`
- Use `onSaveInstanceState()` for UI state
- Restore from database in `onCreate()`

### Common Lifecycle Scenarios

#### Scenario 1: User Opens App

```
App Launch
    ↓
Application.onCreate()
    ↓
Activity.onCreate()
    ↓
Activity.onStart()
    ↓
Activity.onResume()
    ↓
[User Interacts]
```

#### Scenario 2: User Presses Home Button

```
[User Interacts]
    ↓
Activity.onPause()
    ↓
Activity.onStop()
    ↓
[App in Background]
```

#### Scenario 3: User Returns to App

```
[App in Background]
    ↓
Activity.onRestart() (if stopped)
    ↓
Activity.onStart()
    ↓
Activity.onResume()
    ↓
[User Interacts]
```

#### Scenario 4: Screen Rotation

```
[User Interacts]
    ↓
Activity.onPause()
    ↓
Activity.onStop()
    ↓
Activity.onSaveInstanceState()
    ↓
Activity.onDestroy()
    ↓
[New Activity Instance]
    ↓
Activity.onCreate(savedInstanceState)
    ↓
Activity.onStart()
    ↓
Activity.onResume()
    ↓
[User Interacts]
```

#### Scenario 5: Process Death

```
[App in Background]
    ↓
System kills process (low memory)
    ↓
[User Returns]
    ↓
Activity.onCreate(savedInstanceState) ← Bundle restored
    ↓
Activity.onStart()
    ↓
Activity.onResume()
    ↓
[User Interacts]
```

### Lifecycle-Aware Components

Modern Android provides lifecycle-aware components:

#### ViewModel

- Survives configuration changes
- Tied to Activity/Fragment lifecycle
- Automatically cleared when Activity finishes

#### LiveData

- Lifecycle-aware observable
- Only updates active observers
- Automatically handles lifecycle

#### LifecycleObserver

- Observe lifecycle events
- React to lifecycle changes
- Clean up automatically

### Summary

Understanding Android lifecycle is essential for:
- **Resource Management**: Properly allocate and release resources
- **State Preservation**: Save and restore user data
- **Performance**: Keep UI responsive
- **User Experience**: Smooth transitions and data persistence
- **Memory Management**: Prevent leaks and optimize memory usage

The lifecycle system ensures that Android can manage resources efficiently while providing a smooth user experience across different scenarios (configuration changes, background/foreground transitions, process death).

---

## Application-Specific Implementation

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
