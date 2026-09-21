package com.example.kotlin_holy.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kotlin_holy.ui.theme.HolyTheme

/** «Tartib: Oʻsish / Kamayish» — veb nusxadagi kabi */
@Composable
fun OrderToggle(
    order: String,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HolyTheme.colors
    val ascending = order != "desc"
    val angle by animateFloatAsState(if (ascending) 0f else 180f, label = "order-arrow")

    Row(
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onToggle)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            "Tartib:",
            color = colors.inkFaint,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            if (ascending) "Oʻsish" else "Kamayish",
            color = colors.ink,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Icon(
            imageVector = Icons.Filled.KeyboardArrowUp,
            contentDescription = null,
            tint = colors.inkSoft,
            modifier = Modifier.size(16.dp).rotate(angle),
        )
    }
}

/** Yoqib-o'chiriladigan filtr tugmasi, masalan «Faqat oʻqilmaganlar» */
@Composable
fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HolyTheme.colors
    Text(
        text = label,
        color = if (selected) colors.accent else colors.inkSoft,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .clip(CircleShape)
            .background(if (selected) colors.accent.copy(alpha = 0.12f) else Color.Transparent)
            .border(
                1.dp,
                if (selected) colors.accent.copy(alpha = 0.5f) else colors.line,
                CircleShape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}
