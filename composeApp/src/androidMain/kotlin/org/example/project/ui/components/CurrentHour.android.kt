package org.example.project.ui.components

import java.util.Calendar

actual fun getCurrentHour(): Int =
    Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
