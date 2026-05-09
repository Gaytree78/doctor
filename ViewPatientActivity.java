package com.example.hostipal_info;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ViewPatientActivity extends AppCompatActivity {

    private static final String BASE_URL = "http://tsm.ecssofttech.com/Gaytree/hospital/";

    TextView tvTotalPatients, tvTodayVisits;
    LinearLayout recentActivityList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_patient);

        // Bottom Navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        if (bottomNav != null) {
            bottomNav.setOnItemSelectedListener(item -> {
                if (item.getItemId() == R.id.nav_home) {
                    return true;
                } else if (item.getItemId() == R.id.nav_dashboard) {
                    startActivity(new Intent(this, DoctorDashboardActivity.class));
                    return true;
                } else if (item.getItemId() == R.id.nav_login) {
                    logoutUser();
                    return true;
                }
                return false;
            });
        }

        // Bind views
        tvTotalPatients    = findViewById(R.id.tvTotalPatients);
        tvTodayVisits      = findViewById(R.id.tvTodayVisits);
        recentActivityList = findViewById(R.id.recentActivityList);

        // Add Patient Banner
        View cardAdd = findViewById(R.id.cardAddPatient);
        if (cardAdd != null) {
            cardAdd.setOnClickListener(v ->
                    startActivity(new Intent(this, AddPatientActivity.class)));
        }

        // Floating Action Button
        View fab = findViewById(R.id.fabAdd);
        if (fab != null) {
            fab.setOnClickListener(v ->
                    startActivity(new Intent(this, AddPatientActivity.class)));
        }

        // ✅ Search bar
        View searchBar = findViewById(R.id.searchBar);
        if (searchBar != null) {
            searchBar.setOnClickListener(v ->
                    startActivity(new Intent(this, SearchPatientActivity.class)));
        }
        View searchBar2 = findViewById(R.id.etSearch);
        if (searchBar2 != null){
            searchBar2.setOnClickListener(v ->
                    startActivity(new Intent(this, SearchPatientActivity.class)));
        }

        // Filter icon
        View ivFilter = findViewById(R.id.ivFilter);
        if (ivFilter != null) {
            ivFilter.setOnClickListener(v ->
                    Toast.makeText(this, "Filter coming soon", Toast.LENGTH_SHORT).show());
        }

        loadDashboard();
    }

    private void logoutUser() {
        getSharedPreferences("USER_SESSION", MODE_PRIVATE)
                .edit().clear().apply();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboard();
    }

    private void loadDashboard() {
        String url = BASE_URL + "get_dashboard.php";

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.getString("status").equals("success")) {
                            if (tvTotalPatients != null)
                                tvTotalPatients.setText(json.getString("total_patients"));
                            if (tvTodayVisits != null)
                                tvTodayVisits.setText(json.getString("today_visits"));
                            if (recentActivityList != null)
                                buildRecentList(json.getJSONArray("recent_patients"));
                        }
                    } catch (JSONException e) {
                        Toast.makeText(this, "Parse error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Network error: " + error.toString(),
                        Toast.LENGTH_SHORT).show()
        );

        Volley.newRequestQueue(this).add(request);
    }

    private void buildRecentList(JSONArray patients) throws JSONException {
        recentActivityList.removeAllViews();

        for (int i = 0; i < patients.length(); i++) {
            JSONObject p = patients.getJSONObject(i);

            String id        = p.getString("id");
            String name      = p.getString("name");
            String initials  = p.getString("initials");
            String diagnosis = p.optString("diagnosis", "No diagnosis");
            String symptoms  = p.optString("symptoms",  "No symptoms");
            String timeAgo   = p.getString("time_ago");
            String lastVisit = p.optString("last_visit", "");

            View item = LayoutInflater.from(this)
                    .inflate(R.layout.item_patient_activity, recentActivityList, false);

            TextView tvInitials  = item.findViewById(R.id.tvInitials);
            TextView tvName      = item.findViewById(R.id.tvPatientName);
            TextView tvTimeAgo   = item.findViewById(R.id.tvTimeAgo);
            TextView tvDiagnosis = item.findViewById(R.id.tvDiagnosis);
            TextView tvSymptoms  = item.findViewById(R.id.tvSymptoms);
            TextView tvId        = item.findViewById(R.id.tvPatientId);
            TextView tvLastVisit = item.findViewById(R.id.tvLastVisit);

            if (tvInitials  != null) tvInitials.setText(initials);
            if (tvName      != null) tvName.setText(name);
            if (tvTimeAgo   != null) tvTimeAgo.setText(timeAgo);
            if (tvDiagnosis != null) tvDiagnosis.setText(diagnosis);  // ✅ latest
            if (tvSymptoms  != null) tvSymptoms.setText(symptoms);    // ✅ latest
            if (tvId        != null) tvId.setText("ID: #" + id);
            if (tvLastVisit != null) tvLastVisit.setText(
                    lastVisit.equals("No visits yet") ? lastVisit : "Last: " + lastVisit);

            item.setOnClickListener(v -> {
                Intent intent = new Intent(this, PatientProfileActivity.class);
                intent.putExtra("patient_id", id);
                intent.putExtra("name", name);
                startActivity(intent);
            });

            recentActivityList.addView(item);

            // Divider
            if (i < patients.length() - 1) {
                View divider = new View(this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1);
                params.setMarginStart(68);
                divider.setLayoutParams(params);
                divider.setBackgroundColor(0xFFF3F4F6);
                recentActivityList.addView(divider);
            }
        }
    }
}