package com.zhi.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhi.app.ui.theme.GreenLight
import com.zhi.app.ui.theme.GreenSuccess
import com.zhi.app.ui.theme.RedDanger
import com.zhi.app.ui.theme.RedLight

@Composable
fun ServerStatusBadge(
    isRegistered: Boolean,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isRegistered) GreenLight else RedLight
    val dotColor = if (isRegistered) GreenSuccess else RedDanger
    val textColor = if (isRegistered) Color(0xFF065F46) else Color(0xFF991B1B)
    val text = if (isRegistered) "已同步到服务器" else "未同步 / 离线"

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
