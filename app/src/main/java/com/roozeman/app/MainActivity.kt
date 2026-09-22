package com.roozeman.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
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

fun toJalali(gy: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
    val gDaysInMonth = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    var gy2 = if (gm > 2) gy + 1 else gy
    var days = 355666 + (365 * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) +
            ((gy2 + 399) / 400) + gd + gDaysInMonth[gm - 1]
    if (gm > 2 && ((gy % 4 == 0 && gy % 100 != 0) || gy % 400 == 0)) days++
    var jy = -1595 + (33 * (days / 12053))
    days %= 12053
    jy += 4 * (days / 1461)
    days %= 1461
    if (days > 365) {
        jy += (days - 1) / 365
        days = (days - 1) % 365
    }
    val jm: Int
    val jd: Int
    if (days < 186) {
        jm = 1 + (days / 31)
        jd = 1 + (days % 31)
    } else {
        jm = 7 + ((days - 186) / 30)
        jd = 1 + ((days - 186) % 30)
    }
    return Triple(jy, jm, jd)
}

val persianMonths = listOf(
    "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
    "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
)
val persianWeekdays = listOf(
    "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه", "شنبه"
)

data class TaskItem(val title: String, val done: Boolean)
data class ScheduleItem(val time: String, val label: String)

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

            Scaffold(
                bottomBar = { RoozemanBottomBar() },
                floatingActionButton = {
                    FloatingActionButton(onClick = { showAddSheet = true }) {
                        Icon(Icons.Default.Add, contentDescription = "افزودن")
                    }
                }
            ) { padding ->
                HomeScreen(modifier = Modifier.padding(padding))

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
fun HomeScreen(modifier: Modifier = Modifier) {
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
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "سلام مهدی 👋",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "$weekdayName، $jd $monthName $jy",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "⭐ مهم‌ترین کار امروز",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = tasks.firstOrNull()?.title ?: "کاری ثبت نشده")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "🕐 برنامه امروز",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
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

        Text(
            text = "📋 کارهای امروز",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
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
                            onCheckedChange = { checked ->
                                tasks[index] = task.copy(done = checked)
                            }
                        )
                        Text(text = task.title)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "پیشرفت امروز",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "${(progress * 100).toInt()}٪")

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
            val options = listOf(
                "🕐 برنامه", "📋 کار", "💡 ایده", "🎯 هدف", "📝 یادداشت", "🔥 عادت"
            )
            options.forEach { option ->
                Text(
                    text = option,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun RoozemanBottomBar() {
    var selected by remember { mutableStateOf(0) }
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
                onClick = { selected = index },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) }
            )
        }
    }
}
