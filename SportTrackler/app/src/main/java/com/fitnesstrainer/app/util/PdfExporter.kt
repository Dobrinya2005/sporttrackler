package com.fitnesstrainer.app.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.fitnesstrainer.app.data.model.MeasurementResponse
import com.fitnesstrainer.app.data.model.MealSummary
import java.io.File
import java.io.FileOutputStream

object PdfExporter {

    fun exportMeasurements(context: Context, items: List<MeasurementResponse>, firstName: String) {
        val doc  = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        val canvas = page.canvas

        val paintTitle = Paint().apply {
            textSize  = 20f
            isFakeBoldText = true
            color     = Color.parseColor("#002B36")
        }
        val paintHead = Paint().apply {
            textSize = 12f
            isFakeBoldText = true
            color    = Color.parseColor("#5B6AF0")
        }
        val paintBody = Paint().apply {
            textSize = 11f
            color    = Color.parseColor("#657B83")
        }
        val paintLine = Paint().apply {
            color     = Color.parseColor("#CBD0D4")
            strokeWidth = 1f
        }

        var y = 60f
        canvas.drawText("SportTrackler — Замеры тела", 40f, y, paintTitle)
        y += 20f
        canvas.drawText("Клиент: $firstName", 40f, y, paintBody)
        y += 30f
        canvas.drawLine(40f, y, 555f, y, paintLine)
        y += 20f

        // Header
        canvas.drawText("Дата", 40f, y, paintHead)
        canvas.drawText("Вес", 140f, y, paintHead)
        canvas.drawText("ИМТ", 200f, y, paintHead)
        canvas.drawText("Талия", 260f, y, paintHead)
        canvas.drawText("Грудь", 330f, y, paintHead)
        canvas.drawText("% жира", 400f, y, paintHead)
        y += 16f
        canvas.drawLine(40f, y, 555f, y, paintLine)
        y += 14f

        for (item in items.take(30)) {
            canvas.drawText(item.measuredAt.take(10), 40f, y, paintBody)
            canvas.drawText(item.weightKg?.let { "%.1f кг".format(it) } ?: "—", 140f, y, paintBody)
            canvas.drawText(item.bmi?.let { "%.1f".format(it) } ?: "—", 200f, y, paintBody)
            canvas.drawText(item.waistCm?.let { "%.0f".format(it) } ?: "—", 260f, y, paintBody)
            canvas.drawText(item.chestCm?.let { "%.0f".format(it) } ?: "—", 330f, y, paintBody)
            canvas.drawText(item.bodyFatPercent?.let { "%.1f%%".format(it) } ?: "—", 400f, y, paintBody)
            y += 20f
            if (y > 800f) break
        }

        doc.finishPage(page)

        val file = File(context.cacheDir, "measurements_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Поделиться PDF"))
    }

    fun exportFoodDiary(context: Context, meals: List<MealSummary>, date: String, firstName: String) {
        val doc    = PdfDocument()
        val page   = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        val canvas = page.canvas

        val paintTitle = Paint().apply { textSize = 20f; isFakeBoldText = true; color = Color.parseColor("#002B36") }
        val paintHead  = Paint().apply { textSize = 13f; isFakeBoldText = true; color = Color.parseColor("#5B6AF0") }
        val paintMeal  = Paint().apply { textSize = 12f; isFakeBoldText = true; color = Color.parseColor("#002B36") }
        val paintBody  = Paint().apply { textSize = 11f; color = Color.parseColor("#657B83") }
        val paintLine  = Paint().apply { color = Color.parseColor("#CBD0D4"); strokeWidth = 1f }
        val paintTotal = Paint().apply { textSize = 12f; isFakeBoldText = true; color = Color.parseColor("#22C55E") }

        var y = 60f
        canvas.drawText("SportTrackler — Дневник питания", 40f, y, paintTitle)
        y += 20f
        canvas.drawText("$firstName  |  $date", 40f, y, paintBody)
        y += 30f
        canvas.drawLine(40f, y, 555f, y, paintLine)
        y += 20f

        // Column headers
        canvas.drawText("Продукт", 40f, y, paintHead)
        canvas.drawText("г", 300f, y, paintHead)
        canvas.drawText("ккал", 340f, y, paintHead)
        canvas.drawText("Б", 400f, y, paintHead)
        canvas.drawText("Ж", 440f, y, paintHead)
        canvas.drawText("У", 480f, y, paintHead)
        y += 16f
        canvas.drawLine(40f, y, 555f, y, paintLine)
        y += 14f

        var totalCal = 0.0; var totalP = 0.0; var totalF = 0.0; var totalC = 0.0

        val mealNames = mapOf("Breakfast" to "Завтрак", "Lunch" to "Обед", "Dinner" to "Ужин", "Snack" to "Перекус")
        for (meal in meals) {
            if (meal.items.isEmpty()) continue
            canvas.drawText(mealNames[meal.mealType] ?: meal.mealType, 40f, y, paintMeal)
            y += 18f
            for (entry in meal.items) {
                if (y > 800f) break
                val name = if (entry.product.name.length > 32) entry.product.name.take(32) + "…" else entry.product.name
                canvas.drawText(name, 50f, y, paintBody)
                canvas.drawText("%.0f".format(entry.weightG), 300f, y, paintBody)
                canvas.drawText("%.0f".format(entry.calories), 340f, y, paintBody)
                canvas.drawText("%.1f".format(entry.protein), 400f, y, paintBody)
                canvas.drawText("%.1f".format(entry.fat), 440f, y, paintBody)
                canvas.drawText("%.1f".format(entry.carbs), 480f, y, paintBody)
                y += 16f
                totalCal += entry.calories; totalP += entry.protein; totalF += entry.fat; totalC += entry.carbs
            }
            y += 4f
        }

        y += 8f
        canvas.drawLine(40f, y, 555f, y, paintLine)
        y += 16f
        canvas.drawText("ИТОГО", 40f, y, paintTotal)
        canvas.drawText("%.0f ккал".format(totalCal), 300f, y, paintTotal)
        canvas.drawText("Б %.1f".format(totalP), 380f, y, paintBody)
        canvas.drawText("Ж %.1f".format(totalF), 430f, y, paintBody)
        canvas.drawText("У %.1f".format(totalC), 480f, y, paintBody)

        doc.finishPage(page)

        val file = File(context.cacheDir, "food_diary_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Поделиться PDF"))
    }
}
