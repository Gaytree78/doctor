<?php
include "db.php";

$patient_id = $_GET['id'];

$result = $conn->query("SELECT * FROM clinical_notes 
                        WHERE patient_id = '$patient_id'
                        ORDER BY note_date DESC");

$data = [];

while ($row = $result->fetch_assoc()) {
    $data[] = $row;
}

echo json_encode($data);
?>