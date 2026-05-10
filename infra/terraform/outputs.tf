
output "bucket_url" {
  value = google_storage_bucket.fe_static_files.url
  description = "URL of the frontend static files bucket"
}

output "artifact_repo_url" {
  value =  "${var.region}-docker.pkg.dev/${var.project_id}/${google_artifact_registry_repository.nwc_backend_repo.repository_id}"
}