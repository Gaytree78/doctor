<?php
include "db.php";

header('Content-Type: application/json');

$search = $_GET['query'] ?? '';

if (empty($search)) {
    echo json_encode(["status" => "success", "patients" => []]);
    exit;
}

// ✅ Use prepared statement to prevent SQL injection
$like = "%" . $search . "%";

$sql = $conn->prepare("
    SELECT p.id, p.name, p.age, p.gender,
           p.mobile,
           p.diagnosis
    FROM patients p
    WHERE p.name LIKE ?
    OR p.mobile LIKE ?
    ORDER BY p.id DESC
");

$sql->bind_param("ss", $like, $like);
$sql->execute();
$result = $sql->get_result();

$patients = [];
while ($row = $result->fetch_assoc()) {
    $patients[] = $row;
}

echo json_encode([
    "status"   => "success",
    "patients" => $patients
]);
?>
