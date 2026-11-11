package com.rooz.translator

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint

/**
 * Paints translated text boxes on top of the original text positions. Shared by screen overlay and photo view.
 * [density] = canvas pixels per dp *as the user sees it* (so text never gets absurdly big or small).
 */
class OverlayRenderer(private val density: Float) {
    private val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(255, 255, 253, 231) }
    private val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(255, 198, 40, 40); style = Paint.Style.STROKE; strokeWidth = 1.2f * density
    }
    private val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(20, 20, 20) }

    /** Returns the drawn boxes (for hit-testing), in canvas coordinates. */
    fun draw(c: Canvas, items: List<TextItem>, dx: Float = 0f, dy: Float = 0f, scale: Float = 1f, showOriginal: Boolean = false): List<Pair<RectF, TextItem>> {
        val hits = ArrayList<Pair<RectF, TextItem>>()
        val minPx = 7f * density
        val pad = 2f * density
        val rr = 4f * density
        for (it in items) {
            val text = (if (showOriginal) it.original else it.translated) ?: continue
            val r = RectF(it.rect.left * scale + dx - pad, it.rect.top * scale + dy - pad,
                          it.rect.right * scale + dx + pad, it.rect.bottom * scale + dy + pad)
            val startSize = (it.lineHeight * scale * 0.8f).coerceIn(minPx, 40f * density)

            if (it.lineCount <= 1) {
                // Buttons, labels, menu items: keep ONE line, shrink to fit, and if still too long
                // widen the box sideways around its centre instead of wrapping onto a line below.
                tp.textSize = startSize
                val avail = r.width() - pad * 2
                val w0 = tp.measureText(text)
                if (w0 > avail) tp.textSize = (startSize * avail / w0).coerceAtLeast(maxOf(minPx, startSize * 0.6f))
                val need = tp.measureText(text) + pad * 2
                if (need > r.width()) {
                    val cx = r.centerX(); r.left = cx - need / 2; r.right = cx + need / 2
                    // keep inside the canvas
                    if (r.left < 0) r.offset(-r.left, 0f)
                    if (r.right > c.width) r.offset(c.width - r.right, 0f)
                }
                val fm = tp.fontMetrics
                if (fm.descent - fm.ascent > r.height()) { val cy = r.centerY(); val h = fm.descent - fm.ascent; r.top = cy - h / 2; r.bottom = cy + h / 2 }
                c.drawRoundRect(r, rr, rr, bg); c.drawRoundRect(r, rr, rr, border)
                val baseline = r.centerY() - (fm.ascent + fm.descent) / 2
                c.drawText(text, r.centerX() - tp.measureText(text) / 2, baseline, tp)
            } else {
                // Paragraphs: wrap inside the original block, shrinking text to fit.
                val w = r.width().toInt().coerceAtLeast((30 * density).toInt())
                var size = startSize
                var layout: StaticLayout
                while (true) {
                    tp.textSize = size
                    layout = StaticLayout.Builder.obtain(text, 0, text.length, tp, w - (pad * 2).toInt())
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL).setIncludePad(false).build()
                    if (layout.height <= r.height() || size <= minPx) break
                    size = (size * 0.9f).coerceAtLeast(minPx)
                }
                if (layout.height > r.height()) r.bottom = r.top + layout.height + pad
                c.drawRoundRect(r, rr, rr, bg); c.drawRoundRect(r, rr, rr, border)
                c.save(); c.translate(r.left + pad, r.top + ((r.height() - layout.height) / 2f).coerceAtLeast(0f))
                layout.draw(c); c.restore()
            }
            hits += r to it
        }
        return hits
    }
}
