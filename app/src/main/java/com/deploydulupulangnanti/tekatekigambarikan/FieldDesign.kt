package com.deploydulupulangnanti.tekatekigambarikan

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val Ink = Color(0xFF102E36)
internal val InkDeep = Color(0xFF09232B)
internal val InkLift = Color(0xFF193B43)
internal val InkLine = Color(0xFF376069)
internal val Paper = Color(0xFFF8EFDB)
internal val PaperDim = Color(0xFFF0DFC2)
internal val Gold = Color(0xFFF0BD72)
internal val SeaGlass = Color(0xFFACD5C5)
internal val Mist = Color(0xFFB5CDCC)
internal val DarkMuted = Color(0xFF46616A)
internal val Rust = Color(0xFF9B4736)
internal val Serif = FontFamily(Font(R.font.field_serif))

@Composable
internal fun Label(text: String, color: Color = SeaGlass, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier,
        color = color,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.7.sp,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
internal fun Headline(text: String, size: Int, color: Color = Paper, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier,
        color = color,
        fontFamily = Serif,
        fontSize = size.sp,
        lineHeight = (size * 1.12f).sp,
    )
}

@Composable
internal fun PrimaryAction(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = InkDeep),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
            FieldGlyph(Glyph.Forward, InkDeep, Modifier.size(19.dp))
        }
    }
}

@Composable
internal fun FishArt(fish: Fish, description: String?, modifier: Modifier = Modifier) {
    val assets = LocalContext.current.assets
    val bitmap: ImageBitmap = remember(fish.id, assets) {
        assets.open("fish/${fish.id}.webp").use { stream ->
            android.graphics.BitmapFactory.decodeStream(stream).asImageBitmap()
        }
    }
    Image(
        bitmap,
        contentDescription = description,
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}

@Composable
internal fun CurrentLines(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        repeat(9) { index ->
            val y = size.height * (0.21f + 0.083f * index)
            val path = Path().apply {
                moveTo(0f, y + size.height * 0.12f)
                cubicTo(size.width * 0.29f, y - size.height * 0.18f, size.width * 0.60f, y + size.height * 0.19f, size.width, y)
            }
            drawPath(path, SeaGlass.copy(alpha = 0.13f), style = Stroke(width = 1.dp.toPx()))
        }
    }
}

@Composable
internal fun Rule(modifier: Modifier = Modifier, color: Color = InkLine) {
    Canvas(modifier.fillMaxWidth().size(height = 1.dp, width = 1.dp)) {
        drawLine(color, Offset.Zero, Offset(size.width, 0f), strokeWidth = 1.dp.toPx())
    }
}

@Composable
internal fun Stamp(text: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(5.dp)
    Box(modifier.clip(shape).background(InkDeep).border(1.dp, InkLine, shape).padding(horizontal = 8.dp, vertical = 5.dp)) {
        Label(text, Gold)
    }
}

internal enum class Glyph { Dive, Book, Settings, Sound, Back, Forward, Check }

@Composable
internal fun FieldGlyph(glyph: Glyph, color: Color, modifier: Modifier = Modifier) {
    val mirror = LocalLayoutDirection.current == LayoutDirection.Rtl && (glyph == Glyph.Back || glyph == Glyph.Forward)
    Canvas(modifier) {
        val unit = size.minDimension / 24f
        val origin = Offset((size.width - 24 * unit) / 2, (size.height - 24 * unit) / 2)
        fun pt(x: Float, y: Float) = origin + Offset((if (mirror) 24f - x else x) * unit, y * unit)
        fun line(vararg points: Pair<Float, Float>) {
            for (i in 0 until points.lastIndex) {
                val (x, y) = points[i]
                val (nextX, nextY) = points[i + 1]
                drawLine(color, pt(x, y), pt(nextX, nextY), 1.7f * unit, StrokeCap.Round)
            }
        }
        val stroke = Stroke(1.7f * unit, cap = StrokeCap.Round)
        when (glyph) {
            Glyph.Dive -> {
                drawCircle(color, 9f * unit, pt(12f, 12f), style = stroke)
                line(15.5f to 7.5f, 13.8f to 13.8f, 8.5f to 16.5f, 10.3f to 10.2f, 15.5f to 7.5f)
            }
            Glyph.Book -> {
                line(12f to 6f, 8f to 4.8f, 3f to 4.8f, 3f to 18f, 8f to 18f, 12f to 19.4f, 16f to 18f, 21f to 18f, 21f to 4.8f, 16f to 4.8f, 12f to 6f, 12f to 19.4f)
            }
            Glyph.Settings -> {
                listOf(6f to 9f, 12f to 15f, 18f to 10f).forEach { (y, knob) ->
                    line(3f to y, 21f to y)
                    line(knob to y - 2.5f, knob to y + 2.5f)
                }
            }
            Glyph.Sound -> {
                line(3f to 9f, 7f to 9f, 11f to 5f, 11f to 19f, 7f to 15f, 3f to 15f, 3f to 9f)
                line(15f to 8f, 17f to 12f, 15f to 16f)
                line(18f to 5f, 21f to 12f, 18f to 19f)
            }
            Glyph.Back -> {
                line(18f to 12f, 5f to 12f, 11f to 6f)
                line(5f to 12f, 11f to 18f)
            }
            Glyph.Forward -> {
                line(5f to 12f, 19f to 12f, 13f to 6f)
                line(19f to 12f, 13f to 18f)
            }
            Glyph.Check -> line(5f to 12f, 10f to 17f, 19f to 7f)
        }
    }
}
