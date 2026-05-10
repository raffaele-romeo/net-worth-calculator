
output "bucket_url" {
  value = google_storage_bucket.fe_static_files.url
  description = "URL of the frontend static files bucket"
}

output "artifact_repo_url" {
  value =  "${var.region}-docker.pkg.dev/${var.project_id}/${google_artifact_registry_repository.nwc_backend_repo.repository_id}"
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