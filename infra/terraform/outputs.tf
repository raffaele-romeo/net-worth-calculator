output "bucket_url" {
  value       = google_storage_bucket.fe_static_files.url
  description = "URL of the frontend static files bucket"
}

output "artifact_repo_url" {
  value       = "${var.region}-docker.pkg.dev/${var.project_id}/${google_artifact_registry_repository.nwc_backend_repo.repository_id}"
  description = "Fully-qualified Artifact Registry repo path for the backend image"
}

output "redis_host" {
  value       = google_redis_instance.nwc_redis.host
  description = "Private IP of the Memorystore Redis instance"
}

output "redis_port" {
  value       = google_redis_instance.nwc_redis.port
  description = "Port of the Memorystore Redis instance"
}

output "sql_private_ip" {
  value       = google_sql_database_instance.nwc_sql_db.private_ip_address
  description = "Private IP of the Cloud SQL instance"
}

output "cloud_run_sa_email" {
  value       = google_service_account.nwc_service_account.email
  description = "Email of the Cloud Run service account (for IAM bindings, audit lookups)"
}

output "db_password_secret_id" {
  value       = google_secret_manager_secret.nwc_db_pass_secret_manager.secret_id
  description = "Secret Manager secret_id for the DB password (referenced in Cloud Run env)"
}

output "jwt_secret_id" {
  value       = google_secret_manager_secret.nwc_jwt_signing_secret_manager.secret_id
  description = "Secret Manager secret_id for the JWT signing key"
}

output "vpc_connector_id" {
  value       = google_vpc_access_connector.nwc_connector.id
  description = "VPC Connector ID for Cloud Run vpc_access config"
}

output "sql_database_name" {
  value       = google_sql_database.database.name
  description = "Database name to use in JDBC URL"
}

output "sql_user_name" {
  value       = google_sql_user.users.name
  description = "Database user name"
}
