package com.roozeman.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import java.time.LocalDate

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

data class TaskItem(val title: String, val done: Boolean)
data class ScheduleItem(val time: String, val label: String)
data class GoalItem(val title: String, val progress: Float, val daysLeft: Int)
data class IdeaItem(val title: String, val date: String, val tag: String)

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
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            var showAddSheet by remember { mutableStateOf(false) }
            var selectedTab by remember { mutableStateOf(0) }

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
                        0 -> HomeScreen()
                        1 -> CalendarScreen()
                        2 -> GoalsScreen()
                        3 -> IdeasScreen()
                        else -> MoreScreen()
                    }
                }

                if (showAddSheet) {
                    AddSheet(onDismiss = { showAddSheet = false })
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
fun HomeScreen() {
    val today = LocalDate.now()
    val (jy, jm, jd) = toJalali(today.year, today.monthValue, today.dayOfMonth)
    val weekdayIndex = today.dayOfWeek.value % 7
    val weekdayName = persianWeekdays[weekdayIndex]
    val monthName = persianMonths[jm - 1]

    val tasks = remember {
        mutableStateListOf(
            TaskItem("انجام کاری که امروز مهم‌تر از همه است", false),
            TaskItem("چک کردن ایمیل‌ها", true),
            TaskItem("۳۰ دقیقه پیاده‌روی", true),
            TaskItem("مطالعه ۲۰ دقیقه", false)
        )
    }
    val schedule = remember {
        listOf(
            ScheduleItem("۰۸:۰۰", "شروع روز"),
            ScheduleItem("۱۰:۰۰", "کار اصلی"),
            ScheduleItem("۱۳:۰۰", "ناهار و استراحت"),
            ScheduleItem("۱۶:۰۰", "مطالعه"),
            ScheduleItem("۲۰:۰۰", "وقت آزاد")
        )
    }

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
                        Text(text = item.label)
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
                tasks.forEachIndexed { index, task ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = task.done,
                            onCheckedChange = { checked -> tasks[index] = task.copy(done = checked) }
                        )
                        Text(text = task.title)
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
                                    .padding(4.dp)
                                    .height(36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (day != null) {
                                    val isToday = day == jd
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .then(
                                                if (isToday) Modifier
                                                else Modifier
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isToday) {
                                            Card(shape = RoundedCornerShape(10.dp)) {
                                                Box(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                ) {
                                                    Text(text = day.toString(), fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        } else {
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

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun GoalsScreen() {
    val goals = remember {
        listOf(
            GoalItem("یادگیری زبان انگلیسی", 0.6f, 12),
            GoalItem("ورزش منظم", 0.3f, 45),
            GoalItem("مطالعه ۱۲ کتاب امسال", 0.4f, 90)
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(text = "اهداف 🎯", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        goals.forEach { goal ->
            Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "🎯 ${goal.title}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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

@Composable
fun IdeasScreen() {
    var newIdea by remember { mutableStateOf("") }
    val ideas = remember {
        mutableStateListOf(
            IdeaItem("طراحی یک محصول جدید", "۱۳ شهریور", "⭐ مهم"),
            IdeaItem("پیشنهاد ویژگی جدید برای اپ", "۱۰ شهریور", "💡 ایده")
        )
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
                    OutlinedButton(onClick = { }) {
                        Text("🎙️ ضبط سریع")
                    }
                    Button(onClick = {
                        if (newIdea.isNotBlank()) {
                            ideas.add(0, IdeaItem(newIdea, "امروز", "💡 ایده"))
                            newIdea = ""
                        }
                    }) {
                        Text("ثبت")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ideas.forEach { idea ->
            Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "💡 ${idea.title}", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "${idea.date}   ${idea.tag}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun HabitsScreen(onBack: () -> Unit) {
    val habitDays = remember { mutableStateListOf(true, true, false, true, false, false, false) }
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

        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "ورزش", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    dayLabels.forEachIndexed { index, label ->
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = label.take(1), fontSize = 11.sp)
                            Checkbox(
                                checked = habitDays[index],
                                onCheckedChange = { habitDays[index] = it }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(text = "🔥 ۱۲ روز متوالی", fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun MoreScreen() {
    var openSection by remember { mutableStateOf<String?>(null) }

    if (openSection == "عادت‌ها") {
        HabitsScreen(onBack = { openSection = null })
        return
    }

    if (openSection != null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { openSection = null }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "بازگشت")
                }
                Text(text = openSection ?: "", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "این بخش به‌زودی اضافه می‌شود.")
        }
        return
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
fun AddSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "چه چیزی می‌خواهی اضافه کنی؟",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            val options = listOf("🕐 برنامه", "📋 کار", "💡 ایده", "🎯 هدف", "📝 یادداشت", "🔥 عادت")
            options.forEach { option ->
                Text(
                    text = option,
                    fontSize = 16.sp,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
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
