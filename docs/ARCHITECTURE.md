# Component Interaction Documentation

## Overview

This document describes how the main components of the ft_hangouts_42 Android application interact with each other, including data flow, communication patterns, and architectural relationships.

## Architecture Overview

The application follows a **Repository Pattern** with **MVVM-like structure** using Jetpack Compose:

```
UI Layer (Compose)
    ↓
Repository Layer
    ↓
DAO Layer
    ↓
Room Database
```

## Component Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                      MainActivity                           │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              MainScreen (Compose)                    │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌─────────────┐ │   │
│  │  │ContactEdit   │  │Conversation  │  │Contact List │ │   │
│  │  │Screen        │  │Screen        │  │             │ │   │
│  │  └──────────────┘  └──────────────┘  └─────────────┘ │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                            ↓                    ↓
                    ┌──────────────┐    ┌──────────────┐
                    │ContactRepo   │    │MessageRepo   │
                    └──────────────┘    └──────────────┘
                            ↓                    ↓
                    ┌──────────────┐    ┌──────────────┐
                    │ContactDao    │    │MessageDao    │
                    └──────────────┘    └──────────────┘
                            ↓                    ↓
                    ┌─────────────────────────────-─┐
                    │      AppDatabase (Room)       │
                    │  ┌──────────┐  ┌──────────┐   │
                    │  │contacts  │  │messages  │   │
                    │  │  table   │  │  table   │   │
                    │  └──────────┘  └──────────┘   │
                    └──────────────────────────────-┘
                            ↑
                    ┌──────────────┐
                    │ SMSReceiver  │
                    │ (Broadcast)  │
                    └──────────────┘
```

## Core Components

### 1. MainActivity

**Location**: `ui/main/MainActivity.kt`

**Role**: Entry point and coordinator

**Responsibilities**:
- Activity lifecycle management
- Permission handling
- Repository initialization
- Compose UI setup
- Language and theme management

**Dependencies**:
- `ContactRepository`
- `MessageRepository`
- `LocaleHelper`
- Various Android system services

**Interactions**:
- **→ ContactRepository**: Initializes and passes to UI components
- **→ MessageRepository**: Initializes and passes to UI components
- **→ MainScreen**: Provides callbacks and dependencies
- **→ System**: Handles permissions, intents, and broadcasts

### 2. MainScreen

**Location**: `ui/main/MainActivity.kt` (Composable function)

**Role**: Main UI coordinator

**Responsibilities**:
- Contact list display
- Navigation between screens
- Menu management
- State management for UI

**State Management**:
```kotlin
var contacts: List<ContactEntity>
var showEdit: Boolean
var contactToEdit: ContactEntity?
var showConversation: Boolean
var selectedContact: ContactEntity?
var expandedContactId: Long?
var topBarColor: Color
```

**Interactions**:
- **→ ContactRepository**: Fetches contacts (polling every 2 seconds)
- **→ ContactEditScreen**: Opens/closes edit screen
- **→ ConversationScreen**: Opens/closes conversation screen
- **→ SharedPreferences**: Reads/writes top bar color

**Data Flow**:
```
ContactRepository.getAllContacts() 
    → MainScreen.contacts 
    → LazyColumn.items() 
    → ContactCard
```

### 3. ContactRepository

**Location**: `data/ContactRepository.kt`

**Role**: Data access abstraction layer

**Responsibilities**:
- Provides high-level contact operations
- Abstracts database access
- Manages ContactDao instance

**Methods**:
- `add(contact: ContactEntity)`
- `update(contact: ContactEntity)`
- `delete(contact: ContactEntity)`
- `getAllContacts(): List<ContactEntity>`
- `getByPhone(phone: String): ContactEntity?`
- `getById(id: Long): ContactEntity?`

**Interactions**:
- **→ AppDatabase**: Gets database instance
- **→ ContactDao**: Delegates all database operations
- **← MainScreen**: Called for contact list updates
- **← ContactEditScreen**: Called for CRUD operations
- **← SMSReceiver**: Called to check/create contacts

**Data Flow**:
```
MainScreen → ContactRepository.getAllContacts()
    → ContactDao.getAllContacts()
    → Room Database Query
    → List<ContactEntity>
    → MainScreen.contacts
```

### 4. MessageRepository

**Location**: `data/MessageRepository.kt`

**Role**: Data access abstraction layer for messages

**Responsibilities**:
- Provides message operations
- Returns Flow for reactive updates
- Manages MessageDao instance

**Methods**:
- `addMessage(message: MessageEntity)`
- `getMessagesForContact(contactId: Long): Flow<List<MessageEntity>>`

**Interactions**:
- **→ AppDatabase**: Gets database instance
- **→ MessageDao**: Delegates database operations
- **← ConversationScreen**: Called to get/send messages
- **← SMSReceiver**: Called to save incoming messages

**Data Flow**:
```
ConversationScreen → MessageRepository.getMessagesForContact()
    → MessageDao.getMessagesForContact()
    → Room Flow
    → collectAsState()
    → ConversationScreen.messages
```

### 5. ContactEditScreen

**Location**: `ui/contact/ContactEditScreen.kt`

**Role**: Contact creation and editing UI

**Responsibilities**:
- Display contact form
- Handle image selection
- Save/delete contacts
- Validate input

**State Management**:
```kotlin
var name: String
var phone: String
var email: String
var address: String
var notes: String
var avatarPath: String?
```

**Interactions**:
- **→ ContactRepository**: Saves/updates/deletes contacts
- **→ MainActivity**: Receives image picker launchers
- **→ File System**: Saves avatar images
- **← MainScreen**: Receives contact to edit (null = new contact)

**Data Flow**:
```
User Input → ContactEditScreen fields
    → ContactRepository.add/update()
    → ContactDao.insert/update()
    → Room Database
    → MainScreen refreshes contact list
```

**Image Handling Flow**:
```
User selects image
    → imagePickerLauncher.launch()
    → pickedImageUri
    → LaunchedEffect processes URI
    → File saved to app's files directory
    → avatarPath updated
    → ContactEntity.avatarPath
    → Saved to database
```

### 6. ConversationScreen

**Location**: `ui/conversation/ConversationScreen.kt`

**Role**: Messaging interface

**Responsibilities**:
- Display message history
- Send new messages
- Handle keyboard input

**State Management**:
```kotlin
val messages: List<MessageEntity> (from Flow)
var inputText: String
```

**Interactions**:
- **→ MessageRepository**: Gets messages (Flow) and sends new messages
- **← MainScreen**: Receives contactId and contactName
- **→ Room Database**: Indirectly via MessageRepository

**Data Flow**:
```
MessageRepository.getMessagesForContact()
    → Flow<List<MessageEntity>>
    → collectAsState()
    → ConversationScreen.messages
    → LazyColumn.items()
    → MessageItem composables
```

**Message Sending Flow**:
```
User types message
    → inputText state
    → Send button clicked
    → MessageRepository.addMessage()
    → MessageDao.insert()
    → Room Database
    → Flow emits new list
    → UI updates automatically
```

### 7. SMSReceiver

**Location**: `receiver/SMSReceiver.kt`

**Role**: Incoming SMS handler

**Responsibilities**:
- Listen for incoming SMS
- Extract message data
- Create contacts if needed
- Save messages to database

**Interactions**:
- **→ System**: Receives SMS broadcast
- **→ ContactRepository**: Checks/creates contacts
- **→ MessageRepository**: Saves incoming messages
- **→ Room Database**: Indirectly via repositories

**Data Flow**:
```
System broadcasts SMS_RECEIVED
    → SMSReceiver.onReceive()
    → Extract phone number and message body
    → ContactRepository.getByPhone()
    → If not found: ContactRepository.add() (auto-create)
    → MessageRepository.addMessage()
    → Room Database
    → MainScreen polls and updates (2-second delay)
```

**Auto-Contact Creation**:
```
Unknown phone number
    → ContactRepository.getByPhone() returns null
    → Create ContactEntity with:
        - name = phone number
        - phone = phone number
        - notes = "Auto-created from SMS"
    → ContactRepository.add()
    → Contact saved to database
```

### 8. AppDatabase

**Location**: `data/room/AppDatabase.kt`

**Role**: Room database singleton

**Responsibilities**:
- Database initialization
- DAO access
- Database configuration

**Pattern**: Singleton with double-checked locking

**Interactions**:
- **→ ContactDao**: Provides DAO instance
- **→ MessageDao**: Provides DAO instance
- **← Repositories**: Accessed via `getDatabase()`

**Initialization**:
```kotlin
AppDatabase.getDatabase(context)
    → Check INSTANCE
    → If null: synchronized block
    → Room.databaseBuilder()
    → Build database
    → Return singleton instance
```

### 9. DAO Interfaces

#### ContactDao

**Location**: `data/room/ContactDao.kt`

**Methods**:
- `getAllContacts(): List<ContactEntity>`
- `getById(id: Long): ContactEntity?`
- `getByPhone(phone: String): ContactEntity?`
- `insert(contact: ContactEntity)`
- `update(contact: ContactEntity)`
- `delete(contact: ContactEntity)`

**Interactions**:
- **→ Room**: Executes SQL queries
- **← ContactRepository**: Called for all operations

#### MessageDao

**Location**: `data/room/MessageDao.kt`

**Methods**:
- `getMessagesForContact(contactId: Long): Flow<List<MessageEntity>>`
- `insert(message: MessageEntity)`

**Interactions**:
- **→ Room**: Executes SQL queries and returns Flow
- **← MessageRepository**: Called for all operations

## Data Flow Patterns

### 1. Contact List Display

```
MainScreen LaunchedEffect
    ↓ (every 2 seconds)
ContactRepository.getAllContacts()
    ↓
ContactDao.getAllContacts()
    ↓
Room Database Query: SELECT * FROM contacts ORDER BY name ASC
    ↓
List<ContactEntity>
    ↓
MainScreen.contacts state
    ↓
LazyColumn.items(contacts)
    ↓
ContactCard composables
```

### 2. Contact Creation

```
User clicks FAB
    ↓
MainScreen.showEdit = true, contactToEdit = null
    ↓
ContactEditScreen displayed
    ↓
User fills form and clicks Save
    ↓
ContactRepository.add(newContact)
    ↓
ContactDao.insert(contact)
    ↓
Room Database INSERT
    ↓
ContactEditScreen.onClose()
    ↓
MainScreen refreshes contact list (polling will update in 2 seconds)
```

### 3. Contact Editing

```
User long-presses contact → Menu → Edit
    ↓
MainScreen.contactToEdit = contact, showEdit = true
    ↓
ContactEditScreen displayed with contact data
    ↓
User modifies and clicks Save
    ↓
ContactRepository.update(contact.copy(...))
    ↓
ContactDao.update(contact)
    ↓
Room Database UPDATE
    ↓
MainScreen refreshes contact list
```

### 4. Message Sending

```
User opens conversation
    ↓
ConversationScreen displayed
    ↓
MessageRepository.getMessagesForContact(contactId)
    ↓
MessageDao.getMessagesForContact()
    ↓
Room Flow<List<MessageEntity>>
    ↓
collectAsState() → messages state
    ↓
User types and sends message
    ↓
MessageRepository.addMessage(message)
    ↓
MessageDao.insert(message)
    ↓
Room Database INSERT
    ↓
Flow emits new list
    ↓
UI automatically updates
```

### 5. Incoming SMS

```
System receives SMS
    ↓
Broadcast: android.provider.Telephony.SMS_RECEIVED
    ↓
SMSReceiver.onReceive()
    ↓
Extract phone number and message body
    ↓
ContactRepository.getByPhone(phone)
    ↓
If null:
    ContactRepository.add(newContact) → Auto-create contact
    ↓
MessageRepository.addMessage(message)
    ↓
Room Database INSERT
    ↓
MainScreen polling detects new contact/message (within 2 seconds)
```

### 6. Phone Call

```
User clicks Call in menu
    ↓
MainScreen.onMakePhoneCall(phoneNumber)
    ↓
Check CALL_PHONE permission
    ↓
If granted:
    Intent(ACTION_CALL, "tel:$phoneNumber")
    ↓
System dialer app opens
If not granted:
    callPermissionLauncher.launch()
    ↓
User grants/denies
    ↓
Callback handles result
```

### 7. Image Selection

```
User clicks Change Avatar
    ↓
Check READ_EXTERNAL_STORAGE permission
    ↓
If granted:
    imagePickerLauncher.launch("image/*")
    ↓
Gallery opens
    ↓
User selects image
    ↓
pickedImageUri = uri
    ↓
LaunchedEffect processes URI
    ↓
Copy image to app's files directory
    ↓
avatarPath = file.absolutePath
    ↓
ContactEntity.avatarPath
    ↓
Saved to database
```

## Communication Patterns

### 1. Callback Pattern

**Used in**: MainActivity → MainScreen → Child Screens

**Example**:
```kotlin
MainScreen(
    onLanguageChange = { lang -> ... },
    onMakePhoneCall = { phone -> ... },
    ...
)

ContactEditScreen(
    onClose = { ... }
)
```

### 2. State Hoisting

**Used in**: Image selection, language management

**Example**:
```kotlin
// State in MainActivity
var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

// Passed down to screens
ContactEditScreen(
    selectedImageUri = selectedImageUri,
    onSelectedImageUriChanged = { selectedImageUri = it }
)
```

### 3. Repository Pattern

**Used in**: All data access

**Benefits**:
- Abstraction of data source
- Easy to test
- Single source of truth
- Can swap implementations

### 4. Reactive Updates (Flow)

**Used in**: MessageRepository

**Example**:
```kotlin
// DAO returns Flow
fun getMessagesForContact(contactId: Long): Flow<List<MessageEntity>>

// Screen collects
val messages by repo.getMessagesForContact(contactId)
    .collectAsState(initial = emptyList())
```

### 5. Polling Pattern

**Used in**: Contact list updates

**Note**: This is not ideal. Better approach would be Flow/StateFlow.

**Current Implementation**:
```kotlin
LaunchedEffect(Unit) {
    while (true) {
        contacts = contactRepo.getAllContacts()
        delay(2000)
    }
}
```

## Shared State Management

### SharedPreferences

**Two separate files**:
1. `prefs`: Activity lifecycle state
2. `app_prefs`: Application preferences

**Access Pattern**:
```kotlin
// Write
prefs.edit().putString(key, value).apply()

// Read
val value = prefs.getString(key, defaultValue)
```

### Compose State

**Local State**: `remember` / `rememberSaveable` in composables

**Shared State**: Passed via parameters and callbacks

**Example**:
```kotlin
// In MainScreen
var showEdit by rememberSaveable { mutableStateOf(false) }

// Passed to child
if (showEdit) {
    ContactEditScreen(onClose = { showEdit = false })
}
```

## Dependency Injection

**Current Approach**: Manual dependency injection

**Pattern**:
```kotlin
// In MainActivity
contactRepo = ContactRepository(this)
messageRepo = MessageRepository(this)

// Passed to composables
MainScreen(
    contactRepo = contactRepo,
    messageRepo = messageRepo,
    ...
)
```

**Note**: Could be improved with Hilt/Dagger for better testability.

## Threading Model

### Main Thread (UI Thread)
- All Compose UI operations
- State updates
- Callbacks

### Background Threads
- Database operations (Room handles automatically)
- SMSReceiver uses `CoroutineScope(Dispatchers.IO)`
- Image file operations

**Room Threading**:
- DAO suspend functions run on background thread
- Flow emissions on background thread
- `collectAsState()` switches to main thread automatically

## Error Handling

**Current State**: Minimal error handling

**Areas for Improvement**:
- Database operation errors
- Permission denial handling
- Network/file operation errors
- Image loading errors

## Best Practices

1. **Repository Pattern**: Abstracts data access
2. **Separation of Concerns**: UI, business logic, and data access separated
3. **State Hoisting**: State managed at appropriate level
4. **Reactive Updates**: Flow for messages (could be extended to contacts)
5. **Permission Handling**: Modern ActivityResultLauncher API

## Areas for Improvement

1. **Replace Polling with Flow**: Contact list should use Flow instead of polling
2. **Dependency Injection**: Use Hilt for better DI
3. **Error Handling**: Add comprehensive error handling
4. **State Management**: Consider ViewModel for complex state
5. **Testing**: Add unit tests for repositories and ViewModels
