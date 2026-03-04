package org.example.project.ui.components

import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitHour

actual fun getCurrentHour(): Int {
    val calendar = NSCalendar.currentCalendar
    val components = calendar.components(NSCalendarUnitHour, fromDate = platform.Foundation.NSDate())
    return components.hour.toInt()
}
