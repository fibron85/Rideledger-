package com.example.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.DriverAccountEntity
import com.example.data.local.entity.RideEntity
import com.example.domain.RideCalculator
import com.example.ui.viewmodel.DailyReportGroup
import com.example.ui.viewmodel.PeriodSummary
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.xml.parsers.DocumentBuilderFactory

object ReportExporter {

    fun exportToPdf(
        context: Context,
        uri: Uri,
        driver: DriverAccountEntity?,
        settings: AppSettingsEntity,
        periodLabel: String,
        summary: PeriodSummary,
        dailyGroups: List<DailyReportGroup>
    ): Result<Unit> {
        return runCatching {
            val bytes = buildPdfBytes(driver, settings, periodLabel, summary, dailyGroups)
            val outStream = runCatching {
                context.contentResolver.openOutputStream(uri, "w")
            }.getOrNull() ?: context.contentResolver.openOutputStream(uri)
                ?: error("Unable to open output stream for PDF.")
            outStream.use { out ->
                out.write(bytes)
                out.flush()
            }
        }
    }

    fun buildPdfBytes(
        driver: DriverAccountEntity?,
        settings: AppSettingsEntity,
        periodLabel: String,
        summary: PeriodSummary,
        dailyGroups: List<DailyReportGroup>
    ): ByteArray {
        val pdf = PdfDocument()
        try {
            val pageWidth = 595
            val pageHeight = 842
            val margin = 32f
            val contentWidth = pageWidth - (margin * 2)

            val titlePaint = Paint().apply {
                color = Color.parseColor("#0F172A")
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val subTitlePaint = Paint().apply {
                color = Color.parseColor("#475569")
                textSize = 10.5f
            }
            val sectionPaint = Paint().apply {
                color = Color.parseColor("#059669")
                textSize = 12.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val headerBgPaint = Paint().apply {
                color = Color.parseColor("#0F172A")
            }
            val headerTextPaint = Paint().apply {
                color = Color.WHITE
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val altRowPaint = Paint().apply {
                color = Color.parseColor("#F1F5F9")
            }
            val cellPaint = Paint().apply {
                color = Color.parseColor("#1E293B")
                textSize = 8.5f
            }
            val boldCellPaint = Paint().apply {
                color = Color.parseColor("#0F172A")
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val linePaint = Paint().apply {
                color = Color.parseColor("#CBD5E1")
                strokeWidth = 0.8f
            }

            var pageNumber = 1
            var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            var canvas = page.canvas
            var y = margin + 12f

            fun startNewPage() {
                pdf.finishPage(page)
                pageNumber++
                page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                canvas = page.canvas
                y = margin + 12f
            }

            fun ensureSpace(needed: Float) {
                if (y + needed > pageHeight - margin) {
                    startNewPage()
                }
            }

            // Document Header
            canvas.drawText("RideLedger — Driver Income Report", margin, y, titlePaint)
            y += 18f
            val driverLabel = if (!driver?.fullName.isNullOrBlank()) {
                "${driver?.fullName} (ID: ${driver?.driverId ?: "-"})"
            } else {
                "Driver ID: ${driver?.driverId ?: "-"}"
            }
            canvas.drawText("$driverLabel  |  Period: $periodLabel", margin, y, subTitlePaint)
            y += 14f
            canvas.drawText(
                "Shift Timing: ${RideCalculator.formatShiftWindow(settings)}  |  Fuel Rate: ${RideCalculator.formatMoney(settings.fuelCostPerKm, settings.currencyCode)}/km",
                margin,
                y,
                subTitlePaint
            )
            y += 16f
            canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
            y += 18f

            // Period Summary Block
            canvas.drawText("Period Totals (${settings.currencyCode})", margin, y, sectionPaint)
            y += 16f
            val summaryLines = listOf(
                "Rides: ${summary.rideCount}   |   Distance: ${RideCalculator.formatDecimal(summary.totalDistanceKm)} km   |   Fare: ${RideCalculator.formatMoney(summary.grossIncome, settings.currencyCode)}   |   Tips: ${RideCalculator.formatMoney(summary.totalTips, settings.currencyCode)}",
                "Fuel: ${RideCalculator.formatMoney(summary.totalFuel, settings.currencyCode)}   |   Salik: ${RideCalculator.formatMoney(summary.totalSalik, settings.currencyCode)}   |   Commission: ${RideCalculator.formatMoney(summary.totalCommission, settings.currencyCode)}   |   Booking: ${RideCalculator.formatMoney(summary.totalBookingFee, settings.currencyCode)}",
                "NET INCOME: ${RideCalculator.formatMoney(summary.netIncome, settings.currencyCode)}"
            )
            summaryLines.forEachIndexed { idx, line ->
                canvas.drawText(line, margin, y, if (idx == 2) boldCellPaint else cellPaint)
                y += 14f
            }
            y += 12f

            // Daily Shift Summary Table
            canvas.drawText("Daily Shift Summary", margin, y, sectionPaint)
            y += 12f

            fun drawDailyTableHeader() {
                canvas.drawRect(margin, y, margin + contentWidth, y + 18f, headerBgPaint)
                val base = y + 12.5f
                canvas.drawText("Shift Date", margin + 4f, base, headerTextPaint)
                canvas.drawText("Rides", margin + 92f, base, headerTextPaint)
                canvas.drawText("Km", margin + 132f, base, headerTextPaint)
                canvas.drawText("Gross", margin + 178f, base, headerTextPaint)
                canvas.drawText("Fuel", margin + 242f, base, headerTextPaint)
                canvas.drawText("Salik", margin + 298f, base, headerTextPaint)
                canvas.drawText("Comm", margin + 350f, base, headerTextPaint)
                canvas.drawText("Book", margin + 404f, base, headerTextPaint)
                canvas.drawText("Net", margin + 462f, base, headerTextPaint)
                y += 18f
            }

            drawDailyTableHeader()
            dailyGroups.forEachIndexed { idx, group ->
                ensureSpace(20f)
                if (y <= margin + 16f) drawDailyTableHeader()
                if (idx % 2 == 1) {
                    canvas.drawRect(margin, y, margin + contentWidth, y + 16f, altRowPaint)
                }
                val s = group.summary
                val base = y + 11.5f
                canvas.drawText(group.dateIso, margin + 4f, base, cellPaint)
                canvas.drawText(s.rideCount.toString(), margin + 92f, base, cellPaint)
                canvas.drawText(RideCalculator.formatDecimal(s.totalDistanceKm), margin + 132f, base, cellPaint)
                canvas.drawText(RideCalculator.formatDecimal(s.grossIncome), margin + 178f, base, cellPaint)
                canvas.drawText(RideCalculator.formatDecimal(s.totalFuel), margin + 242f, base, cellPaint)
                canvas.drawText(RideCalculator.formatDecimal(s.totalSalik), margin + 298f, base, cellPaint)
                canvas.drawText(RideCalculator.formatDecimal(s.totalCommission), margin + 350f, base, cellPaint)
                canvas.drawText(RideCalculator.formatDecimal(s.totalBookingFee), margin + 404f, base, cellPaint)
                canvas.drawText(RideCalculator.formatDecimal(s.netIncome), margin + 462f, base, boldCellPaint)
                y += 16f
            }

            y += 18f
            ensureSpace(48f)
            canvas.drawText("Ride-by-Ride Breakdown", margin, y, sectionPaint)
            y += 12f

            fun drawRideTableHeader() {
                canvas.drawRect(margin, y, margin + contentWidth, y + 18f, headerBgPaint)
                val base = y + 12.5f
                canvas.drawText("Date", margin + 4f, base, headerTextPaint)
                canvas.drawText("Time", margin + 62f, base, headerTextPaint)
                canvas.drawText("Platform", margin + 96f, base, headerTextPaint)
                canvas.drawText("Pickup", margin + 148f, base, headerTextPaint)
                canvas.drawText("Pay", margin + 216f, base, headerTextPaint)
                canvas.drawText("Km", margin + 268f, base, headerTextPaint)
                canvas.drawText("Gross", margin + 306f, base, headerTextPaint)
                canvas.drawText("Fuel", margin + 356f, base, headerTextPaint)
                canvas.drawText("Fees", margin + 404f, base, headerTextPaint)
                canvas.drawText("Net", margin + 468f, base, headerTextPaint)
                y += 18f
            }

            drawRideTableHeader()
            var rowIdx = 0
            dailyGroups.forEach { group ->
                group.rides.forEach { ride ->
                    ensureSpace(18f)
                    if (y <= margin + 16f) drawRideTableHeader()
                    if (rowIdx % 2 == 1) {
                        canvas.drawRect(margin, y, margin + contentWidth, y + 16f, altRowPaint)
                    }
                    val base = y + 11.5f
                    val otherFees = ride.salikCost + ride.boltCommission + ride.bookingFee
                    val pickupLabel = ride.pickupPointName.ifBlank { "-" }.take(11)
                    canvas.drawText(group.dateIso, margin + 4f, base, cellPaint)
                    canvas.drawText(RideCalculator.toTimeLabel(ride.rideDateMillis), margin + 62f, base, cellPaint)
                    canvas.drawText(ride.platformName.take(8), margin + 96f, base, cellPaint)
                    canvas.drawText(pickupLabel, margin + 148f, base, cellPaint)
                    canvas.drawText(ride.paymentMethod.take(9), margin + 216f, base, cellPaint)
                    canvas.drawText(RideCalculator.formatDecimal(ride.distanceKm), margin + 268f, base, cellPaint)
                    canvas.drawText(RideCalculator.formatDecimal(ride.grossFare), margin + 306f, base, cellPaint)
                    canvas.drawText(RideCalculator.formatDecimal(ride.fuelCost), margin + 356f, base, cellPaint)
                    canvas.drawText(RideCalculator.formatDecimal(otherFees), margin + 404f, base, cellPaint)
                    canvas.drawText(RideCalculator.formatDecimal(ride.netIncome), margin + 468f, base, boldCellPaint)
                    y += 16f
                    rowIdx++
                }
            }

            pdf.finishPage(page)
            val baos = ByteArrayOutputStream()
            pdf.writeTo(baos)
            return baos.toByteArray()
        } finally {
            runCatching { pdf.close() }
        }
    }

    fun exportToExcelXlsx(
        context: Context,
        uri: Uri,
        driver: DriverAccountEntity?,
        settings: AppSettingsEntity,
        periodLabel: String,
        summary: PeriodSummary,
        dailyGroups: List<DailyReportGroup>
    ): Result<Unit> {
        return runCatching {
            val bytes = buildXlsxBytes(driver, settings, periodLabel, summary, dailyGroups)
            val outStream = runCatching {
                context.contentResolver.openOutputStream(uri, "w")
            }.getOrNull() ?: context.contentResolver.openOutputStream(uri)
                ?: error("Unable to open output stream for Excel file.")
            outStream.use { out ->
                out.write(bytes)
                out.flush()
            }
        }
    }

    internal fun buildXlsxBytes(
        driver: DriverAccountEntity?,
        settings: AppSettingsEntity,
        periodLabel: String,
        summary: PeriodSummary,
        dailyGroups: List<DailyReportGroup>
    ): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zip ->
            fun addEntry(name: String, content: String) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }

            addEntry("[Content_Types].xml", CONTENT_TYPES_XML)
            addEntry("_rels/.rels", ROOT_RELS_XML)
            addEntry("xl/workbook.xml", WORKBOOK_XML)
            addEntry("xl/_rels/workbook.xml.rels", WORKBOOK_RELS_XML)
            addEntry("xl/styles.xml", STYLES_XML)
            addEntry(
                "xl/worksheets/sheet1.xml",
                buildDailySummarySheetXml(driver, settings, periodLabel, summary, dailyGroups)
            )
            addEntry(
                "xl/worksheets/sheet2.xml",
                buildAllRidesSheetXml(settings, dailyGroups)
            )
        }
        return baos.toByteArray()
    }

    private fun buildDailySummarySheetXml(
        driver: DriverAccountEntity?,
        settings: AppSettingsEntity,
        periodLabel: String,
        summary: PeriodSummary,
        dailyGroups: List<DailyReportGroup>
    ): String {
        val rows = mutableListOf<List<XlsxCell>>()
        rows.add(listOf(XlsxCell.Str("RideLedger Income Report", style = 1)))
        rows.add(
            listOf(
                XlsxCell.Str("Driver: ${driver?.fullName?.ifBlank { "Driver #${driver.driverId}" } ?: "-"} (ID: ${driver?.driverId ?: "-"})"),
                XlsxCell.Str("Period: $periodLabel"),
                XlsxCell.Str("Shift: ${RideCalculator.formatShiftWindow(settings)}"),
                XlsxCell.Str("Currency: ${settings.currencyCode}")
            )
        )
        rows.add(emptyList())
        rows.add(
            listOf(
                XlsxCell.Str("Shift Date", 1),
                XlsxCell.Str("Rides", 1),
                XlsxCell.Str("Distance (km)", 1),
                XlsxCell.Str("Fare", 1),
                XlsxCell.Str("Tips", 1),
                XlsxCell.Str("Fuel Cost", 1),
                XlsxCell.Str("Salik Cost", 1),
                XlsxCell.Str("Bolt Commission", 1),
                XlsxCell.Str("Booking Fee", 1),
                XlsxCell.Str("Net Income", 1)
            )
        )

        dailyGroups.forEach { group ->
            val s = group.summary
            rows.add(
                listOf(
                    XlsxCell.Str(group.dateIso),
                    XlsxCell.Num(s.rideCount.toDouble()),
                    XlsxCell.Num(s.totalDistanceKm),
                    XlsxCell.Num(s.grossIncome),
                    XlsxCell.Num(s.totalTips),
                    XlsxCell.Num(s.totalFuel),
                    XlsxCell.Num(s.totalSalik),
                    XlsxCell.Num(s.totalCommission),
                    XlsxCell.Num(s.totalBookingFee),
                    XlsxCell.Num(s.netIncome)
                )
            )
        }

        rows.add(
            listOf(
                XlsxCell.Str("TOTAL", 1),
                XlsxCell.Num(summary.rideCount.toDouble(), 1),
                XlsxCell.Num(summary.totalDistanceKm, 1),
                XlsxCell.Num(summary.grossIncome, 1),
                XlsxCell.Num(summary.totalTips, 1),
                XlsxCell.Num(summary.totalFuel, 1),
                XlsxCell.Num(summary.totalSalik, 1),
                XlsxCell.Num(summary.totalCommission, 1),
                XlsxCell.Num(summary.totalBookingFee, 1),
                XlsxCell.Num(summary.netIncome, 1)
            )
        )
        return renderWorksheetXml(rows)
    }

    private fun buildAllRidesSheetXml(
        settings: AppSettingsEntity,
        dailyGroups: List<DailyReportGroup>
    ): String {
        val rows = mutableListOf<List<XlsxCell>>()
        rows.add(
            listOf(
                XlsxCell.Str("Shift Date", 1),
                XlsxCell.Str("Ride Date", 1),
                XlsxCell.Str("Ride Time", 1),
                XlsxCell.Str("Platform", 1),
                XlsxCell.Str("Pickup Point", 1),
                XlsxCell.Str("Payment Method", 1),
                XlsxCell.Str("Distance (km)", 1),
                XlsxCell.Str("Fare (${settings.currencyCode})", 1),
                XlsxCell.Str("Fuel Cost", 1),
                XlsxCell.Str("Salik Cost", 1),
                XlsxCell.Str("Bolt Commission", 1),
                XlsxCell.Str("Booking Fee", 1),
                XlsxCell.Str("Net Income", 1),
                XlsxCell.Str("Notes", 1),
                XlsxCell.Str("Finance Status", 1),
                XlsxCell.Str("Finance Submitted At", 1),
                XlsxCell.Str("Tips (${settings.currencyCode})", 1)
            )
        )

        dailyGroups.forEach { group ->
            group.rides.forEach { ride: RideEntity ->
                rows.add(
                    listOf(
                        XlsxCell.Str(group.dateIso),
                        XlsxCell.Str(RideCalculator.toIsoDate(ride.rideDateMillis)),
                        XlsxCell.Str(RideCalculator.toTimeLabel(ride.rideDateMillis)),
                        XlsxCell.Str(ride.platformName),
                        XlsxCell.Str(ride.pickupPointName.ifBlank { "-" }),
                        XlsxCell.Str(ride.paymentMethod.ifBlank { "Cash" }),
                        XlsxCell.Num(ride.distanceKm),
                        XlsxCell.Num(ride.grossFare),
                        XlsxCell.Num(ride.fuelCost),
                        XlsxCell.Num(ride.salikCost),
                        XlsxCell.Num(ride.boltCommission),
                        XlsxCell.Num(ride.bookingFee),
                        XlsxCell.Num(ride.netIncome),
                        XlsxCell.Str(ride.notes),
                        XlsxCell.Str(if (ride.isSubmittedToFinance) "Submitted" else "Pending"),
                        XlsxCell.Num((ride.financeSubmittedAt ?: 0L).toDouble()),
                        XlsxCell.Num(ride.tips)
                    )
                )
            }
        }
        return renderWorksheetXml(rows)
    }

    fun importFromExcelXlsx(
        context: Context,
        uri: Uri,
        driverId: String,
        settings: AppSettingsEntity
    ): Result<List<RideEntity>> {
        return runCatching {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: error("Unable to open selected Excel file.")
            parseXlsxBytes(bytes, driverId, settings)
        }
    }

    fun importFromExcelFile(
        file: java.io.File,
        driverId: String,
        settings: AppSettingsEntity
    ): Result<List<RideEntity>> {
        return runCatching {
            require(file.exists() && file.canRead()) { "Selected backup file cannot be read." }
            parseXlsxBytes(file.readBytes(), driverId, settings)
        }
    }

    internal fun parseXlsxBytes(
        bytes: ByteArray,
        driverId: String,
        settings: AppSettingsEntity
    ): List<RideEntity> {
        require(bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()) {
            "Selected file is not a valid .xlsx Excel workbook."
        }
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    entries[entry.name.trimStart('/')] = zis.readBytes()
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        require(entries.isNotEmpty()) { "Selected file is not a valid Excel (.xlsx) backup." }

        val sharedStrings = entries["xl/sharedStrings.xml"]?.let { parseSharedStrings(it) } ?: emptyList()
        val sheetPath = resolveAllRidesSheetPath(entries)
            ?: error("Could not find 'All Rides' sheet in this Excel file.")
        val sheetBytes = entries[sheetPath]
            ?: error("Worksheet '$sheetPath' is missing in the Excel file.")

        val doc = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }.newDocumentBuilder().parse(ByteArrayInputStream(sheetBytes))

        val rowNodes = doc.getElementsByTagNameNS("*", "row").let {
            if (it.length > 0) it else doc.getElementsByTagName("row")
        }
        if (rowNodes.length <= 1) return emptyList()

        val parsedRides = mutableListOf<RideEntity>()
        val headerRow = parseRowCells(rowNodes.item(0) as Element, sharedStrings)
        val isAllRidesHeader = headerRow[3]?.contains("Platform", ignoreCase = true) == true
        val startIndex = if (isAllRidesHeader) 1 else 0

        for (i in startIndex until rowNodes.length) {
            val rowEl = rowNodes.item(i) as? Element ?: continue
            val cells = parseRowCells(rowEl, sharedStrings)

            val shiftDateRaw = cells[0]?.trim().orEmpty()
            val rideDateRaw = cells[1]?.trim().orEmpty()
            val rideTimeRaw = cells[2]?.trim().orEmpty()
            val platformName = cells[3]?.trim().orEmpty()
            if (platformName.isBlank() || platformName.equals("Platform", ignoreCase = true)) continue

            val grossFare = cells[7]?.toDoubleOrNull() ?: continue
            if (grossFare <= 0.0) continue

            val pickupRaw = cells[4]?.trim().orEmpty()
            val pickupPointName = if (pickupRaw == "-" || pickupRaw.isBlank()) "" else pickupRaw
            val paymentMethod = cells[5]?.trim().orEmpty().ifBlank {
                if (RideCalculator.isDtcEventBookingPlatform(platformName)) "Account" else "Cash"
            }
            val distanceKm = RideCalculator.round2((cells[6]?.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0))
            val fuelCost = RideCalculator.round2((cells[8]?.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0))
            val salikCost = RideCalculator.round2((cells[9]?.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0))
            val boltCommission = RideCalculator.round2((cells[10]?.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0))
            val bookingFee = RideCalculator.round2((cells[11]?.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0))
            val tips = RideCalculator.round2((cells[16]?.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0))
            val netIncome = RideCalculator.round2(
                cells[12]?.toDoubleOrNull()
                    ?: RideCalculator.calculateNetIncome(
                        grossFare = grossFare,
                        fuelCost = fuelCost,
                        salikCost = salikCost,
                        boltCommission = boltCommission,
                        bookingFee = bookingFee,
                        tips = tips
                    )
            )
            val notes = cells[13]?.trim().orEmpty()
            val financeStatusRaw = cells[14]?.trim().orEmpty()
            val isSubmittedToFinance = financeStatusRaw.equals("Submitted", ignoreCase = true)
            val financeSubmittedAt = cells[15]?.toDoubleOrNull()?.toLong()?.takeIf { it > 0L }
                ?: if (isSubmittedToFinance) System.currentTimeMillis() else null

            val rideMillis = parseRideTimestamp(rideDateRaw, shiftDateRaw, rideTimeRaw) ?: continue
            val resolvedShiftIso = if (shiftDateRaw.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                shiftDateRaw
            } else {
                RideCalculator.computeShiftDateIso(rideMillis, settings)
            }
            val fuelRateApplied = if (distanceKm > 0.0) RideCalculator.round2(fuelCost / distanceKm) else 0.0

            parsedRides.add(
                RideEntity(
                    driverId = driverId,
                    rideDateMillis = rideMillis,
                    rideDateIso = resolvedShiftIso,
                    platformName = platformName,
                    pickupPointName = pickupPointName,
                    paymentMethod = paymentMethod,
                    distanceKm = distanceKm,
                    grossFare = RideCalculator.round2(grossFare),
                    fuelCostPerKmApplied = fuelRateApplied,
                    fuelCost = fuelCost,
                    salikCost = salikCost,
                    boltCommission = boltCommission,
                    bookingFee = bookingFee,
                    netIncome = netIncome,
                    tips = tips,
                    notes = notes,
                    isSubmittedToFinance = isSubmittedToFinance,
                    financeSubmittedAt = financeSubmittedAt
                )
            )
        }
        return parsedRides
    }

    private fun resolveAllRidesSheetPath(entries: Map<String, ByteArray>): String? {
        val workbookBytes = entries["xl/workbook.xml"]
        val relsBytes = entries["xl/_rels/workbook.xml.rels"]
        if (workbookBytes != null && relsBytes != null) {
            val relTargets = mutableMapOf<String, String>()
            val relDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(ByteArrayInputStream(relsBytes))
            val relNodes = relDoc.getElementsByTagName("Relationship")
            for (i in 0 until relNodes.length) {
                val el = relNodes.item(i) as? Element ?: continue
                val id = el.getAttribute("Id")
                val target = el.getAttribute("Target").trimStart('/')
                if (id.isNotBlank() && target.isNotBlank()) {
                    relTargets[id] = if (target.startsWith("xl/")) target else "xl/$target"
                }
            }

            val wbDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(ByteArrayInputStream(workbookBytes))
            val sheetNodes = wbDoc.getElementsByTagName("sheet")
            for (i in 0 until sheetNodes.length) {
                val el = sheetNodes.item(i) as? Element ?: continue
                val name = el.getAttribute("name")
                if (name.equals("All Rides", ignoreCase = true)) {
                    val rid = el.getAttribute("r:id").ifBlank { el.getAttribute("id") }
                    val target = relTargets[rid]
                    if (target != null && entries.containsKey(target)) return target
                }
            }
        }

        if (entries.containsKey("xl/worksheets/sheet2.xml")) return "xl/worksheets/sheet2.xml"
        return entries.keys.firstOrNull { it.startsWith("xl/worksheets/") && it.endsWith(".xml") }
    }

    private fun parseSharedStrings(bytes: ByteArray): List<String> {
        val doc = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(ByteArrayInputStream(bytes))
        val siNodes = doc.getElementsByTagNameNS("*", "si").let {
            if (it.length > 0) it else doc.getElementsByTagName("si")
        }
        val list = ArrayList<String>(siNodes.length)
        for (i in 0 until siNodes.length) {
            val el = siNodes.item(i) as? Element ?: continue
            list.add(el.textContent ?: "")
        }
        return list
    }

    private fun Element.childrenByLocalName(localName: String) =
        getElementsByTagNameNS("*", localName).let {
            if (it.length > 0) it else getElementsByTagName(localName)
        }

    private fun parseRowCells(rowEl: Element, sharedStrings: List<String>): Map<Int, String> {
        val result = mutableMapOf<Int, String>()
        val cellNodes = rowEl.childrenByLocalName("c")
        for (i in 0 until cellNodes.length) {
            val cellEl = cellNodes.item(i) as? Element ?: continue
            val ref = cellEl.getAttribute("r")
            val colIdx = colIndexFromRef(ref, fallback = i)
            val type = cellEl.getAttribute("t")
            val value = when (type) {
                "inlineStr" -> {
                    val isNodes = cellEl.childrenByLocalName("t")
                    if (isNodes.length > 0) isNodes.item(0).textContent ?: "" else cellEl.textContent ?: ""
                }
                "s" -> {
                    val vNodes = cellEl.childrenByLocalName("v")
                    val idx = if (vNodes.length > 0) vNodes.item(0).textContent?.toIntOrNull() else null
                    if (idx != null && idx in sharedStrings.indices) sharedStrings[idx] else ""
                }
                "str" -> {
                    val vNodes = cellEl.childrenByLocalName("v")
                    if (vNodes.length > 0) vNodes.item(0).textContent ?: "" else ""
                }
                else -> {
                    val vNodes = cellEl.childrenByLocalName("v")
                    if (vNodes.length > 0) vNodes.item(0).textContent ?: "" else ""
                }
            }
            result[colIdx] = value
        }
        return result
    }

    private fun colIndexFromRef(ref: String, fallback: Int): Int {
        if (ref.isBlank()) return fallback
        val letters = ref.takeWhile { it.isLetter() }.uppercase(Locale.US)
        if (letters.isEmpty()) return fallback
        var col = 0
        for (ch in letters) {
            col = col * 26 + (ch - 'A' + 1)
        }
        return col - 1
    }

    private fun parseRideTimestamp(
        rideDateRaw: String,
        shiftDateRaw: String,
        rideTimeRaw: String
    ): Long? {
        val dateIso = parseExcelOrIsoDate(rideDateRaw) ?: parseExcelOrIsoDate(shiftDateRaw) ?: return null
        val timeLabel = parseExcelOrHmTime(rideTimeRaw) ?: "12:00"
        return try {
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply { isLenient = false }
                .parse("$dateIso $timeLabel")?.time
        } catch (_: Exception) {
            null
        }
    }

    private fun parseExcelOrIsoDate(raw: String): String? {
        if (raw.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) return raw
        val serial = raw.toDoubleOrNull()
        if (serial != null && serial > 20000.0) {
            val days = serial.toInt()
            val cal = Calendar.getInstance().apply {
                set(1899, Calendar.DECEMBER, 30, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.DAY_OF_MONTH, days)
            }
            return RideCalculator.toIsoDate(cal.timeInMillis)
        }
        return null
    }

    private fun parseExcelOrHmTime(raw: String): String? {
        if (raw.matches(Regex("\\d{1,2}:\\d{2}"))) {
            val parts = raw.split(":")
            val h = parts[0].toIntOrNull()?.coerceIn(0, 23) ?: return null
            val m = parts[1].toIntOrNull()?.coerceIn(0, 59) ?: return null
            return RideCalculator.toTimeLabel(h, m)
        }
        val fraction = raw.toDoubleOrNull()
        if (fraction != null && fraction in 0.0..1.0) {
            val totalMinutes = kotlin.math.round(fraction * 24.0 * 60.0).toInt().coerceIn(0, 23 * 60 + 59)
            return RideCalculator.toTimeLabel(totalMinutes / 60, totalMinutes % 60)
        }
        return null
    }

    private sealed class XlsxCell(val style: Int) {
        class Str(val text: String, style: Int = 0) : XlsxCell(style)
        class Num(val number: Double, style: Int = 0) : XlsxCell(style)
    }

    private fun renderWorksheetXml(rows: List<List<XlsxCell>>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")
        rows.forEachIndexed { rIdx, rowCells ->
            val rowNum = rIdx + 1
            sb.append("""<row r="$rowNum">""")
            rowCells.forEachIndexed { cIdx, cell ->
                val ref = "${colName(cIdx)}$rowNum"
                val styleAttr = if (cell.style > 0) """ s="${cell.style}"""" else ""
                when (cell) {
                    is XlsxCell.Str -> {
                        sb.append("""<c r="$ref" t="inlineStr"$styleAttr><is><t>${escapeXml(cell.text)}</t></is></c>""")
                    }
                    is XlsxCell.Num -> {
                        sb.append("""<c r="$ref"$styleAttr><v>${cell.number}</v></c>""")
                    }
                }
            }
            sb.append("</row>")
        }
        sb.append("</sheetData></worksheet>")
        return sb.toString()
    }

    private fun colName(indexZeroBased: Int): String {
        var idx = indexZeroBased
        val sb = StringBuilder()
        while (idx >= 0) {
            sb.append(('A' + (idx % 26)))
            idx = (idx / 26) - 1
        }
        return sb.reverse().toString()
    }

    private fun escapeXml(input: String): String =
        input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")

    private const val CONTENT_TYPES_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>"""

    private const val ROOT_RELS_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private const val WORKBOOK_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Daily Summary" sheetId="1" r:id="rId1"/>
    <sheet name="All Rides" sheetId="2" r:id="rId2"/>
  </sheets>
</workbook>"""

    private const val WORKBOOK_RELS_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

    private const val STYLES_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="2">
    <font><sz val="11"/><name val="Calibri"/></font>
    <font><b/><sz val="11"/><name val="Calibri"/></font>
  </fonts>
  <fills count="2">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
  </fills>
  <borders count="1">
    <border><left/><right/><top/><bottom/><diagonal/></border>
  </borders>
  <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
  <cellXfs count="2">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0"/>
  </cellXfs>
</styleSheet>"""
}
