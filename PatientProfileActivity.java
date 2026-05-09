package com.example.hostipal_info;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.FileProvider;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PatientProfileActivity extends AppCompatActivity {

    String currentBloodReportUrl = "";
    String currentUrineReportUrl = "";
    String currentGeneralFileUrl = "";
    List<Map<String, String>> currentVisits = new ArrayList<>();
    Map<String, String> currentPatientData  = new HashMap<>();

    private static final String BASE_URL = "http://tsm.ecssofttech.com/Gaytree/hospital/";

    // ── Views ──────────────────────────────────────
    TextView tvTopName, tvName, tvPatientInfo;
    TextView tvAge, tvGender, tvBlood;
    TextView tvSymptoms, tvDiagnosis, tvAllergies;
    TextView tvMobile, tvAddress, tvEmergency;
    LinearLayout timelineContainer;

    ImageView imgBloodReport, imgUrineReport;
    TextView tvNoBloodReport, tvNoUrineReport;

    // ── State ──────────────────────────────────────
    String currentPatientId   = "";
    String currentPatientName = "";
    String currentMobile      = "";

    // ── Visit upload state ─────────────────────────
    Uri visitBloodUri = null;
    Uri visitUrineUri = null;
    TextView btnVisitBlood = null;
    TextView btnVisitUrine = null;

    private enum VisitUploadTarget { BLOOD, URINE }
    private VisitUploadTarget visitUploadTarget;

    private final ActivityResultLauncher<Intent> visitFilePickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK
                                && result.getData() != null) {
                            Uri uri = result.getData().getData();
                            if (uri != null) handleVisitFilePicked(uri);
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        TextView btnGeneratePDF = findViewById(R.id.btnGeneratePDF);
        if (btnGeneratePDF != null)
            btnGeneratePDF.setOnClickListener(v -> generateAndSharePDF());



        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        tvTopName         = findViewById(R.id.tvTopName);
        tvName            = findViewById(R.id.tvName);
        tvPatientInfo     = findViewById(R.id.tvPatientInfo);
        tvAge             = findViewById(R.id.tvAge);
        tvGender          = findViewById(R.id.tvGender);
        tvBlood           = findViewById(R.id.tvBlood);
        tvSymptoms        = findViewById(R.id.tvSymptoms);
        tvDiagnosis       = findViewById(R.id.tvDiagnosis);
        tvAllergies       = findViewById(R.id.tvAllergies);
        tvMobile          = findViewById(R.id.tvMobile);
        tvAddress         = findViewById(R.id.tvAddress);
        tvEmergency       = findViewById(R.id.tvEmergency);
        timelineContainer = findViewById(R.id.timelineContainer);
        imgBloodReport    = findViewById(R.id.imgBloodReport);
        imgUrineReport    = findViewById(R.id.imgUrineReport);
        tvNoBloodReport   = findViewById(R.id.tvNoBloodReport);
        tvNoUrineReport   = findViewById(R.id.tvNoUrineReport);

        TextView btnSendMessage = findViewById(R.id.btnSendMessage);
        if (btnSendMessage != null)
            btnSendMessage.setOnClickListener(v -> sendWhatsAppMessage());

        TextView btnAddVisit = findViewById(R.id.btnAddVisit);
        if (btnAddVisit != null)
            btnAddVisit.setOnClickListener(v -> showAddVisitDialog());

        currentPatientId = getIntent().getStringExtra("patient_id");
        if (currentPatientId != null && !currentPatientId.isEmpty()) {
            fetchPatientDetails(currentPatientId);
        } else {
            Toast.makeText(this, "Invalid patient ID", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    // ─────────────────────────────────────────────
    //  Fetch Patient Details
    // ─────────────────────────────────────────────

    private void fetchPatientDetails(String patientId) {
        String url = BASE_URL + "get_patient.php?patient_id=" + patientId;

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    String cleaned = response.trim();
                    int start = cleaned.indexOf("{");
                    if (start > 0) cleaned = cleaned.substring(start);
                    if (start < 0) {
                        Toast.makeText(this, "Server error", Toast.LENGTH_LONG).show();
                        return;
                    }
                    try {
                        JSONObject json = new JSONObject(cleaned);
                        if (json.getString("status").equals("success")) {
                            displayPatient(json);
                            loadReportImages(json);
                            buildTimeline(json.getJSONArray("visits"));
                        } else {
                            Toast.makeText(this,
                                    json.optString("message", "Not found"),
                                    Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(this,
                                "Parse error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                },
                error -> Toast.makeText(this,
                        "Network error: " + error.toString(),
                        Toast.LENGTH_SHORT).show()
        );

        Volley.newRequestQueue(this).add(request);
    }

    // ─────────────────────────────────────────────
    //  Display Patient Info
    // ─────────────────────────────────────────────

    private void displayPatient(JSONObject json) throws JSONException {
        String id        = json.getString("id");
        String name      = json.getString("name");
        String age       = json.getString("age");
        String gender    = json.getString("gender");
        String blood     = json.getString("blood_group");
        String mobile    = json.optString("mobile",            "N/A");
        String address   = json.optString("address",           "N/A");
        String emergency = json.optString("emergency_contact", "N/A");
        String symptoms  = json.optString("symptoms",          "N/A");
        String diagnosis = json.optString("diagnosis",         "N/A");
        String allergies = json.optString("allergies",         "N/A");

        currentPatientName = name;
        currentMobile      = mobile;

        if (tvTopName     != null) tvTopName.setText(name);
        if (tvName        != null) tvName.setText(name);
        if (tvPatientInfo != null) tvPatientInfo.setText("ID: #" + id + " • " + gender);
        if (tvAge         != null) tvAge.setText(age);
        if (tvGender      != null) tvGender.setText(gender);
        if (tvBlood       != null) tvBlood.setText(blood);
        if (tvSymptoms    != null) tvSymptoms.setText(symptoms);
        if (tvDiagnosis   != null) tvDiagnosis.setText(diagnosis);
        if (tvAllergies   != null) tvAllergies.setText(allergies);
        if (tvMobile      != null) tvMobile.setText("📞 " + mobile);
        if (tvAddress     != null) tvAddress.setText("🏠 " + address);
        if (tvEmergency   != null) tvEmergency.setText("🚨 " + emergency);

        // Store full patient data for PDF / visit fee
        currentPatientData.clear();
        currentPatientData.put("id",                json.optString("id",                ""));
        currentPatientData.put("name",              json.optString("name",              ""));
        currentPatientData.put("age",               json.optString("age",               ""));
        currentPatientData.put("gender",            json.optString("gender",            ""));
        currentPatientData.put("blood_group",       json.optString("blood_group",       ""));
        currentPatientData.put("mobile",            json.optString("mobile",            ""));
        currentPatientData.put("address",           json.optString("address",           ""));
        currentPatientData.put("emergency_contact", json.optString("emergency_contact", ""));
        currentPatientData.put("symptoms",          json.optString("symptoms",          ""));
        currentPatientData.put("diagnosis",         json.optString("diagnosis",         ""));
        currentPatientData.put("allergies",         json.optString("allergies",         ""));
        currentPatientData.put("notes",             json.optString("notes",             ""));
        currentPatientData.put("abha_number",       json.optString("abha_number",       ""));
        currentPatientData.put("fee",               json.optString("fee",               "100")); // ← NEW
    }

    // ─────────────────────────────────────────────
    //  Load Report Images (patient-level header)
    // ─────────────────────────────────────────────

    private void loadReportImages(JSONObject json) {
        currentBloodReportUrl = json.optString("blood_report_url", "");
        currentUrineReportUrl = json.optString("urine_report_url", "");
        currentGeneralFileUrl = json.optString("general_file_url", "");

        String bloodUrl = currentBloodReportUrl;
        String urineUrl = currentUrineReportUrl;

        // Blood report
        if (!bloodUrl.isEmpty() && !bloodUrl.equals("null")) {
            if (imgBloodReport  != null) imgBloodReport.setVisibility(View.VISIBLE);
            if (tvNoBloodReport != null) tvNoBloodReport.setVisibility(View.GONE);
            Glide.with(this)
                    .load(bloodUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_close_clear_cancel)
                    .into(imgBloodReport);
            if (imgBloodReport != null)
                imgBloodReport.setOnClickListener(v -> openUrl(bloodUrl));
        } else {
            if (imgBloodReport  != null) imgBloodReport.setVisibility(View.GONE);
            if (tvNoBloodReport != null) tvNoBloodReport.setVisibility(View.VISIBLE);
        }

        // Urine report
        if (!urineUrl.isEmpty() && !urineUrl.equals("null")) {
            if (imgUrineReport  != null) imgUrineReport.setVisibility(View.VISIBLE);
            if (tvNoUrineReport != null) tvNoUrineReport.setVisibility(View.GONE);
            Glide.with(this)
                    .load(urineUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_close_clear_cancel)
                    .into(imgUrineReport);
            if (imgUrineReport != null)
                imgUrineReport.setOnClickListener(v -> openUrl(urineUrl));
        } else {
            if (imgUrineReport  != null) imgUrineReport.setVisibility(View.GONE);
            if (tvNoUrineReport != null) tvNoUrineReport.setVisibility(View.VISIBLE);
        }
    }

    private void openUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        startActivity(intent);
    }

    // ─────────────────────────────────────────────
    //  Visit Timeline — shows fee + per-visit reports
    // ─────────────────────────────────────────────

    private void buildTimeline(JSONArray visits) throws JSONException {
        currentVisits.clear();

        for (int i = 0; i < visits.length(); i++) {
            JSONObject v = visits.getJSONObject(i);
            Map<String, String> visitMap = new HashMap<>();
            visitMap.put("visit_date",       v.optString("visit_date",       ""));
            visitMap.put("symptoms",         v.optString("symptoms",         ""));
            visitMap.put("diagnosis",        v.optString("diagnosis",        ""));
            visitMap.put("notes",            v.optString("notes",            ""));
            visitMap.put("fee",              v.optString("fee",              "0"));
            visitMap.put("blood_report_url", v.optString("blood_report_url", ""));
            visitMap.put("urine_report_url", v.optString("urine_report_url", ""));
            currentVisits.add(visitMap);
        }

        if (timelineContainer == null) return;
        timelineContainer.removeAllViews();

        if (visits.length() == 0) {
            TextView empty = new TextView(this);
            empty.setText("No visit history yet.");
            empty.setTextColor(0xFF9CA3AF);
            empty.setTextSize(13);
            empty.setPadding(16, 16, 16, 16);
            timelineContainer.addView(empty);
            return;
        }

        for (int i = 0; i < visits.length(); i++) {
            JSONObject v       = visits.getJSONObject(i);
            String date        = v.optString("visit_date",       "");
            String symptoms    = v.optString("symptoms",         "N/A");
            String diagnosis   = v.optString("diagnosis",        "N/A");
            String notes       = v.optString("notes",            "");
            int    fee         = v.optInt("fee",                 0);        // ← NEW
            String bloodUrl    = v.optString("blood_report_url", "");       // ← NEW
            String urineUrl    = v.optString("urine_report_url", "");       // ← NEW

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundColor(0xFFFFFFFF);
            card.setPadding(16, 16, 16, 16);

            LinearLayout.LayoutParams cardParams =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, 0, 0, 10);
            card.setLayoutParams(cardParams);

            // Date + fee on same line
            TextView tvDate = new TextView(this);
            String feeText  = fee > 0 ? "  •  ₹" + fee : "";              // ← NEW
            tvDate.setText("📅 " + date + feeText);                        // ← NEW
            tvDate.setTextColor(0xFF2563EB);
            tvDate.setTextSize(12);
            tvDate.setTypeface(null, android.graphics.Typeface.BOLD);
            card.addView(tvDate);

            addVisitRow(card, "Symptoms",  symptoms);
            addVisitRow(card, "Diagnosis", diagnosis);
            if (!notes.isEmpty()) addVisitRow(card, "Notes", notes);

            timelineContainer.addView(card);
        }
    }

    private void addVisitRow(LinearLayout parent, String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rp =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        rp.setMargins(0, 6, 0, 0);
        row.setLayoutParams(rp);

        TextView tvLabel = new TextView(this);
        tvLabel.setText(label + ": ");
        tvLabel.setTextColor(0xFF6B7280);
        tvLabel.setTextSize(12);

        TextView tvValue = new TextView(this);
        tvValue.setText(value);
        tvValue.setTextColor(0xFF111827);
        tvValue.setTextSize(12);
        tvValue.setTypeface(null, android.graphics.Typeface.BOLD);

        row.addView(tvLabel);
        row.addView(tvValue);
        parent.addView(row);
    }

    // ─────────────────────────────────────────────
    //  Add Visit Dialog
    // ─────────────────────────────────────────────

    private void showAddVisitDialog() {
        visitBloodUri = null;
        visitUrineUri = null;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Today's Visit");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 10);

        LinearLayout.LayoutParams p =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 12, 0, 0);

        EditText etSymptoms = new EditText(this);
        etSymptoms.setHint("Symptoms");
        layout.addView(etSymptoms);

        EditText etDiagnosis = new EditText(this);
        etDiagnosis.setHint("Diagnosis");
        etDiagnosis.setLayoutParams(p);
        layout.addView(etDiagnosis);

        EditText etNotes = new EditText(this);
        etNotes.setHint("Notes (optional)");
        etNotes.setLayoutParams(p);
        layout.addView(etNotes);

        // Fee field — pre-filled from patient's stored fee
        EditText etFee = new EditText(this);
        etFee.setHint("Consultation Fee (₹)");
        etFee.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        String storedFee = currentPatientData.containsKey("fee")
                ? currentPatientData.get("fee") : "100";
        etFee.setText(storedFee);                                           // ← NEW
        etFee.setLayoutParams(p);
        layout.addView(etFee);

        TextView tvReportsLabel = new TextView(this);
        tvReportsLabel.setText("Upload Reports (optional)");
        tvReportsLabel.setTextColor(0xFF6B7280);
        tvReportsLabel.setTextSize(12);
        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 20, 0, 6);
        tvReportsLabel.setLayoutParams(lp);
        layout.addView(tvReportsLabel);

        btnVisitBlood = new TextView(this);
        btnVisitBlood.setText("🩸 Upload Blood Report");
        btnVisitBlood.setTextColor(0xFF2563EB);
        btnVisitBlood.setTextSize(13);
        btnVisitBlood.setPadding(0, 10, 0, 10);
        btnVisitBlood.setLayoutParams(p);
        btnVisitBlood.setClickable(true);
        btnVisitBlood.setFocusable(true);
        btnVisitBlood.setOnClickListener(v -> openVisitFilePicker(VisitUploadTarget.BLOOD));
        layout.addView(btnVisitBlood);

        btnVisitUrine = new TextView(this);
        btnVisitUrine.setText("🧪 Upload Urine Report");
        btnVisitUrine.setTextColor(0xFF2563EB);
        btnVisitUrine.setTextSize(13);
        btnVisitUrine.setPadding(0, 10, 0, 10);
        btnVisitUrine.setLayoutParams(p);
        btnVisitUrine.setClickable(true);
        btnVisitUrine.setFocusable(true);
        btnVisitUrine.setOnClickListener(v -> openVisitFilePicker(VisitUploadTarget.URINE));
        layout.addView(btnVisitUrine);

        builder.setView(layout);

        builder.setPositiveButton("Save Visit", (dialog, which) -> {
            String symptoms  = etSymptoms.getText().toString().trim();
            String diagnosis = etDiagnosis.getText().toString().trim();
            String notes     = etNotes.getText().toString().trim();
            String feeStr    = etFee.getText().toString().trim();           // ← NEW
            int fee = feeStr.isEmpty() ? 100 : Integer.parseInt(feeStr);   // ← NEW

            if (symptoms.isEmpty() && diagnosis.isEmpty()) {
                Toast.makeText(this,
                        "Enter at least symptoms or diagnosis",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            saveVisit(symptoms, diagnosis, notes, fee);                    // ← NEW: pass fee
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // ─────────────────────────────────────────────
    //  Visit File Picker
    // ─────────────────────────────────────────────

    private void openVisitFilePicker(VisitUploadTarget target) {
        visitUploadTarget = target;
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES,
                new String[]{"image/jpeg", "image/png", "application/pdf"});
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        visitFilePickerLauncher.launch(Intent.createChooser(intent, "Select File"));
    }

    private void handleVisitFilePicked(Uri uri) {
        if (visitUploadTarget == VisitUploadTarget.BLOOD) {
            visitBloodUri = uri;
            if (btnVisitBlood != null) btnVisitBlood.setText("✓ Blood Report Selected");
        } else {
            visitUrineUri = uri;
            if (btnVisitUrine != null) btnVisitUrine.setText("✓ Urine Report Selected");
        }
    }

    // ─────────────────────────────────────────────
    //  Save Visit
    // ─────────────────────────────────────────────

    private void saveVisit(
            String symptoms, String diagnosis, String notes, int fee) { // ← NEW: fee param

        String boundary = "Boundary-" + System.currentTimeMillis();
        byte[] body;

        try {
            body = buildVisitBody(boundary, symptoms, diagnosis, notes, fee); // ← NEW
        } catch (Exception e) {
            Toast.makeText(this, "Build error: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        final byte[] finalBody = body;
        String url = BASE_URL + "add_visit.php";

        Request<String> request =
                new Request<String>(Request.Method.POST, url,
                        error -> Toast.makeText(this,
                                "Network error: " + error.toString(),
                                Toast.LENGTH_SHORT).show()
                ) {
                    @Override public String getBodyContentType() {
                        return "multipart/form-data; boundary=" + boundary;
                    }
                    @Override public byte[] getBody() { return finalBody; }

                    @Override
                    protected Response<String> parseNetworkResponse(NetworkResponse response) {
                        return Response.success(
                                new String(response.data),
                                HttpHeaderParser.parseCacheHeaders(response));
                    }

                    @Override
                    protected void deliverResponse(String response) {
                        try {
                            String cleaned = response.trim();
                            int start = cleaned.indexOf("{");
                            if (start > 0) cleaned = cleaned.substring(start);

                            JSONObject json = new JSONObject(cleaned);
                            if (json.getString("status").equals("success")) {
                                int feeRecorded = json.optInt("fee_recorded", 0);
                                Toast.makeText(PatientProfileActivity.this,
                                        "Visit saved ✓  ₹" + feeRecorded + " added to revenue",
                                        Toast.LENGTH_SHORT).show();
                                fetchPatientDetails(currentPatientId); // refresh
                            } else {
                                Toast.makeText(PatientProfileActivity.this,
                                        "Failed: " + json.optString("message"),
                                        Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            Toast.makeText(PatientProfileActivity.this,
                                    "Error: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                };

        request.setRetryPolicy(new DefaultRetryPolicy(
                30000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        Volley.newRequestQueue(this).add(request);
    }

    // ─────────────────────────────────────────────
    //  Build Multipart Body for Visit
    // ─────────────────────────────────────────────

    private byte[] buildVisitBody(
            String boundary,
            String symptoms,
            String diagnosis,
            String notes,
            int fee                                                         // ← NEW
    ) throws IOException {

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        String LB = "\r\n";

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("patient_id", currentPatientId);
        fields.put("symptoms",   symptoms);
        fields.put("diagnosis",  diagnosis);
        fields.put("notes",      notes);
        fields.put("fee",        String.valueOf(fee));                      // ← NEW

        for (Map.Entry<String, String> e : fields.entrySet()) {
            dos.writeBytes("--" + boundary + LB);
            dos.writeBytes("Content-Disposition: form-data; name=\""
                    + e.getKey() + "\"" + LB);
            dos.writeBytes(LB);
            dos.write(e.getValue().getBytes("UTF-8"));
            dos.writeBytes(LB);
        }

        appendVisitFile(dos, boundary, "blood_report", visitBloodUri, LB);
        appendVisitFile(dos, boundary, "urine_report", visitUrineUri, LB);

        dos.writeBytes("--" + boundary + "--" + LB);
        dos.flush();
        return bos.toByteArray();
    }

    private void appendVisitFile(
            DataOutputStream dos, String boundary,
            String fieldName, Uri uri, String LB) throws IOException {
        if (uri == null) return;
        String fileName = getFileNameFromUri(uri);
        String mime     = getContentResolver().getType(uri);
        if (mime == null) mime = "application/octet-stream";
        byte[] bytes = readBytes(uri);
        dos.writeBytes("--" + boundary + LB);
        dos.writeBytes("Content-Disposition: form-data; name=\""
                + fieldName + "\"; filename=\"" + fileName + "\"" + LB);
        dos.writeBytes("Content-Type: " + mime + LB);
        dos.writeBytes(LB);
        dos.write(bytes);
        dos.writeBytes(LB);
    }

    // ─────────────────────────────────────────────
    //  Send WhatsApp / SMS
    // ─────────────────────────────────────────────

    private void sendWhatsAppMessage() {
        if (currentMobile.equals("N/A") || currentMobile.isEmpty()) {
            Toast.makeText(this, "No mobile number available", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Send Message to " + currentPatientName);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 10);

        EditText etMessage = new EditText(this);
        etMessage.setHint("Type your message...");
        etMessage.setMinLines(3);
        etMessage.setMaxLines(6);
        layout.addView(etMessage);

        TextView tvChoose = new TextView(this);
        tvChoose.setText("Send via:");
        tvChoose.setTextColor(0xFF6B7280);
        tvChoose.setTextSize(13);
        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 20, 0, 8);
        tvChoose.setLayoutParams(lp);
        layout.addView(tvChoose);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout.LayoutParams btnParams =
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        btnParams.setMargins(0, 0, 8, 0);

        TextView btnWhatsApp = new TextView(this);
        btnWhatsApp.setText("💬 WhatsApp");
        btnWhatsApp.setGravity(android.view.Gravity.CENTER);
        btnWhatsApp.setTextColor(0xFFFFFFFF);
        btnWhatsApp.setTextSize(13);
        btnWhatsApp.setBackgroundColor(0xFF25D366);
        btnWhatsApp.setPadding(16, 20, 16, 20);
        btnWhatsApp.setLayoutParams(btnParams);
        btnWhatsApp.setClickable(true);
        btnWhatsApp.setFocusable(true);

        LinearLayout.LayoutParams btnParams2 =
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        TextView btnSMS = new TextView(this);
        btnSMS.setText("📱 SMS");
        btnSMS.setGravity(android.view.Gravity.CENTER);
        btnSMS.setTextColor(0xFFFFFFFF);
        btnSMS.setTextSize(13);
        btnSMS.setBackgroundColor(0xFF2563EB);
        btnSMS.setPadding(16, 20, 16, 20);
        btnSMS.setLayoutParams(btnParams2);
        btnSMS.setClickable(true);
        btnSMS.setFocusable(true);

        btnRow.addView(btnWhatsApp);
        btnRow.addView(btnSMS);
        layout.addView(btnRow);

        TextView btnBoth = new TextView(this);
        btnBoth.setText("📨 Send via Both");
        btnBoth.setGravity(android.view.Gravity.CENTER);
        btnBoth.setTextColor(0xFFFFFFFF);
        btnBoth.setTextSize(13);
        btnBoth.setBackgroundColor(0xFF7C3AED);
        btnBoth.setPadding(16, 20, 16, 20);
        LinearLayout.LayoutParams bothParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        bothParams.setMargins(0, 10, 0, 0);
        btnBoth.setLayoutParams(bothParams);
        btnBoth.setClickable(true);
        btnBoth.setFocusable(true);
        layout.addView(btnBoth);

        builder.setView(layout);
        builder.setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.create();

        btnWhatsApp.setOnClickListener(v -> {
            String message = etMessage.getText().toString().trim();
            if (message.isEmpty()) {
                Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show();
                return;
            }
            dialog.dismiss();
            openWhatsApp(currentMobile, message);
        });

        btnSMS.setOnClickListener(v -> {
            String message = etMessage.getText().toString().trim();
            if (message.isEmpty()) {
                Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show();
                return;
            }
            dialog.dismiss();
            openSMS(currentMobile, message);
        });

        btnBoth.setOnClickListener(v -> {
            String message = etMessage.getText().toString().trim();
            if (message.isEmpty()) {
                Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show();
                return;
            }
            dialog.dismiss();
            openWhatsApp(currentMobile, message);
            new android.os.Handler().postDelayed(
                    () -> openSMS(currentMobile, message), 1500);
        });

        dialog.show();
    }

    private void openWhatsApp(String mobile, String message) {
        String phone = mobile.replaceAll("[^0-9]", "");
        if (!phone.startsWith("91") && phone.length() == 10) phone = "91" + phone;
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(
                    "https://api.whatsapp.com/send?phone=" + phone
                            + "&text=" + Uri.encode(message)));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "WhatsApp not installed", Toast.LENGTH_SHORT).show();
        }
    }

    private void openSMS(String mobile, String message) {
        String phone = mobile.replaceAll("[^0-9]", "");
        try {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("smsto:" + phone));
            intent.putExtra("sms_body", message);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "SMS app not found", Toast.LENGTH_SHORT).show();
        }
    }

    // ─────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────

    private String getFileNameFromUri(Uri uri) {
        String name = "file";
        try (Cursor c = getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int index = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) name = c.getString(index);
            }
        } catch (Exception e) {
            name = uri.getLastPathSegment();
        }
        return name;
    }

    private byte[] readBytes(Uri uri) throws IOException {
        InputStream is = getContentResolver().openInputStream(uri);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int nRead;
        while ((nRead = is.read(data)) != -1) buffer.write(data, 0, nRead);
        is.close();
        return buffer.toByteArray();
    }

    // ─────────────────────────────────────────────
    //  Generate & Share PDF
    // ─────────────────────────────────────────────

    private void generateAndSharePDF() {
        Toast.makeText(this, "Generating PDF...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                ReportGenerator generator = new ReportGenerator(this);
                File pdfFile = generator.generatePDF(
                        currentPatientData,
                        currentVisits,
                        currentBloodReportUrl.isEmpty() ? null : currentBloodReportUrl,
                        currentUrineReportUrl.isEmpty() ? null : currentUrineReportUrl
                );
                runOnUiThread(() -> {
                    Toast.makeText(this, "PDF Generated!", Toast.LENGTH_SHORT).show();
                    showPDFOptions(pdfFile);
                });
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "PDF Error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void showPDFOptions(File pdfFile) {
        Uri pdfUri = FileProvider.getUriForFile(
                this, getPackageName() + ".provider", pdfFile);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("PDF Generated!");
        builder.setMessage("What would you like to do?");

        builder.setPositiveButton("📖 Open", (d, w) -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(pdfUri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try { startActivity(intent); }
            catch (Exception e) {
                Toast.makeText(this, "No PDF viewer installed", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNeutralButton("📤 Share", (d, w) -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, pdfUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Medical Report - " + currentPatientName);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share PDF via"));
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // ─────────────────────────────────────────────
    //  Share Reports
    // ─────────────────────────────────────────────

    private void shareReports() {
        boolean hasBlood   = !currentBloodReportUrl.isEmpty() && !currentBloodReportUrl.equals("null");
        boolean hasUrine   = !currentUrineReportUrl.isEmpty() && !currentUrineReportUrl.equals("null");
        boolean hasGeneral = !currentGeneralFileUrl.isEmpty() && !currentGeneralFileUrl.equals("null");

        if (!hasBlood && !hasUrine && !hasGeneral) {
            Toast.makeText(this, "No reports available to share", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Share Reports");

        List<String> options = new ArrayList<>();
        List<String> urls    = new ArrayList<>();

        if (hasBlood)   { options.add("🩸 Blood Report");  urls.add(currentBloodReportUrl); }
        if (hasUrine)   { options.add("🧪 Urine Report");  urls.add(currentUrineReportUrl); }
        if (hasGeneral) { options.add("📄 General File");  urls.add(currentGeneralFileUrl); }
        options.add("📋 All Reports");

        String[] items = options.toArray(new String[0]);

        builder.setItems(items, (dialog, which) -> {
            if (which == options.size() - 1) shareAllReports(urls);
            else shareReportUrl(urls.get(which));
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void shareReportUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Medical Report - " + currentPatientName);
        intent.putExtra(Intent.EXTRA_TEXT, "Report for " + currentPatientName + ":\n" + url);
        startActivity(Intent.createChooser(intent, "Share via"));
    }

    private void shareAllReports(List<String> urls) {
        StringBuilder sb = new StringBuilder();
        sb.append("Medical Reports for ").append(currentPatientName).append(":\n\n");
        for (String url : urls) sb.append(url).append("\n\n");
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Medical Reports - " + currentPatientName);
        intent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        startActivity(Intent.createChooser(intent, "Share via"));
    }
}