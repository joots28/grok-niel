package com.otiniel.copy.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.otiniel.copy.ui.theme.CopyBorder
import com.otiniel.copy.ui.theme.CopyMuted
import com.otiniel.copy.ui.theme.CopyPrimary
import com.otiniel.copy.ui.theme.CopySurface
import com.otiniel.copy.ui.theme.CopySurfaceRaised
import com.otiniel.copy.ui.theme.CopyText

@Composable
fun CopyWordmark(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            modifier = Modifier.size(38.dp),
            color = CopySurfaceRaised,
            shape = RoundedCornerShape(13.dp),
            border = BorderStroke(1.dp, CopyBorder),
        ) {
            androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "C/",
                    color = CopyPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text("COPY", fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.6.sp)
            Text("PERSONAL ASSISTANT", fontSize = 9.sp, color = CopyMuted, letterSpacing = 1.1.sp)
        }
    }
}

@Composable
fun PageHeader(
    eyebrow: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(
            text = eyebrow.uppercase(),
            color = CopyPrimary,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.sp,
        )
        Text(
            text = title,
            color = CopyText,
            fontSize = 28.sp,
            lineHeight = 33.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(text = subtitle, color = CopyMuted, fontSize = 13.sp, lineHeight = 19.sp)
    }
}

@Composable
fun DemoLabel(text: String = "MODO DEMONSTRAÇÃO") {
    Surface(
        color = CopyPrimary.copy(alpha = 0.12f),
        shape = CircleShape,
        border = BorderStroke(1.dp, CopyPrimary.copy(alpha = 0.28f)),
    ) {
        Text(
            text = text.uppercase(),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            color = CopyPrimary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
        )
    }
}

@Composable
fun AccentCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CopySurface),
        border = BorderStroke(1.dp, CopyBorder),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
fun LabelValue(label: String, value: String, valueColor: Color = CopyText) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label.uppercase(), color = CopyMuted, fontSize = 9.sp, letterSpacing = 1.1.sp)
        Text(value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
