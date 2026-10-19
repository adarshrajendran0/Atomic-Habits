package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.Habit
import com.example.data.cheatDaySet
import com.example.data.cueTimeList
import com.example.ui.HabitViewModel
import com.example.ui.theme.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun TodayScreen(
    viewModel: HabitViewModel
) {
    val context = LocalContext.current
    val habits by viewModel.allActiveHabits.collectAsState()
    val logs by viewModel.allCompletionLogs.collectAsState()
    val userIdentity by viewModel.userIdentity.collectAsState()
    val coachingMsg by viewModel.coachingMessage.collectAsState()
    val reduceMotion by viewModel.reduceMotion.collectAsState()

    // Date navigation state (forgiving log: Today, Yesterday, Days ago)
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val dateList = remember {
        (0..4).map { LocalDate.now().minusDays(it.toLong()) }
    }

    var showQuickAddDialog by remember { mutableStateOf(false) }
    var editingHabit by remember { mutableStateOf<Habit?>(null) }

    // Progressive disclosure calculation
    val showFilters = habits.size >= 6
    var filterTimeOfDay by remember { mutableStateOf("All") }

    val formattedSelectedDateString = selectedDate.toString() // YYYY-MM-DD

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = if (userIdentity.isNotBlank()) "Showing up as" else "Habit Tracker",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold
                )
                if (userIdentity.isNotBlank()) {
                    Text(
                        text = userIdentity,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            IconButton(
                onClick = {
                    viewModel.triggerTapHaptic(context)
                    editingHabit = null
                    showQuickAddDialog = true
                },
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.onBackground, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Quick Add Habit",
                    tint = MaterialTheme.colorScheme.background
                )
            }
        }

        // Forgiving Quick Date Switcher row (reduces friction of backlogging)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(dateList) { date ->
                val isSelected = date == selectedDate
                val label = when (date) {
                    LocalDate.now() -> "Today"
                    LocalDate.now().minusDays(1) -> "Yesterday"
                    else -> date.format(DateTimeFormatter.ofPattern("E d"))
                }

                Surface(
                    modifier = Modifier.clickable {
                        viewModel.triggerTapHaptic(context)
                        selectedDate = date
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Coaching Message Alert
        coachingMsg?.let { msg ->
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Coach Tip",
                        modifier = Modifier.padding(top = 2.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "COACH TIP",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = {
                            viewModel.triggerTapHaptic(context)
                            viewModel.dismissCoachingMessage()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss Coach Tip",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }

        // Progressive disclosure filter layout
        if (showFilters) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Morning", "Afternoon", "Evening").forEach { filterType ->
                    val isSel = filterTimeOfDay == filterType
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                viewModel.triggerTapHaptic(context)
                                filterTimeOfDay = filterType
                            },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent),
                        color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f)
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = filterType,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Habits list
        val currentHour = LocalTime.now().hour
        val targetTimePeriod = when {
            currentHour in 5..11 -> "morning"
            currentHour in 12..16 -> "afternoon"
            else -> "evening"
        }
        val currentSsid = viewModel.currentWifiSsid.collectAsState().value

        val filteredHabits = habits.filter { habit ->
            if (!showFilters || filterTimeOfDay == "All") {
                true
            } else {
                habit.timeOfDay.equals(filterTimeOfDay, ignoreCase = true)
            }
        }.sortedByDescending { habit ->
            var score = if (habit.timeOfDay.equals(targetTimePeriod, ignoreCase = true)) 100 else 0
            val cues = habit.cueTimeList()
            if (cues.isNotEmpty()) {
                val minDeltaMinutes = cues.map { Math.abs(java.time.Duration.between(it, LocalTime.now()).toMinutes()) }.minOrNull() ?: 9999
                if (minDeltaMinutes <= 120) {
                    score += (200 - minDeltaMinutes.toInt())
                }
            }
            if (habit.linkedWifiSsid != null && habit.linkedWifiSsid == currentSsid) score += 500
            score
        }

        val actionableHabits = filteredHabits.filter { selectedDate.dayOfWeek !in it.cheatDaySet() }
        val restingHabits = filteredHabits.filter { selectedDate.dayOfWeek in it.cheatDaySet() }

        if (filteredHabits.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "No cues",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No cues defined for ${filterTimeOfDay.lowercase()}.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Reducing friction begins with creating a single habit.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = {
                            viewModel.triggerTapHaptic(context)
                            editingHabit = null
                            showQuickAddDialog = true
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground)
                    ) {
                        Text("Make it Obvious")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(actionableHabits, key = { it.id }) { habit ->
                    val log = logs.find { it.habitId == habit.id && it.dateString == formattedSelectedDateString }
                    val isCompleted = log != null
                    HabitTrackingItem(
                        habit = habit,
                        isCompleted = isCompleted,
                        reduceMotion = reduceMotion,
                        loggedQuantity = log?.loggedQuantity,
                        onToggle = {
                            viewModel.toggleCompletion(habit.id, formattedSelectedDateString, context)
                        },
                        onLogQuantity = { qty ->
                            viewModel.logQuantity(habit.id, formattedSelectedDateString, qty, context)
                        }
                    )
                }

                if (restingHabits.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        restingHabits.forEach { habit ->
                            Text(
                                text = "Resting today: ${habit.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    val locationSuggestionsEnabled by viewModel.locationSuggestionsEnabled.collectAsState()

    // Quick Add Dialog
    if (showQuickAddDialog) {
        QuickAddHabitDialog(
            userIdentity = userIdentity,
            existingHabit = editingHabit,
            onDismiss = {
                viewModel.triggerTapHaptic(context)
                showQuickAddDialog = false
                editingHabit = null
            },
            onSave = { existing, name, identity, time, placeText, timeSpecific, starter, stack, cheatDays, linkedWifi, targetQuantity, targetUnit, goalDurationDays, cueTimes ->
                viewModel.triggerTapHaptic(context)
                if (existing != null) {
                    viewModel.updateHabit(existing.copy(
                        name = name, 
                        identityTag = identity, 
                        timeOfDay = time, 
                        place = placeText,
                        timeOfDaySpecific = timeSpecific, 
                        starterVersion = starter, 
                        stackAnchorRef = stack,
                        cheatDays = cheatDays, 
                        linkedWifiSsid = linkedWifi,
                        targetQuantity = targetQuantity,
                        targetUnit = targetUnit,
                        goalDurationDays = goalDurationDays,
                        cueTimes = cueTimes
                    ))
                } else {
                    viewModel.addHabit(name, identity, time, placeText, timeSpecific, starter, stack, "", cheatDays, linkedWifi, targetQuantity, targetUnit, goalDurationDays, cueTimes)
                }
                showQuickAddDialog = false
                editingHabit = null
            },
            onTimeOfDayClick = {
                viewModel.triggerTapHaptic(context)
            },
            locationSuggestionsEnabled = locationSuggestionsEnabled
        )
    }
}

@Composable
fun HabitTrackingItem(
    habit: Habit,
    isCompleted: Boolean,
    reduceMotion: Boolean,
    loggedQuantity: Int?,
    onToggle: () -> Unit,
    onLogQuantity: (Int) -> Unit
) {
    // Fill/Checkmark transition (no linear motion, standard spring or fast animation based on preference)
    val checkProgress by animateFloatAsState(
        targetValue = if (isCompleted) 1f else 0f,
        animationSpec = if (reduceMotion) tween(0) else tween(durationMillis = 280),
        label = "habit_check_progress"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = habit.targetQuantity == null) { onToggle() },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (habit.targetQuantity == null) {
                    // Visual checkmark signaling purely on fill/weight
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        // Drawing outline circle & filling it smoothly
                        val primaryColor = MaterialTheme.colorScheme.primary
                        val outlineColor = MaterialTheme.colorScheme.outline
                        val backgroundColor = MaterialTheme.colorScheme.background

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val radius = size.minDimension / 2
                            val strokeWidth = 2.dp.toPx()

                            if (checkProgress < 1f) {
                                // Drawing light outline
                                drawCircle(
                                    color = outlineColor.copy(alpha = 0.5f),
                                    radius = radius - strokeWidth / 2,
                                    style = Stroke(strokeWidth)
                                )
                            }

                            // Drawing filled inner shape using spring scale progression
                            if (checkProgress > 0f) {
                                drawCircle(
                                    color = primaryColor,
                                    radius = (radius - strokeWidth) * checkProgress
                                )
                            }
                        }

                        if (isCompleted) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Completed",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = habit.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                    
                    // Implementation Intention tagline
                    val cueListString = habit.cueTimeList().joinToString(", ") { it.format(DateTimeFormatter.ofPattern("h:mm a")) }
                    val timeString = if (cueListString.isNotBlank()) "at $cueListString" else if (habit.timeOfDaySpecific.isNotBlank()) "at ${habit.timeOfDaySpecific}" else ""
                    val placeString = if (habit.place.isNotBlank()) "in ${habit.place}" else ""
                    val stackString = if (habit.stackAnchorRef.isNotBlank()) "after I ${habit.stackAnchorRef}" else ""

                    val tagLine = listOf(stackString, timeString, placeString)
                        .filter { it.isNotBlank() }
                        .joinToString(" ")
                        .replaceFirstChar { it.uppercase() }

                    if (tagLine.isNotBlank()) {
                        Text(
                            text = tagLine,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }

                    if (habit.starterVersion.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.background,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "2-Min: ${habit.starterVersion}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    
                    if (habit.goalDurationDays != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val daysIn = habit.currentStreak.coerceAtMost(habit.goalDurationDays)
                        Text("Day $daysIn of ${habit.goalDurationDays}", style = MaterialTheme.typography.labelMedium)
                        LinearProgressIndicator(
                            progress = { daysIn / habit.goalDurationDays.toFloat() },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.onBackground,
                            trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
                        )
                    }
                }

                // Streak Pill (loss aversion indicator)
                if (habit.currentStreak > 0) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.height(28.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Streak",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${habit.currentStreak}d",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }

            if (habit.targetQuantity != null) {
                Spacer(Modifier.height(16.dp))
                var quantity by remember { mutableStateOf(loggedQuantity ?: habit.targetQuantity) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { quantity = (quantity - 1).coerceAtLeast(0) }) { 
                        Text("-", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    Text("$quantity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { quantity++ }) { Icon(Icons.Default.Add, contentDescription = null) }
                    Spacer(Modifier.width(8.dp))
                    Text("/ ${habit.targetQuantity} ${habit.targetUnit}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                    Spacer(Modifier.weight(1f))
                    FilledTonalButton(onClick = { onLogQuantity(quantity) }) { Text(if (isCompleted) "Update" else "Log") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickAddHabitDialog(
    userIdentity: String,
    existingHabit: Habit? = null,
    onDismiss: () -> Unit,
    onSave: (existing: Habit?, name: String, identity: String, time: String, place: String, timeSpecific: String, starter: String, stack: String, cheatDays: String, linkedWifi: String?, targetQuantity: Int?, targetUnit: String, goalDurationDays: Int?, cueTimes: String) -> Unit,
    onTimeOfDayClick: () -> Unit,
    locationSuggestionsEnabled: Boolean
) {
    var habitName by remember { mutableStateOf(existingHabit?.name ?: "") }
    var timeSpecific by remember { mutableStateOf(existingHabit?.timeOfDaySpecific ?: "") }
    var placeText by remember { mutableStateOf(existingHabit?.place ?: "") }
    var stackAnchor by remember { mutableStateOf(existingHabit?.stackAnchorRef ?: "") }
    var starterValue by remember { mutableStateOf(existingHabit?.starterVersion ?: "") }
    var selectedTimeOfDay by remember { mutableStateOf(existingHabit?.timeOfDay?.lowercase() ?: "morning") }
    var selectedCheatDays by remember { mutableStateOf(existingHabit?.cheatDaySet() ?: emptySet()) }
    var selectedWifiSsid by remember { mutableStateOf(existingHabit?.linkedWifiSsid) }
    
    val displayFormatter = DateTimeFormatter.ofPattern("h:mm a")
    var cueTimes by remember { mutableStateOf(existingHabit?.cueTimeList()?.map { it.format(displayFormatter) } ?: emptyList<String>()) }
    var showTimePicker by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState(
        initialHour = 7,
        initialMinute = 30,
        is24Hour = false
    )

    var isGoalBased by remember { mutableStateOf(existingHabit?.targetQuantity != null) }
    var targetQuantityText by remember { mutableStateOf(existingHabit?.targetQuantity?.toString() ?: "") }
    var targetUnit by remember { mutableStateOf(existingHabit?.targetUnit ?: "") }
    var goalDurationText by remember { mutableStateOf(existingHabit?.goalDurationDays?.toString() ?: "") }
    
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (existingHabit == null) "New Atomic Habit" else "Edit Habit",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Habit Name
                OutlinedTextField(
                    value = habitName,
                    onValueChange = { habitName = it },
                    label = { Text("What habit do you want to start?") },
                    placeholder = { Text("e.g. Run, Read, Write, Meditate") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                TextButton(onClick = { isGoalBased = !isGoalBased }) {
                    Text(if (isGoalBased) "Remove target" else "Give this habit a target (optional)")
                }

                if (isGoalBased) {
                    Text(
                        "Tracking a number can sharpen a habit — but it's entirely optional. Most habits are healthier left simple.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = targetQuantityText, onValueChange = { targetQuantityText = it.filter(Char::isDigit) },
                            label = { Text("Target") }, placeholder = { Text("20") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = targetUnit, onValueChange = { targetUnit = it },
                            label = { Text("Unit") }, placeholder = { Text("pushups") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = goalDurationText, onValueChange = { goalDurationText = it.filter(Char::isDigit) },
                        label = { Text("Run this as a challenge for ___ days (optional)") },
                        placeholder = { Text("100") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Implementation intentions
                Text(
                    text = "Implementation Intention:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Text("At time(s)...", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    cueTimes.forEach { time ->
                        InputChip(
                            selected = false, onClick = {},
                            label = { Text(time) },
                            trailingIcon = {
                                IconButton(onClick = { cueTimes = cueTimes - time }, modifier = Modifier.size(18.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove $time", modifier = Modifier.size(14.dp))
                                }
                            }
                        )
                    }
                    AssistChip(
                        onClick = { showTimePicker = true },
                        label = { Text(if (cueTimes.isEmpty()) "Add a time" else "+ Add another") },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }

                if (showTimePicker) {
                    Dialog(onDismissRequest = { showTimePicker = false }) {
                        Surface(shape = RoundedCornerShape(16.dp)) {
                            Column(Modifier.padding(20.dp)) {
                                TimePicker(state = timePickerState)
                                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                    TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                                    TextButton(onClick = {
                                        val time = LocalTime.of(timePickerState.hour, timePickerState.minute)
                                        val formatted = time.format(displayFormatter)
                                        if (formatted !in cueTimes) cueTimes = cueTimes + formatted
                                        showTimePicker = false
                                    }) { Text("Set") }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = placeText,
                    onValueChange = { placeText = it },
                    label = { Text("In place...") },
                    placeholder = { Text("office") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(Modifier.height(12.dp))
                Text("Cheat days (optional)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Some days your environment just won't cooperate — mark them and they won't count against your streak.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val days = listOf(
                        java.time.DayOfWeek.MONDAY to "M", java.time.DayOfWeek.TUESDAY to "T", java.time.DayOfWeek.WEDNESDAY to "W",
                        java.time.DayOfWeek.THURSDAY to "T", java.time.DayOfWeek.FRIDAY to "F", java.time.DayOfWeek.SATURDAY to "S", java.time.DayOfWeek.SUNDAY to "S"
                    )
                    days.forEach { (day, label) ->
                        val isSelected = day in selectedCheatDays
                        Surface(
                            modifier = Modifier.size(36.dp).clickable {
                                // Assume this is a quick dialog without access to triggerTapHaptic if it needs a viewModel
                                selectedCheatDays = if (isSelected) selectedCheatDays - day else selectedCheatDays + day
                            },
                            shape = CircleShape,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(label, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (locationSuggestionsEnabled) {
                    Text("Link to a place (optional)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Tying a habit to home, work, or wherever it actually happens helps it surface at the right moment.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(8.dp))

                    val currentSsid = try {
                        com.example.location.LocationHelper.getCurrentWifiSsid(context)
                    } catch (e: Exception) { null }

                    OutlinedButton(onClick = {
                        if (currentSsid != null) selectedWifiSsid = currentSsid
                    }) {
                        Text(currentSsid?.let { "Use “$it” (the network you're on now)" } ?: "Connect to a WiFi network to link it")
                    }

                    selectedWifiSsid?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Linked to WiFi network: $it · this habit will be prioritized when you're connected to it",
                             style = MaterialTheme.typography.bodySmall
                        )
                        TextButton(onClick = { selectedWifiSsid = null }, modifier = Modifier.padding(top = 2.dp)) { 
                            Text("Remove link") 
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text("or", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
                    Spacer(Modifier.height(8.dp))

                    val backgroundLocationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
                    ) { isGranted ->
                        // In future: navigate to geofence picker or show dialog if granted
                    }
                    
                    val hasGeofence = existingHabit?.linkedLatitude != null

                    OutlinedButton(onClick = {
                        val hasBackgroundPerm = androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        
                        if (!hasBackgroundPerm && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                            backgroundLocationPermissionLauncher.launch(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        } else {
                            // Show geofence picker in a full implementation
                        }
                    }) {
                        Text(if (hasGeofence) "Change linked location" else "Link to a general location instead (uses GPS)")
                    }
                    
                    if (!hasGeofence) {
                        Text(
                            "To recognize this place even when the app isn't open, Android needs an additional permission for background location.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                }

                // Habit Stacking Anchor
                OutlinedTextField(
                    value = stackAnchor,
                    onValueChange = { stackAnchor = it },
                    label = { Text("Stack (After I...)") },
                    placeholder = { Text("e.g. brush my teeth, pour coffee") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 2-Minute Rule Version
                OutlinedTextField(
                    value = starterValue,
                    onValueChange = { starterValue = it },
                    label = { Text("2-Min Starter Version") },
                    placeholder = { Text("e.g. put on jogging suit, read 1 page") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Time of Day selection label
                Text(
                    text = "Morning / Afternoon / Evening:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("morning", "afternoon", "evening").forEach { time ->
                        val isSel = selectedTimeOfDay == time
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    onTimeOfDayClick()
                                    selectedTimeOfDay = time
                                },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isSel) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant),
                            color = if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = time.replaceFirstChar { it.lowercase() },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (habitName.isNotBlank()) {
                                onSave(
                                    existingHabit,
                                    habitName,
                                    userIdentity.ifBlank { "myself" },
                                    selectedTimeOfDay,
                                    placeText,
                                    timeSpecific,
                                    starterValue,
                                    stackAnchor,
                                    selectedCheatDays.joinToString(",") { it.name },
                                    selectedWifiSsid,
                                    if (isGoalBased) targetQuantityText.toIntOrNull() else null,
                                    if (isGoalBased) targetUnit else "",
                                    if (isGoalBased) goalDurationText.toIntOrNull() else null,
                                    cueTimes.joinToString(",")
                                )
                            }
                        },
                        enabled = habitName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(if (existingHabit == null) "Add Habit" else "Save Changes")
                    }
                }
            }
        }
    }
}
