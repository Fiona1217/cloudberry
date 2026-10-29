
package com.example.cloudberry

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import android.content.Context
import androidx.compose.material3.OutlinedTextField
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import kotlin.math.abs
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.OutlinedTextFieldDefaults
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions

private val RuleBackground = Color(0xFFFFFEFC)
private val RuleText = Color(0xFF49382D)
private val RuleMuted = Color(0xFF896B52)
private val RuleBorder = Color(0xFFEADFD2)
private val RuleButton = Color(0xFFF4E5D4)

@Composable
fun AddPlaylistRuleScreen(
    playlists: List<SpotifyPlaylist>,
    modifier: Modifier = Modifier,
    initialRule: PlaylistRule? = null,
    existingRules: List<PlaylistRule>,
    onBackClick: () -> Unit,
    onRequestPlaylistAccess: () -> Unit,
    onSaveRule: (PlaylistRule) -> Unit
) {

    var showExitConfirmation by remember(initialRule?.id) { mutableStateOf(false) }
    var showPlaylistPicker by remember(initialRule?.id) { mutableStateOf(false) }
    var selectedPlaylists by remember(initialRule?.id) { mutableStateOf(initialRule?.playlists ?: emptyList()) }
    var playlistOrder by remember(initialRule?.id) {
        mutableStateOf(
            initialRule?.playlistOrder
                ?.takeIf { it.isNotEmpty() }
                ?: initialRule?.playlists
                ?: emptyList()
        )
    }
    var randomSortEnabled by remember(initialRule?.id) { mutableStateOf(initialRule?.randomOrder ?: false) }
    var triggerOption by remember(initialRule?.id) { mutableStateOf(initialRule?.triggerOption ?: "season only") }
    var showSeasonPicker by remember(initialRule?.id) { mutableStateOf(false) }
    var selectedSeason by remember(initialRule?.id) { mutableStateOf(initialRule?.season ?: "") }
    var rainEnabled by remember(initialRule?.id) { mutableStateOf(initialRule?.rainEnabled ?: false) }
    var rainScope by remember(initialRule?.id) { mutableStateOf(initialRule?.rainScope ?: "season") }
    var showTimePicker by remember(initialRule?.id) { mutableStateOf(false) }
    var startTimeIndex by remember(initialRule?.id) { mutableStateOf(initialRule?.timeRanges?.firstOrNull()?.first ?: 36) }
    var endTimeIndex by remember(initialRule?.id) { mutableStateOf(initialRule?.timeRanges?.firstOrNull()?.second ?: 44) }
    var selectedTimeRanges by remember(initialRule?.id) { mutableStateOf(initialRule?.timeRanges ?: emptyList()) }
    var validationMessage by remember(initialRule?.id) { mutableStateOf<String?>(null) }
    var showRuleSuccess by remember(initialRule?.id) { mutableStateOf(false) }
    var isTimeWheelScrolling by remember(initialRule?.id) { mutableStateOf(false) }
    var showAllSelectedPlaylists by remember(initialRule?.id) { mutableStateOf(false) }
    val sortedPlaylists = playlists.sortedBy { playlist -> selectedPlaylists.contains(playlist) }

    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var ruleName by remember(initialRule?.id) {
        mutableStateOf(initialRule?.name ?: "")
    }

    BackHandler(enabled = !showExitConfirmation) {
        showExitConfirmation = true
    }


    LaunchedEffect(validationMessage) {
        if (validationMessage != null) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(top = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    showExitConfirmation = true
                }
            ) {
                Icon(
                    modifier = Modifier
                        .offset(x = (-12).dp),
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Go back",
                    tint = RuleText
                )
            }

            Text(
                modifier = Modifier
                    .offset(x = (9).dp),
                text = if (initialRule == null) {
                    "add playlist rule"
                } else {
                    "edit playlist rule"
                },
                color = RuleText,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "rule name",
            modifier = Modifier.fillMaxWidth(),
            color = RuleMuted,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        OutlinedTextField(
            value = ruleName,
            onValueChange = { ruleName = it },
            modifier = Modifier
                .fillMaxWidth(),
            placeholder = {
                Text(
                    text = "enter a name for this rule",
                    color = Color(0xFFB9A18A),
                    fontSize = 14.sp
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF49382D),
                unfocusedBorderColor = Color(0xFFD6C2AE),
                focusedTextColor = Color(0xFF49382D),
                unfocusedTextColor = Color(0xFF49382D),
                cursorColor = Color(0xFF49382D)
            ),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                }
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "choose playlists",
            modifier = Modifier.fillMaxWidth(),
            color = RuleMuted,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(5.dp))

        Button(
            onClick = {
                showPlaylistPicker = true

                if (playlists.isEmpty()) {
                    onRequestPlaylistAccess()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = RuleButton,
                contentColor = RuleText
            )
        ) {
            Text(
                text = "+ add playlist",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
        if (selectedPlaylists.isEmpty()) {
            Text(
                text = "no playlists selected",
                modifier = Modifier
                    .fillMaxWidth(),
                color = RuleMuted,
                fontSize = 12.sp
            )
        } else {
            val maxVisiblePlaylists = 3
            val visiblePlaylists = if (showAllSelectedPlaylists) {
                selectedPlaylists
            } else {
                selectedPlaylists.take(maxVisiblePlaylists)
            }
            val hiddenCount = selectedPlaylists.size - visiblePlaylists.size

            Spacer(modifier = Modifier.height(5.dp))

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(
                            max = if (showAllSelectedPlaylists) 350.dp
                            else 174.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {

                    items(
                        items = visiblePlaylists,
                        key = { it.id }
                    ) { playlist ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PlaylistCover(
                                imageUrl = playlist.imageUrl,
                                modifier = Modifier.size(42.dp)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = playlist.name,
                                modifier = Modifier.weight(1f),
                                color = RuleText,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            IconButton(
                                onClick = {
                                    selectedPlaylists = selectedPlaylists.filterNot {
                                        it.id == playlist.id
                                    }

                                    playlistOrder =
                                        playlistOrder.filterNot {
                                            it.id == playlist.id
                                        }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove ${playlist.name}",
                                    tint = RuleMuted
                                )
                            }
                        }
                    }
                }

                if (hiddenCount > 0 || showAllSelectedPlaylists) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                showAllSelectedPlaylists =
                                    !showAllSelectedPlaylists
                            }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (showAllSelectedPlaylists) {
                                "show less"
                            } else {
                                "+$hiddenCount more playlist${if (hiddenCount == 1) "" else "s"}"
                            },
                            color = RuleMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

    }
        if (playlistOrder.size >= 2) {
            Spacer(modifier = Modifier.height(25.dp))

            Text(
                text = "sort playlists",
                modifier = Modifier.fillMaxWidth(),
                color = RuleMuted,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )


            if (selectedPlaylists.isEmpty()) {
                Text(
                    text = "",
                    modifier = Modifier.fillMaxWidth(),
                    color = RuleMuted,
                    fontSize = 12.sp
                )
            } else {
                if(!randomSortEnabled){
                    val lazyListState = rememberLazyListState()

                    val reorderState = rememberReorderableLazyListState(
                        lazyListState = lazyListState
                    ) { from, to ->
                        playlistOrder = playlistOrder.toMutableList().apply {
                            add(to.index, removeAt(from.index))
                        }
                    }

                    Text(
                        text = "drag to change the order",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        color = RuleMuted,
                        fontSize = 12.sp
                    )

                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(
                            items = playlistOrder,
                            key = { it.id }
                        ) { playlist ->
                            ReorderableItem(
                                state = reorderState,
                                key = playlist.id
                            ) { isDragging ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isDragging) RuleButton
                                            else RuleBackground
                                        )
                                        .padding(
                                            horizontal = 10.dp,
                                            vertical = 8.dp
                                        ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    PlaylistCover(
                                        imageUrl = playlist.imageUrl,
                                        modifier = Modifier.size(42.dp)
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Text(
                                        text = playlist.name,
                                        modifier = Modifier.weight(1f),
                                        color = RuleText,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Icon(
                                        imageVector = Icons.Default.DragHandle,
                                        contentDescription =
                                            "Drag to reorder ${playlist.name}",
                                        tint = RuleMuted,
                                        modifier = Modifier.draggableHandle()
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {

                    Text(
                        text = "shuffle",
                        color = RuleText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Switch(
                        checked = randomSortEnabled,
                        onCheckedChange = { randomSortEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = RuleBackground,
                            checkedTrackColor = RuleMuted,
                            uncheckedThumbColor = RuleBackground,
                            uncheckedTrackColor = RuleBorder,
                            uncheckedBorderColor = RuleMuted
                        )
                    )
                }
            }
        }



        Spacer(modifier = Modifier.height(25.dp))

        Text(
            text = "condition options",
            modifier = Modifier.fillMaxWidth(),
            color = RuleMuted,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        val triggerOptions = listOf(
            "season only",
            "time only",
            "season and time"
        )
        triggerOptions.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        triggerOption = option
                        if (option == "time only") {
                            selectedSeason = ""
                        } else if (option == "season only") {
                            selectedTimeRanges = emptyList()
                        }
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = triggerOption == option,
                    onClick = {
                        triggerOption = option

                        if (option == "time only") {
                            selectedSeason = ""
                        } else if (option == "season only") {
                            selectedTimeRanges = emptyList()
                        }
                    },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = RuleMuted,
                    )
                )

                Text(
                    text = option,
                    color = RuleText,
                    fontSize = 14.sp
                )
            }

            if (
                triggerOption == option &&
                option in listOf(
                    "season only",
                    "season and time"
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, bottom = 10.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = {
                            showSeasonPicker = true
                        },
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(
                            horizontal = 12.dp,
                            vertical = 0.dp
                        ),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RuleText,
                            contentColor = RuleBackground
                        )
                    ) {
                        Text(
                            text = if (selectedSeason.isBlank()) {
                                "+ choose season"
                            } else {
                                "edit season"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (selectedSeason.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedSeason,
                                color = RuleText,
                                fontSize = 12.sp,
                            )

                            IconButton(
                                onClick = {
                                    selectedSeason = ""
                                },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove selected season",
                                    tint = RuleMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
            if (
                triggerOption == option &&
                option in listOf(
                    "time only",
                    "season and time"
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, bottom = 10.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = {
                            if (selectedTimeRanges.isNotEmpty()) {
                                startTimeIndex = selectedTimeRanges.first().first
                                endTimeIndex = selectedTimeRanges.first().second
                            }

                            showTimePicker = true
                        },
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(
                            horizontal = 12.dp,
                            vertical = 0.dp
                        ),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RuleText,
                            contentColor = RuleBackground
                        )
                    ) {
                        Text(
                            text = if (selectedTimeRanges.isEmpty()) {
                                "+ add time"
                            } else {
                                "edit time"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (selectedTimeRanges.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedTimeRanges.joinToString(", ") { range ->
                                    "${formatTime(range.first)} – ${formatTime(range.second)}"
                                },
                                modifier = Modifier.weight(1f, fill = false),
                                color = RuleText,
                                fontSize = 12.sp
                            )

                            IconButton(
                                onClick = {
                                    selectedTimeRanges = emptyList()
                                },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove selected time range",
                                    tint = RuleMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp, bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "rain",
                    color = RuleText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Switch(
                    checked = rainEnabled,
                    onCheckedChange = { enabled ->
                        rainEnabled = enabled
                        if (!enabled) {
                            rainScope = "season"
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = RuleBackground,
                        checkedTrackColor = RuleMuted,
                        uncheckedThumbColor = RuleBackground,
                        uncheckedTrackColor = RuleBorder,
                        uncheckedBorderColor = RuleMuted
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(25.dp))

        Button(
            onClick = {
                val basicMessage = when {
                    ruleName.isBlank() ->
                        "please enter a rule name."

                    selectedPlaylists.isEmpty() ->
                        "please select at least one playlist."

                    triggerOption == "season only" &&
                            selectedSeason.isBlank() &&
                            !(rainEnabled && rainScope == "all") ->
                        "please select a season."

                    triggerOption == "time only" &&
                            selectedTimeRanges.isEmpty() ->
                        "please add a time range."

                    triggerOption == "season and time" &&
                            selectedSeason.isBlank() &&
                            !(rainEnabled && rainScope == "all") ->
                        "please select a season."

                    triggerOption == "season and time" &&
                            selectedTimeRanges.isEmpty() ->
                        "please add a time range."

                    rainEnabled &&
                            rainScope == "season" &&
                            selectedSeason.isBlank() ->
                        "please select a season for rain."

                    rainEnabled &&
                            rainScope !in listOf("season", "all") ->
                        "please choose a valid rain option."

                    else -> null
                }

                val candidateRule = PlaylistRule(
                    id = initialRule?.id
                        ?: java.util.UUID.randomUUID().toString(),
                    name = ruleName.trim(),
                    playlists = selectedPlaylists,
                    playlistOrder = playlistOrder,
                    randomOrder = randomSortEnabled,
                    triggerOption = triggerOption,
                    season = selectedSeason,
                    rainEnabled = rainEnabled,
                    rainScope = if (triggerOption == "time only") {
                        "all"
                    } else {
                        rainScope
                    },
                    timeRanges = selectedTimeRanges
                )

                val duplicateNameRule = existingRules.firstOrNull { existing ->
                    existing.id != candidateRule.id &&
                            existing.name.trim().equals(
                                candidateRule.name,
                                ignoreCase = true
                            )
                }

                val duplicateConditionsRule = existingRules.firstOrNull { existing ->
                    existing.id != candidateRule.id &&
                            hasSameRuleConditions(existing, candidateRule)
                }

                val duplicateMessage = when {
                    duplicateNameRule != null ->
                        "rule name conflicts with \"${duplicateNameRule.name}\"."

                    duplicateConditionsRule != null ->
                        "conditions duplicate \"${duplicateConditionsRule.name}\"."

                    else -> null
                }

                val message = basicMessage ?: duplicateMessage

                validationMessage = message

                if (message == null) {
                    onSaveRule(candidateRule)
                } else {
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
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = RuleText,
                contentColor = RuleBackground
            )
        ) {
            Text(
                text = if (initialRule == null) {
                    "+ add rule"
                } else {
                    "save changes"
                },
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        validationMessage?.let { message ->
            Text(
                text = message,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                color = Color(0xFFB34736),
                fontSize = 12.sp
            )
        }

        if (showRuleSuccess) {
            AlertDialog(
                onDismissRequest = { showRuleSuccess = false },
                containerColor = RuleBackground,
                title = {
                    Text(
                        text = "rule is valid!",
                        color = RuleText,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                text = {
                    Text(
                        text = "Your playlist rule passed validation. Saving it will be the next step.",
                        color = RuleMuted
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = { showRuleSuccess = false }
                    ) {
                        Text("okay", color = RuleText)
                    }
                }
            )
        }

        if (showTimePicker) {
            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                containerColor = RuleBackground,
                title = {
                    Text(
                        text = "choose time range",
                        color = RuleText,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Text(
                                text = "start time",
                                color = RuleMuted,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Text(
                                text = "end time",
                                color = RuleMuted,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            TimeWheel(
                                selectedIndex = startTimeIndex,
                                onSelectedIndexChange = {
                                    startTimeIndex = it
                                },
                                onScrollStateChange = { isScrolling ->
                                    isTimeWheelScrolling = isScrolling
                                }
                            )

                            TimeWheel(
                                selectedIndex = endTimeIndex,
                                onSelectedIndexChange = {
                                    endTimeIndex = it
                                },
                                onScrollStateChange = { isScrolling ->
                                    isTimeWheelScrolling = isScrolling
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "${formatTime(startTimeIndex)} – ${formatTime(endTimeIndex)}",
                            color = RuleText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        if (startTimeIndex == endTimeIndex) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "start and end times are the same",
                                color = RuleMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = !isTimeWheelScrolling,
                        onClick = {
                            selectedTimeRanges = listOf(startTimeIndex to endTimeIndex)
                            showTimePicker = false
                        }
                    ) {
                        Text(
                            text = "add",
                            color = if (isTimeWheelScrolling) {
                                RuleMuted.copy(alpha = 0.4f)
                            } else {
                                RuleText
                            }
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        enabled = !isTimeWheelScrolling,
                        onClick = {
                            showTimePicker = false
                        }
                    ) {
                        Text(
                            text = "cancel",
                            color = if (isTimeWheelScrolling) {
                                RuleMuted.copy(alpha = 0.4f)
                            } else {
                                RuleMuted
                            }
                        )
                    }
                }
            )
        }

        if (showSeasonPicker) {
            val seasonOptions = listOf(
                "spring",
                "summer",
                "fall",
                "winter"
            )

            AlertDialog(
                onDismissRequest = {
                    showSeasonPicker = false
                },
                containerColor = RuleBackground,
                title = {
                    Text(
                        text = "choose season",
                        color = RuleText,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                text = {
                    Column {
                        seasonOptions.forEach { season ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        selectedSeason = season
                                        showSeasonPicker = false
                                    }
                                    .padding(horizontal = 8.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedSeason == season,
                                    onClick = {
                                        selectedSeason = season
                                        showSeasonPicker = false
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = RuleMuted
                                    )
                                )

                                Text(
                                    text = season,
                                    color = RuleText,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showSeasonPicker = false
                        }
                    ) {
                        Text("cancel", color = RuleMuted)
                    }
                }
            )
        }
    }


    if (showExitConfirmation) {
        AlertDialog(
            onDismissRequest = {
                showExitConfirmation = false
            },
            containerColor = RuleBackground,
            title = {
                Text(
                    text = "discard changes?",
                    color = RuleText,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    text = if (initialRule == null) {
                        "are you sure you want to exit? your new rule won't be saved."
                    } else {
                        "are you sure you want to exit? your changes to this rule won't be saved."
                    },
                    color = RuleMuted
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitConfirmation = false
                        onBackClick()
                    }
                ) {
                    Text(
                        text = "discard",
                        color = Color(0xFFB34736)
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showExitConfirmation = false
                    }
                ) {
                    Text(
                        text = "keep editing",
                        color = RuleText
                    )
                }
            }
        )
    }


    if (showPlaylistPicker) {
        AlertDialog(
            onDismissRequest = { showPlaylistPicker = false },
            containerColor = RuleBackground,
            title = {
                Text(
                    text = "choose a playlist",
                    color = RuleText,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                if (playlists.isEmpty()) {
                    Text(
                        text = "loading your spotify playlists...",
                        color = RuleMuted,
                        fontSize = 14.sp
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp),
                    ) {
                        items(
                            items = sortedPlaylists,
                            key = { it.id }
                        ) { playlist ->
                            val alreadySelected =
                                selectedPlaylists.any {
                                    it.id == playlist.id
                                }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        if (!alreadySelected) {
                                            selectedPlaylists = selectedPlaylists + playlist
                                            playlistOrder = playlistOrder + playlist
                                            showPlaylistPicker = false
                                        }
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PlaylistCover(
                                    imageUrl = playlist.imageUrl,
                                    modifier = Modifier.size(44.dp)
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                Text(
                                    text = playlist.name,
                                    modifier = Modifier.weight(1f),
                                    color = RuleText,
                                    fontSize = 14.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (alreadySelected) {
                                    Text(
                                        text = "added",
                                        color = RuleMuted,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            HorizontalDivider(color = RuleBorder)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showPlaylistPicker = false }
                ) {
                    Text("cancel", color = RuleMuted)
                }
            }
        )
    }
}

@Composable
private fun PlaylistCover(
    imageUrl: String?,
    modifier: Modifier = Modifier
) {
    var bitmap by remember(imageUrl) {
        mutableStateOf<Bitmap?>(null)
    }

    LaunchedEffect(imageUrl) {
        bitmap = if (imageUrl.isNullOrBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                try {
                    val connection = URL(imageUrl)
                        .openConnection() as HttpURLConnection

                    connection.connectTimeout = 8000
                    connection.readTimeout = 8000

                    try {
                        connection.inputStream.use {
                            BitmapFactory.decodeStream(it)
                        }
                    } finally {
                        connection.disconnect()
                    }
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFEADFD2)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Playlist cover",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = "♫",
                color = RuleMuted,
                fontSize = 22.sp
            )
        }
    }
}

private fun formatTime(index: Int): String {
    val normalizedIndex = ((index % 48) + 48) % 48
    val hour = normalizedIndex / 2
    val minute = if (normalizedIndex % 2 == 0) "00" else "30"

    return "%02d:%s".format(hour, minute)
}

private fun hasSameRuleConditions(
    first: PlaylistRule,
    second: PlaylistRule
): Boolean {
    if (first.triggerOption != second.triggerOption) {
        return false
    }

    val sameSeason =
        first.season.trim().equals(
            second.season.trim(),
            ignoreCase = true
        )

    val sameTimeRanges =
        first.timeRanges.toSet() == second.timeRanges.toSet()

    return first.rainEnabled == second.rainEnabled &&
            first.rainScope == second.rainScope &&
            when (first.triggerOption) {
                "season only" ->
                    (first.rainEnabled && first.rainScope == "all") ||
                            sameSeason

                "time only" ->
                    sameTimeRanges

                "season and time" ->
                    ((first.rainEnabled && first.rainScope == "all") ||
                            sameSeason) && sameTimeRanges

                else -> false
            }
}


@Composable
private fun TimeWheel(
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    onScrollStateChange: (Boolean) -> Unit
) {
    val itemHeight = 38.dp
    val totalItems = 48 * 100
    val middleCycle = 48 * 50

    // Start with the selected time in the center of the wheel.
    val initialItem = middleCycle + selectedIndex

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialItem - 2
    )
    val isScrolling = listState.isScrollInProgress
    LaunchedEffect(isScrolling) { onScrollStateChange(isScrolling) }
    val coroutineScope = rememberCoroutineScope()

    // The selected time is the item closest to the viewport center.
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val layoutInfo = listState.layoutInfo
            val viewportCenter =
                (layoutInfo.viewportStartOffset +
                        layoutInfo.viewportEndOffset) / 2

            val centeredItem = layoutInfo.visibleItemsInfo.minByOrNull {
                abs(
                    (it.offset + it.size / 2) - viewportCenter
                )
            }

            if (centeredItem != null) {
                val newIndex = centeredItem.index % 48
                if (newIndex != selectedIndex) {
                    onSelectedIndexChange(newIndex)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .width(100.dp)
            .height(itemHeight * 5)
            .clip(RoundedCornerShape(14.dp))
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            flingBehavior = rememberSnapFlingBehavior(
                lazyListState = listState
            )
        ) {
            items(totalItems) { index ->
                val timeIndex = index % 48

                val layoutInfo = listState.layoutInfo
                val viewportCenter =
                    (layoutInfo.viewportStartOffset +
                            layoutInfo.viewportEndOffset) / 2

                val itemInfo = layoutInfo.visibleItemsInfo
                    .firstOrNull { it.index == index }

                val distanceFromCenter = if (itemInfo != null) {
                    abs(
                        (itemInfo.offset + itemInfo.size / 2) -
                                viewportCenter
                    ).toFloat() / itemHeight.value.coerceAtLeast(1f)
                } else {
                    2f
                }

                val itemAlpha = when {
                    distanceFromCenter < 0.5f -> 1f
                    distanceFromCenter < 1.5f -> 0.55f
                    distanceFromCenter < 2.5f -> 0.35f
                    distanceFromCenter < 3.5f -> 0.25f
                    distanceFromCenter < 4.5f -> 0.15f
                    distanceFromCenter < 5.5f -> 0.1f
                    distanceFromCenter < 6f -> 0.05f
                    distanceFromCenter < 6.5f -> 0.03f
                    else -> 0f
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .clickable {
                            coroutineScope.launch {
                                listState.animateScrollToItem(index = index - 2)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = formatTime(timeIndex),
                        modifier = Modifier.graphicsLayer {
                            alpha = itemAlpha
                        },
                        color = RuleText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }

        // Fixed center selection highlight, drawn above the scrolling list.
        if (!isScrolling) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .align(Alignment.Center)
                    .background(RuleMuted),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = formatTime(selectedIndex),
                    color = RuleBackground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
