package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.CompletionLog
import com.example.data.Habit
import com.example.data.cheatDaySet
import com.example.ui.HabitViewModel
import com.example.ui.theme.*
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreaksProgressScreen(
    viewModel: HabitViewModel
) {
    val context = LocalContext.current
    val habits by viewModel.allActiveHabits.collectAsState()
    val logs by viewModel.allCompletionLogs.collectAsState()
    val userIdentity by viewModel.userIdentity.collectAsState()

    var selectedHabit by remember { mutableStateOf<Habit?>(null) }
    var expandedDropdown by remember { mutableStateOf(false) }

    // Auto-select first active habit if null
    LaunchedEffect(habits) {
        if (selectedHabit == null && habits.isNotEmpty()) {
            selectedHabit = habits.first()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // App top header
        Text(
            text = "Consistency & Milestones",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        if (habits.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No active habits to analyze.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            val activeHabit = selectedHabit ?: habits.firstOrNull()

            if (activeHabit != null) {
                // Habit Selector Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.triggerTapHaptic(context)
                                expandedDropdown = true
                            },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = activeHabit.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select habit",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        habits.forEach { habit ->
                            DropdownMenuItem(
                                text = { Text(habit.name, fontWeight = FontWeight.SemiBold) },
                                onClick = {
                                    viewModel.triggerTapHaptic(context)
                                    selectedHabit = habit
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                val habitLogs = logs.filter { it.habitId == activeHabit.id }

                // Scoreboard Cards Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        label = "Current Streak",
                        value = "${activeHabit.currentStreak}d",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Best Streak",
                        value = "${activeHabit.bestStreak}d",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 28-Day Grid (Loss Aversion Visuals)
                Text(
                    text = "Loss Aversion Chain (Last 28 Days)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Protect the chain. Missed days appear as empty gaps in the pattern.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Grid of 4 rows of 7 columns representing the last 28 days
                        val today = LocalDate.now()
                        val dateGridList = remember(habitLogs) {
                            (0..27).map { today.minusDays(it.toLong()) }.reversed()
                        }

                        val chunkedDays = dateGridList.chunked(7)

                        chunkedDays.forEach { rowDays ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                rowDays.forEach { date ->
                                    val dateStr = date.toString(); val strokeColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                                    val isCompleted = habitLogs.any { it.dateString == dateStr }
                                    val isExempt = date.dayOfWeek in activeHabit.cheatDaySet()

                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(
                                                color = if (isCompleted) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!isCompleted) {
                                            if (isExempt) {
                                                // Third state: faint outline/hatch for cheat days
                                                ComposeCanvas(modifier = Modifier.fillMaxSize()) {
                                                    drawRoundRect(
                                                        color = strokeColor.copy(alpha = 0.3f), // much fainter
                                                        size = size,
                                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f, 20f),
                                                        style = Stroke(
                                                            width = 2f,
                                                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                                                        )
                                                    )
                                                }
                                                Text(
                                                    text = date.dayOfMonth.toString(),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f), // quite faint
                                                    fontSize = 9.sp
                                                )
                                            } else {
                                                // Missed: empty outline to represent visual gap
                                                ComposeCanvas(modifier = Modifier.fillMaxSize()) {
                                                    drawRoundRect(
                                                        color = strokeColor,
                                                        size = size,
                                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f, 20f),
                                                        style = Stroke(3f)
                                                    )
                                                }
                                                // Mini text for day number to see history
                                                Text(
                                                    text = date.dayOfMonth.toString(),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                                    fontSize = 9.sp
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = date.dayOfMonth.toString(),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (activeHabit.targetQuantity != null) {
                    ProgressionInsights(habit = activeHabit, logs = habitLogs)
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Milestone Goals
                Text(
                    text = "Streak Milestones",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(10.dp))

                listOf(7, 30, 100).forEach { goal ->
                    val current = activeHabit.currentStreak
                    val progress = if (current >= goal) 1f else current.toFloat() / goal.toFloat()

                    MilestoneGoalRow(
                        goalDays = goal,
                        currentStreak = current,
                        progress = progress
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Social Story-sized Image Share Action
                Button(
                    onClick = {
                        viewModel.triggerTapHaptic(context)
                        shareStreakStory(context, activeHabit, userIdentity)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onBackground,
                        contentColor = MaterialTheme.colorScheme.background
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Share snapshot")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Story Snapshot")
                }
            }
        }
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueStyle: androidx.compose.ui.text.TextStyle? = null
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = valueStyle ?: MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun MilestoneGoalRow(
    goalDays: Int,
    currentStreak: Int,
    progress: Float
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 600),
        label = "milestone_slider"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Milestone",
                        tint = if (currentStreak >= goalDays) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$goalDays-Day Streak",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = if (currentStreak >= goalDays) "Completed!" else "$currentStreak / $goalDays days",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress bar (fills from light gray toward solid black/white)
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                strokeCap = StrokeCap.Round
            )
        }
    }
}

/**
 * Renders programmatic Bitmap scaled for 1080x1920 social stories and shares it via Intent
 */
fun shareStreakStory(context: Context, habit: Habit, identity: String) {
    val width = 1080
    val height = 1920
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Background - Solid Dark Aesthetic Theme
    val bgPaint = Paint().apply {
        color = AndroidColor.BLACK
        style = Paint.Style.FILL
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

    // Subtle Grid Canvas Border Decoration
    val borderPaint = Paint().apply {
        color = AndroidColor.DKGRAY
        style = Paint.Style.STROKE
        strokeWidth = 10f
    }
    canvas.drawRect(40f, 40f, (width - 40).toFloat(), (height - 40).toFloat(), borderPaint)

    // Text Painting - Identity Title
    val titlePaint = Paint().apply {
        color = AndroidColor.WHITE
        textSize = 50f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("SHOWING UP AS", (width / 2).toFloat(), 250f, titlePaint)

    // Identity Content (e.g., "A ATHLETE")
    val identityPaint = Paint().apply {
        color = AndroidColor.WHITE
        textSize = 90f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }
    canvas.drawText(identity.ifBlank { "A CONSISTENT PERSON" }.uppercase(), (width / 2).toFloat(), 380f, identityPaint)

    // Horizontal styling divider rule
    val linePaint = Paint().apply {
        color = AndroidColor.GRAY
        strokeWidth = 3f
    }
    canvas.drawLine(200f, 450f, (width - 200).toFloat(), 450f, linePaint)

    // Habit focus statement
    val labelPaint = Paint().apply {
        color = AndroidColor.LTGRAY
        textSize = 45f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("Habit Practice:", (width / 2).toFloat(), 600f, labelPaint)

    val habitNamePaint = Paint().apply {
        color = AndroidColor.WHITE
        textSize = 72f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.BOLD_ITALIC)
    }
    canvas.drawText(habit.name.uppercase(), (width / 2).toFloat(), 720f, habitNamePaint)

    // Circular Streak Display Design
    val discPaint = Paint().apply {
        color = AndroidColor.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 14f
        isAntiAlias = true
    }
    // Draw visual ring represents the momentum
    val rect = RectF(
        (width / 2 - 250).toFloat(),
        900f,
        (width / 2 + 250).toFloat(),
        1400f
    )
    canvas.drawArc(rect, 0f, 360f, false, discPaint)

    // Streak value inside the ring
    val valPaint = Paint().apply {
        color = AndroidColor.WHITE
        textSize = 140f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }
    canvas.drawText("${habit.currentStreak}", (width / 2).toFloat(), 1170f, valPaint)

    val subValPaint = Paint().apply {
        color = AndroidColor.LTGRAY
        textSize = 40f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("DAY UNBROKEN CHAIN", (width / 2).toFloat(), 1250f, subValPaint)

    // Bottom Quote Section from James Clear
    val quotePaint = Paint().apply {
        color = AndroidColor.GRAY
        textSize = 40f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.SERIF, android.graphics.Typeface.ITALIC)
    }
    canvas.drawText("\"Every action you take is a vote for the\"", (width / 2).toFloat(), 1600f, quotePaint)
    canvas.drawText("\"type of person you wish to become.\"", (width / 2).toFloat(), 1660f, quotePaint)

    // Footer
    val footerPaint = Paint().apply {
        color = AndroidColor.DKGRAY
        textSize = 32f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("ATOMIC HABITS APP", (width / 2).toFloat(), 1800f, footerPaint)

    // Save and Share Intent
    try {
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, "atomic_streak_share.png")
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()

        val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

        if (contentUri != null) {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setDataAndType(contentUri, context.contentResolver.getType(contentUri))
                putExtra(Intent.EXTRA_STREAM, contentUri)
                type = "image/png"
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Streak Report"))
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun ProgressionInsights(habit: Habit, logs: List<CompletionLog>) {
    val quantities = logs.mapNotNull { it.loggedQuantity }
    val totalLogged = quantities.sum()
    val averagePerLog = if (quantities.isNotEmpty()) quantities.average() else 0.0
    val personalBest = quantities.maxOrNull() ?: 0

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatCard(label = "Total Logged", value = "$totalLogged ${habit.targetUnit}", modifier = Modifier.weight(1f), valueStyle = MaterialTheme.typography.titleMedium)
        StatCard(label = "Daily Avg", value = "${"%.1f".format(averagePerLog)} ${habit.targetUnit}", modifier = Modifier.weight(1f), valueStyle = MaterialTheme.typography.titleMedium)
        StatCard(label = "Top Log", value = "$personalBest ${habit.targetUnit}", modifier = Modifier.weight(1f), valueStyle = MaterialTheme.typography.titleMedium)
    }
    
    Spacer(Modifier.height(24.dp))

    Text("Last 14 Days — ${habit.targetUnit.ifBlank { "Amount" } } Logged",
        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground)
    Spacer(Modifier.height(4.dp))
    Text("Each bar is a day's honest count — partial days count too. The dashed line is your target.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
    Spacer(Modifier.height(10.dp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        val target = habit.targetQuantity!!.toFloat()
        val today = LocalDate.now()
        val last14 = (0..13).map { today.minusDays(it.toLong()) }.reversed()
        val barColor = MaterialTheme.colorScheme.primary
        val trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)

        ComposeCanvas(modifier = Modifier.fillMaxWidth().height(140.dp).padding(16.dp)) {
            val barCount = last14.size
            val gap = 6.dp.toPx()
            val barWidth = (size.width - gap * (barCount - 1)) / barCount
            val chartHeight = size.height

            // Dashed target line at the height representing "100% of target"
            drawLine(
                color = trackColor.copy(alpha = 0.9f),
                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                strokeWidth = 2f,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
            )

            last14.forEachIndexed { index, date ->
                val log = logs.firstOrNull { it.dateString == date.toString() }
                val qty = log?.loggedQuantity?.toFloat() ?: 0f
                val ratio = (qty / target).coerceIn(0f, 1.4f) // allow slight overshoot to render above the line
                val barHeight = chartHeight * (ratio.coerceAtMost(1f))
                val x = index * (barWidth + gap)

                // Track (full height, faint) so empty days are still visible as a shape, not a void
                drawRoundRect(
                    color = trackColor,
                    topLeft = androidx.compose.ui.geometry.Offset(x, 0f),
                    size = androidx.compose.ui.geometry.Size(barWidth, chartHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                )
                if (qty > 0f) {
                    drawRoundRect(
                        color = if (ratio >= 1f) barColor else barColor.copy(alpha = 0.55f),
                        topLeft = androidx.compose.ui.geometry.Offset(x, chartHeight - barHeight),
                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                    )
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text(last14.first().format(DateTimeFormatter.ofPattern("MMM d")),
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            Text("Today", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        }
    }
    
    Spacer(Modifier.height(16.dp))
    
    val loggedDays = quantities.size
    val daysAtOrAboveTarget = quantities.count { it >= habit.targetQuantity!! }
    val consistencyPct = if (loggedDays > 0) (daysAtOrAboveTarget * 100 / loggedDays) else 0

    Text(
        "On $daysAtOrAboveTarget of the $loggedDays days you've logged, you hit your full target of " +
        "${habit.targetQuantity} ${habit.targetUnit} ($consistencyPct%). The other ${loggedDays - daysAtOrAboveTarget} " +
        "still kept the chain alive — showing up partial beats not showing up.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
    )

    if (habit.goalDurationDays != null) {
        val impliedTotal = habit.targetQuantity!! * habit.goalDurationDays
        val pctOfJourney = (totalLogged.toFloat() / impliedTotal.toFloat()).coerceIn(0f, 1f)

        Spacer(Modifier.height(16.dp))
        Text("$totalLogged of $impliedTotal ${habit.targetUnit} — the full ${habit.goalDurationDays}-day arc",
            style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        LinearProgressIndicator(
            progress = { pctOfJourney },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = MaterialTheme.colorScheme.onBackground,
            trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
        )
    }
}
