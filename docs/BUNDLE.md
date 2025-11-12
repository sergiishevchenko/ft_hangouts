# Android Bundle Documentation

## Overview

This document describes the Android Bundle mechanism, its usage in the ft_hangouts_42 application, and how it relates to state preservation and Activity lifecycle management.

## What is Bundle?

A `Bundle` is a key-value container class in Android that can store various types of data. It's primarily used for:
- Passing data between Activities via Intents
- Saving and restoring Activity instance state
- Passing data to Fragments
- Storing configuration data

### Bundle Characteristics

- **Parcelable/Serializable**: Can store Parcelable and Serializable objects
- **Primitive Types**: Supports all primitive types (Int, String, Boolean, Long, etc.)
- **Collections**: Can store arrays and lists of supported types
- **Size Limit**: Has a practical size limit (~1MB) for IPC transactions
- **Persistence**: Saved instance state Bundle persists across configuration changes and process death

## Bundle in Activity Lifecycle

### Standard Activity Lifecycle with Bundle

```
Activity Created
    ↓
onCreate(savedInstanceState: Bundle?)
    ↓
onStart()
    ↓
onResume()
    ↓
[Activity Running]
    ↓
Configuration Change / Process Death
    ↓
onSaveInstanceState(outState: Bundle)
    ↓
onDestroy()
    ↓
Activity Recreated
    ↓
onCreate(savedInstanceState: Bundle?) ← Restored Bundle
```

### Key Lifecycle Methods

#### onCreate(savedInstanceState: Bundle?)

**Purpose**: Called when Activity is first created or recreated after being destroyed.

**Bundle State**:
- `null` on first creation
- Contains saved state on recreation after configuration change or process death

**Example**:
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    if (savedInstanceState == null) {
        // First creation - initialize default state
    } else {
        // Recreation - restore saved state
        val savedValue = savedInstanceState.getString("key")
    }
}
```

#### onSaveInstanceState(outState: Bundle)

**Purpose**: Called before Activity is destroyed to save its state.

**When Called**:
- Before configuration changes (screen rotation)
- Before process death (low memory situations)
- Before Activity is finished (in some cases)

**Important**: NOT called when:
- User explicitly finishes Activity (back button)
- Activity is finishing normally
- System is killing process for memory (use onPause/onStop for critical data)

**Example**:
```kotlin
override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putString("userInput", editText.text.toString())
    outState.putInt("selectedItem", spinner.selectedItemPosition)
    outState.putBoolean("isExpanded", expandableLayout.isExpanded)
}
```

#### onRestoreInstanceState(savedInstanceState: Bundle)

**Purpose**: Alternative method to restore state (called after `onStart()`).

**Note**: Less commonly used - restoration is typically done in `onCreate()`.

**Example**:
```kotlin
override fun onRestoreInstanceState(savedInstanceState: Bundle) {
    super.onRestoreInstanceState(savedInstanceState)
    val savedValue = savedInstanceState.getString("key")
    // Restore UI state
}
```

## Bundle Usage in ft_hangouts_42

### Current Implementation

The application currently uses Bundle minimally:

**Location**: `MainActivity.onCreate()`

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // Bundle is received but not used for state restoration
    // State is managed via SharedPreferences and rememberSaveable
}
```

**Current Approach**: The application does NOT use Bundle for state preservation. Instead, it uses:
1. **SharedPreferences**: For persistent settings and background tracking
2. **rememberSaveable**: For Compose UI state that survives configuration changes
3. **Room Database**: For persistent data storage

### Why Bundle is Not Used

The application uses alternative state preservation methods because:

1. **Compose State Management**: Jetpack Compose's `rememberSaveable` automatically handles state preservation using Bundle internally, but abstracts it away from the developer.

2. **Persistent Storage**: Important data (contacts, messages) is stored in Room database, which persists across app restarts.

3. **SharedPreferences**: User preferences and app configuration are stored in SharedPreferences, which persists independently of Activity lifecycle.

4. **Modern Approach**: The application follows modern Android development patterns where Bundle is less frequently used directly.

## Bundle vs Other State Preservation Methods

### Comparison Table

| Method | Use Case | Persistence | Size Limit | When to Use |
|--------|----------|-------------|-------------|-------------|
| **Bundle** | Activity instance state | Configuration changes, process death | ~1MB | Temporary UI state, form inputs |
| **SharedPreferences** | App preferences, settings | App restarts | Small data only | User preferences, app configuration |
| **Room Database** | Structured data | Permanent | Large | Contacts, messages, user data |
| **rememberSaveable** | Compose UI state | Configuration changes | Automatic | Compose composable state |
| **ViewModel** | UI-related data | Configuration changes | Depends on data | Complex UI state, business logic |

### When to Use Bundle

**Use Bundle for**:
- Temporary UI state (scroll position, form inputs)
- State that should survive configuration changes
- Small, simple data structures
- Data that doesn't need to persist across app restarts

**Don't Use Bundle for**:
- Large amounts of data (use database)
- Complex objects (use Parcelable carefully)
- Data that needs to persist across app restarts (use SharedPreferences or database)
- Sensitive data (use encrypted storage)

## How rememberSaveable Uses Bundle

### Internal Mechanism

Jetpack Compose's `rememberSaveable` uses Bundle internally:

```kotlin
// This Compose code:
var showEdit by rememberSaveable { mutableStateOf(false) }

// Internally uses Bundle like this:
// onSaveInstanceState:
outState.putBoolean("showEdit", showEdit)

// onCreate:
val showEdit = savedInstanceState?.getBoolean("showEdit") ?: false
```

### Automatic Bundle Management

Compose automatically:
1. Saves state to Bundle in `onSaveInstanceState()`
2. Restores state from Bundle in `onCreate()`
3. Handles Parcelable objects
4. Manages Bundle keys automatically

**Example in Application**:
```kotlin
// MainScreen.kt
var showEdit by rememberSaveable { mutableStateOf(false) }
var contactToEdit by rememberSaveable { mutableStateOf<ContactEntity?>(null) }
var showConversation by rememberSaveable { mutableStateOf(false) }
var selectedContact by rememberSaveable { mutableStateOf<ContactEntity?>(null) }
```

These states are automatically saved to and restored from Bundle by Compose.

## Bundle Data Types

### Supported Types

Bundle can store:

**Primitives**:
- `Boolean`, `Byte`, `Char`, `Short`, `Int`, `Long`, `Float`, `Double`
- `String`, `CharSequence`

**Arrays**:
- `BooleanArray`, `ByteArray`, `CharArray`, `ShortArray`, `IntArray`, `LongArray`, `FloatArray`, `DoubleArray`
- `String[]`, `CharSequence[]`

**Collections**:
- `ArrayList<T>` (where T is a supported type)
- `Parcelable` objects
- `Serializable` objects

**Special Types**:
- `Bundle` (nested bundles)
- `Parcelable` objects
- `Serializable` objects
- `Size`, `SizeF`, `IBinder`

### Example Usage

```kotlin
val bundle = Bundle().apply {
    // Primitives
    putString("name", "John")
    putInt("age", 30)
    putBoolean("isActive", true)
    putLong("timestamp", System.currentTimeMillis())
    
    // Arrays
    putStringArray("tags", arrayOf("android", "kotlin"))
    putIntArray("scores", intArrayOf(90, 85, 92))
    
    // Parcelable
    putParcelable("contact", contactEntity)
    
    // Serializable
    putSerializable("date", Date())
    
    // Nested Bundle
    val nested = Bundle().apply {
        putString("key", "value")
    }
    putBundle("nested", nested)
    
    // ArrayList
    putStringArrayList("items", arrayListOf("item1", "item2"))
}
```

## Bundle in Intent Communication

### Passing Data Between Activities

While not used in this application, Bundle is commonly used for passing data via Intents:

```kotlin
// Sending Activity
val intent = Intent(this, DetailActivity::class.java).apply {
    putExtra("contactId", contactId)
    putExtra("contactName", contactName)
    putParcelable("contact", contactEntity)
}
startActivity(intent)

// Receiving Activity
val contactId = intent.getLongExtra("contactId", -1)
val contactName = intent.getStringExtra("contactName")
val contact = intent.getParcelableExtra<ContactEntity>("contact")
```

### Intent Extras are Bundles

Internally, Intent extras are stored in a Bundle:

```kotlin
// These are equivalent:
intent.putExtra("key", value)
intent.extras?.putString("key", value)
```

## Potential Bundle Usage in Application

### What Could Be Saved in Bundle

If the application were to use Bundle directly, it could save:

**Temporary UI State**:
```kotlin
override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putLong("expandedContactId", expandedContactId ?: -1L)
    outState.putInt("scrollPosition", recyclerViewScrollPosition)
}
```

**Form State** (if not using rememberSaveable):
```kotlin
outState.putString("editName", nameField.text.toString())
outState.putString("editPhone", phoneField.text.toString())
```

### Current Alternative Approach

The application handles these scenarios differently:

1. **UI State**: Uses `rememberSaveable` (which uses Bundle internally)
2. **Form State**: Uses `rememberSaveable` in `ContactEditScreen`
3. **Scroll Position**: Not saved (acceptable for this app)
4. **Menu State**: Resets on language change (intentional behavior)

## Bundle Best Practices

### 1. Keep Bundle Small

**Problem**: Bundle has size limits (~1MB for IPC).

**Solution**: Only save essential, small data.

```kotlin
// ❌ Bad: Saving large objects
outState.putSerializable("largeList", hugeList)

// ✅ Good: Save IDs and reconstruct
outState.putLong("selectedContactId", contactId)
```

### 2. Use Appropriate Data Types

**Problem**: Using wrong types can cause issues.

**Solution**: Use most appropriate type.

```kotlin
// ✅ Good
outState.putLong("timestamp", timestamp)
outState.putString("phone", phoneNumber)

// ❌ Avoid if possible
outState.putSerializable("timestamp", Date()) // Use Long instead
```

### 3. Handle Null Safety

**Problem**: Bundle values can be null.

**Solution**: Always provide defaults.

```kotlin
// ✅ Good
val value = savedInstanceState?.getString("key") ?: "default"

// ❌ Risky
val value = savedInstanceState!!.getString("key")!!
```

### 4. Use Keys Constants

**Problem**: String literals are error-prone.

**Solution**: Define constants.

```kotlin
companion object {
    private const val KEY_EXPANDED_ID = "expanded_contact_id"
    private const val KEY_SCROLL_POS = "scroll_position"
}

override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putLong(KEY_EXPANDED_ID, expandedContactId ?: -1L)
}
```

### 5. Don't Save Everything

**Problem**: Saving too much can cause performance issues.

**Solution**: Only save what's necessary.

```kotlin
// ❌ Bad: Saving everything
outState.putParcelableArrayList("allContacts", contacts)

// ✅ Good: Save minimal state
outState.putLong("selectedContactId", selectedContact?.id ?: -1L)
// Reconstruct from database if needed
```

## Bundle and Process Death

### Process Death Scenario

When Android kills the app process due to low memory:

1. **onSaveInstanceState()** is called
2. Bundle is saved by system
3. Process is killed
4. User returns to app
5. **onCreate(savedInstanceState)** is called with saved Bundle
6. App restores state from Bundle

### What Gets Saved Automatically

Android automatically saves:
- View hierarchy state (EditText text, scroll position, etc.)
- Fragment state (if using FragmentManager)

### What You Must Save Manually

You must manually save:
- Custom state variables
- Complex objects
- Non-view state

### Application's Approach

The application handles process death through:

1. **Room Database**: Contacts and messages persist automatically
2. **SharedPreferences**: Settings persist automatically
3. **rememberSaveable**: Compose state is saved to Bundle automatically
4. **No Manual Bundle**: No manual Bundle saving needed

## Bundle Limitations

### Size Limitations

- **IPC Limit**: ~1MB for Intent extras
- **Practical Limit**: Smaller is better for performance
- **Recommendation**: Keep under 50KB when possible

### Type Limitations

- Cannot store arbitrary objects (must be Parcelable/Serializable)
- Parcelable is preferred over Serializable (performance)
- Some types require special handling

### Performance Considerations

- Bundle operations are relatively fast
- Large Bundles can slow down Activity creation
- Parcelable is faster than Serializable

## Migration from Bundle to Modern Approaches

### Traditional Approach (Bundle)

```kotlin
class MainActivity : AppCompatActivity() {
    private var expandedContactId: Long? = null
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong("expandedId", expandedContactId ?: -1L)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        expandedContactId = savedInstanceState?.getLong("expandedId")?.takeIf { it != -1L }
    }
}
```

### Modern Approach (Compose + rememberSaveable)

```kotlin
@Composable
fun MainScreen() {
    var expandedContactId by rememberSaveable { mutableStateOf<Long?>(null) }
    // Automatically saved/restored via Bundle internally
}
```

### Application's Hybrid Approach

The application uses:
- **rememberSaveable**: For Compose UI state (uses Bundle internally)
- **SharedPreferences**: For persistent preferences
- **Room Database**: For data persistence
- **No Manual Bundle**: No direct Bundle manipulation needed

## Summary

### Bundle in Android

- **Purpose**: Temporary state preservation across configuration changes
- **Lifetime**: Survives configuration changes and process death
- **Size**: Limited (~1MB practical limit)
- **Usage**: Primarily for Activity instance state

### Bundle in ft_hangouts_42

- **Direct Usage**: Minimal (only in `onCreate` parameter)
- **Indirect Usage**: Via `rememberSaveable` in Compose
- **Alternative**: SharedPreferences and Room Database for persistence
- **Rationale**: Modern Android development patterns favor higher-level abstractions

### Key Takeaways

1. Bundle is used internally by Compose's `rememberSaveable`
2. The application doesn't need manual Bundle management
3. Persistent data is stored in Room Database
4. User preferences are stored in SharedPreferences
5. UI state is managed by Compose automatically

### When to Consider Manual Bundle Usage

Consider using Bundle directly if:
- Not using Compose (traditional Views)
- Need fine-grained control over state saving
- Working with Fragments that need state
- Passing complex data between Activities
- Need to save state that Compose doesn't handle automatically

For this application, the current approach (rememberSaveable + SharedPreferences + Room) is appropriate and follows modern Android best practices.
