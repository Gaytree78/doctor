<?php
ob_start();
error_reporting(0);
ini_set('display_errors', 0);
header('Content-Type: application/json');

include "db.php";

$patient_id = $_GET['patient_id'] ?? '';

if (empty($patient_id)) {
    ob_end_clean();
    echo json_encode(["status" => "error", "message" => "No patient ID"]);
    exit;
}

// Get patient data
$sql = $conn->prepare("SELECT * FROM patients WHERE id = ?");
$sql->bind_param("i", $patient_id);
$sql->execute();
$patient = $sql->get_result()->fetch_assoc();

if (!$patient) {
    ob_end_clean();
    echo json_encode(["status" => "error", "message" => "Patient not found"]);
    exit;
}

// Get all visits
$visitSql = $conn->prepare("
    SELECT * FROM patient_visits
    WHERE patient_id = ?
    ORDER BY visit_date DESC
");
$visitSql->bind_param("i", $patient_id);
$visitSql->execute();
$visitResult = $visitSql->get_result();
$visits = [];
while ($v = $visitResult->fetch_assoc()) {
    $visits[] = $v;
}

$baseUrl = "http://tsm.ecssofttech.com/Gaytree/hospital/";

ob_end_clean();
echo json_encode([
    "status"           => "success",
    "patient"          => $patient,
    "visits"           => $visits,
    "blood_report_url" => !empty($patient['blood_report'])
        ? $baseUrl . $patient['blood_report'] : null,
    "urine_report_url" => !empty($patient['urine_report'])
        ? $baseUrl . $patient['urine_report'] : null,
    "general_file_url" => !empty($patient['general_file'])
        ? $baseUrl . $patient['general_file'] : null,
]);
?>
