<?php
ob_start();
error_reporting(0);
ini_set('display_errors', 0);
header('Content-Type: application/json');
include "db.php";

$uploadDir = __DIR__ . "/uploads/reports/";
if (!is_dir($uploadDir)) mkdir($uploadDir, 0755, true);

function saveUploadedFile($key, $uploadDir) {
    if (!isset($_FILES[$key]) || $_FILES[$key]['error'] != UPLOAD_ERR_OK) return null;
    $file    = $_FILES[$key];
    $allowed = ["image/jpeg", "image/png", "application/pdf"];
    $finfo   = new finfo(FILEINFO_MIME_TYPE);
    $mime    = $finfo->file($file['tmp_name']);
    if (!in_array($mime, $allowed)) return null;
    $ext     = pathinfo($file['name'], PATHINFO_EXTENSION);
    $newName = uniqid() . "." . $ext;
    $dest    = $uploadDir . $newName;
    if (move_uploaded_file($file['tmp_name'], $dest)) return "uploads/reports/" . $newName;
    return null;
}

$patient_id = $_POST['patient_id'] ?? '';
$symptoms   = trim($_POST['symptoms']  ?? '');
$diagnosis  = trim($_POST['diagnosis'] ?? '');
$notes      = trim($_POST['notes']     ?? '');
$fee        = (int)($_POST['fee']      ?? 0);

if (empty($patient_id)) {
    ob_end_clean();
    echo json_encode(["status" => "error", "message" => "Patient ID missing"]);
    exit;
}

// If fee not sent, fetch from patients table
if ($fee <= 0) {
    $feeRow = $conn->prepare("SELECT fee FROM patients WHERE id = ?");
    $feeRow->bind_param("i", $patient_id);
    $feeRow->execute();
    $feeRow->bind_result($fee);
    $feeRow->fetch();
    $feeRow->close();
    if ($fee <= 0) $fee = 100;
}

// Save uploaded files
$bloodPath = saveUploadedFile("blood_report", $uploadDir);
$urinePath = saveUploadedFile("urine_report", $uploadDir);

// Insert visit row — includes fee, blood_report, urine_report
$sql = $conn->prepare("
    INSERT INTO patient_visits
        (patient_id, symptoms, diagnosis, notes, fee,
         blood_report, urine_report, visit_date, created_at)
    VALUES (?, ?, ?, ?, ?, ?, ?, CURDATE(), NOW())
");
if (!$sql) {
    ob_end_clean();
    echo json_encode(["status" => "error", "message" => "Prepare failed: " . $conn->error]);
    exit;
}

$sql->bind_param("isssiss",
    $patient_id, $symptoms, $diagnosis, $notes,
    $fee, $bloodPath, $urinePath
);

if (!$sql->execute()) {
    ob_end_clean();
    echo json_encode(["status" => "error", "message" => "Insert failed: " . $sql->error]);
    exit;
}

// ── Always update patients table with the latest report ──────────
// This keeps the REPORTS section in the profile always up to date.
// Only overwrite if a new file was actually uploaded for that type.
if ($bloodPath && $urinePath) {
    $upd = $conn->prepare(
        "UPDATE patients SET blood_report=?, urine_report=? WHERE id=?"
    );
    $upd->bind_param("ssi", $bloodPath, $urinePath, $patient_id);
    $upd->execute();
} elseif ($bloodPath) {
    $upd = $conn->prepare(
        "UPDATE patients SET blood_report=? WHERE id=?"
    );
    $upd->bind_param("si", $bloodPath, $patient_id);
    $upd->execute();
} elseif ($urinePath) {
    $upd = $conn->prepare(
        "UPDATE patients SET urine_report=? WHERE id=?"
    );
    $upd->bind_param("si", $urinePath, $patient_id);
    $upd->execute();
}
// ─────────────────────────────────────────────────────────────────

ob_end_clean();
echo json_encode([
    "status"       => "success",
    "message"      => "Visit saved successfully",
    "fee_recorded" => $fee
]);
?>