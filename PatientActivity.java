package com.example.hostipal_info;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

public class PatientActivity extends AppCompatActivity {

    private static final String BASE_URL = "http://tsm.ecssofttech.com/Gaytree/hospital/";

    TextView tvName, tvDiagnosis, tvMobile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        tvName = findViewById(R.id.tvName);
        tvDiagnosis = findViewById(R.id.tvDiagnosis);
        tvMobile = findViewById(R.id.tvMobile);

        String id = getIntent().getStringExtra("patient_id");

        loadPatient(id);
    }

    private void loadPatient(String id) {

        String url = BASE_URL + "get_patient.php?id=" + id;

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject obj = new JSONObject(response);

                        tvName.setText(obj.getString("name"));
                        tvDiagnosis.setText(obj.getString("diagnosis"));
                        tvMobile.setText(obj.getString("mobile"));

                    } catch (Exception e) {
                        Toast.makeText(this, "Parse error", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show()
        );

        Volley.newRequestQueue(this).add(request);
    }
}