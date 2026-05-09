<?php
include "db.php";

$patient_id = $_POST['patient_id'];
$title = $_POST['title'];
$desc = $_POST['description'];
$date = $_POST['date'];

$sql = "INSERT INTO clinical_notes 
(patient_id, note_title, note_description, note_date)
VALUES ('$patient_id','$title','$desc','$date')";

if ($conn->query($sql)) {
    echo json_encode(["status" => "success"]);
} else {
    echo json_encode(["status" => "error"]);
}
?>