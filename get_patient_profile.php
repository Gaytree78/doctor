<?php
include "db.php";

$patient_id = $_GET['id'];

$sql = "SELECT 
    p.name, p.age, p.gender, p.blood_group,
    c.mobile, c.address, c.emergency_contact_name, c.emergency_contact_phone,
    m.symptoms, m.diagnosis, m.allergies
FROM patients p
LEFT JOIN patient_contact c ON p.id = c.patient_id
LEFT JOIN patient_medical m ON p.id = m.patient_id
WHERE p.id = '$patient_id'";

$result = $conn->query($sql);
$row = $result->fetch_assoc();

echo json_encode($row);
?>