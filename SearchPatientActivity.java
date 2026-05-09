package com.example.hostipal_info;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

public class SearchPatientActivity extends AppCompatActivity {

    EditText etSearch;
    LinearLayout resultsContainer;
    LinearLayout recentContainer;
    SharedPreferences prefs;

    private static final String BASE_URL = "http://tsm.ecssofttech.com/Gaytree/hospital/";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        etSearch         = findViewById(R.id.etSearch);
        resultsContainer = findViewById(R.id.searchResultsContainer);
        recentContainer  = findViewById(R.id.recentContainer);
        prefs            = getSharedPreferences("search_history", MODE_PRIVATE);

        // Back button
        ImageView btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        loadRecentSearches();

        // Live search
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                searchPatients(s.toString().trim());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void saveRecentSearch(String text) {
        if (text.trim().isEmpty()) return;

        String old = prefs.getString("data", "");
        ArrayList<String> list = new ArrayList<>();

        // Build existing list
        if (!old.isEmpty()) {
            list.addAll(Arrays.asList(old.split(",")));
        }

        // Remove if already exists (to move it to top)
        list.remove(text.trim());

        // Add to top
        list.add(0, text.trim());

        // Keep max 10 recent searches
        if (list.size() > 10) {
            list = new ArrayList<>(list.subList(0, 10));
        }

        prefs.edit().putString("data", String.join(",", list)).apply();
        loadRecentSearches();
    }

    private void removeRecentSearch(String text) {
        String old = prefs.getString("data", "");
        ArrayList<String> list = new ArrayList<>();

        if (!old.isEmpty()) {
            list.addAll(Arrays.asList(old.split(",")));
        }

        // ✅ Remove the specific item
        list.remove(text.trim());

        prefs.edit().putString("data", String.join(",", list)).apply();
        loadRecentSearches(); // refresh UI
    }

    private void loadRecentSearches() {
        recentContainer.removeAllViews();

        String data    = prefs.getString("data", "");
        if (data.isEmpty()) return;

        String[] items = data.split(",");

        for (String s : items) {
            if (s.trim().isEmpty()) continue;

            // ✅ Each row: search icon + text + X button
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(8, 8, 8, 8);

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            row.setLayoutParams(rowParams);

            // Search icon
            TextView tvIcon = new TextView(this);
            tvIcon.setText("🔍");
            tvIcon.setPadding(0, 0, 12, 0);
            tvIcon.setTextSize(14);

            // Search term text
            TextView tvText = new TextView(this);
            tvText.setText(s.trim());
            tvText.setTextSize(14);
            tvText.setTextColor(0xFF111827);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f // weight — takes remaining space
            );
            tvText.setLayoutParams(textParams);

            // ✅ X (cancel/remove) button
            TextView tvRemove = new TextView(this);
            tvRemove.setText("✕");
            tvRemove.setTextSize(14);
            tvRemove.setTextColor(0xFF9CA3AF);
            tvRemove.setPadding(12, 0, 4, 0);

            // Click whole row → fill search bar
            final String searchTerm = s.trim();
            row.setOnClickListener(v -> {
                etSearch.setText(searchTerm);
                searchPatients(searchTerm);
            });

            // ✅ Click X → remove only this item
            tvRemove.setOnClickListener(v -> {
                removeRecentSearch(searchTerm);
            });

            row.addView(tvIcon);
            row.addView(tvText);
            row.addView(tvRemove);

            recentContainer.addView(row);
        }
    }

    private void searchPatients(String query) {
        resultsContainer.removeAllViews();

        if (query.isEmpty()) return;

        String url = BASE_URL + "search_patient.php?query=" + query;

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        JSONArray arr   = json.getJSONArray("patients");

                        resultsContainer.removeAllViews();

                        if (arr.length() == 0) {
                            TextView empty = new TextView(this);
                            empty.setText("No patients found.");
                            empty.setPadding(12, 12, 12, 12);
                            empty.setTextColor(0xFF9CA3AF);
                            resultsContainer.addView(empty);
                            return;
                        }

                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject p = arr.getJSONObject(i);

                            String patientId   = p.getString("id");
                            String patientName = p.getString("name");
                            String mobile      = p.optString("mobile", "N/A");
                            String age         = p.optString("age", "--");
                            String gender      = p.optString("gender", "--");

                            View item = LayoutInflater.from(this)
                                    .inflate(R.layout.item_search_patient, resultsContainer, false);

                            TextView tvName   = item.findViewById(R.id.tvName);
                            TextView tvMobile = item.findViewById(R.id.tvMobile);
                            TextView tvInfo   = item.findViewById(R.id.tvInfo);

                            if (tvName   != null) tvName.setText(patientName);
                            if (tvMobile != null) tvMobile.setText("📞 " + mobile);
                            if (tvInfo   != null) tvInfo.setText(age + " yrs • " + gender);

                            // ✅ Click → open profile + save recent
                            item.setOnClickListener(v -> {
                                saveRecentSearch(query);

                                Intent intent = new Intent(this, PatientProfileActivity.class);
                                intent.putExtra("patient_id", patientId);
                                intent.putExtra("name", patientName);
                                startActivity(intent);
                            });

                            resultsContainer.addView(item);
                        }

                    } catch (Exception e) {
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Network Error", Toast.LENGTH_SHORT).show()
        );

        Volley.newRequestQueue(this).add(request);
    }
}