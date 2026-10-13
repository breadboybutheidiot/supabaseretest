package com.example.supabaseretest

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import coil3.compose.SubcomposeAsyncImage
import com.example.supabaseretest.ui.theme.SupabaseRetestTheme
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from as postgrestFrom
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private const val USER_TABLE = "names"
private const val GROUP_TABLE = "Group"
private const val CURATOR_TABLE = "curators"
private const val LOGIN_TABLE = "loginAndPass"

val supabase = createSupabaseClient(
    supabaseUrl = "https://yzzkgsfuxwryqxoowjpa.supabase.co",
    supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inl6emtnc2Z1eHdyeXF4b293anBhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzE1ODc2NDAsImV4cCI6MjA4NzE2MzY0MH0.0gSsCdeljUkZOYdaLDwEulPVtFdxo_XeaA0Tryyirsg"
) {
    install(Postgrest)
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
    @SerialName("image_url") val imageUrl: String? = null
)

@Serializable
data class GroupItem(
    val id: Long,
    val name: String
)

@Serializable
data class CuratorItem(
    val id: Long,
    val name: String,
    @SerialName("group_id") val groupId: Long? = null
)

@Serializable
data class NewUser(
    @SerialName("firstName") val firstName: String,
    @SerialName("lastName") val lastName: String,
    @SerialName("bornDate") val bornDate: String,
    @SerialName("group_id") val groupId: Long,
    @SerialName("image_url") val imageUrl: String? = null
)

@Serializable
data class UpdateUser(
    @SerialName("firstName") val firstName: String,
    @SerialName("lastName") val lastName: String,
    @SerialName("bornDate") val bornDate: String,
    @SerialName("group_id") val groupId: Long,
    @SerialName("image_url") val imageUrl: String? = null
)

private enum class AppScreen {
    LIST,
    ADD,
    EDIT,
    DELETE
}

private enum class MainTab {
    STUDENTS,
    GROUPS,
    CURATORS,
    PROFILE
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SupabaseRetestTheme {
                UsersApp(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
fun UsersApp(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()

    var users by remember { mutableStateOf<List<UserItem>>(emptyList()) }
    var groups by remember { mutableStateOf<List<GroupItem>>(emptyList()) }
    var curators by remember { mutableStateOf<List<CuratorItem>>(emptyList()) }
    var selectedUser by remember { mutableStateOf<UserItem?>(null) }
    var selectedGroupId by remember { mutableStateOf<Long?>(null) }
    var currentScreen by remember { mutableStateOf(AppScreen.LIST) }
    var currentTab by remember { mutableStateOf(MainTab.STUDENTS) }
    var authenticated by remember { mutableStateOf(false) }
    var accountLogin by remember { mutableStateOf("") }
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

            curators = supabase
                .postgrestFrom(CURATOR_TABLE)
                .select()
                .decodeAs<List<CuratorItem>>()
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

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (authenticated && currentScreen == AppScreen.LIST) {
                MainNavigationBar(
                    selectedTab = currentTab,
                    onTabSelected = { tab ->
                        if (tab != currentTab) {
                            currentTab = tab
                            selectedGroupId = null
                            selectedUser = null
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        val contentModifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()

        if (!authenticated) {
            LoginRegisterScreen(
                modifier = contentModifier,
                onAuthenticated = { login ->
                    accountLogin = login
                    authenticated = true
                }
            )
        } else {
            BackHandler(
                enabled = currentScreen == AppScreen.LIST &&
                    (selectedGroupId != null || currentTab != MainTab.STUDENTS)
            ) {
                if (selectedGroupId != null) {
                    selectedGroupId = null
                } else {
                    currentTab = MainTab.STUDENTS
                }
                selectedUser = null
            }

            when (currentScreen) {
                AppScreen.LIST -> when (currentTab) {
                    MainTab.STUDENTS -> {
                        UsersListScreen(
                            users = users,
                            groups = groups,
                            selectedUser = selectedUser,
                            loading = loading,
                            error = error,
                            modifier = contentModifier,
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
                        )
                    }

                    MainTab.GROUPS -> {
                        GroupsTabScreen(
                            groups = groups,
                            students = users,
                            curators = curators,
                            selectedGroupId = selectedGroupId,
                            loading = loading,
                            error = error,
                            modifier = contentModifier,
                            onSelectGroup = { group -> selectedGroupId = group.id },
                            onBack = { selectedGroupId = null }
                        )
                    }

                    MainTab.CURATORS -> CuratorsTabScreen(
                        curators = curators,
                        groups = groups,
                        loading = loading,
                        error = error,
                        modifier = contentModifier,
                        onSelectGroup = { group ->
                            currentTab = MainTab.GROUPS
                            selectedGroupId = group.id
                            selectedUser = null
                        }
                    )

                    MainTab.PROFILE -> ProfileTabScreen(
                        login = accountLogin,
                        modifier = contentModifier,
                        onLogout = {
                            authenticated = false
                            accountLogin = ""
                            selectedUser = null
                            selectedGroupId = null
                            currentTab = MainTab.STUDENTS
                            currentScreen = AppScreen.LIST
                            error = null
                        }
                    )
                }

                AppScreen.ADD -> UserFormScreen(
                    groups = groups,
                    modifier = contentModifier,
                    user = null,
                    onBack = { currentScreen = AppScreen.LIST },
                    onSaved = {
                        currentScreen = AppScreen.LIST
                        selectedUser = null
                        scope.launch { loadData() }
                    }
                )

                AppScreen.EDIT -> {
                    val user = selectedUser
                    if (user == null) {
                        currentScreen = AppScreen.LIST
                    } else {
                        UserFormScreen(
                            groups = groups,
                            modifier = contentModifier,
                            user = user,
                            onBack = { currentScreen = AppScreen.LIST },
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
                            modifier = contentModifier,
                            onBack = { currentScreen = AppScreen.LIST },
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
    }
}

@Composable
fun LoginRegisterScreen(
    modifier: Modifier = Modifier,
    onAuthenticated: (String) -> Unit
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

                                onAuthenticated(cleanLogin)
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
                                onAuthenticated(cleanLogin)
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
    onDelete: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Студенты", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(12.dp))

        if (loading) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(10.dp))
        }

        if (users.isEmpty() && !loading) {
            EmptyState(
                title = if (error == null) "Студентов пока нет" else "Список не загрузился",
                description = if (error == null) {
                    "Добавь первую запись, чтобы она появилась в списке."
                } else {
                    "Проверь подключение к базе данных."
                },
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        } else {
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
                                imageUrl = user.imageUrl,
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
private fun MainNavigationBar(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = selectedTab == MainTab.STUDENTS,
            onClick = { onTabSelected(MainTab.STUDENTS) },
            icon = {},
            label = { Text("Студенты") }
        )
        NavigationBarItem(
            selected = selectedTab == MainTab.GROUPS,
            onClick = { onTabSelected(MainTab.GROUPS) },
            icon = {},
            label = { Text("Группы") }
        )
        NavigationBarItem(
            selected = selectedTab == MainTab.CURATORS,
            onClick = { onTabSelected(MainTab.CURATORS) },
            icon = {},
            label = { Text("Кураторы") }
        )
        NavigationBarItem(
            selected = selectedTab == MainTab.PROFILE,
            onClick = { onTabSelected(MainTab.PROFILE) },
            icon = {},
            label = { Text("Профиль") }
        )
    }
}

@Composable
private fun GroupsTabScreen(
    groups: List<GroupItem>,
    students: List<UserItem>,
    curators: List<CuratorItem>,
    selectedGroupId: Long?,
    loading: Boolean,
    error: String?,
    modifier: Modifier = Modifier,
    onSelectGroup: (GroupItem) -> Unit,
    onBack: () -> Unit
) {
    val selectedGroup = groups.firstOrNull { it.id == selectedGroupId }
    val groupStudents = students.filter { it.groupId == selectedGroup?.id }
    val groupCurators = curators.filter { it.groupId == selectedGroup?.id }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (selectedGroup == null) {
            Text("Группы", style = MaterialTheme.typography.headlineMedium)
        } else {
            TextButton(onClick = onBack) {
                Text("← Все группы")
            }
            Text(selectedGroup.name, style = MaterialTheme.typography.headlineSmall)
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

        if (selectedGroup != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Куратор", style = MaterialTheme.typography.labelLarge)
                    if (groupCurators.isEmpty()) {
                        Text(
                            if (error == null) "Куратор не указан" else "Не удалось загрузить куратора",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        groupCurators.forEach { curator ->
                            Text(curator.name)
                        }
                    }
                }
            }

            if (groupStudents.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("Студенты", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(groupStudents, key = { it.id }) { student ->
                        StudentSummaryCard(student = student)
                    }
                }
            }
        } else if (groups.isEmpty() && !loading) {
            EmptyState(
                title = if (error == null) "Групп пока нет" else "Группы не загрузились",
                description = if (error == null) {
                    "Когда группы появятся в базе, их список будет показан здесь."
                } else {
                    "Проверь подключение к базе данных."
                },
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(groups, key = { it.id }) { group ->
                    val studentCount = students.count { it.groupId == group.id }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .clickable { onSelectGroup(group) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    group.name.trim().firstOrNull()?.uppercase() ?: "?",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }

                            Spacer(Modifier.width(14.dp))

                            Column(Modifier.weight(1f)) {
                                Text(
                                    group.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (studentCount > 0) {
                                    Text(
                                        "Студентов: $studentCount",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Text("›", style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CuratorsTabScreen(
    curators: List<CuratorItem>,
    groups: List<GroupItem>,
    loading: Boolean,
    error: String?,
    modifier: Modifier = Modifier,
    onSelectGroup: (GroupItem) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Кураторы", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))

        if (loading) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(10.dp))
        }

        if (curators.isEmpty() && !loading) {
            EmptyState(
                title = if (error == null) "Кураторов пока нет" else "Список кураторов не загрузился",
                description = if (error == null) {
                    "Добавленные в базу кураторы появятся здесь."
                } else {
                    "Проверь таблицу curators и подключение к базе данных."
                },
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(curators, key = { it.id }) { curator ->
                    val group = groups.firstOrNull { it.id == curator.groupId }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UserAvatar(name = curator.name, imageUrl = null, size = 56)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    curator.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                when {
                                    group != null -> TextButton(
                                        onClick = { onSelectGroup(group) }
                                    ) {
                                        Text("Группа: ${group.name} · открыть студентов")
                                    }

                                    curator.groupId == null -> Text(
                                        "Группа не указана",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    else -> Text(
                                        "Группа не найдена (ID ${curator.groupId})",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileTabScreen(
    login: String,
    modifier: Modifier = Modifier,
    onLogout: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Профиль", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatar(name = login, imageUrl = null, size = 64)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Аккаунт", style = MaterialTheme.typography.labelLarge)
                    Text(login.ifBlank { "Неизвестный пользователь" })
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Выйти из аккаунта")
        }
    }
}

@Composable
private fun StudentSummaryCard(student: UserItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                name = student.firstName,
                imageUrl = student.imageUrl,
                size = 56
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    "${student.firstName} ${student.lastName}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text("Дата рождения: ${student.bornDate}")
            }
        }
    }
}

@Composable
private fun EmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
        SubcomposeAsyncImage(
            model = imageUrl,
            contentDescription = "Фото пользователя",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape),
            loading = {
                AvatarPlaceholder(name)
            },
            error = {
                AvatarPlaceholder(name)
            }
        )
    }
}

@Composable
private fun AvatarPlaceholder(name: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            name.trim().firstOrNull()?.uppercase() ?: "?",
            style = MaterialTheme.typography.headlineMedium
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
    var imageUrl by remember(user?.id) {
        mutableStateOf(user?.imageUrl.orEmpty())
    }
    var error by remember(user?.id) {
        mutableStateOf<String?>(null)
    }
    var saving by remember(user?.id) {
        mutableStateOf(false)
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

        val cleanImageUrl = imageUrl.trim().takeIf { it.isNotEmpty() }
        if (cleanImageUrl != null && !isHttpUrl(cleanImageUrl)) {
            error = "Вставь ссылку на изображение, начинающуюся с http:// или https://"
            return
        }

        scope.launch {
            saving = true
            error = null

            try {
                if (user == null) {
                    supabase
                        .postgrestFrom(USER_TABLE)
                        .insert(
                            NewUser(
                                firstName = firstName.trim(),
                                lastName = lastName.trim(),
                                bornDate = bornDate,
                                groupId = selectedGroup,
                                imageUrl = cleanImageUrl
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
                                imageUrl = cleanImageUrl
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
                imageUrl = imageUrl.trim().takeIf(::isHttpUrl),
                size = 104
            )
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = imageUrl,
            onValueChange = {
                imageUrl = it
                error = null
            },
            label = { Text("Ссылка на аватар") },
            placeholder = { Text("https://example.com/avatar.jpg") },
            supportingText = { Text("Вставь прямую ссылку на изображение") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(8.dp))

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

private fun isHttpUrl(value: String): Boolean {
    val uri = Uri.parse(value.trim())
    return uri.scheme in setOf("http", "https") && !uri.host.isNullOrBlank()
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
            imageUrl = user.imageUrl,
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
