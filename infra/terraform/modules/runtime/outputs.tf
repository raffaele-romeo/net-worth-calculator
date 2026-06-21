output "service_name" {
  value       = google_cloud_run_v2_service.nwc_backend.name
  description = "Cloud Run service name (consumed by the serverless NEG)"
}

output "service_uri" {
  value       = google_cloud_run_v2_service.nwc_backend.uri
  description = "Cloud Run service URI (reachable only via a load balancer)"
}

output "service_account_email" {
  value       = google_service_account.nwc_service_account.email
  description = "Email of the Cloud Run service account"
}

output "artifact_repo_url" {
  value       = "${var.region}-docker.pkg.dev/${var.project_id}/${google_artifact_registry_repository.nwc_backend_repo.repository_id}"
  description = "Fully-qualified Artifact Registry repo path for the backend image"
}
