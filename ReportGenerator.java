package com.example.hostipal_info;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.os.Environment;
import android.util.Log;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReportGenerator {

    private final Context context;
    private static final String TAG        = "ReportGenerator";
    private static final int    PAGE_WIDTH  = 595;
    private static final int    PAGE_HEIGHT = 842;
    private static final int    MARGIN      = 40;
    private static final int    LINE_HEIGHT = 24;

    public ReportGenerator(Context context) {
        this.context = context;
    }

    // ─────────────────────────────────────────────
    //  Main entry — generates PDF file
    // ─────────────────────────────────────────────

    public File generatePDF(
            Map<String, String> patient,
            List<Map<String, String>> visits,
            String bloodReportUrl,
            String urineReportUrl
    ) throws IOException {

        // ✅ Download images FIRST on this background thread
        Bitmap bloodBitmap = null;
        Bitmap urineBitmap = null;

        if (bloodReportUrl != null && !bloodReportUrl.isEmpty()) {
            Log.d(TAG, "Downloading blood report: " + bloodReportUrl);
            bloodBitmap = downloadBitmap(bloodReportUrl);
            Log.d(TAG, "Blood bitmap: " + (bloodBitmap != null ? "OK" : "FAILED"));
        }

        if (urineReportUrl != null && !urineReportUrl.isEmpty()) {
            Log.d(TAG, "Downloading urine report: " + urineReportUrl);
            urineBitmap = downloadBitmap(urineReportUrl);
            Log.d(TAG, "Urine bitmap: " + (urineBitmap != null ? "OK" : "FAILED"));
        }

        PdfDocument pdf = new PdfDocument();

        // ── Page 1: Patient Info + Visits ─────────
        PdfDocument.PageInfo page1Info =
                new PdfDocument.PageInfo.Builder(
                        PAGE_WIDTH, PAGE_HEIGHT, 1).create();
        PdfDocument.Page page1 = pdf.startPage(page1Info);
        drawPage1(page1.getCanvas(), patient, visits);
        pdf.finishPage(page1);

        // ── Page 2: Lab Report Images ─────────────
        // ✅ Always add page 2 if either image exists
        if (bloodBitmap != null || urineBitmap != null) {
            PdfDocument.PageInfo page2Info =
                    new PdfDocument.PageInfo.Builder(
                            PAGE_WIDTH, PAGE_HEIGHT, 2).create();
            PdfDocument.Page page2 = pdf.startPage(page2Info);
            drawReportImages(page2.getCanvas(), bloodBitmap, urineBitmap);
            pdf.finishPage(page2);
        } else {
            // ✅ Add page 2 with "not available" message
            PdfDocument.PageInfo page2Info =
                    new PdfDocument.PageInfo.Builder(
                            PAGE_WIDTH, PAGE_HEIGHT, 2).create();
            PdfDocument.Page page2 = pdf.startPage(page2Info);
            drawNoReportsPage(page2.getCanvas(),
                    bloodReportUrl, urineReportUrl);
            pdf.finishPage(page2);
        }

        // ── Save PDF ──────────────────────────────
        String patientName = patient.getOrDefault("name", "Patient")
                .replaceAll("\\s+", "_");
        String fileName = "MedicalReport_" + patientName + "_"
                + new SimpleDateFormat("yyyyMMdd_HHmm",
                Locale.getDefault()).format(new Date())
                + ".pdf";

        File dir = context.getExternalFilesDir(
                Environment.DIRECTORY_DOCUMENTS);
        if (dir != null && !dir.exists()) dir.mkdirs();

        File file = new File(dir, fileName);
        FileOutputStream fos = new FileOutputStream(file);
        pdf.writeTo(fos);
        fos.close();
        pdf.close();

        Log.d(TAG, "PDF saved to: " + file.getAbsolutePath());
        return file;
    }

    // ─────────────────────────────────────────────
    //  Page 1 — Patient Info + Visit History
    // ─────────────────────────────────────────────

    private void drawPage1(
            Canvas canvas,
            Map<String, String> patient,
            List<Map<String, String>> visits
    ) {
        // Header bar
        Paint headerBg = new Paint();
        headerBg.setColor(Color.parseColor("#2563EB"));
        canvas.drawRect(0, 0, PAGE_WIDTH, 70, headerBg);

        Paint headerText = new Paint();
        headerText.setColor(Color.WHITE);
        headerText.setTextSize(22);
        headerText.setTypeface(Typeface.create(
                Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("MEDICAL REPORT", MARGIN, 46, headerText);

        Paint dateText = new Paint();
        dateText.setColor(Color.parseColor("#BFDBFE"));
        dateText.setTextSize(10);
        canvas.drawText("Generated: " + new SimpleDateFormat(
                        "dd MMM yyyy", Locale.getDefault()).format(new Date()),
                PAGE_WIDTH - 160, 46, dateText);

        int y = 90;

        // Patient Info
        y = drawSectionHeader(canvas, y, "PATIENT INFORMATION");
        y = drawRow(canvas, y, "Name",
                patient.getOrDefault("name", "N/A"));
        y = drawRow(canvas, y, "Patient ID",
                "PAT-" + formatId(
                        patient.getOrDefault("id", "0")));
        y = drawRow(canvas, y, "Age",
                patient.getOrDefault("age", "N/A") + " years");
        y = drawRow(canvas, y, "Gender",
                patient.getOrDefault("gender", "N/A"));
        y = drawRow(canvas, y, "Blood Group",
                patient.getOrDefault("blood_group", "N/A"));
        y = drawRow(canvas, y, "Mobile",
                patient.getOrDefault("mobile", "N/A"));
        y = drawRow(canvas, y, "Address",
                patient.getOrDefault("address", "N/A"));
        y = drawRow(canvas, y, "Emergency Contact",
                patient.getOrDefault("emergency_contact", "N/A"));
        y = drawRow(canvas, y, "ABHA Number",
                patient.getOrDefault("abha_number", "N/A"));

        y += 10;

        // Medical Info
        y = drawSectionHeader(canvas, y, "MEDICAL INFORMATION");
        y = drawRow(canvas, y, "Symptoms",
                patient.getOrDefault("symptoms", "N/A"));
        y = drawRow(canvas, y, "Diagnosis",
                patient.getOrDefault("diagnosis", "N/A"));
        y = drawRow(canvas, y, "Allergies",
                patient.getOrDefault("allergies", "N/A"));
        y = drawRow(canvas, y, "Notes",
                patient.getOrDefault("notes", "N/A"));

        y += 10;

        // Visit History
        if (visits != null && !visits.isEmpty()) {
            y = drawSectionHeader(canvas, y, "VISIT HISTORY");

            for (Map<String, String> visit : visits) {
                if (y > PAGE_HEIGHT - 110) break;

                Paint cardBg = new Paint();
                cardBg.setColor(Color.parseColor("#F0F7FF"));
                canvas.drawRect(MARGIN, y,
                        PAGE_WIDTH - MARGIN, y + 85, cardBg);

                // Visit border
                Paint border = new Paint();
                border.setColor(Color.parseColor("#BFDBFE"));
                border.setStyle(Paint.Style.STROKE);
                border.setStrokeWidth(1);
                canvas.drawRect(MARGIN, y,
                        PAGE_WIDTH - MARGIN, y + 85, border);

                Paint datePaint = new Paint();
                datePaint.setColor(Color.parseColor("#2563EB"));
                datePaint.setTextSize(12);
                datePaint.setTypeface(Typeface.create(
                        Typeface.DEFAULT, Typeface.BOLD));
                canvas.drawText("📅 " + visit.getOrDefault(
                        "visit_date", ""), MARGIN + 8, y + 18, datePaint);

                Paint detailPaint = new Paint();
                detailPaint.setColor(Color.parseColor("#374151"));
                detailPaint.setTextSize(11);
                canvas.drawText("Symptoms:  "
                                + truncate(visit.getOrDefault(
                                "symptoms", "N/A"), 60),
                        MARGIN + 8, y + 36, detailPaint);
                canvas.drawText("Diagnosis: "
                                + truncate(visit.getOrDefault(
                                "diagnosis", "N/A"), 60),
                        MARGIN + 8, y + 52, detailPaint);

                String notes = visit.getOrDefault("notes", "");
                if (!notes.isEmpty()) {
                    canvas.drawText("Notes:     " + truncate(notes, 60),
                            MARGIN + 8, y + 68, detailPaint);
                }

                y += 94;
            }
        }

        drawFooter(canvas);
    }

    // ─────────────────────────────────────────────
    //  Page 2 — Lab Report Images
    // ─────────────────────────────────────────────

    private void drawReportImages(
            Canvas canvas,
            Bitmap bloodBitmap,
            Bitmap urineBitmap
    ) {
        // Header
        Paint headerBg = new Paint();
        headerBg.setColor(Color.parseColor("#2563EB"));
        canvas.drawRect(0, 0, PAGE_WIDTH, 70, headerBg);

        Paint headerText = new Paint();
        headerText.setColor(Color.WHITE);
        headerText.setTextSize(22);
        headerText.setTypeface(Typeface.create(
                Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("LAB REPORTS", MARGIN, 46, headerText);

        int y = 90;

        int availableWidth  = PAGE_WIDTH - (MARGIN * 2);
        int maxImageHeight  = 300;

        // ✅ Blood Report Image
        if (bloodBitmap != null) {
            y = drawSectionHeader(canvas, y, "🩸 BLOOD REPORT");

            // Scale image to fit page width
            int imgWidth  = availableWidth;
            int imgHeight = (int) (bloodBitmap.getHeight()
                    * ((float) imgWidth / bloodBitmap.getWidth()));
            imgHeight = Math.min(imgHeight, maxImageHeight);

            Bitmap scaled = Bitmap.createScaledBitmap(
                    bloodBitmap, imgWidth, imgHeight, true);

            // Image border
            Paint borderPaint = new Paint();
            borderPaint.setColor(Color.parseColor("#E5E7EB"));
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(1);
            canvas.drawRect(MARGIN, y,
                    MARGIN + imgWidth, y + imgHeight, borderPaint);

            canvas.drawBitmap(scaled, MARGIN, y, null);
            y += imgHeight + 20;

        } else {
            y = drawSectionHeader(canvas, y, "🩸 BLOOD REPORT");
            y = drawNotAvailable(canvas, y);
        }

        // ✅ Urine Report Image
        if (y < PAGE_HEIGHT - 100) {
            if (urineBitmap != null) {
                y = drawSectionHeader(canvas, y, "🧪 URINE REPORT");

                int imgWidth  = availableWidth;
                int imgHeight = (int) (urineBitmap.getHeight()
                        * ((float) imgWidth / urineBitmap.getWidth()));
                imgHeight = Math.min(imgHeight, maxImageHeight);

                // If image won't fit, cap it
                if (y + imgHeight > PAGE_HEIGHT - 60) {
                    imgHeight = PAGE_HEIGHT - y - 60;
                }

                if (imgHeight > 50) {
                    Bitmap scaled = Bitmap.createScaledBitmap(
                            urineBitmap, imgWidth, imgHeight, true);

                    Paint borderPaint = new Paint();
                    borderPaint.setColor(Color.parseColor("#E5E7EB"));
                    borderPaint.setStyle(Paint.Style.STROKE);
                    borderPaint.setStrokeWidth(1);
                    canvas.drawRect(MARGIN, y,
                            MARGIN + imgWidth, y + imgHeight, borderPaint);

                    canvas.drawBitmap(scaled, MARGIN, y, null);
                }

            } else {
                y = drawSectionHeader(canvas, y, "🧪 URINE REPORT");
                drawNotAvailable(canvas, y);
            }
        }

        drawFooter(canvas);
    }

    // ─────────────────────────────────────────────
    //  Page 2 fallback — No reports uploaded
    // ─────────────────────────────────────────────

    private void drawNoReportsPage(
            Canvas canvas,
            String bloodUrl,
            String urineUrl
    ) {
        Paint headerBg = new Paint();
        headerBg.setColor(Color.parseColor("#2563EB"));
        canvas.drawRect(0, 0, PAGE_WIDTH, 70, headerBg);

        Paint headerText = new Paint();
        headerText.setColor(Color.WHITE);
        headerText.setTextSize(22);
        headerText.setTypeface(Typeface.create(
                Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("LAB REPORTS", MARGIN, 46, headerText);

        int y = 120;

        if (bloodUrl != null && !bloodUrl.isEmpty()) {
            // URL exists but image failed to load
            y = drawSectionHeader(canvas, y, "🩸 BLOOD REPORT");
            Paint urlPaint = new Paint();
            urlPaint.setColor(Color.parseColor("#2563EB"));
            urlPaint.setTextSize(10);
            canvas.drawText("View at: " + bloodUrl,
                    MARGIN + 8, y + 15, urlPaint);
            y += 40;
        } else {
            y = drawSectionHeader(canvas, y, "🩸 BLOOD REPORT");
            y = drawNotAvailable(canvas, y);
        }

        if (urineUrl != null && !urineUrl.isEmpty()) {
            y = drawSectionHeader(canvas, y, "🧪 URINE REPORT");
            Paint urlPaint = new Paint();
            urlPaint.setColor(Color.parseColor("#2563EB"));
            urlPaint.setTextSize(10);
            canvas.drawText("View at: " + urineUrl,
                    MARGIN + 8, y + 15, urlPaint);
        } else {
            y = drawSectionHeader(canvas, y, "🧪 URINE REPORT");
            drawNotAvailable(canvas, y);
        }

        drawFooter(canvas);
    }

    // ─────────────────────────────────────────────
    //  Drawing Helpers
    // ─────────────────────────────────────────────

    private int drawSectionHeader(Canvas canvas, int y, String title) {
        Paint bg = new Paint();
        bg.setColor(Color.parseColor("#EFF6FF"));
        canvas.drawRect(MARGIN, y,
                PAGE_WIDTH - MARGIN, y + 28, bg);

        Paint text = new Paint();
        text.setColor(Color.parseColor("#1E40AF"));
        text.setTextSize(12);
        text.setTypeface(Typeface.create(
                Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText(title, MARGIN + 8, y + 19, text);

        return y + 36;
    }

    private int drawRow(Canvas canvas, int y,
                        String label, String value) {
        Paint labelPaint = new Paint();
        labelPaint.setColor(Color.parseColor("#6B7280"));
        labelPaint.setTextSize(11);
        canvas.drawText(label + ":",
                MARGIN + 8, y + 15, labelPaint);

        Paint valuePaint = new Paint();
        valuePaint.setColor(Color.parseColor("#111827"));
        valuePaint.setTextSize(11);
        valuePaint.setTypeface(Typeface.create(
                Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText(truncate(value != null
                        ? value : "N/A", 55),
                210, y + 15, valuePaint);

        Paint line = new Paint();
        line.setColor(Color.parseColor("#F3F4F6"));
        canvas.drawLine(MARGIN, y + 20,
                PAGE_WIDTH - MARGIN, y + 20, line);

        return y + LINE_HEIGHT;
    }

    private int drawNotAvailable(Canvas canvas, int y) {
        Paint paint = new Paint();
        paint.setColor(Color.parseColor("#9CA3AF"));
        paint.setTextSize(11);
        canvas.drawText("Not uploaded",
                MARGIN + 8, y + 16, paint);
        return y + 30;
    }

    private void drawFooter(Canvas canvas) {
        Paint line = new Paint();
        line.setColor(Color.parseColor("#E5E7EB"));
        canvas.drawLine(MARGIN, PAGE_HEIGHT - 40,
                PAGE_WIDTH - MARGIN, PAGE_HEIGHT - 40, line);

        Paint text = new Paint();
        text.setColor(Color.parseColor("#9CA3AF"));
        text.setTextSize(9);
        canvas.drawText(
                "Generated by Hospital Info App  •  "
                        + new SimpleDateFormat("dd MMM yyyy HH:mm",
                        Locale.getDefault()).format(new Date()),
                MARGIN, PAGE_HEIGHT - 22, text);
    }

    // ─────────────────────────────────────────────
    //  ✅ Fixed downloadBitmap — uses HttpURLConnection
    // ─────────────────────────────────────────────

    private Bitmap downloadBitmap(String urlString) {
        HttpURLConnection connection = null;
        InputStream inputStream = null;

        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(15000); // 15 seconds
            connection.setReadTimeout(15000);
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            connection.connect();

            int responseCode = connection.getResponseCode();
            Log.d(TAG, "Image download response: "
                    + responseCode + " for " + urlString);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                inputStream = connection.getInputStream();
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                if (bitmap == null) {
                    Log.e(TAG, "BitmapFactory returned null for: "
                            + urlString);
                }
                return bitmap;
            } else {
                Log.e(TAG, "HTTP error " + responseCode
                        + " for: " + urlString);
                return null;
            }

        } catch (Exception e) {
            Log.e(TAG, "Download failed: " + e.getMessage()
                    + " URL: " + urlString);
            return null;
        } finally {
            try {
                if (inputStream  != null) inputStream.close();
                if (connection   != null) connection.disconnect();
            } catch (IOException ignored) {}
        }
    }

    // ─────────────────────────────────────────────
    //  Utility
    // ─────────────────────────────────────────────

    private String truncate(String text, int maxLength) {
        if (text == null) return "N/A";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }

    private String formatId(String id) {
        try {
            return String.format("%05d", Integer.parseInt(id));
        } catch (Exception e) {
            return id;
        }
    }
}