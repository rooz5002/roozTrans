package com.rooz.translator

import android.app.Activity
import android.content.Context
import android.graphics.Color

/** A colour scheme for the whole app. Keep in sync with res/values/themes.xml (same colours). */
data class Palette(
    val id: String, val name: String, val style: Int, val dark: Boolean,
    val bg: Int, val surface: Int, val text: Int, val muted: Int, val accent: Int, val onAccent: Int, val line: Int,
)

object Themes {
    private fun c(s: String) = Color.parseColor(s)
    val all = listOf(
        Palette("clay", "Clay", R.style.Theme_Clay, false, c("#FAF9F5"), c("#F0EEE6"), c("#1F1E1D"), c("#8A877F"), c("#C96442"), c("#FFFFFF"), c("#E5E2D9")),
        Palette("red", "China red", R.style.Theme_Red, false, c("#FFFFFF"), c("#F4F0EF"), c("#1C1B1B"), c("#8A8483"), c("#C62828"), c("#FFFFFF"), c("#E8E1E0")),
        Palette("ink", "Ink", R.style.Theme_Ink, false, c("#FFFFFF"), c("#F2F2F2"), c("#111111"), c("#8C8C8C"), c("#111111"), c("#FFFFFF"), c("#E3E3E3")),
        Palette("ocean", "Ocean", R.style.Theme_Ocean, false, c("#FFFFFF"), c("#EEF3FA"), c("#14202E"), c("#7D8A99"), c("#1F6FEB"), c("#FFFFFF"), c("#DCE5F0")),
        Palette("jade", "Jade", R.style.Theme_Jade, false, c("#FBFCFB"), c("#ECF3EF"), c("#15221C"), c("#7F8F87"), c("#0E8A64"), c("#FFFFFF"), c("#DAE6DF")),
        Palette("plum", "Plum", R.style.Theme_Plum, false, c("#FFFFFF"), c("#F4F0F8"), c("#1E1A24"), c("#8A8294"), c("#7A4CC2"), c("#FFFFFF"), c("#E6DEEF")),
        Palette("midnight", "Midnight", R.style.Theme_Midnight, true, c("#1A1A1E"), c("#27272D"), c("#ECECEC"), c("#8E8E96"), c("#E3A857"), c("#1A1A1E"), c("#34343B")),
        Palette("graphite", "Graphite", R.style.Theme_Graphite, true, c("#121212"), c("#1F1F1F"), c("#EDEDED"), c("#8A8A8A"), c("#FF5A52"), c("#121212"), c("#2E2E2E")),
    )
    fun current(ctx: Context): Palette = Prefs(ctx).theme.let { id -> all.firstOrNull { it.id == id } ?: all[0] }
    /** Call before super.onCreate(); returns the id applied so the activity can notice a later change. */
    fun apply(a: Activity): String = current(a).also { a.setTheme(it.style) }.id
}
