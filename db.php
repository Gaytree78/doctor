<?php
$host = "135.125.65.213";
$user = "doctor";
$pass = "Rahulecs@123";
$db = "llqzqipe_MedicalDoctor";

$conn = new mysqli($host, $user, $pass, $db);

if ($conn->connect_error) {
    die("Connection Failed: " . $conn->connect_error);
}
?>