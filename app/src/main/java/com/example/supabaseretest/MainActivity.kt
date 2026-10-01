package com.example.supabaseretest

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.webkit.MimeTypeMap
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.supabaseretest.ui.theme.SupabaseRetestTheme
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from as postgrestFrom
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

private const val USER_TABLE = "names"
private const val GROUP_TABLE = "Group"
private const val LOGIN_TABLE = "loginAndPass"
private const val AVATAR_BUCKET = "avatars"

val supabase = createSupabaseClient(
    supabaseUrl = "https://yzzkgsfuxwryqxoowjpa.supabase.co",
    supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inl6emtnc2Z1eHdyeXF4b293anBhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzE1ODc2NDAsImV4cCI6MjA4NzE2MzY0MH0.0gSsCdeljUkZOYdaLDwEulPVtFdxo_XeaA0Tryyirsg"
) {
    install(Postgrest)
    install(Storage)
}

@Serializable
data class LoginItem(
    val id: Long? = null,
    val login: String,
    val password: String,
    val status: String
)

@Serializable
data class NewLoginItem(
    val login: String,
    val password: String,
    val status: String
)

@Serializable
data class UserItem(
    val id: Long,
    @SerialName("firstName") val firstName: String,
    @SerialName("lastName") val lastName: String,
    @SerialName("bornDate") val bornDate: String,
    @SerialName("group_id") val groupId: Long? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null
)

@Serializable
data class GroupItem(
    val id: Long,
    val name: String
)

@Serializable
data class NewUser(
    @SerialName("firstName") val firstName: String,
    @SerialName("lastName") val lastName: String,
    @SerialName("bornDate") val bornDate: String,
    @SerialName("group_id") val groupId: Long,
    @SerialName("avatar_url") val avatarUrl: String? = null
)

@Serializable
data class UpdateUser(
    @SerialName("firstName") val firstName: String,
    @SerialName("lastName") val lastName: String,
    @SerialName("bornDate") val bornDate: String,
    @SerialName("group_id") val groupId: Long,
    @SerialName("avatar_url") val avatarUrl: String? = null
)

private enum class AppScreen {
    LIST,
    ADD,
    EDIT,
    DELETE
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SupabaseRetestTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    UsersApp(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun UsersApp(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()

    var users by remember { mutableStateOf<List<UserItem>>(emptyList()) }
    var groups by remember { mutableStateOf<List<GroupItem>>(emptyList()) }
    var selectedUser by remember { mutableStateOf<UserItem?>(null) }
    var currentScreen by remember { mutableStateOf(AppScreen.LIST) }
    var authenticated by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun loadData() {
        try {
            loading = true
            error = null

            users = supabase
                .postgrestFrom(USER_TABLE)
                .select()
                .decodeAs<List<UserItem>>()

            groups = supabase
                .postgrestFrom(GROUP_TABLE)
                .select()
                .decodeAs<List<GroupItem>>()
        } catch (e: Exception) {
            error = "Ошибка загрузки: ${e.localizedMessage}"
        } finally {
            loading = false
        }
    }

    LaunchedEffect(authenticated) {
        if (authenticated) {
            loadData()
        }
    }

    if (!authenticated) {
        LoginRegisterScreen(
            modifier = modifier,
            onAuthenticated = {
                authenticated = true
            }
        )
        return
    }

    when (currentScreen) {
        AppScreen.LIST -> {
            UsersListScreen(
                users = users,
                groups = groups,
                selectedUser = selectedUser,
                loading = loading,
                error = error,
                modifier = modifier,
                onSelectUser = { user ->
                    selectedUser =
                        if (selectedUser?.id == user.id) null else user
                },
                onAdd = {
                    error = null
                    currentScreen = AppScreen.ADD
                },
                onEdit = {
                    if (selectedUser != null) {
                        error = null
                        currentScreen = AppScreen.EDIT
                    }
                },
                onDelete = {
                    if (selectedUser != null) {
                        error = null
                        currentScreen = AppScreen.DELETE
                    }
                },
                onLogout = {
                    authenticated = false
                    selectedUser = null
                    currentScreen = AppScreen.LIST
                }
            )
        }

        AppScreen.ADD -> {
            UserFormScreen(
                groups = groups,
                modifier = modifier,
                user = null,
                onBack = {
                    currentScreen = AppScreen.LIST
                },
                onSaved = {
                    currentScreen = AppScreen.LIST
                    selectedUser = null
                    scope.launch { loadData() }
                }
            )
        }

        AppScreen.EDIT -> {
            val user = selectedUser

            if (user == null) {
                currentScreen = AppScreen.LIST
            } else {
                UserFormScreen(
                    groups = groups,
                    modifier = modifier,
                    user = user,
                    onBack = {
                        currentScreen = AppScreen.LIST
                    },
                    onSaved = {
                        currentScreen = AppScreen.LIST
                        selectedUser = null
                        scope.launch { loadData() }
                    }
                )
            }
        }

        AppScreen.DELETE -> {
            val user = selectedUser

            if (user == null) {
                currentScreen = AppScreen.LIST
            } else {
                DeleteUserScreen(
                    user = user,
                    groups = groups,
                    modifier = modifier,
                    onBack = {
                        currentScreen = AppScreen.LIST
                    },
                    onDeleted = {
                        currentScreen = AppScreen.LIST
                        selectedUser = null
                        scope.launch { loadData() }
                    }
                )
            }
        }
    }
}

@Composable
fun LoginRegisterScreen(
    modifier: Modifier = Modifier,
    onAuthenticated: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var registering by remember { mutableStateOf(false) }
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (registering) "Регистрация" else "Вход",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(Modifier.height(20.dp))

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(10.dp))
        }

        OutlinedTextField(
            value = login,
            onValueChange = {
                login = it
                error = null
            },
            label = { Text("Логин") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                error = null
            },
            label = { Text("Пароль") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )

        if (registering) {
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    error = null
                },
                label = { Text("Повтори пароль") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
        }

        Spacer(Modifier.height(18.dp))

        Button(
            onClick = {
                val cleanLogin = login.trim()

                if (cleanLogin.isBlank() || password.isBlank()) {
                    error = "Введи логин и пароль"
                    return@Button
                }

                if (registering && password != confirmPassword) {
                    error = "Пароли не совпадают"
                    return@Button
                }

                scope.launch {
                    loading = true
                    error = null

                    try {
                        if (registering) {
                            val found = supabase
                                .postgrestFrom(LOGIN_TABLE)
                                .select {
                                    filter {
                                        eq("login", cleanLogin)
                                    }
                                }
                                .decodeAs<List<LoginItem>>()

                            if (found.isNotEmpty()) {
                                error = "Такой логин уже занят"
                            } else {
                                supabase
                                    .postgrestFrom(LOGIN_TABLE)
                                    .insert(
                                        NewLoginItem(
                                            login = cleanLogin,
                                            password = password,
                                            status = "active"
                                        )
                                    )

                                onAuthenticated()
                            }
                        } else {
                            val found = supabase
                                .postgrestFrom(LOGIN_TABLE)
                                .select {
                                    filter {
                                        eq("login", cleanLogin)
                                        eq("password", password)
                                    }
                                }
                                .decodeAs<List<LoginItem>>()

                            if (found.isEmpty()) {
                                error = "Неверный логин или пароль"
                            } else {
                                onAuthenticated()
                            }
                        }
                    } catch (e: Exception) {
                        error = "Ошибка: ${e.localizedMessage}"
                    } finally {
                        loading = false
                    }
                }
            },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                when {
                    loading -> "Подождите..."
                    registering -> "Зарегистрироваться"
                    else -> "Войти"
                }
            )
        }

        TextButton(
            onClick = {
                registering = !registering
                error = null
            },
            enabled = !loading
        ) {
            Text(
                if (registering) {
                    "Уже есть аккаунт? Войти"
                } else {
                    "Нет аккаунта? Зарегистрироваться"
                }
            )
        }
    }
}

@Composable
fun UsersListScreen(
    users: List<UserItem>,
    groups: List<GroupItem>,
    selectedUser: UserItem?,
    loading: Boolean,
    error: String?,
    modifier: Modifier = Modifier,
    onSelectUser: (UserItem) -> Unit,
    onAdd: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Пользователи",
                style = MaterialTheme.typography.headlineMedium
            )
            TextButton(onClick = onLogout) {
                Text("Выйти")
            }
        }

        Spacer(Modifier.height(12.dp))

        if (loading) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(10.dp))
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(users, key = { it.id }) { user ->
                val isSelected = selectedUser?.id == user.id
                val groupName = groups
                    .firstOrNull { it.id == user.groupId }
                    ?.name ?: "Без группы"

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clickable { onSelectUser(user) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(
                            name = user.firstName,
                            imageUrl = user.avatarUrl,
                            size = 70
                        )

                        Spacer(Modifier.width(14.dp))

                        Column {
                            Text(
                                "${user.firstName} ${user.lastName}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text("Дата рождения: ${user.bornDate}")
                            Text("Группа: $groupName")
                        }
                    }
                }
            }
        }

        HorizontalDivider()
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onAdd,
                modifier = Modifier.weight(1f)
            ) {
                Text("Добавить")
            }

            Button(
                onClick = onEdit,
                enabled = selectedUser != null,
                modifier = Modifier.weight(1f)
            ) {
                Text("Изменить")
            }

            OutlinedButton(
                onClick = onDelete,
                enabled = selectedUser != null,
                modifier = Modifier.weight(1f)
            ) {
                Text("Удалить")
            }
        }
    }
}

@Composable
fun UserAvatar(
    name: String,
    imageUrl: String?,
    size: Int
) {
    if (imageUrl.isNullOrBlank()) {
        Box(
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                name.trim().firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.headlineMedium
            )
        }
    } else {
        AsyncImage(
            model = imageUrl,
            contentDescription = "Фото пользователя",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape)
        )
    }
}

@Composable
fun GroupSelector(
    groups: List<GroupItem>,
    selectedGroupId: Long?,
    onGroupSelected: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedGroup = groups.firstOrNull { it.id == selectedGroupId }

    Column(Modifier.fillMaxWidth()) {
        Text("Группа", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(6.dp))

        Box(Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(selectedGroup?.name ?: "Выбрать группу")
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                groups.forEach { group ->
                    DropdownMenuItem(
                        text = { Text(group.name) },
                        onClick = {
                            onGroupSelected(group.id)
                            expanded = false
                        }
                    )
                }

                if (groups.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("Нет групп") },
                        onClick = { expanded = false }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirthDatePicker(
    date: String,
    onDateSelected: (String) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    val utc = remember { TimeZone.getTimeZone("UTC") }

    fun toMillis(value: String): Long? {
        if (value.isBlank()) return null

        return try {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = utc
                isLenient = false
            }.parse(value)?.time
        } catch (_: Exception) {
            null
        }
    }

    OutlinedButton(
        onClick = { showPicker = true },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            if (date.isBlank()) {
                "Выбрать дату рождения"
            } else {
                "Дата рождения: $date"
            }
        )
    }

    if (showPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = toMillis(date),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis <= System.currentTimeMillis()
                }

                override fun isSelectableYear(year: Int): Boolean {
                    val currentYear = SimpleDateFormat(
                        "yyyy",
                        Locale.US
                    ).format(Date()).toInt()
                    return year <= currentYear
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { millis ->
                            val format = SimpleDateFormat(
                                "yyyy-MM-dd",
                                Locale.US
                            ).apply {
                                timeZone = utc
                            }
                            onDateSelected(format.format(Date(millis)))
                        }
                        showPicker = false
                    },
                    enabled = state.selectedDateMillis != null
                ) {
                    Text("Выбрать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Отмена")
                }
            }
        ) {
            DatePicker(
                state = state,
                showModeToggle = false
            )
        }
    }
}

@Composable
fun UserFormScreen(
    groups: List<GroupItem>,
    modifier: Modifier = Modifier,
    user: UserItem?,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    var firstName by remember(user?.id) {
        mutableStateOf(user?.firstName ?: "")
    }
    var lastName by remember(user?.id) {
        mutableStateOf(user?.lastName ?: "")
    }
    var bornDate by remember(user?.id) {
        mutableStateOf(user?.bornDate ?: "")
    }
    var groupId by remember(user?.id) {
        mutableStateOf(user?.groupId)
    }
    var selectedPhoto by remember(user?.id) {
        mutableStateOf<Uri?>(null)
    }
    var error by remember(user?.id) {
        mutableStateOf<String?>(null)
    }
    var saving by remember(user?.id) {
        mutableStateOf(false)
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        selectedPhoto = uri
    }

    BackHandler {
        onBack()
    }

    fun saveUser() {
        if (firstName.isBlank() || lastName.isBlank()) {
            error = "Заполни имя и фамилию"
            return
        }

        if (bornDate.isBlank()) {
            error = "Выбери дату рождения"
            return
        }

        val selectedGroup = groupId

        if (selectedGroup == null) {
            error = "Выбери группу"
            return
        }

        scope.launch {
            saving = true
            error = null

            try {
                val avatarUrl = selectedPhoto?.let {
                    uploadAvatar(context, it)
                } ?: user?.avatarUrl

                if (user == null) {
                    supabase
                        .postgrestFrom(USER_TABLE)
                        .insert(
                            NewUser(
                                firstName = firstName.trim(),
                                lastName = lastName.trim(),
                                bornDate = bornDate,
                                groupId = selectedGroup,
                                avatarUrl = avatarUrl
                            )
                        )
                } else {
                    supabase
                        .postgrestFrom(USER_TABLE)
                        .update(
                            UpdateUser(
                                firstName = firstName.trim(),
                                lastName = lastName.trim(),
                                bornDate = bornDate,
                                groupId = selectedGroup,
                                avatarUrl = avatarUrl
                            )
                        ) {
                            filter {
                                eq("id", user.id)
                            }
                        }
                }

                onSaved()
            } catch (e: Exception) {
                error = "Ошибка сохранения: ${e.localizedMessage}"
            } finally {
                saving = false
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("← Назад")
        }

        Text(
            text = if (user == null) {
                "Добавить пользователя"
            } else {
                "Изменить пользователя"
            },
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(Modifier.height(18.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            UserAvatar(
                name = firstName,
                imageUrl = selectedPhoto?.toString() ?: user?.avatarUrl,
                size = 100
            )
        }

        Spacer(Modifier.height(10.dp))

        OutlinedButton(
            onClick = {
                photoPicker.launch(
                    PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageOnly
                    )
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Выбрать фото из галереи")
        }

        Spacer(Modifier.height(16.dp))

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(10.dp))
        }

        OutlinedTextField(
            value = firstName,
            onValueChange = { firstName = it },
            label = { Text("Имя") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = lastName,
            onValueChange = { lastName = it },
            label = { Text("Фамилия") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(16.dp))

        BirthDatePicker(
            date = bornDate,
            onDateSelected = { bornDate = it }
        )

        Spacer(Modifier.height(16.dp))

        GroupSelector(
            groups = groups,
            selectedGroupId = groupId,
            onGroupSelected = { groupId = it }
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { saveUser() },
            enabled = !saving,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                when {
                    saving && user == null -> "Добавление..."
                    saving -> "Сохранение..."
                    user == null -> "Добавить пользователя"
                    else -> "Сохранить изменения"
                }
            )
        }
    }
}

private suspend fun uploadAvatar(
    context: Context,
    uri: Uri
): String {
    val mimeType = context.contentResolver.getType(uri)
    val extension = MimeTypeMap
        .getSingleton()
        .getExtensionFromMimeType(mimeType)
        ?: "jpg"

    val bytes = context.contentResolver
        .openInputStream(uri)
        ?.use { it.readBytes() }
        ?: throw IllegalStateException("Не удалось прочитать выбранное фото")

    val path = "${UUID.randomUUID()}.$extension"

    supabase.storage.from(AVATAR_BUCKET).upload(path, bytes) {
        upsert = true
    }

    return supabase.storage.from(AVATAR_BUCKET).publicUrl(path)
}

@Composable
fun DeleteUserScreen(
    user: UserItem,
    groups: List<GroupItem>,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onDeleted: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val groupName = groups
        .firstOrNull { it.id == user.groupId }
        ?.name ?: "Без группы"

    BackHandler {
        onBack()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth()) {
            TextButton(onClick = onBack) {
                Text("← Назад")
            }
        }

        Spacer(Modifier.height(32.dp))

        UserAvatar(
            name = user.firstName,
            imageUrl = user.avatarUrl,
            size = 120
        )

        Spacer(Modifier.height(18.dp))

        Text(
            "${user.firstName} ${user.lastName}",
            style = MaterialTheme.typography.headlineSmall
        )
        Text("Дата рождения: ${user.bornDate}")
        Text("Группа: $groupName")

        Spacer(Modifier.height(24.dp))

        Text("Ты точно хочешь удалить этого пользователя?")

        Spacer(Modifier.height(14.dp))

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(10.dp))
        }

        Button(
            onClick = {
                scope.launch {
                    loading = true
                    error = null

                    try {
                        supabase
                            .postgrestFrom(USER_TABLE)
                            .delete {
                                filter {
                                    eq("id", user.id)
                                }
                            }

                        onDeleted()
                    } catch (e: Exception) {
                        error = "Ошибка удаления: ${e.localizedMessage}"
                    } finally {
                        loading = false
                    }
                }
            },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (loading) "Удаление..." else "Удалить пользователя")
        }

        Spacer(Modifier.height(10.dp))

        OutlinedButton(
            onClick = onBack,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Отмена")
        }
    }
}