package io.github.droidkaigi.confsched.feature.sessions.timetable.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.droidkaigi.confsched.core.model.KaigiColorScheme
import io.github.droidkaigi.confsched.core.model.Room
import io.github.droidkaigi.confsched.core.model.TimetableItem
import io.github.droidkaigi.confsched.core.model.TimetableItemId
import io.github.droidkaigi.confsched.core.preview.KaigiSchemeProvider
import io.github.droidkaigi.confsched.core.preview.LocalePreviews
import io.github.droidkaigi.confsched.core.preview.wrapper.KaigiPreviewTheme
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import kotlin.math.abs

@Composable
internal fun TimetableGridSection(
    uiState: TimetableGridSectionUiState,
    onItemClick: (TimetableItemId) -> Unit,
    initialScale: TimetableGridScale = TimetableGridScale.Default,
) {
    var hourHeightValue by rememberSaveable { mutableStateOf(initialScale.hourHeight.value) }
    var pinchStartHourHeightValue by rememberSaveable { mutableStateOf(initialScale.hourHeight.value) }
    var pinching by rememberSaveable { mutableStateOf(false) }
    val hourHeight by animateDpAsState(
        targetValue = hourHeightValue.dp,
        animationSpec = tween(durationMillis = if (pinching) 0 else 180),
        label = "TimetableGridHourHeight",
    )
    val rooms = uiState.sessions.toTimetableGridRooms()
    val endMinute = uiState.sessions
        .maxOfOrNull { it.endsAt.toTimetableGridMinuteOfDay() }
        ?.coerceAtLeast(TimetableGridDefaultDayEndMinutes)
        ?: TimetableGridDefaultDayEndMinutes
    val contentHeight = timetableGridContentHeight(endMinute, hourHeight)
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxSize()
            .timetableGridZoom(
                onPinchStart = {
                    pinching = true
                    pinchStartHourHeightValue = hourHeightValue
                },
                onZoom = { zoomRatio ->
                    hourHeightValue = (pinchStartHourHeightValue * zoomRatio)
                        .coerceIn(
                            TimetableGridDefaultHourHeight.value,
                            TimetableGridExpandedHourHeight.value,
                        )
                },
                onPinchEnd = {
                    pinching = false
                    val middle = (TimetableGridDefaultHourHeight.value + TimetableGridExpandedHourHeight.value) / 2f
                    hourHeightValue = if (hourHeightValue >= middle) {
                        TimetableGridExpandedHourHeight.value
                    } else {
                        TimetableGridDefaultHourHeight.value
                    }
                },
            )
            .verticalScroll(verticalScrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        TimetableGridTimeGutter(
            endMinute = endMinute,
            hourHeight = hourHeight,
            nowMinute = uiState.nowMinute,
            modifier = Modifier
                .width(TimetableGridTimeGutterWidth)
                .height(contentHeight),
        )
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.horizontalScroll(horizontalScrollState),
            horizontalArrangement = Arrangement.spacedBy(TimetableGridRoomColumnGap),
        ) {
            rooms.forEach { room ->
                TimetableGridRoomColumn(
                    room = room,
                    sessions = uiState.sessions.filter { it.room == room }.toPersistentList(),
                    contentHeight = contentHeight,
                    endMinute = endMinute,
                    hourHeight = hourHeight,
                    nowMinute = uiState.nowMinute,
                    onItemClick = onItemClick,
                    modifier = Modifier
                        .width(TimetableGridRoomColumnWidth)
                        .height(contentHeight),
                )
            }
        }
    }
}

@Composable
private fun TimetableGridTimeGutter(
    endMinute: Int,
    hourHeight: Dp,
    nowMinute: Int?,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        for (minute in TimetableGridDayStartMinutes..endMinute step 60) {
            Text(
                modifier = Modifier.offset(
                    y = timetableGridMinuteOffsetY(minute = minute, hourHeight = hourHeight),
                ),
                text = minute.toTimetableGridTimeLabel(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        if (nowMinute != null && nowMinute in TimetableGridDayStartMinutes..endMinute) {
            TimetableGridNowLabel(
                minute = nowMinute,
                modifier = Modifier.offset(
                    y = timetableGridLineOffsetY(
                        minute = nowMinute,
                        endMinute = endMinute,
                        hourHeight = hourHeight,
                        lineHeight = TimetableGridNowLineHeight,
                    ) - TimetableGridNowLabelHeight / 2,
                ),
            )
        }
    }
}

@Composable
private fun TimetableGridRoomColumn(
    room: Room,
    sessions: PersistentList<TimetableItem>,
    contentHeight: androidx.compose.ui.unit.Dp,
    endMinute: Int,
    hourHeight: Dp,
    nowMinute: Int?,
    onItemClick: (TimetableItemId) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        TimetableGridRoomHeader(room = room)
        Box(
            modifier = Modifier
                .offset(y = TimetableGridHeaderHeight)
                .width(TimetableGridRoomColumnWidth)
                .height(contentHeight - TimetableGridHeaderHeight)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)),
        )
        for (minute in TimetableGridDayStartMinutes..endMinute step 60) {
            TimetableGridHourRule(
                modifier = Modifier.offset(
                    y = timetableGridLineOffsetY(
                        minute = minute,
                        endMinute = endMinute,
                        hourHeight = hourHeight,
                        lineHeight = TimetableGridHourRuleHeight,
                    ),
                ),
            )
        }
        sessions.forEach { item ->
            TimetableGridCell(
                title = item.title,
                room = item.room,
                speaker = item.speaker,
                startsAt = item.startsAt,
                endsAt = item.endsAt,
                height = timetableGridSessionHeight(
                    startsAt = item.startsAt,
                    endsAt = item.endsAt,
                    hourHeight = hourHeight,
                ),
                onItemClick = { onItemClick(item.id) },
                modifier = Modifier.offset(
                    y = timetableGridSessionOffsetY(
                        startsAt = item.startsAt,
                        hourHeight = hourHeight,
                    ),
                ),
            )
        }
        if (nowMinute != null && nowMinute in TimetableGridDayStartMinutes..endMinute) {
            TimetableGridNowLine(
                modifier = Modifier.offset(
                    y = timetableGridLineOffsetY(
                        minute = nowMinute,
                        endMinute = endMinute,
                        hourHeight = hourHeight,
                        lineHeight = TimetableGridNowLineHeight,
                    ),
                ),
            )
        }
    }
}

@Composable
private fun TimetableGridHourRule(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(TimetableGridHourRuleHeight)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

@Composable
private fun TimetableGridNowLine(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(TimetableGridNowLineHeight)
            .background(MaterialTheme.colorScheme.primary),
    )
}

@Composable
private fun BoxScope.TimetableGridNowLabel(
    minute: Int,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier
            .width(TimetableGridNowLabelWidth)
            .height(TimetableGridNowLabelHeight)
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(percent = 50),
            )
            .padding(horizontal = 4.dp),
        text = minute.toTimetableGridTimeLabel(),
        color = MaterialTheme.colorScheme.onPrimary,
        style = MaterialTheme.typography.labelSmall,
        textAlign = TextAlign.Center,
        maxLines = 1,
    )
}

private val TimetableGridHourRuleHeight = 1.dp
private val TimetableGridNowLineHeight = 2.dp
private val TimetableGridNowLabelWidth = 42.dp
private val TimetableGridNowLabelHeight = 18.dp

private fun timetableGridLineOffsetY(
    minute: Int,
    endMinute: Int,
    hourHeight: Dp,
    lineHeight: Dp,
): Dp {
    val y = timetableGridMinuteOffsetY(minute = minute, hourHeight = hourHeight)
    val maxY = timetableGridContentHeight(endMinute = endMinute, hourHeight = hourHeight) - lineHeight
    return y.coerceIn(TimetableGridHeaderHeight, maxY)
}

private fun Modifier.timetableGridZoom(
    onPinchStart: () -> Unit,
    onZoom: (Float) -> Unit,
    onPinchEnd: () -> Unit,
): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        var initialVerticalSpan: Float? = null
        var started = false

        while (true) {
            val event = awaitPointerEvent()
            val pressedChanges = event.changes.filter { it.pressed }
            if (pressedChanges.isEmpty()) break
            if (pressedChanges.size < 2) {
                initialVerticalSpan = null
                continue
            }

            val verticalSpan = pressedChanges.verticalSpan()
            val baseline = initialVerticalSpan ?: verticalSpan.also { initialVerticalSpan = it }
            if (!started && baseline > 24f) {
                started = true
                onPinchStart()
            }
            if (baseline > 24f) {
                val ratio = verticalSpan / baseline
                onZoom(ratio)
                pressedChanges.forEach { it.consume() }
            }
        }
        if (started) {
            onPinchEnd()
        }
    }
}

private fun List<PointerInputChange>.verticalSpan(): Float {
    return abs(this[0].position.y - this[1].position.y)
}

private val TimetableGridDefaultRooms: PersistentList<Room> = listOf(
    Room.NARWHAL,
    Room.OTTER,
    Room.PANDA,
    Room.QUAIL,
    Room.MEERKAT,
).toPersistentList()

private fun PersistentList<TimetableItem>.toTimetableGridRooms(): PersistentList<Room> {
    val unknownRooms = map { it.room }
        .filter { it == Room.UNKNOWN }
        .distinct()
    return (TimetableGridDefaultRooms + unknownRooms)
        .distinct()
        .toPersistentList()
}

@LocalePreviews
@Composable
private fun TimetableGridSectionPreview(
    @PreviewParameter(KaigiSchemeProvider::class) colorScheme: KaigiColorScheme,
) {
    KaigiPreviewTheme(colorScheme) {
        TimetableGridSection(
            uiState = TimetableGridSectionUiState.fake(),
            onItemClick = {},
        )
    }
}

@LocalePreviews
@Composable
private fun TimetableGridSectionExpandedPreview(
    @PreviewParameter(KaigiSchemeProvider::class) colorScheme: KaigiColorScheme,
) {
    KaigiPreviewTheme(colorScheme) {
        TimetableGridSection(
            uiState = TimetableGridSectionUiState.fake(),
            onItemClick = {},
            initialScale = TimetableGridScale.Expanded,
        )
    }
}
