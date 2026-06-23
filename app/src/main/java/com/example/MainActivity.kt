package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.Task
import com.example.ui.FilterType
import com.example.ui.TaskViewModel
import com.example.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                
                // Permission Request on Launch
                var hasNotificationPermission by remember {
                    mutableStateOf(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                        } else {
                            true
                        }
                    )
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = { isGranted ->
                        hasNotificationPermission = isGranted
                        if (!isGranted) {
                            Toast.makeText(
                                context,
                                "يرجى تمكين التنبيهات من الإعدادات لضمان وصول التذكيرات",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                )

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("main_screen_scaffold"),
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    PlannerDashboard(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerDashboard(
    modifier: Modifier = Modifier,
    viewModel: TaskViewModel = viewModel()
) {
    val context = LocalContext.current
    val tasks by viewModel.allTasks.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val selectedPriorityFilter by viewModel.selectedPriorityFilter.collectAsState()
    val activeAlert by viewModel.activeAlert.collectAsState()

    var showAddSheet by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }

    // Foreground Active alert handler
    activeAlert?.let { triggeredTask ->
        Dialog(onDismissRequest = { viewModel.dismissActiveAlert() }) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1F1C18)
                ),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("foreground_alert_dialog")
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Alert icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                shape = CircleShape
                            )
                            .padding(12.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = if (triggeredTask.isAppointment) "⏰ حان الآن موعد:" else "📝 تذكير بالمهمة:",
                        color = Color.Gray,
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = triggeredTask.title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    if (triggeredTask.description.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = triggeredTask.description,
                            color = Color.LightGray,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = { viewModel.dismissActiveAlert() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("dismiss_alert_button")
                    ) {
                        Text(
                            text = "حسناً، فهمت",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }

    // Filter Logic
    val filteredTasks = remember(tasks, searchQuery, selectedFilter, selectedPriorityFilter) {
        tasks.filter { task ->
            val matchesSearch = task.title.contains(searchQuery, ignoreCase = true) || 
                                task.description.contains(searchQuery, ignoreCase = true)
            
            val matchesType = when (selectedFilter) {
                FilterType.ALL -> true
                FilterType.TASK -> !task.isAppointment
                FilterType.APPOINTMENT -> task.isAppointment
            }

            val matchesPriority = when (selectedPriorityFilter) {
                "ALL" -> true
                else -> task.priority == selectedPriorityFilter
            }

            matchesSearch && matchesType && matchesPriority
        }
    }

    // Metrics calculations
    val totalCount = tasks.size
    val completedCount = tasks.count { it.isCompleted }
    val progressFraction = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Card
            HeaderSection(
                completedCount = completedCount,
                totalCount = totalCount,
                progressFraction = progressFraction
            )

            // Search and Filters
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchTasks(it) },
                    placeholder = { 
                        Text(
                            "ابحث عن المهام أو المواعيد...", 
                            color = Color.Gray,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Right
                        ) 
                    },
                    trailingIcon = { 
                        Icon(
                            Icons.Default.Search, 
                            contentDescription = "Search", 
                            tint = MaterialTheme.colorScheme.primary
                        ) 
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = Color.DarkGray
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_field"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Type Filter Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterTabButton(
                        text = "الكل",
                        selected = selectedFilter == FilterType.ALL,
                        modifier = Modifier.weight(1f),
                        testTag = "filter_all",
                        onClick = { viewModel.setFilter(FilterType.ALL) }
                    )
                    FilterTabButton(
                        text = "المهام 📝",
                        selected = selectedFilter == FilterType.TASK,
                        modifier = Modifier.weight(1f),
                        testTag = "filter_tasks",
                        onClick = { viewModel.setFilter(FilterType.TASK) }
                    )
                    FilterTabButton(
                        text = "المواعيد ⏰",
                        selected = selectedFilter == FilterType.APPOINTMENT,
                        modifier = Modifier.weight(1f),
                        testTag = "filter_appointments",
                        onClick = { viewModel.setFilter(FilterType.APPOINTMENT) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Priority Filter Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "الأولوية:",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    
                    PriorityChip(
                        label = "الكل",
                        selected = selectedPriorityFilter == "ALL",
                        color = Color.LightGray,
                        onClick = { viewModel.setPriorityFilter("ALL") }
                    )
                    PriorityChip(
                        label = "عالية 🔥",
                        selected = selectedPriorityFilter == "HIGH",
                        color = Color(0xFFFF5D62),
                        onClick = { viewModel.setPriorityFilter("HIGH") }
                    )
                    PriorityChip(
                        label = "متوسطة ⚡",
                        selected = selectedPriorityFilter == "MEDIUM",
                        color = Color(0xFFFFB64D),
                        onClick = { viewModel.setPriorityFilter("MEDIUM") }
                    )
                    PriorityChip(
                        label = "منخفضة 🌱",
                        selected = selectedPriorityFilter == "LOW",
                        color = Color(0xFF7EFF9F),
                        onClick = { viewModel.setPriorityFilter("LOW") }
                    )
                }
            }

            // Tasks List
            if (filteredTasks.isEmpty()) {
                EmptyStateView(
                    imageResId = R.drawable.img_hero_planner_1782185673354, // Checked path
                    searchQuery = searchQuery
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredTasks, key = { it.id }) { task ->
                        TaskItemCard(
                            task = task,
                            onToggleComplete = { viewModel.toggleTaskCompletion(task) },
                            onDelete = { viewModel.deleteTask(task) },
                            onEdit = {
                                taskToEdit = task
                                showAddSheet = true
                            }
                        )
                    }
                }
            }
        }

        // Floating Action Button to Add
        FloatingActionButton(
            onClick = {
                taskToEdit = null
                showAddSheet = true
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.Black,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 24.dp, start = 24.dp)
                .testTag("add_task_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Item", modifier = Modifier.size(28.dp))
        }
    }

    // Modal sheet for Add / Edit
    if (showAddSheet) {
        AddEditTaskDialog(
            task = taskToEdit,
            onDismiss = { showAddSheet = false },
            onSave = { title, description, isAppointment, dueTimestamp, priority, alertScheduled, reminderTimestamp ->
                if (taskToEdit == null) {
                    viewModel.addTask(
                        title, description, isAppointment, dueTimestamp, priority, alertScheduled, reminderTimestamp
                    )
                } else {
                    viewModel.editTask(
                        taskToEdit!!.copy(
                            title = title,
                            description = description,
                            isAppointment = isAppointment,
                            dueTimestamp = dueTimestamp,
                            priority = priority,
                            alertScheduled = alertScheduled,
                            reminderTimestamp = reminderTimestamp
                        )
                    )
                }
                showAddSheet = false
            }
        )
    }
}

@Composable
fun HeaderSection(
    completedCount: Int,
    totalCount: Int,
    progressFraction: Float
) {
    // Current Gregorian Date & Hour-based greeting
    val locale = Locale("ar")
    val dayFormat = SimpleDateFormat("EEEE", locale)
    val dateFormat = SimpleDateFormat("dd MMMM yyyy", locale)
    val today = Date()
    val dayName = dayFormat.format(today)
    val dateString = dateFormat.format(today)

    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        currentHour in 5..11 -> "صباح الخير"
        currentHour in 12..17 -> "مرحباً بك اليوم"
        else -> "مساء الخير والإنتاج"
    }

    Card(
        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("header_section")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF2C2415),
                                Color.Transparent
                            ),
                            radius = size.width,
                            center = androidx.compose.ui.geometry.Offset(size.width, 0f)
                        )
                    )
                }
                .padding(24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Greeting and Date
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = greeting,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$dayName، $dateString",
                        color = Color.LightGray,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // Beautiful brand avatar or mini icon representing calendar list
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Yowmi logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFF2E2920), shape = RoundedCornerShape(12.dp))
                        .padding(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Progress stats container
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF252932)
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(56.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { progressFraction },
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 6.dp,
                            trackColor = Color.DarkGray,
                            modifier = Modifier.fillMaxSize()
                        )
                        Text(
                            text = "${(progressFraction * 100).toInt()}%",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "معدل تنظيم اليوم",
                            color = Color.Gray,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (totalCount == 0) "لا توجد مهام حالياً" else "أنجزت $completedCount من أصل $totalCount مهام",
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FilterTabButton(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    testTag: String,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary else Color(0xFF252932)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
            .testTag(testTag)
    ) {
        Text(
            text = text,
            color = if (selected) Color.Black else Color.LightGray,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
fun PriorityChip(
    label: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) color.copy(alpha = 0.25f) else Color(0xFF1D2128)
            )
            .border(
                1.dp,
                if (selected) color else Color.DarkGray,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else Color.Gray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun EmptyStateView(
    imageResId: Int,
    searchQuery: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = imageResId),
            contentDescription = "No tasks illustration",
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(20.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (searchQuery.isNotEmpty()) "لا توجد نتائج مطابقة لبحثك" else "تطبيق مهام يومي — يومي",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (searchQuery.isNotEmpty()) "يرجى التحقق من صياغة كلمات البحث والفلترة" else "احرص على جدولة أعمالك ومواعيدك بفعالية وتفاعل مع إشعارات التنبيه لتبقى على تواصل.",
            color = Color.Gray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun TaskItemCard(
    task: Task,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    // Priority color mapping
    val priorityIndicatorColor = when (task.priority) {
        "HIGH" -> Color(0xFFFF5D62)
        "MEDIUM" -> Color(0xFFFFB64D)
        else -> Color(0xFF7EFF9F)
    }

    val priorityLabel = when (task.priority) {
        "HIGH" -> "عالية 🔥"
        "MEDIUM" -> "متوسطة ⚡"
        else -> "منخفضة 🌱"
    }

    // Format Due Date Time beautifully in Arabic style
    val format = SimpleDateFormat("hh:mm a - dd/MM", Locale("ar"))
    val dateLabel = format.format(Date(task.dueTimestamp))

    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1D2128)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            0.5.dp, 
            if (task.isCompleted) Color.DarkGray else Color.LightGray.copy(alpha = 0.15f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_item_${task.id}")
            .clickable { onEdit() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkmark button (Status)
            IconButton(
                onClick = onToggleComplete,
                modifier = Modifier.testTag("task_complete_btn_${task.id}")
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            if (task.isCompleted) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
                            shape = CircleShape
                        )
                        .border(
                            2.dp,
                            if (task.isCompleted) Color(0xFF8C98AC) else MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (task.isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = Color(0xFF8C98AC),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Center details
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp),
                horizontalAlignment = Alignment.End
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Type Badge (Task/Appointment)
                    Box(
                        modifier = Modifier
                            .background(
                                if (task.isAppointment) Color(0xFF2C253B) else Color(0xFF1D2721),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (task.isAppointment) "موعد ⏰" else "مهمة 📝",
                            color = if (task.isAppointment) Color(0xFFD6BBFF) else Color(0xFF96F2BE),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Title
                    Text(
                        text = task.title,
                        color = if (task.isCompleted) Color.Gray else Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (task.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = task.description,
                        color = Color.LightGray.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom row with timing, priority and alarm statuses
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left action buttons (Edit & Delete)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("task_delete_btn_${task.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color(0xFFFF5D62).copy(alpha = 0.85f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Metadata badges
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Alarm scheduler active status icon
                        if (task.alertScheduled && !task.isCompleted) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "تنبيه نشط",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Alarm Active",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }

                        // Priority Label Indicator
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = priorityLabel,
                                color = priorityIndicatorColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(priorityIndicatorColor, shape = CircleShape)
                            )
                        }

                        // Due date and time badge
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = dateLabel,
                                color = Color.Gray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Clock",
                                tint = Color.Gray,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditTaskDialog(
    task: Task?,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        description: String,
        isAppointment: Boolean,
        dueTimestamp: Long,
        priority: String,
        alertScheduled: Boolean,
        reminderTimestamp: Long
    ) -> Unit
) {
    var title by remember { mutableStateOf(task?.title ?: "") }
    var description by remember { mutableStateOf(task?.description ?: "") }
    var isAppointment by remember { mutableStateOf(task?.isAppointment ?: false) }
    var priority by remember { mutableStateOf(task?.priority ?: "MEDIUM") }
    
    // Simple inline interactive date/times
    var dateDaysOffset by remember { mutableStateOf(0) } // 0 = today, 1 = tomorrow, 2 = day after
    var selectedHour by remember { mutableStateOf(12) }
    var selectedMinute by remember { mutableStateOf(0) }
    var isPm by remember { mutableStateOf(true) }

    // Alarm configuration state
    var alertScheduled by remember { mutableStateOf(task?.alertScheduled ?: true) }

    // Initialize hour details on Edit Mode
    LaunchedEffect(task) {
        if (task != null) {
            val calendar = Calendar.getInstance().apply { timeInMillis = task.dueTimestamp }
            selectedHour = calendar.get(Calendar.HOUR)
            if (selectedHour == 0) selectedHour = 12
            selectedMinute = calendar.get(Calendar.MINUTE)
            isPm = calendar.get(Calendar.AM_PM) == Calendar.PM

            // Evaluate dateDaysOffset
            val todayCal = Calendar.getInstance()
            val diffMs = task.dueTimestamp - todayCal.timeInMillis
            val diffDays = (diffMs / (1000 * 60 * 60 * 24)).toInt()
            dateDaysOffset = when {
                diffDays <= 0 -> 0
                diffDays == 1 -> 1
                else -> 2
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1D2128)
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .verticalScroll(rememberScrollState())
                .testTag("add_edit_sheet")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Header Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                    Text(
                        text = if (task == null) "إضافة حدث جديد" else "تعديل الحدث",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Type Toggle Selection
                Text(
                    text = "نوع الحدث",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isAppointment) Color(0xFF2C253B) else Color(0xFF121418))
                            .border(1.dp, if (isAppointment) Color(0xFFB18EFF) else Color.DarkGray, shape = RoundedCornerShape(12.dp))
                            .clickable { isAppointment = true }
                            .padding(vertical = 12.dp)
                            .testTag("select_appointment_type")
                    ) {
                        Text(
                            text = "موعد وعقد اجتماع ⏰",
                            color = if (isAppointment) Color(0xFFD6BBFF) else Color.Gray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (!isAppointment) Color(0xFF1D2D24) else Color(0xFF121418))
                            .border(1.dp, if (!isAppointment) Color(0xFF7EFF9F) else Color.DarkGray, shape = RoundedCornerShape(12.dp))
                            .clickable { isAppointment = false }
                            .padding(vertical = 12.dp)
                            .testTag("select_task_type")
                    ) {
                        Text(
                            text = "مهمة يومية قياسية 📝",
                            color = if (!isAppointment) Color(0xFF96F2BE) else Color.Gray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Form text fields
                Text(
                    text = "عنوان الحدث",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.DarkGray
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_title_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "التفاصيل والوصف",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.DarkGray
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .testTag("task_desc_input"),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Date Selection Buttons
                Text(
                    text = "تاريخ الموعد/المهمة",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("اليوم", "غداً", "اليوم التالي").forEachIndexed { index, name ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (dateDaysOffset == index) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color(0xFF121418)
                                )
                                .border(
                                    1.dp,
                                    if (dateDaysOffset == index) MaterialTheme.colorScheme.primary else Color.DarkGray,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { dateDaysOffset = index }
                                .padding(vertical = 10.dp)
                        ) {
                            Text(
                                text = name,
                                color = if (dateDaysOffset == index) MaterialTheme.colorScheme.primary else Color.Gray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Hours and Minutes Increment Selector (Reliable & beautifully custom responsive UI)
                Text(
                    text = "الوقت المختار",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF121418), shape = RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    // AM/PM Switch
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Button(
                            onClick = { isPm = !isPm },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPm) Color(0xFF32281A) else Color(0xFF1E262A)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isPm) "مساءً PM" else "صباحاً AM",
                                color = if (isPm) Color(0xFFFFB64D) else Color(0xFF7EFF9F),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Minutes
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            selectedMinute = (selectedMinute + 5) % 60
                        }) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Add minutes", tint = Color.LightGray)
                        }
                        Text(
                            text = String.format("%02d", selectedMinute),
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        IconButton(onClick = {
                            selectedMinute = if (selectedMinute >= 5) selectedMinute - 5 else 55
                        }) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Sub minutes", tint = Color.LightGray)
                        }
                    }

                    Text(":", color = Color.Gray, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)

                    // Hours
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            selectedHour = if (selectedHour < 12) selectedHour + 1 else 1
                        }) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Add hours", tint = Color.LightGray)
                        }
                        Text(
                            text = String.format("%02d", selectedHour),
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        IconButton(onClick = {
                            selectedHour = if (selectedHour > 1) selectedHour - 1 else 12
                        }) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Sub hours", tint = Color.LightGray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Priority Selection
                Text(
                    text = "مستوى الأولوية والاهتمام",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val priorities = listOf(
                        "LOW" to ("منخفض 🌱" to Color(0xFF7EFF9F)),
                        "MEDIUM" to ("متوسط ⚡" to Color(0xFFFFB64D)),
                        "HIGH" to ("عالي 🔥" to Color(0xFFFF5D62))
                    )
                    
                    priorities.forEach { (key, details) ->
                        val (label, tint) = details
                        val isSel = priority == key
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSel) tint.copy(alpha = 0.2f) else Color(0xFF121418)
                                )
                                .border(
                                    1.dp,
                                    if (isSel) tint else Color.DarkGray,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { priority = key }
                                .padding(vertical = 10.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (isSel) Color.White else Color.Gray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Toggle Alert Alarm Notifications Active
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF121418), shape = RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = alertScheduled,
                        onCheckedChange = { alertScheduled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color.DarkGray
                        )
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "تفعيل المنبه والتنبيهات",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "إرسال إشعار للنظام عند حان الموعد",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Alerts Active",
                            tint = if (alertScheduled) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons (Confirm / Cancel)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, Color.DarkGray),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("إلغاء", color = Color.LightGray, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (title.trim().isEmpty()) {
                                return@Button
                            }

                            // Calculate Target dueTimestamp
                            val calendar = Calendar.getInstance()
                            calendar.add(Calendar.DAY_OF_YEAR, dateDaysOffset)
                            
                            // Adjust Hours / Minutes based on selections
                            var militaryHour = selectedHour
                            if (isPm) {
                                if (militaryHour < 12) militaryHour += 12
                            } else {
                                if (militaryHour == 12) militaryHour = 0
                            }
                            
                            calendar.set(Calendar.HOUR_OF_DAY, militaryHour)
                            calendar.set(Calendar.MINUTE, selectedMinute)
                            calendar.set(Calendar.SECOND, 0)
                            calendar.set(Calendar.MILLISECOND, 0)

                            val calculatedDueTimestamp = calendar.timeInMillis
                            
                            // Let the reminder be set exactly at the task's dueTimestamp
                            val calculatedReminderTimestamp = if (alertScheduled) calculatedDueTimestamp else 0L

                            onSave(
                                title.trim(),
                                description.trim(),
                                isAppointment,
                                calculatedDueTimestamp,
                                priority,
                                alertScheduled,
                                calculatedReminderTimestamp
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp)
                            .testTag("save_task_btn")
                    ) {
                        Text(
                            text = if (task == null) "جدولة وتأكيد" else "تعديل الحدث",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}
