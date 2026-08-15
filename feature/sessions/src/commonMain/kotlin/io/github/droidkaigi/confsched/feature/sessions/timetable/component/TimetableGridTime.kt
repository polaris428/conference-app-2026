package io.github.droidkaigi.confsched.feature.sessions.timetable.component

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal val TimetableGridHeaderHeight = 34.dp
internal val TimetableGridSessionGap = 4.dp
internal val TimetableGridDefaultHourHeight = 132.dp
internal val TimetableGridExpandedHourHeight = 220.dp
internal val TimetableGridTimeGutterWidth = 52.dp
internal val TimetableGridRoomColumnWidth = 156.dp
internal val TimetableGridRoomColumnGap = 8.dp

internal const val TimetableGridDayStartMinutes = 10 * 60
internal const val TimetableGridDefaultDayEndMinutes = 18 * 60

internal enum class TimetableGridScale(val hourHeight: Dp) {
    Default(TimetableGridDefaultHourHeight),
    Expanded(TimetableGridExpandedHourHeight),
}

internal fun String.toTimetableGridMinuteOfDay(): Int {
    val hour = substringBefore(":").toInt()
    val minute = substringAfter(":").toInt()
    return hour * 60 + minute
}

internal fun timetableGridMinuteOffsetY(
    minute: Int,
    hourHeight: Dp,
): Dp {
    val startOffsetMinutes = minute - TimetableGridDayStartMinutes
    return TimetableGridHeaderHeight + (hourHeight.value * startOffsetMinutes / 60f).dp
}

internal fun timetableGridSessionOffsetY(
    startsAt: String,
    hourHeight: Dp = TimetableGridDefaultHourHeight,
): Dp {
    return timetableGridMinuteOffsetY(
        minute = startsAt.toTimetableGridMinuteOfDay(),
        hourHeight = hourHeight,
    )
}

internal fun timetableGridSessionHeight(
    startsAt: String,
    endsAt: String,
    hourHeight: Dp = TimetableGridDefaultHourHeight,
): Dp {
    val durationMinutes = endsAt.toTimetableGridMinuteOfDay() - startsAt.toTimetableGridMinuteOfDay()
    return (hourHeight.value * durationMinutes / 60f).dp - TimetableGridSessionGap
}

internal fun timetableGridContentHeight(
    endMinute: Int,
    hourHeight: Dp = TimetableGridDefaultHourHeight,
): Dp {
    val durationMinutes = endMinute - TimetableGridDayStartMinutes
    return TimetableGridHeaderHeight + (hourHeight.value * durationMinutes / 60f).dp
}

internal fun Int.toTimetableGridTimeLabel(): String {
    val hour = this / 60
    val minute = this % 60
    return "$hour:${minute.toString().padStart(2, '0')}"
}
