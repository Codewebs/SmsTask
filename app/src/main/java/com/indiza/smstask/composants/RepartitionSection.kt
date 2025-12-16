package com.indiza.smstask.composants

import android.app.Application
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.indiza.smstask.viewmodel.StatsViewModel
import kotlin.math.min

@Composable
fun RepartitionSection(
    modifier: Modifier = Modifier,
    percentSent: Float = 0.85f, // 85%
) {


    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        DonutChart(
            progress = percentSent,
            modifier = Modifier.size(130.dp)
        )

        Spacer(Modifier.width(20.dp))

    }
}

@Composable
fun DonutChart(
    progress: Float,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {

        Canvas(modifier = Modifier.fillMaxSize()) {

            val stroke = 22.dp.toPx()
            val size = min(size.width, size.height)

            val rect = Rect(
                left = (size - size) / 2f,
                top = (size - size) / 2f,
                right = size,
                bottom = size
            )

            // 🔵 Envoyés
            drawArc(
                color = Color(0xFF00BCD4),
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = rect.topLeft,
                size = Size(rect.width, rect.height),
                style = Stroke(stroke)
            )

            // 🟡 En attente
            drawArc(
                color = Color(0xFFFFC107),
                startAngle = -90f + (360f * progress),
                sweepAngle = 360f * (0.10f),
                useCenter = false,
                topLeft = rect.topLeft,
                size = Size(rect.width, rect.height),
                style = Stroke(stroke)
            )

            // 🔴 Échecs
            drawArc(
                color = Color(0xFFF44336),
                startAngle = -90f + (360f * (progress + 0.10f)),
                sweepAngle = 360f * (0.05f),
                useCenter = false,
                topLeft = rect.topLeft,
                size = Size(rect.width, rect.height),
                style = Stroke(stroke)
            )
        }

        Text(
            text = "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.titleLarge.copy(
                color = Color(0xFF00FF7F),
                fontWeight = FontWeight.Bold
            )
        )
    }
}


// Solution 1: Utiliser RowScope pour avoir accès à weight()
@Composable
fun RowScope.HeaderCell(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .weight(1f)
            .padding(4.dp),
        color = Color.LightGray,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun RowScope.TableCell(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .weight(1f)
            .padding(4.dp),
        color = Color.White
    )
}

// Solution alternative 2: Si vous préférez garder Box, utilisez aussi RowScope
@Composable
fun RowScope.HeaderCellWithBox(text: String) {
    Box(
        modifier = Modifier
            .weight(1f)
            .padding(4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            color = Color.LightGray,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun RowScope.TableCellWithBox(text: String) {
    Box(
        modifier = Modifier
            .weight(1f)
            .padding(4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            color = Color.White
        )
    }
}

@Composable
fun TableRow(h: String, s: String, err: String, failed: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Utilisez HeaderCell et TableCell avec RowScope
        HeaderCell(h)
        TableCell(s)
        TableCell(err)
        TableCell(failed)
    }
}