<?php
ob_start();
error_reporting(0);
ini_set('display_errors', 0);
header("Content-Type: application/json");
header("Access-Control-Allow-Origin: *");

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

$name        = trim($_POST['name']        ?? '');
$age         = (int)($_POST['age']        ?? 0);
$gender      = trim($_POST['gender']      ?? '');
$blood_group = trim($_POST['blood_group'] ?? '');
$mobile      = trim($_POST['mobile']      ?? '');
$address     = trim($_POST['address']     ?? '');
$emergency   = trim($_POST['emergency']   ?? '');
$symptoms    = trim($_POST['symptoms']    ?? '');
$diagnosis   = trim($_POST['diagnosis']   ?? '');
$notes       = trim($_POST['notes']       ?? '');
$abha        = trim($_POST['abha_number'] ?? '');
$fee         = (int)($_POST['fee']        ?? 100);

if (empty($name) || empty($age) || empty($mobile)) {
    ob_end_clean();
    echo json_encode(["status" => "error", "message" => "Required fields missing"]);
    exit;
}

// Duplicate mobile check
$check = $conn->prepare("SELECT id FROM patients WHERE mobile = ?");
$check->bind_param("s", $mobile);
$check->execute();
$check->store_result();
if ($check->num_rows > 0) {
    ob_end_clean();
    echo json_encode(["status" => "duplicate", "message" => "Mobile already exists"]);
    exit;
}
$check->close();

$general = saveUploadedFile("general_file", $uploadDir);
$blood   = saveUploadedFile("blood_report", $uploadDir);
$urine   = saveUploadedFile("urine_report", $uploadDir);

$sql = "INSERT INTO patients
            (name, age, gender, blood_group, mobile,
             address, emergency_contact, symptoms, diagnosis,
             notes, abha_number, general_file, blood_report,
             urine_report, fee, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";

$stmt = $conn->prepare($sql);
if (!$stmt) {
    ob_end_clean();
    echo json_encode(["status" => "error", "message" => "Prepare failed: " . $conn->error]);
    exit;
}

// 15 placeholders: name(s) age(i) gender(s) blood(s) mobile(s)
//                  address(s) emergency(s) symptoms(s) diagnosis(s)
//                  notes(s) abha(s) general(s) blood(s) urine(s) fee(i)
$stmt->bind_param(
    "sissssssssssssi",
    $name, $age, $gender, $blood_group, $mobile,
    $address, $emergency, $symptoms, $diagnosis,
    $notes, $abha, $general, $blood, $urine, $fee
);

if (!$stmt->execute()) {
    ob_end_clean();
    echo json_encode(["status" => "error", "message" => "Execute failed: " . $stmt->error]);
    exit;
}

$patientId = $conn->insert_id;

// Auto-insert first visit row
$vstmt = $conn->prepare(
    "INSERT INTO patient_visits (patient_id, visit_date, fee) VALUES (?, CURDATE(), ?)"
);
$vstmt->bind_param("ii", $patientId, $fee);
$vstmt->execute();
$vstmt->close();

$displayId = "PAT-" . str_pad($patientId, 5, "0", STR_PAD_LEFT);

ob_end_clean();
echo json_encode([
    "status"             => "success",
    "patient_id"         => $patientId,
    "patient_id_display" => $displayId,
    "message"            => "Patient Added Successfully"
]);

$stmt->close();
$conn->close();
?>