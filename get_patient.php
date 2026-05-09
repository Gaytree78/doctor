<?php
ob_start();
error_reporting(0);
ini_set('display_errors', 0);
header('Content-Type: application/json');
include "db.php";

$patient_id = $_GET['patient_id'] ?? '';
if (empty($patient_id)) {
    ob_end_clean();
    echo json_encode(["status" => "error", "message" => "No patient ID provided"]);
    exit;
}

$sql = $conn->prepare("SELECT * FROM patients WHERE id = ?");
if (!$sql) {
    ob_end_clean();
    echo json_encode(["status" => "error", "message" => "Prepare failed: " . $conn->error]);
    exit;
}
$sql->bind_param("i", $patient_id);
$sql->execute();
$row = $sql->get_result()->fetch_assoc();

if (!$row) {
    ob_end_clean();
    echo json_encode(["status" => "error", "message" => "Patient not found"]);
    exit;
}

$baseUrl = "http://tsm.ecssofttech.com/Gaytree/hospital/";

// ── Latest blood/urine from patient_visits (most recent upload) ──
// This ensures the REPORTS section always shows the latest uploaded
// report even if it came from a visit, not from add_patient
$latestReports = null;
$reportSql = $conn->prepare("
    SELECT blood_report, urine_report
    FROM patient_visits
    WHERE patient_id = ?
      AND (blood_report IS NOT NULL OR urine_report IS NOT NULL)
    ORDER BY visit_date DESC, id DESC
    LIMIT 1
");
if ($reportSql) {
    $reportSql->bind_param("i", $patient_id);
    $reportSql->execute();
    $latestReports = $reportSql->get_result()->fetch_assoc();
}

// Use latest visit report if exists, otherwise fall back to patient-level
$bloodReport = (!empty($latestReports['blood_report']))
    ? $latestReports['blood_report']
    : ($row['blood_report'] ?? '');

$urineReport = (!empty($latestReports['urine_report']))
    ? $latestReports['urine_report']
    : ($row['urine_report'] ?? '');

// ── Latest visit symptoms/diagnosis ──────────────────────────────
$latestVisit = null;
$latestSql = $conn->prepare("
    SELECT symptoms, diagnosis FROM patient_visits
    WHERE patient_id = ?
    ORDER BY visit_date DESC, id DESC LIMIT 1
");
if ($latestSql) {
    $latestSql->bind_param("i", $patient_id);
    $latestSql->execute();
    $latestVisit = $latestSql->get_result()->fetch_assoc();
}

$currentSymptoms  = ($latestVisit && !empty($latestVisit['symptoms']))
    ? $latestVisit['symptoms']  : ($row['symptoms']  ?? 'N/A');
$currentDiagnosis = ($latestVisit && !empty($latestVisit['diagnosis']))
    ? $latestVisit['diagnosis'] : ($row['diagnosis'] ?? 'N/A');

// ── Visit history with per-visit report URLs ──────────────────────
$visits = [];
$visitSql = $conn->prepare("
    SELECT id, symptoms, diagnosis, visit_date, notes, fee,
           blood_report, urine_report
    FROM patient_visits
    WHERE patient_id = ?
    ORDER BY visit_date DESC, id DESC
");
if ($visitSql) {
    $visitSql->bind_param("i", $patient_id);
    $visitSql->execute();
    $visitResult = $visitSql->get_result();

    while ($v = $visitResult->fetch_assoc()) {
        $visits[] = [
            "visit_id"         => $v['id'],
            "symptoms"         => $v['symptoms']  ?? "N/A",
            "diagnosis"        => $v['diagnosis'] ?? "N/A",
            "notes"            => $v['notes']     ?? "",
            "fee"              => (int)($v['fee'] ?? 0),
            "visit_date"       => date("d M Y", strtotime($v['visit_date'])),
            // Per-visit report URLs — shown inside visit timeline cards
            "blood_report_url" => !empty($v['blood_report'])
                ? $baseUrl . $v['blood_report'] : "",
            "urine_report_url" => !empty($v['urine_report'])
                ? $baseUrl . $v['urine_report'] : ""
        ];
    }
}

ob_end_clean();
echo json_encode([
    "status"            => "success",
    "id"                => $row['id']               ?? "",
    "name"              => $row['name']              ?? "",
    "age"               => $row['age']               ?? "",
    "gender"            => $row['gender']            ?? "",
    "blood_group"       => $row['blood_group']       ?? "",
    "mobile"            => $row['mobile']            ?? "",
    "address"           => $row['address']           ?? "",
    "emergency_contact" => $row['emergency_contact'] ?? "",
    "allergies"         => $row['allergies']         ?? "",
    "notes"             => $row['notes']             ?? "",
    "abha_number"       => $row['abha_number']       ?? "",
    "fee"               => (int)($row['fee']         ?? 100),
    "symptoms"          => $currentSymptoms,
    "diagnosis"         => $currentDiagnosis,
    // ── REPORTS SECTION: always latest available ──────────────
    "blood_report_url"  => !empty($bloodReport)
        ? $baseUrl . $bloodReport : "",
    "urine_report_url"  => !empty($urineReport)
        ? $baseUrl . $urineReport : "",
    "general_file_url"  => !empty($row['general_file'])
        ? $baseUrl . $row['general_file'] : "",
    "visits"            => $visits
]);
?>
