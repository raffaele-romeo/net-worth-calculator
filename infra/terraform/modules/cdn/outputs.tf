output "public_ip" {
  value       = google_compute_global_address.fe_public_ip.address
  description = "Public IP fronting the frontend + API"
}

output "frontend_public_url" {
  value       = "http://${google_compute_global_address.fe_public_ip.address}"
  description = "Public URL serving the frontend and /v1/* API"
}

output "fe_bucket_url" {
  value       = google_storage_bucket.fe_static_files.url
  description = "gs:// URL of the frontend static files bucket"
}
