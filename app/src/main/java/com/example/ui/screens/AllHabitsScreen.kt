package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.Habit
import com.example.ui.HabitViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllHabitsScreen(
    viewModel: HabitViewModel
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val habits by viewModel.allHabits.collectAsState()

    var showArchivedOnly by remember { mutableStateOf(false) }
    var editingHabit by remember { mutableStateOf<Habit?>(null) }
    var showQuickAddDialog by remember { mutableStateOf(false) }

    // Progressive disclosure: Hide archived toggle until user has at least some habits configured
    val hasArchivedHabits = habits.any { it.isArchived }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        // App top header
        Text(
            text = "My Habits & Vault",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        // Optional Archived Switch Row (Progressive disclosure)
        if (hasArchivedHabits) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Show Vaulted (Archived) Habits Only",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Switch(
                    checked = showArchivedOnly,
                    onCheckedChange = {
                        viewModel.triggerTapHaptic(context)
                        showArchivedOnly = it
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.background,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        val displayedHabits = habits.filter { it.isArchived == showArchivedOnly }

        if (displayedHabits.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (showArchivedOnly) "No vault items." else "Your habits notebook is empty.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(displayedHabits, key = { it.id }) { habit ->
                    HabitSettingsCard(
                        habit = habit,
                        onEdit = {
                            viewModel.triggerTapHaptic(context)
                            editingHabit = habit
                            showQuickAddDialog = true
                        },
                        onToggleArchive = {
                            viewModel.triggerTapHaptic(context)
                            viewModel.toggleHabitArchive(habit)
                        },
                        onDelete = {
                            viewModel.triggerTapHaptic(context)
                            viewModel.deleteHabit(habit)
                        }
                    )
                }
            }
        }
    }

    val userIdentity by viewModel.userIdentity.collectAsState()
    val locationSuggestionsEnabled by viewModel.locationSuggestionsEnabled.collectAsState()

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
fun HabitSettingsCard(
    habit: Habit,
    onEdit: () -> Unit,
    onToggleArchive: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habit.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (habit.identityTag.isNotBlank()) {
                    Text(
                        text = "Identity: ${habit.identityTag}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${habit.timeOfDay.replaceFirstChar { it.uppercase() }}s • Max streak: ${habit.bestStreak}d",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Edit key
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit habit",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                // Archive key
                OutlinedButton(
                    onClick = onToggleArchive,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
                ) {
                    Text(
                        text = if (habit.isArchived) "RESTORE" else "VAULT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Delete key
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
