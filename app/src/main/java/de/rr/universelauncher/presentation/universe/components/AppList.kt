package de.rr.universelauncher.presentation.universe.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.rr.universelauncher.domain.model.AppInfo
import androidx.core.graphics.createBitmap

@Composable
fun AppList(
    apps: List<AppInfo>,
    onAppClicked: (AppInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxHeight()
            .background(
                Color.Black.copy(alpha = 0.7f),
                RoundedCornerShape(8.dp)
            )
            .padding(8.dp)
    ) {
        items(apps) { app ->
            AppListItem(
                app = app,
                onClick = { onAppClicked(app) }
            )
        }
    }
}

@Composable
private fun AppListItem(
    app: AppInfo,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = rememberDrawablePainter(app.icon),
            contentDescription = app.appName,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = app.appName,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = app.packageName,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun rememberDrawablePainter(drawable: android.graphics.drawable.Drawable): androidx.compose.ui.graphics.painter.Painter {
    return remember(drawable) {
        val bitmap = createBitmap(64, 64)
        val canvas = android.graphics.Canvas(bitmap)
        drawable.setBounds(0, 0, 64, 64)
        drawable.draw(canvas)
        BitmapPainter(bitmap.asImageBitmap())
    }
}