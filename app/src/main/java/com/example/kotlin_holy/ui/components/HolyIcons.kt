package com.example.kotlin_holy.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Veb nusxadagi chiziqli SVG belgilar (viewBox 0 0 24 24, stroke="currentColor").
 * Yo'llar aynan o'sha; faqat yopishgan yoy bayroqlari ("a1 1 0 011 1") ajratib yozilgan.
 * Rang Icon(tint = …) orqali beriladi.
 */
object HolyIcons {

    private fun circle(cx: Float, cy: Float, r: Float) =
        "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0Z"

    private fun rect(x: Float, y: Float, w: Float, h: Float, rx: Float) =
        "M${x + rx} ${y}H${x + w - rx}a$rx $rx 0 0 1 $rx ${rx}V${y + h - rx}a$rx $rx 0 0 1 ${-rx} ${rx}" +
            "H${x + rx}a$rx $rx 0 0 1 ${-rx} ${-rx}V${y + rx}a$rx $rx 0 0 1 $rx ${-rx}Z"

    private fun icon(name: String, strokeWidth: Float, vararg paths: String, filled: Boolean = false): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            paths.forEach { d ->
                addPath(
                    pathData = addPathNodes(d),
                    fill = if (filled) SolidColor(Color.Black) else null,
                    stroke = if (filled) null else SolidColor(Color.Black),
                    strokeLineWidth = strokeWidth,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

    val Menu = icon("menu", 2f, "M4 6h16M4 12h16M4 18h16")

    val Settings = icon(
        "settings", 1.8f,
        circle(12f, 12f, 3f),
        "M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 " +
            "1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09A1.65 1.65 0 0 0 8 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06" +
            "a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09" +
            "A1.65 1.65 0 0 0 4.6 8a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 3.6 " +
            "1.65 1.65 0 0 0 10 2.09V2a2 2 0 1 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06" +
            "a2 2 0 1 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 8v0a1.65 1.65 0 0 0 1.51 1H21a2 2 0 1 1 0 4h-.09" +
            "a1.65 1.65 0 0 0-1.51 1z",
    )

    val Sun = icon(
        "sun", 2f,
        circle(12f, 12f, 4f),
        "M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4",
    )

    val Moon = icon("moon", 2f, "M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z")

    val Monitor = icon("monitor", 2f, rect(2f, 4f, 20f, 13f, 2f), "M8 21h8M12 17v4")

    val Bookmark = icon("bookmark", 2f, "M6 3h12a1 1 0 0 1 1 1v17l-7-4-7 4V4a1 1 0 0 1 1-1z")
    val BookmarkFilled = icon("bookmark-filled", 2f, "M6 3h12a1 1 0 0 1 1 1v17l-7-4-7 4V4a1 1 0 0 1 1-1z", filled = true)

    val ChevronLeft = icon("chevron-left", 2f, "M15 18l-6-6 6-6")
    val ChevronRight = icon("chevron-right", 2f, "M9 18l6-6-6-6")
    val ChevronUp = icon("chevron-up", 2.5f, "M18 15l-6-6-6 6")
    val ChevronDown = icon("chevron-down", 2.5f, "M6 9l6 6 6-6")

    val Search = icon("search", 2f, circle(11f, 11f, 7f), "M20 20l-3.5-3.5")
    val Check = icon("check", 3f, "M5 12.5l4.5 4.5L19 7.5")
    val Close = icon("close", 2f, "M6 6l12 12M18 6L6 18")
    val Plus = icon("plus", 2f, "M12 5v14M5 12h14")
    val Trash = icon("trash", 2f, "M3 6h18M8 6V4h8v2M19 6l-1 14H6L5 6M10 11v6M14 11v6")

    val Grid = icon(
        "grid", 2f,
        rect(3f, 3f, 7f, 7f, 1.5f), rect(14f, 3f, 7f, 7f, 1.5f),
        rect(3f, 14f, 7f, 7f, 1.5f), rect(14f, 14f, 7f, 7f, 1.5f),
    )
    val ListView = icon("list", 2f, "M8 6h13M8 12h13M8 18h13M3.5 6h.01M3.5 12h.01M3.5 18h.01")

    val Refresh = icon("refresh", 2f, "M21 12a9 9 0 1 1-2.64-6.36", "M21 3v6h-6")
    val Download = icon("download", 2f, "M12 3v12M7 10l5 5 5-5M5 21h14")
    val Clock = icon("clock", 2f, circle(12f, 12f, 9f), "M12 7v5l3 2")

    /* ---- Yon panel bo'limlari ---- */
    val Surahs = icon("surahs", 1.8f, "M4 5.5A2.5 2.5 0 0 1 6.5 3H20v15H6.5A2.5 2.5 0 0 0 4 20.5z", "M4 5.5v15")
    val Pages = icon("pages", 1.8f, rect(4f, 3f, 16f, 18f, 2f), "M8 8h8M8 12h8M8 16h5")
    val Juz = icon(
        "juz", 1.8f,
        "M3 6h7a3 3 0 0 1 3 3v10a2.5 2.5 0 0 0-2.5-2.5H3z",
        "M21 6h-3a3 3 0 0 0-3 3v10a2.5 2.5 0 0 1 2.5-2.5H21z",
    )
    val Hadith = icon("hadith", 1.8f, "M4 4v16M8 8v12M12 6v14M16 6l4 14")
    val Library = icon(
        "library", 1.8f,
        "M4 19.5v-15A2.5 2.5 0 0 1 6.5 2H19a1 1 0 0 1 1 1v18a1 1 0 0 1-1 1H6.5a1 1 0 0 1 0-5H20",
        "M10 2v8l3-3 3 3V2",
    )
    val Words = icon(
        "words", 1.8f,
        "M4 19.5A2.5 2.5 0 0 1 6.5 17H20",
        "M6.5 3H20v18H6.5A2.5 2.5 0 0 1 4 18.5v-13A2.5 2.5 0 0 1 6.5 3z",
        "M9 7.5h7M9 11h5",
    )

    /* ---- Qiroat (faqat Android nusxasida) ---- */
    val Play = icon("play", 2f, "M7 4.5v15l12.5-7.5z", filled = true)
    val Pause = icon("pause", 2f, rect(6f, 4.5f, 4f, 15f, 1f), rect(14f, 4.5f, 4f, 15f, 1f), filled = true)
    val Stop = icon("stop", 2f, rect(5.5f, 5.5f, 13f, 13f, 2f), filled = true)
    val Headphones = icon(
        "headphones", 2f,
        "M3 18v-6a9 9 0 0 1 18 0v6",
        "M21 19a2 2 0 0 1-2 2h-1a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2h3zM3 19a2 2 0 0 0 2 2h1a2 2 0 0 0 2-2v-3a2 2 0 0 0-2-2H3z",
    )
}
