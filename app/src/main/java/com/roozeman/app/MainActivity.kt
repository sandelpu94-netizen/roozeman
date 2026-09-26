package com.roozeman.app

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.LayoutDirection
import java.time.LocalDate
import java.util.Calendar

fun toJalali(gyIn: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
    val gDaysInMonth = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
    var jy = if (gyIn <= 1600) 0 else 979
    val gy2 = if (gyIn <= 1600) gyIn - 621 else gyIn - 1600
    val gy3 = if (gm > 2) gy2 + 1 else gy2
    var days = (365 * gy2) + ((gy3 + 3) / 4) - ((gy3 + 99) / 100) + ((gy3 + 399) / 400) - 80 + gd + gDaysInMonth[gm - 1]
    jy += 33 * (days / 12053)
    days %= 12053
    jy += 4 * (days / 1461)
    days %= 1461
    if (days > 365) {
        jy += (days - 1) / 365
        days = (days - 1) % 365
    }
    val jm = if (days < 186) 1 + (days / 31) else 7 + ((days - 186) / 30)
    val jd = 1 + (if (days < 186) (days % 31) else ((days - 186) % 30))
    return Triple(jy, jm, jd)
}

val persianMonths = listOf(
    "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
    "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
)
val persianWeekdays = listOf(
    "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه", "شنبه"
)
val persianWeekdaysShort = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")


private val RoozemanLightColors = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFF03A9A4),
    background = Color(0xFFFAF8FC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF1ECF6)
)
private val RoozemanDarkColors = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    secondary = Color(0xFF4DD0CB),
    background = Color(0xFF141218),
    surface = Color(0xFF1D1B20)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RoozemanApp()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoozemanApp() {
    var darkModeSetting by remember { mutableStateOf(0) }
    val isDark = when (darkModeSetting) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }

    MaterialTheme(
        colorScheme = if (isDark) RoozemanDarkColors else RoozemanLightColors
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            val context = LocalContext.current
            val dbHelper = remember { DbHelper(context) }
            val db = remember { dbHelper.writableDatabase }

            val notifPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) {}
            LaunchedEffect(Unit) {
                createNotificationChannel(context)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            var tasksVersion by remember { mutableStateOf(0) }
            var ideasVersion by remember { mutableStateOf(0) }
            var goalsVersion by remember { mutableStateOf(0) }
            var habitsVersion by remember { mutableStateOf(0) }
            var notesVersion by remember { mutableStateOf(0) }
            var scheduleVersion by remember { mutableStateOf(0) }

            var showAddSheet by remember { mutableStateOf(false) }
            var selectedTab by remember { mutableStateOf(0) }

            val onResetAll = {
                resetAllData(db)
                tasksVersion += 1
                ideasVersion += 1
                goalsVersion += 1
                habitsVersion += 1
                notesVersion += 1
            }

            Scaffold(
                bottomBar = {
                    RoozemanBottomBar(selected = selectedTab, onSelect = { selectedTab = it })
                },
                floatingActionButton = {
                    FloatingActionButton(onClick = { showAddSheet = true }) {
                        Icon(Icons.Default.Add, contentDescription = "افزودن")
                    }
                }
            ) { padding ->
                Box(modifier = Modifier.padding(padding)) {
                    when (selectedTab) {
                        0 -> HomeScreen(db, tasksVersion, scheduleVersion, onTasksChanged = { tasksVersion += 1 }, onScheduleChanged = { scheduleVersion += 1 })
                        1 -> CalendarScreen()
                        2 -> GoalsScreen(db, goalsVersion, onGoalsChanged = { goalsVersion += 1 })
                        3 -> IdeasScreen(db, ideasVersion, onIdeasChanged = { ideasVersion += 1 })
                        else -> MoreScreen(
                            db = db,
                            habitsVersion = habitsVersion,
                            onHabitsChanged = { habitsVersion += 1 },
                            notesVersion = notesVersion,
                            onNotesChanged = { notesVersion += 1 },
                            darkModeSetting = darkModeSetting,
                            onDarkModeChange = { darkModeSetting = it },
                            onResetAll = onResetAll
                        )
                    }
                }

                if (showAddSheet) {
                    AddSheet(
                        onDismiss = { showAddSheet = false },
                        onAddTask = { title, recurrence, rh, rm ->
                            val newId = insertTask(db, title, recurrence, rh, rm)
                            tasksVersion += 1
                            if (rh != null && rm != null) scheduleTaskReminder(context, newId, rh, rm)
                        },
                        onAddIdea = { title -> insertIdea(db, title); ideasVersion += 1 },
                        onAddGoal = { title, recurrence -> insertGoal(db, title, recurrence); goalsVersion += 1 },
                        onAddNote = { text -> insertNote(db, text); notesVersion += 1 },
                        onAddSchedule = { time, label -> insertSchedule(db, time, label); scheduleVersion += 1 },
                        onAddHabit = { title -> insertHabit(db, title); habitsVersion += 1 }
                    )
                }
            }
        }
    }
}

@Composable
fun isSystemInDarkTheme(): Boolean {
    return androidx.compose.foundation.isSystemInDarkTheme()
}
@Composable
fun HomeScreen(db: android.database.sqlite.SQLiteDatabase, version: Int, scheduleVersion: Int, onTasksChanged: () -> Unit, onScheduleChanged: () -> Unit) {
    val context = LocalContext.current
    val tasks = remember(version) { loadTasks(db) }

    val today = LocalDate.now()
    val (jy, jm, jd) = toJalali(today.year, today.monthValue, today.dayOfMonth)
    val weekdayIndex = today.dayOfWeek.value % 7
    val weekdayName = persianWeekdays[weekdayIndex]
    val monthName = persianMonths[jm - 1]


    val schedule = remember(scheduleVersion) { loadSchedule(db) }
    val doneCount = tasks.count { it.done }
    val progress = if (tasks.isNotEmpty()) doneCount.toFloat() / tasks.size else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(text = "سلام مهدی 👋", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(
            text = "$weekdayName، $jd $monthName $jy",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "⭐ مهم‌ترین کار امروز", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = tasks.firstOrNull()?.title ?: "کاری ثبت نشده")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "🕐 برنامه امروز", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                schedule.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = item.time, fontWeight = FontWeight.Medium)
                        Text(text = item.label, modifier = Modifier.weight(1f))
                        TextButton(onClick = { deleteSchedule(db, item.id); onScheduleChanged() }) { Text("✕") }
                    }
                    if (index != schedule.lastIndex) {
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "📋 کارهای امروز", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (tasks.isEmpty()) {
                    Text(text = "هنوز کاری ثبت نشده")
                }
                tasks.forEach { task ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = task.done,
                            onCheckedChange = { checked ->
                                val newId = updateTaskDone(db, task.id, checked)
                                if (checked && task.reminderHour != null) {
                                    cancelTaskReminder(context, task.id)
                                }
                                if (newId != null && task.reminderHour != null && task.reminderMinute != null) {
                                    scheduleTaskReminder(context, newId, task.reminderHour, task.reminderMinute)
                                }
                                onTasksChanged()
                            }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = task.title)
                            if (task.recurrence == "monthly" || task.reminderHour != null) {
                                Row {
                                    if (task.recurrence == "monthly") {
                                        Text(text = "🔁 ماهانه", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    if (task.reminderHour != null) {
                                        Text(
                                            text = "⏰ ${"%02d".format(task.reminderHour)}:${"%02d".format(task.reminderMinute ?: 0)}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        TextButton(onClick = {
                            cancelTaskReminder(context, task.id)
                            deleteTask(db, task.id)
                            onTasksChanged()
                        }) {
                            Text("✕")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "پیشرفت امروز", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(10.dp),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "${(progress * 100).toInt()}٪")

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun CalendarScreen() {
    val today = LocalDate.now()
    val (jy, jm, jd) = toJalali(today.year, today.monthValue, today.dayOfMonth)
    val monthName = persianMonths[jm - 1]
    var selectedDay by remember { mutableStateOf(jd) }

    val firstDayGregorian = today.minusDays((jd - 1).toLong())
    var daysInMonth = 0
    var cursor = firstDayGregorian
    while (true) {
        val (_, mm, _) = toJalali(cursor.year, cursor.monthValue, cursor.dayOfMonth)
        if (mm != jm) break
        daysInMonth++
        cursor = cursor.plusDays(1)
    }
    val firstWeekdayIndex = firstDayGregorian.dayOfWeek.value % 7
    val leadingBlanks = (firstWeekdayIndex + 1) % 7

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(text = "تقویم 📅", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "$monthName $jy", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))

        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    persianWeekdaysShort.forEach { d ->
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Text(text = d, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                val cells = mutableListOf<Int?>()
                repeat(leadingBlanks) { cells.add(null) }
                for (day in 1..daysInMonth) cells.add(day)
                while (cells.size % 7 != 0) cells.add(null)

                cells.chunked(7).forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        week.forEach { day ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(3.dp)
                                    .height(40.dp)
                                    .then(
                                        if (day != null) Modifier.clickable { selectedDay = day } else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (day != null) {
                                    val isToday = day == jd
                                    val isSelected = day == selectedDay && !isToday
                                    when {
                                        isToday -> {
                                            Card(shape = RoundedCornerShape(10.dp)) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(horizontal = 4.dp, vertical = 4.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(text = day.toString(), fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                        isSelected -> {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(text = day.toString())
                                            }
                                        }
                                        else -> {
                                            Text(text = day.toString())
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "روز انتخاب‌شده: $selectedDay $monthName", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun GoalsScreen(db: android.database.sqlite.SQLiteDatabase, version: Int, onGoalsChanged: () -> Unit) {
    val goals = remember(version) { loadGoals(db) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(text = "اهداف 🎯", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        if (goals.isEmpty()) {
            Text(text = "هنوز هدفی ثبت نشده")
        }
        goals.forEach { goal ->
            Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "🎯 ${goal.title}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            if (goal.recurrence == "monthly") {
                                Text(text = "🔁 هدف ماهانه", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        TextButton(onClick = {
                            deleteGoal(db, goal.id)
                            onGoalsChanged()
                        }) {
                           Text("✕")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { goal.progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "${(goal.progress * 100).toInt()}٪  •  ${goal.daysLeft} روز تا هدف", fontSize = 13.sp)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdeasScreen(db: android.database.sqlite.SQLiteDatabase, version: Int, onIdeasChanged: () -> Unit) {
    val ideas = remember(version) { loadIdeas(db) }
    var newIdea by remember { mutableStateOf("") }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val text = matches?.firstOrNull()
            if (text != null) {
                newIdea = text
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(text = "ایده‌های من 💡", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = newIdea,
                    onValueChange = { newIdea = it },
                    placeholder = { Text("💭 ایده‌ای که الان به ذهنت رسید را بنویس...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedButton(onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
                        try {
                            speechLauncher.launch(intent)
                        } catch (e: Exception) {
                        }
                    }) {
                        Text("🎙️ ضبط سریع")
                    }
                    Button(onClick = {
                        if (newIdea.isNotBlank()) {
                            insertIdea(db, newIdea)
                            onIdeasChanged()
                            newIdea = ""
                        }
                    }) {
                        Text("ثبت")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (ideas.isEmpty()) {
            Text(text = "هنوز ایده‌ای ثبت نشده")
        }
        ideas.forEach { idea ->
            key(idea.id) {
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart || value == SwipeToDismissBoxValue.StartToEnd) {
                            deleteIdea(db, idea.id)
                            onIdeasChanged()
                            true
                        } else {
                            false
                        }
                    }
                )
                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(20.dp))
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text("🗑️ حذف", color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                        }
                    }
                ) {
                    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "💡 ${idea.title}", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = "${idea.date}   ${idea.tag}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun HabitsScreen(db: android.database.sqlite.SQLiteDatabase, version: Int, onHabitsChanged: () -> Unit, onBack: () -> Unit) {
    val habits = remember(version) { loadHabits(db) }
    val dayLabels = listOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "بازگشت")
            }
            Text(text = "عادت‌های امروز 🔥", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))

        habits.forEach { habit ->
            Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = habit.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        TextButton(onClick = { deleteHabit(db, habit.id); onHabitsChanged() }) { Text("✕") }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        dayLabels.forEachIndexed { index, label ->
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = label.take(1), fontSize = 11.sp)
                                Checkbox(
                                    checked = habit.days[index],
                                    onCheckedChange = { checked ->
                                        updateHabitDay(db, habit.id, index, checked)
                                        onHabitsChanged()
                                    }
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "🔥 " + habit.streak.toString() + " روز متوالی", fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun NotesScreen(db: android.database.sqlite.SQLiteDatabase, version: Int, onNotesChanged: () -> Unit, onBack: () -> Unit) {
    val notes = remember(version) { loadNotes(db) }
    var newNote by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "بازگشت")
            }
            Text(text = "یادداشت‌ها 📝", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))

        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = newNote,
                    onValueChange = { newNote = it },
                    placeholder = { Text("یادداشت جدید...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        if (newNote.isNotBlank()) {
                            insertNote(db, newNote)
                            onNotesChanged()
                            newNote = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("ثبت یادداشت")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (notes.isEmpty()) {
            Text(text = "هنوز یادداشتی ثبت نشده")
        }
        notes.forEach { note ->
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = note.text, modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        deleteNote(db, note.id)
                        onNotesChanged()
                    }) {
                        Text("✕")
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun ReportScreen(db: android.database.sqlite.SQLiteDatabase, onBack: () -> Unit) {
    val tasks = loadTasks(db)
    val goals = loadGoals(db)
    val habits = loadHabits(db)
    val ideas = loadIdeas(db)

    val doneTasks = tasks.count { it.done }
    val avgGoalProgress = if (goals.isNotEmpty()) (goals.map { it.progress }.average() * 100).toInt() else 0
    val bestStreak = habits.maxOfOrNull { it.streak } ?: 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "بازگشت")
            }
            Text(text = "گزارش 📊", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))

        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "کارها", fontWeight = FontWeight.Bold) 
             Spacer(modifier = Modifier.height(4.dp))
                Text(text = "$doneTasks از ${tasks.size} کار انجام شده")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "اهداف", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "${goals.size} هدف ثبت‌شده، میانگین پیشرفت $avgGoalProgress٪")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "عادت‌ها", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "بهترین رکورد: $bestStreak روز متوالی")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "ایده‌ها", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "${ideas.size} ایده ثبت‌شده")
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun SettingsScreen(
    darkModeSetting: Int,
    onDarkModeChange: (Int) -> Unit,
    onResetAll: () -> Unit,
    onBack: () -> Unit
) {
    var showConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "بازگشت")
            }
            Text(text = "تنظیمات ⚙️", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(20.dp))

        Text(text = "ظاهر برنامه", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val options = listOf("خودکار" to 0, "روشن" to 1, "تاریک" to 2)
            options.forEach { (label, value) ->
                val selected = darkModeSetting == value
                Button(
                    onClick = { onDarkModeChange(value) },
                    colors = if (selected) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
                ) {
                    Text(label)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(text = "داده‌ها", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = { showConfirm = true },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("پاک کردن همه اطلاعات")
        }

        Spacer(modifier = Modifier.height(80.dp))
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("پاک کردن اطلاعات") },
            text = { Text("همه کارها، ایده‌ها، اهداف و یادداشت‌ها پاک می‌شوند. مطمئنی؟") },
            confirmButton = {
                TextButton(onClick = {
                    onResetAll()
                    showConfirm = false
                }) {
                    Text("بله، پاک کن")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text("انصراف")
                }
            }
        )
    }
}

@Composable
fun MoreScreen(
    db: android.database.sqlite.SQLiteDatabase,
    habitsVersion: Int,
    onHabitsChanged: () -> Unit,
    notesVersion: Int,
    onNotesChanged: () -> Unit,
    darkModeSetting: Int,
    onDarkModeChange: (Int) -> Unit,
    onResetAll: () -> Unit
) {
    var openSection by remember { mutableStateOf<String?>(null) }

    when (openSection) {
        "عادت‌ها" -> {
            HabitsScreen(db, habitsVersion, onHabitsChanged, onBack = { openSection = null })
            return
        }
        "گزارش" -> {
            ReportScreen(db, onBack = { openSection = null })
            return
        }
        "یادداشت‌ها" -> {
            NotesScreen(db, notesVersion, onNotesChanged, onBack = { openSection = null })
            return
        }
        "تنظیمات" -> {
            SettingsScreen(darkModeSetting, onDarkModeChange, onResetAll, onBack = { openSection = null })
            return
        }
    }

    val menuItems = listOf("🔥 عادت‌ها", "📊 گزارش", "📝 یادداشت‌ها", "⚙️ تنظیمات")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(text = "بیشتر ☰", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        menuItems.forEach { item ->
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { openSection = item.substringAfter(" ") }
            ) {
                Text(
                    text = item,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSheet(
    onDismiss: () -> Unit,
    onAddTask: (String, String, Int?, Int?) -> Unit,
    onAddIdea: (String) -> Unit,
    onAddGoal: (String, String) -> Unit,
    onAddNote: (String) -> Unit,
    onAddSchedule: (String, String) -> Unit,
    onAddHabit: (String) -> Unit
) {
    var step by remember { mutableStateOf("menu") }
    var inputText by remember { mutableStateOf("") }
    var reminderEnabled by remember { mutableStateOf(false) }
    var reminderHour by remember { mutableStateOf(9) }
    var reminderMinute by remember { mutableStateOf(0) }
    var recurring by remember { mutableStateOf(false) }
    val ctx = LocalContext.current

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            when (step) {
                "menu" -> {
                    Text(
                        text = "چه چیزی می‌خواهی اضافه کنی؟",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    val options = listOf(
                        "🕐 برنامه" to "schedule",
                        "📋 کار" to "task",
                        "💡 ایده" to "idea",
                        "🎯 هدف" to "goal",
                        "📝 یادداشت" to "note",
                        "🔥 عادت" to "habit"
                    )
                    options.forEach { (label, target) ->
                        Text(
                            text = label,
                            fontSize = 16.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { step = target }
                                .padding(vertical = 12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
                "soon" -> {
                    Text(text = "این بخش به‌زودی اضافه می‌شود.", fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { step = "menu" }) {
                        Text("بازگشت")
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
                "task" -> {
                    Text(text = "افزودن کار جدید", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("متن را بنویس...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = reminderEnabled,
                            onClick = {
                                TimePickerDialog(
                                    ctx,
                                    { _, h, m -> reminderHour = h; reminderMinute = m; reminderEnabled = true },
                                    reminderHour, reminderMinute, true
                                ).show()
                            },
                            label = {
                                Text(
                                    if (reminderEnabled)
                                        "⏰ ${"%02d".format(reminderHour)}:${"%02d".format(reminderMinute)}"
                                    else "⏰ یادآوری"
                                )
                            }
                        )
                        FilterChip(
                            selected = recurring,
                            onClick = { recurring = !recurring },
                            label = { Text("🔁 تکرار ماهانه") }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                onAddTask(
                                    inputText,
                                    if (recurring) "monthly" else "none",
                                    if (reminderEnabled) reminderHour else null,
                                    if (reminderEnabled) reminderMinute else null
                                )
                                inputText = ""
                                reminderEnabled = false
                                recurring = false
                                step = "menu"
                                onDismiss()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ثبت")
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
                "goal" -> {
                    Text(text = "افزودن هدف جدید", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("متن را بنویس...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FilterChip(
                        selected = recurring,
                        onClick = { recurring = !recurring },
                        label = { Text("🔁 هدف ماهانه (تکرارشونده)") }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                onAddGoal(inputText, if (recurring) "monthly" else "none")
                                inputText = ""
                                recurring = false
                                step = "menu"
                                onDismiss()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ثبت")
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
                else -> {
                    val title = when (step) {
                        "idea" -> "افزودن ایده جدید"
                        "note" -> "افزودن یادداشت جدید"
                        "schedule" -> "افزودن برنامه (مثال: ۱۸:۰۰ ورزش)"
                        "habit" -> "افزودن عادت جدید"
                        else -> "افزودن"
                    }
                    Text(text = title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("متن را بنویس...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                when (step) {
                                    "idea" -> onAddIdea(inputText)
                                    "note" -> onAddNote(inputText)
                                    "schedule" -> {
                                        val parts = inputText.trim().split(" ", limit = 2)
                                        onAddSchedule(parts.getOrElse(0) { "" }, parts.getOrElse(1) { "" })
                                    }
                                    "habit" -> onAddHabit(inputText)
                                }
                                inputText = ""
                                step = "menu"
                                onDismiss()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ثبت")
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
fun RoozemanBottomBar(selected: Int, onSelect: (Int) -> Unit) {
    val items = listOf(
        Triple("خانه", Icons.Default.Home, 0),
        Triple("تقویم", Icons.Default.DateRange, 1),
        Triple("اهداف", Icons.Default.Star, 2),
        Triple("ایده‌ها", Icons.Default.Info, 3),
        Triple("بیشتر", Icons.Default.Menu, 4)
    )
    NavigationBar {
        items.forEach { (label, icon, index) ->
            NavigationBarItem(
                selected = selected == index,
                onClick = { onSelect(index) },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) }
            )
        }
    }
}   
