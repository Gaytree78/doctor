package com.example.hostipal_info;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class DoctorDashboardActivity extends AppCompatActivity {

    private static final String TAG      = "Dashboard";
    private static final String BASE_URL = "http://tsm.ecssofttech.com/Gaytree/hospital/";

    // Header stat views
    TextView tvTotalPatients, tvTodayVisits, tvDashHeaderRevenue;

    // Revenue card views
    TextView tvRevenueAmount, tvRevenueVisitCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // ── Bind header stats ──────────────────────────────────
        tvTotalPatients     = findViewById(R.id.tvDashTotalPatients);
        tvTodayVisits       = findViewById(R.id.tvDashTodayVisits);
        tvDashHeaderRevenue = findViewById(R.id.tvDashHeaderRevenue);  // ← NEW

        // ── Bind revenue card views ────────────────────────────
        tvRevenueAmount     = findViewById(R.id.tvRevenueAmount);      // ← NEW
        tvRevenueVisitCount = findViewById(R.id.tvRevenueVisitCount);  // ← NEW

        // ── Card: Add Patient ──────────────────────────────────
        findViewById(R.id.cardAdd).setOnClickListener(v ->
                startActivity(new Intent(this, AddPatientActivity.class)));

        // ── Card: View Patients ────────────────────────────────
        findViewById(R.id.cardView).setOnClickListener(v ->
                startActivity(new Intent(this, ViewPatientActivity.class)));

        // ── Card: Search ───────────────────────────────────────
        findViewById(R.id.cardSearch).setOnClickListener(v ->
                startActivity(new Intent(this, SearchPatientActivity.class)));

        // ── Card: Reports ──────────────────────────────────────


        // ── Card: Revenue ──────────────────────────────────────
        // Click → show breakdown dialog with option to add extra fee
        findViewById(R.id.cardRevenue).setOnClickListener(v ->
                showRevenueDialog());

        // ── Card: Appointments ─────────────────────────────────
        findViewById(R.id.cardAppointment).setOnClickListener(v ->
                Toast.makeText(this, "Appointments coming soon!", Toast.LENGTH_SHORT).show());

        // ── Card: Logout ───────────────────────────────────────
        findViewById(R.id.cardLogout).setOnClickListener(v ->
                showLogoutConfirm());

        loadStats();
    }

    // ─────────────────────────────────────────────────────────────
    // Refresh stats every time user comes back to dashboard
    // (e.g. after adding a new patient)
    // ─────────────────────────────────────────────────────────────
    @Override
    protected void onResume() {
        super.onResume();
        loadStats();
    }

    // ─────────────────────────────────────────────────────────────
    // Loads total patients, today visits, today revenue from server
    // ─────────────────────────────────────────────────────────────
    private void loadStats() {
        String url = BASE_URL + "get_dashboard_stats.php";

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    Log.d(TAG, "Stats raw: " + response);

                    // Strip any PHP warnings/notices before the JSON
                    String cleaned = response.trim();
                    int start = cleaned.indexOf('{');
                    if (start > 0) cleaned = cleaned.substring(start);
                    if (start < 0) return; // no JSON at all

                    try {
                        JSONObject json = new JSONObject(cleaned);
                        if (!"success".equals(json.getString("status"))) return;

                        int totalPatients = json.getInt("total_patients");
                        int todayVisits   = json.getInt("today_visits");
                        int todayRevenue  = json.getInt("today_revenue");   // ← NEW

                        // Header stat pills
                        if (tvTotalPatients != null)
                            tvTotalPatients.setText(String.valueOf(totalPatients));
                        if (tvTodayVisits != null)
                            tvTodayVisits.setText(String.valueOf(todayVisits));
                        if (tvDashHeaderRevenue != null)
                            tvDashHeaderRevenue.setText(String.valueOf(todayRevenue));

                        // Revenue card
                        if (tvRevenueAmount != null)
                            tvRevenueAmount.setText("₹ " + todayRevenue);
                        if (tvRevenueVisitCount != null)
                            tvRevenueVisitCount.setText(todayVisits
                                    + " visit" + (todayVisits == 1 ? "" : "s") + " today");

                    } catch (JSONException e) {
                        Log.e(TAG, "Stats JSON error: " + e.getMessage());
                    }
                },
                error -> Log.e(TAG, "Stats network error: " + error.toString())
        );

        Volley.newRequestQueue(this).add(request);
    }

    // ─────────────────────────────────────────────────────────────
    // Revenue dialog — shows breakdown + "Add Extra Fee" button
    // ─────────────────────────────────────────────────────────────
    private void showRevenueDialog() {
        String url = BASE_URL + "get_dashboard.php";

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    String cleaned = response.trim();
                    int start = cleaned.indexOf('{');
                    if (start > 0) cleaned = cleaned.substring(start);

                    try {
                        JSONObject json = new JSONObject(cleaned);
                        if (!"success".equals(json.getString("status"))) return;

                        int todayVisits   = json.getInt("today_visits");
                        int todayRevenue  = json.getInt("today_revenue");
                        int feePerVisit   = json.optInt("fee_per_visit", 100);
                        int extraFee      = json.optInt("extra_fee_today", 0);  // ← NEW
                        int baseRevenue   = todayVisits * feePerVisit;

                        String msg =
                                "📅  Date             Today\n\n" +
                                        "👥  Visits today     " + todayVisits + "\n\n" +
                                        "💊  Fee per visit    ₹" + feePerVisit + "\n" +
                                        "     " + todayVisits + " × ₹" + feePerVisit
                                        + " = ₹" + baseRevenue + "\n\n" +
                                        "➕  Extra fees       ₹" + extraFee + "\n\n" +
                                        "──────────────────────────\n\n" +
                                        "💰  Total Revenue    ₹" + todayRevenue + "\n\n" +
                                        "Resets automatically at midnight.";

                        new AlertDialog.Builder(this)
                                .setTitle("💰 Today's Revenue")
                                .setMessage(msg)
                                // "Add Extra Fee" button — lets doctor add a custom amount
                                .setNeutralButton("➕ Add Extra Fee", (d, w) ->
                                        showAddExtraFeeDialog())
                                .setPositiveButton("Close", null)
                                .show();

                    } catch (JSONException e) {
                        Log.e(TAG, "Revenue dialog error: " + e.getMessage());
                        Toast.makeText(this, "Could not load revenue", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show()
        );

        Volley.newRequestQueue(this).add(request);
    }

    // ─────────────────────────────────────────────────────────────
    // Extra fee dialog — doctor enters any custom amount
    // POSTs to add_extra_fee.php → stored in daily_extra_fees table
    // ─────────────────────────────────────────────────────────────
    private void showAddExtraFeeDialog() {
        // Input field for the extra amount
        EditText etAmount = new EditText(this);
        etAmount.setHint("Enter extra amount (₹)");
        etAmount.setInputType(InputType.TYPE_CLASS_NUMBER);

        // Note field (optional reason)
        EditText etNote = new EditText(this);
        etNote.setHint("Reason (optional, e.g. consultation)");
        etNote.setInputType(InputType.TYPE_CLASS_TEXT);

        // Wrap both in a vertical layout
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 20, 60, 10);
        layout.addView(etAmount);
        layout.addView(etNote);

        new AlertDialog.Builder(this)
                .setTitle("Add Extra Fee")
                .setView(layout)
                .setPositiveButton("Add", (dialog, which) -> {
                    String amountStr = etAmount.getText().toString().trim();
                    String note      = etNote.getText().toString().trim();

                    if (amountStr.isEmpty()) {
                        Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int amount = Integer.parseInt(amountStr);
                    if (amount <= 0) {
                        Toast.makeText(this, "Amount must be greater than 0", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    postExtraFee(amount, note);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ─────────────────────────────────────────────────────────────
    // POSTs extra fee to add_extra_fee.php
    // ─────────────────────────────────────────────────────────────
    private void postExtraFee(int amount, String note) {
        String url = BASE_URL + "add_extra_fee.php";

        StringRequest request = new StringRequest(Request.Method.POST, url,
                response -> {
                    Log.d(TAG, "Extra fee response: " + response);
                    String cleaned = response.trim();
                    int start = cleaned.indexOf('{');
                    if (start > 0) cleaned = cleaned.substring(start);

                    try {
                        JSONObject json = new JSONObject(cleaned);
                        if ("success".equals(json.getString("status"))) {
                            Toast.makeText(this,
                                    "₹" + amount + " added to today's revenue!",
                                    Toast.LENGTH_SHORT).show();
                            loadStats(); // ← refresh cards immediately
                        } else {
                            Toast.makeText(this,
                                    "Failed: " + json.optString("message", "Unknown error"),
                                    Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Extra fee parse error: " + e.getMessage());
                    }
                },
                error -> {
                    Log.e(TAG, "Extra fee network error: " + error.toString());
                    Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show();
                }
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("amount", String.valueOf(amount));
                params.put("note",   note);
                return params;
            }
        };

        Volley.newRequestQueue(this).add(request);
    }

    // ─────────────────────────────────────────────────────────────
    // Reports dialog — total patients + visits summary
    // ─────────────────────────────────────────────────────────────
    private void showReportsDialog() {
        String url = BASE_URL + "get_dashboard.php";

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    String cleaned = response.trim();
                    int start = cleaned.indexOf('{');
                    if (start > 0) cleaned = cleaned.substring(start);

                    try {
                        JSONObject json = new JSONObject(cleaned);
                        if ("success".equals(json.getString("status"))) {
                            String msg =
                                    "👥  Total Patients:    " + json.getString("total_patients") + "\n\n" +
                                            "📅  Visits Today:      " + json.getString("today_visits")   + "\n\n" +
                                            "🏥  Total Visits Ever: " + json.optString("total_visits", "0") + "\n\n" +
                                            "💰  Today's Revenue:   ₹" + json.optInt("today_revenue", 0);

                            new AlertDialog.Builder(this)
                                    .setTitle("📊 Hospital Reports")
                                    .setMessage(msg)
                                    .setPositiveButton("Close", null)
                                    .show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(this, "Could not load reports", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show()
        );

        Volley.newRequestQueue(this).add(request);
    }

    // ─────────────────────────────────────────────────────────────
    // Logout confirmation
    // ─────────────────────────────────────────────────────────────
    private void showLogoutConfirm() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    getSharedPreferences("USER_SESSION", MODE_PRIVATE)
                            .edit().clear().apply();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                            Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}