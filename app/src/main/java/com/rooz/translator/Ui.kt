package com.rooz.translator

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.button.MaterialButton

/** Tiny helpers so the screens can be built in code without XML. */
fun Context.dp(v: Int) = (v * resources.displayMetrics.density).toInt()

fun Context.vbox(pad: Int = 0, block: LinearLayout.() -> Unit = {}) = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL; setPadding(dp(pad), dp(pad), dp(pad), dp(pad)); block()
}
fun Context.hbox(block: LinearLayout.() -> Unit = {}) = LinearLayout(this).apply {
    orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL; block()
}
fun Context.card(block: LinearLayout.() -> Unit) = vbox(14, block).apply {
    background = rounded(Themes.current(context).surface, 16)
    layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        .apply { setMargins(0, 0, 0, dp(12)) }
}
fun Context.rounded(fill: Int, radiusDp: Int, stroke: Int? = null, strokeDp: Int = 1) = GradientDrawable().apply {
    setColor(fill); cornerRadius = dp(radiusDp).toFloat(); stroke?.let { setStroke(dp(strokeDp), it) }
}
fun Context.label(s: String, size: Float = 15f, bold: Boolean = false) = TextView(this).apply {
    text = s; textSize = size; if (bold) setTypeface(typeface, Typeface.BOLD)
}
fun Context.button(s: String, outlined: Boolean = false, onClick: () -> Unit) =
    MaterialButton(this, null, if (outlined) com.google.android.material.R.attr.materialButtonOutlinedStyle
                               else com.google.android.material.R.attr.materialButtonStyle).apply {
        text = s; isAllCaps = false; setOnClickListener { onClick() }
    }
fun LinearLayout.weighted(v: View, w: Float = 1f, marginEnd: Int = 0) =
    addView(v, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, w).apply { setMargins(0, 0, context.dp(marginEnd), 0) })
