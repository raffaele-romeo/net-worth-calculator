output "sql_private_ip" {
  value       = google_sql_database_instance.nwc_sql_db.private_ip_address
  description = "Private IP of the Cloud SQL instance"
}

output "sql_user_name" {
  value       = google_sql_user.users.name
  description = "Database user name"
}

output "sql_database_name" {
  value       = google_sql_database.database.name
  description = "Database name"
}

output "redis_host" {
  value       = google_redis_instance.nwc_redis.host
  description = "Private IP of the Memorystore Redis instance"
}

output "redis_port" {
  value       = google_redis_instance.nwc_redis.port
  description = "Port of the Memorystore Redis instance"
}

output "db_password_secret_name" {
  value       = google_secret_manager_secret.db_password.secret_id
  description = "Short secret id for the DB password (Cloud Run env secret_key_ref)"
}

output "db_password_secret_id" {
  value       = google_secret_manager_secret.db_password.id
  description = "Full resource id for the DB password secret (IAM binding)"
}

output "jwt_secret_name" {
  value       = google_secret_manager_secret.jwt_signing_key.secret_id
  description = "Short secret id for the JWT signing key"
}

output "jwt_secret_id" {
  value       = google_secret_manager_secret.jwt_signing_key.id
  description = "Full resource id for the JWT signing key secret (IAM binding)"
}
