<?php
include "db.php";

header('Content-Type: application/json');

// Total patients
$totalResult   = $conn->query("SELECT COUNT(*) as total FROM patients");
$totalPatients = $totalResult->fetch_assoc()['total'];

// Today's visits
$todayResult = $conn->query("
    SELECT COUNT(*) as today 
    FROM patient_visits 
    WHERE visit_date = CURDATE()
");
$todayVisits = $todayResult->fetch_assoc()['today'];

// Recent patients — last 10 with latest diagnosis
$recentResult = $conn->query("
    SELECT 
        p.id,
        p.name,
        p.created_at,
        -- ✅ Latest visit diagnosis, fallback to registration diagnosis
        COALESCE(
            (SELECT diagnosis FROM patient_visits 
             WHERE patient_id = p.id 
             AND diagnosis IS NOT NULL 
             AND diagnosis != ''
             ORDER BY visit_date DESC, id DESC 
             LIMIT 1),
            pm.diagnosis
        ) AS latest_diagnosis,
        -- ✅ Latest visit symptoms, fallback to registration symptoms
        COALESCE(
            (SELECT symptoms FROM patient_visits 
             WHERE patient_id = p.id 
             AND symptoms IS NOT NULL 
             AND symptoms != ''
             ORDER BY visit_date DESC, id DESC 
             LIMIT 1),
            pm.symptoms
        ) AS latest_symptoms,
        -- ✅ Latest visit date
        (SELECT visit_date FROM patient_visits 
         WHERE patient_id = p.id 
         ORDER BY visit_date DESC, id DESC 
         LIMIT 1) AS last_visit_date
    FROM patients p
    LEFT JOIN patient_medical pm ON p.id = pm.patient_id
    ORDER BY p.created_at DESC
    LIMIT 10
");

$recentPatients = [];
while ($row = $recentResult->fetch_assoc()) {

    // Time ago
    $diff = time() - strtotime($row['created_at']);
    if ($diff < 60)        $timeAgo = "just now";
    elseif ($diff < 3600)  $timeAgo = floor($diff / 60) . "m ago";
    elseif ($diff < 86400) $timeAgo = floor($diff / 3600) . "h ago";
    else                   $timeAgo = floor($diff / 86400) . "d ago";

    // Initials
    $nameParts = explode(" ", trim($row['name']));
    $initials  = strtoupper(substr($nameParts[0], 0, 1));
    if (count($nameParts) > 1)
        $initials .= strtoupper(substr(end($nameParts), 0, 1));

    // Last visit date label
    $lastVisit = $row['last_visit_date']
        ? date("d M Y", strtotime($row['last_visit_date']))
        : "No visits yet";

    $recentPatients[] = [
        "id"             => $row['id'],
        "name"           => $row['name'],
        "initials"       => $initials,
        "diagnosis"      => $row['latest_diagnosis'] ?? "No diagnosis",
        "symptoms"       => $row['latest_symptoms']  ?? "No symptoms",
        "last_visit"     => $lastVisit,
        "time_ago"       => $timeAgo
    ];
}

echo json_encode([
    "status"          => "success",
    "total_patients"  => $totalPatients,
    "today_visits"    => $todayVisits,
    "recent_patients" => $recentPatients
]);
?>