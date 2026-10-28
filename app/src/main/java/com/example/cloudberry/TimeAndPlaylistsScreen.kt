
package com.example.cloudberry

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.sp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.foundation.BorderStroke
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.layout.size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun TimeAndPlaylistsScreen(
    modifier: Modifier = Modifier,
    rules: List<PlaylistRule>,
    priority: String,
    activeRuleId: String?,
    currentPlaylistName: String,
    isPlaying: Boolean,
    onPriorityChange: (String) -> Unit,
    onAddClick: () -> Unit,
    onEditRule: (PlaylistRule) -> Unit = {},
    onDeleteRule: (PlaylistRule) -> Unit = {}
) {
    val ruleText = Color(0xFF49382D)
    val ruleMuted = Color(0xFF896B52)
    val ruleBackground = Color(0xFFFFFEFC)
    val ruleButton = Color(0xFFF4E5D4)
    val deleteRed = Color(0xFFD32F2F)

    var expandedRuleIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var ruleToDelete by remember { mutableStateOf<PlaylistRule?>(null) }
    var showPriorityDialog by remember { mutableStateOf(false) }
    val rulesScrollState = rememberScrollState()
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "rules",
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = ruleText
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = {
                    showPriorityDialog = true
                },
                shape = CircleShape,
                border = BorderStroke(1.dp, ruleMuted),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = ruleMuted
                )
            ) {
                Text(
                    text = "priority: $priority",
                    fontSize = 15.sp,
                    color = ruleMuted
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ruleText,
                    contentColor = Color(0xFFFFF8EF)
                ),
                shape = CircleShape
            ) {
                Text(
                    text = "+ add",
                    fontSize = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rulesScrollState)
        ) {
            if (rules.isEmpty()) {
                Text(
                    text = "no rules added yet",
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = ruleMuted,
                    fontSize = 14.sp
                )
            } else {
                rules.forEach { rule ->
                    val isExpanded = rule.id in expandedRuleIds
                    val playlistBelongsToRule = rule.playlists.any {
                        it.name.equals(currentPlaylistName, ignoreCase = true)
                    }

                    val isActiveRule =
                        rule.id == activeRuleId && playlistBelongsToRule

                    val shouldAnimateEqualizer =
                        isActiveRule && isPlaying

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isActiveRule) {
                                    Modifier.border(
                                        width = 2.dp,
                                        color = Color(0xFF896B52),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = ruleBackground
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 2.dp
                        )
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Rule header: name and condition icons on the left, arrow on the right.
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        interactionSource = remember {
                                            MutableInteractionSource()
                                        },
                                        indication = null
                                    ) {
                                        expandedRuleIds =
                                            if (isExpanded) {
                                                expandedRuleIds - rule.id
                                            } else {
                                                expandedRuleIds + rule.id
                                            }
                                    }
                                    .padding(
                                        horizontal = 16.dp,
                                        vertical = 18.dp
                                    ),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Rule name uses remaining space; condition icons keep their size.
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = rule.name,
                                        modifier = Modifier.weight(1f, fill = false),
                                        color = ruleText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Row(
                                        modifier = Modifier,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Keep all your existing time, season, rain,
                                        // and equalizer icon conditions here.
                                        if (
                                            rule.triggerOption == "time only" ||
                                            rule.triggerOption == "season or time" ||
                                            rule.triggerOption == "season and time"
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AccessTime,
                                                contentDescription = "Time rule",
                                                modifier = Modifier.size(18.dp),
                                                tint = ruleMuted
                                            )
                                        }

                                        if (
                                            rule.triggerOption == "season only" ||
                                            rule.triggerOption == "season or time" ||
                                            rule.triggerOption == "season and time"
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Eco,
                                                contentDescription = "Season rule",
                                                modifier = Modifier.size(18.dp),
                                                tint = ruleMuted
                                            )
                                        }

                                        if (rule.rainEnabled) {
                                            Icon(
                                                imageVector = Icons.Default.WaterDrop,
                                                contentDescription = "Rain condition enabled",
                                                modifier = Modifier.size(18.dp),
                                                tint = ruleMuted
                                            )
                                        }

                                        if (isActiveRule) {
                                            PlayingEqualizer(
                                                isPlaying = shouldAnimateEqualizer,
                                                color = ruleMuted
                                            )
                                        }
                                    }
                                }

                                // Arrow stays on the far right.
                                Icon(
                                    imageVector = if (isExpanded) {
                                        Icons.Default.KeyboardArrowUp
                                    } else {
                                        Icons.Default.KeyboardArrowDown
                                    },
                                    contentDescription = if (isExpanded) {
                                        "Collapse rule"
                                    } else {
                                        "Expand rule"
                                    },
                                    tint = ruleMuted
                                )
                            }

                            // Animated details section.
                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = expandVertically(
                                    expandFrom = Alignment.Top
                                ) + fadeIn(),
                                exit = shrinkVertically(
                                    shrinkTowards = Alignment.Top
                                ) + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            start = 16.dp,
                                            end = 16.dp,
                                            bottom = 16.dp
                                        )
                                ) {

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "playlists",
                                            color = ruleMuted,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )

                                        if (rule.randomOrder) {
                                            Text(
                                                text = " (shuffle)",
                                                color = ruleMuted,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }

                                    val displayedPlaylists =
                                        if (rule.randomOrder) {
                                            rule.playlists
                                        } else {
                                            rule.playlistOrder
                                        }

                                    displayedPlaylists.forEachIndexed { index, playlist ->
                                        Text(
                                            text = if (rule.randomOrder) {
                                                "• ${playlist.name}"
                                            } else {
                                                "${index + 1}. ${playlist.name}"
                                            },
                                            color = ruleText,
                                            fontSize = 10.sp
                                        )
                                    }

                                    when (rule.triggerOption) {
                                        "season only" -> {
                                            Text(
                                                text = "season",
                                                color = ruleMuted,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )

                                            Text(
                                                text = rule.season,
                                                color = ruleText,
                                                fontSize = 10.sp
                                            )
                                        }

                                        "time only" -> {
                                            Text(
                                                text = "time",
                                                color = ruleMuted,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )

                                            Text(
                                                text = rule.timeRanges.joinToString(", ") { range ->
                                                    "${formatRuleTime(range.first)} – ${formatRuleTime(range.second)}"
                                                },
                                                color = ruleText,
                                                fontSize = 14.sp
                                            )
                                        }

                                        "season and time" -> {
                                            Text(
                                                text = "season",
                                                color = ruleMuted,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )

                                            Text(
                                                text = rule.season,
                                                color = ruleText,
                                                fontSize = 10.sp
                                            )

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Text(
                                                text = "time",
                                                color = ruleMuted,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )

                                            Text(
                                                text = rule.timeRanges.joinToString(", ") { range ->
                                                    "${formatRuleTime(range.first)} – ${formatRuleTime(range.second)}"
                                                },
                                                color = ruleText,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }

                                    Text(
                                        text = "rain",
                                        color = ruleMuted,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    Text(
                                        text = if (rule.rainEnabled) "yes" else "no",
                                        color = ruleText,
                                        fontSize = 10.sp
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

// Equal-width Edit and Delete buttons.
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                onEditRule(rule)
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = ruleText,
                                                contentColor = Color(0xFFFFF8EF)
                                            )
                                        ) {
                                            Text(
                                                text = "edit",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.sp
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                ruleToDelete = rule
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = deleteRed,
                                                contentColor = Color.White
                                            )
                                        ) {
                                            Text(
                                                text = "delete",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 12.sp
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

        ruleToDelete?.let { rule ->
            AlertDialog(
                onDismissRequest = {
                    ruleToDelete = null
                },
                title = {
                    Text(
                        text = "Delete rule?",
                        color = ruleText,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                text = {
                    Text(
                        text = "are you sure you want to delete \"${rule.name}\"? This action cannot be undone.",
                        color = ruleMuted
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteRule(rule)
                            ruleToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = deleteRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Delete",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                dismissButton = {
                    Button(
                        onClick = {
                            ruleToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ruleButton,
                            contentColor = ruleText
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel")
                    }
                },
                containerColor = ruleBackground
            )
        }
    }

    if (showPriorityDialog) {
        AlertDialog(
            onDismissRequest = {
                showPriorityDialog = false
            },
            containerColor = ruleBackground,
            title = {
                Text(
                    text = "priority",
                    color = ruleText,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column {
                    if (priority.equals("time", ignoreCase = true)) {
                        Text(
                            text = "how it works",
                            color = ruleText,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "When a time-only rule and a season-only rule match at the same time, the time-only rule is chosen.",
                            color = ruleMuted
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "example",
                            color = ruleText,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "If your morning playlist is scheduled for 8:00–10:00 and a winter playlist is active, the morning playlist wins during that overlap.",
                            color = ruleMuted
                        )
                    } else {
                        Text(
                            text = "how it works",
                            color = ruleText,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "When a season-only rule and a time-only rule match at the same time, the season-only rule is chosen.",
                            color = ruleMuted
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "example",
                            color = ruleText,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "If a winter playlist is active while your morning playlist is scheduled for 8:00–10:00, the winter playlist wins during that overlap.",
                            color = ruleMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "important",
                        color = ruleText,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Rules that match both the season and time always take priority over rules that match only one condition.",
                        color = ruleMuted
                    )
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            val newPriority = if (priority.equals("time", ignoreCase = true)) {
                                "season"
                            } else {
                                "time"
                            }

                            onPriorityChange(newPriority)
                            vibrateDevice(context)
                        }
                    ) {
                        Text(
                            text = priority,
                            color = ruleText
                        )
                    }

                    TextButton(
                        onClick = {
                            showPriorityDialog = false
                        }
                    ) {
                        Text("close", color = ruleText)
                    }
                }
            }
        )
    }
}

private fun formatRuleTime(index: Int): String {
    val totalMinutes = index * 30
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60

    return "%02d:%02d".format(hours, minutes)
}

private fun vibrateDevice(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(
            Context.VIBRATOR_MANAGER_SERVICE
        ) as VibratorManager

        manager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    if (vibrator.hasVibrator()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    150,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(150)
        }
    }
}

@Composable
private fun PlayingEqualizer(
    isPlaying: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    val bar1: Float
    val bar2: Float
    val bar3: Float

    if (isPlaying) {
        val transition = rememberInfiniteTransition(
            label = "rule equalizer"
        )

        val animatedBar1 by transition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 500
                    0.3f at 0
                    1f at 180
                    0.45f at 350
                    0.3f at 500
                },
                repeatMode = RepeatMode.Restart
            ),
            label = "equalizer bar 1"
        )

        val animatedBar2 by transition.animateFloat(
            initialValue = 0.8f,
            targetValue = 0.25f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 420
                    0.8f at 0
                    0.25f at 150
                    1f at 300
                    0.8f at 420
                },
                repeatMode = RepeatMode.Restart
            ),
            label = "equalizer bar 2"
        )

        val animatedBar3 by transition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 600
                    0.5f at 0
                    1f at 220
                    0.3f at 420
                    0.5f at 600
                },
                repeatMode = RepeatMode.Restart
            ),
            label = "equalizer bar 3"
        )

        bar1 = animatedBar1
        bar2 = animatedBar2
        bar3 = animatedBar3
    } else {
        bar1 = 0.3f
        bar2 = 0.3f
        bar3 = 0.3f
    }

    Canvas(modifier = modifier.size(width = 20.dp, height = 20.dp)) {
        val barWidth = size.width / 5f
        val gap = barWidth
        val heights = listOf(bar1, bar2, bar3)

        heights.forEachIndexed { index, fraction ->
            val height = size.height * (
                    if (isPlaying) fraction else 0.3f
                    )

            val x = index * (barWidth + gap) + barWidth / 2f

            drawLine(
                color = color,
                start = Offset(x, (size.height - height) / 2f),
                end = Offset(x, (size.height + height) / 2f),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
        }
    }
}