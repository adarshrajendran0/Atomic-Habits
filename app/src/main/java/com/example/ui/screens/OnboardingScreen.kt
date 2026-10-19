package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.data.Habit
import com.example.ui.HabitViewModel
import com.example.ui.theme.Black
import com.example.ui.theme.Gray10
import com.example.ui.theme.Gray30
import com.example.ui.theme.Gray60
import com.example.ui.theme.White

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    viewModel: HabitViewModel,
    onOnboardingComplete: () -> Unit
) {
    var step by remember { mutableStateOf(1) }
    var selectedIdentity by remember { mutableStateOf("") }
    var customIdentity by remember { mutableStateOf("") }
    
    // Step 2 Habit Creation State
    var habitName by remember { mutableStateOf("") }
    var timeOfDay by remember { mutableStateOf("morning") }
    var timeOfDaySpecific by remember { mutableStateOf("8:00 AM") }
    var place by remember { mutableStateOf("my desk") }
    var stackAnchorRef by remember { mutableStateOf("") }
    var starterVersion by remember { mutableStateOf("") }
    
    val identities = listOf(
        IdentityOption("a Runner", "exercise", "Run 30 minutes", "Put on running shoes", "waking up"),
        IdentityOption("a Reader", "reading", "Read 20 pages", "Read 1 page", "making coffee"),
        IdentityOption("a Writer", "creativity", "Write 1000 words", "Write 1 sentence", "sitting at desk"),
        IdentityOption("an Early Riser", "mindfulness", "Meditate 10 minutes", "Sit still on cushion", "brushing teeth")
    )

    val currentIdentityText = if (selectedIdentity == "Custom") customIdentity else selectedIdentity

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> -width } + fadeOut()
                },
                label = "onboarding_step_anim"
            ) { targetStep ->
                when (targetStep) {
                    1 -> {
                        // Step 1: Identity Selection
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Who do you want to become?",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Atomic Habits start with identity, not results. Focus on the kind of person you want to become.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(32.dp))

                            identities.forEach { option ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .clickable {
                                            selectedIdentity = option.title
                                            habitName = option.defaultHabit
                                            starterVersion = option.defaultStarter
                                            stackAnchorRef = option.defaultAnchor
                                            step = 2
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selectedIdentity == option.title) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Become ${option.title}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (selectedIdentity == option.title) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "Example: ${option.defaultHabit}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (selectedIdentity == option.title) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        }
                                        if (selectedIdentity == option.title) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Custom Identity option
                            OutlinedTextField(
                                value = customIdentity,
                                onValueChange = {
                                    customIdentity = it
                                    selectedIdentity = "Custom"
                                },
                                label = { Text("Or type a custom identity...") },
                                placeholder = { Text("e.g. Creator, Athlete, Calm Person") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.onBackground,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    if (customIdentity.isNotBlank()) {
                                        selectedIdentity = "Custom"
                                        step = 2
                                    }
                                })
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            if (selectedIdentity == "Custom" && customIdentity.isNotBlank()) {
                                Button(
                                    onClick = { step = 2 },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Text("Continue")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(Icons.Default.ArrowForward, contentDescription = "Next")
                                }
                            }
                        }
                    }
                    2 -> {
                        // Step 2: Implementation Intention & 2-Minute Scaling
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Design Your Habit Cue",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Implementation intentions force you to declare when and where you will show up as $currentIdentityText.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(24.dp))

                            // Habit Name
                            OutlinedTextField(
                                value = habitName,
                                onValueChange = { habitName = it },
                                label = { Text("I will...") },
                                placeholder = { Text("e.g. read, run, write") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.onBackground,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Structure design row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = timeOfDaySpecific,
                                    onValueChange = { timeOfDaySpecific = it },
                                    label = { Text("At what time?") },
                                    placeholder = { Text("e.g. 7:00 AM, After breakfast") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.onBackground,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    )
                                )

                                OutlinedTextField(
                                    value = place,
                                    onValueChange = { place = it },
                                    label = { Text("In what place?") },
                                    placeholder = { Text("e.g. living room, my desk") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.onBackground,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Habit stacking anchor suggestion
                            Text(
                                text = "Atomic Habit Stacking (Optional)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "One of the best ways to build a new habit is to find an current anchor behavior you already do each day and stack your new behavior on top of it.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = stackAnchorRef,
                                onValueChange = { stackAnchorRef = it },
                                placeholder = { Text("e.g. waking up, brushing my teeth, making coffee") },
                                label = { Text("After I...") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.onBackground,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // 2-Minute Rule suggestion
                            Text(
                                text = "Apply the 2-Minute Rule",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Scale down your habit so it takes 2 minutes or less to do. This ensures frictionless starting. You can always level it up once you've shown up.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = starterVersion,
                                onValueChange = { starterVersion = it },
                                placeholder = { Text("e.g. Read 1 page, put on jogging suit") },
                                label = { Text("Starter / 2-minute version") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.onBackground,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Time of Day selection
                            Text(
                                text = "Time of day group:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("morning", "afternoon", "evening").forEach { time ->
                                    val isSel = timeOfDay == time
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { timeOfDay = time },
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, if (isSel) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                        color = if (isSel) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.background
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = time.replaceFirstChar { it.uppercase() },
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSel) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            Button(
                                onClick = {
                                    if (habitName.isNotBlank()) {
                                        val initialHabit = Habit(
                                            name = habitName,
                                            identityTag = currentIdentityText,
                                            timeOfDay = timeOfDay,
                                            place = place,
                                            timeOfDaySpecific = timeOfDaySpecific,
                                            starterVersion = starterVersion,
                                            stackAnchorRef = stackAnchorRef
                                        )
                                        viewModel.completeOnboarding(currentIdentityText, initialHabit)
                                        onOnboardingComplete()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = habitName.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text("Complete Setup & Start")
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Default.Check, contentDescription = "Complete")
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            TextButton(
                                onClick = { step = 1 },
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onBackground)
                            ) {
                                Text("Go Back to identity")
                            }
                        }
                    }
                }
            }
        }
    }
}

data class IdentityOption(
    val title: String,
    val category: String,
    val defaultHabit: String,
    val defaultStarter: String,
    val defaultAnchor: String
)
