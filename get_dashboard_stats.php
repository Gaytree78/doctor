<?php
// ─────────────────────────────────────────────────────────────────
// get_dashboard_stats.php
// Returns: total patients, today's visit count, today's revenue
//
// Revenue rule: ₹100 per visit logged in patient_visits for TODAY
// This resets automatically each day because we filter by CURDATE()
// ─────────────────────────────────────────────────────────────────

header("Content-Type: application/json");
include "db.php";

$fee_per_visit = 100; // ← change this to adjust the per-visit fee

// 1. Total patients ever registered
$totalPatients = 0;
$r1 = $conn->query("SELECT COUNT(*) AS total FROM patients");
if ($r1) {
    $totalPatients = $r1->fetch_assoc()['total'];
}

// 2. Today's visit count (rows in patient_visits where visit_date = today)
$todayVisits = 0;
$r2 = $conn->query("SELECT COUNT(*) AS today FROM patient_visits WHERE visit_date = CURDATE()");
if ($r2) {
    $todayVisits = (int) $r2->fetch_assoc()['today'];
}

// 3. Today's revenue = visits today × fee_per_visit
$todayRevenue = $todayVisits * $fee_per_visit;

$conn->close();

echo json_encode([
    "status"         => "success",
    "total_patients" => (int) $totalPatients,
    "today_visits"   => $todayVisits,
    "today_revenue"  => $todayRevenue,
    "fee_per_visit"  => $fee_per_visit
]);
?>