output "frontend_public_url" {
  value       = module.cdn.frontend_public_url
  description = "Public URL serving the frontend and /v1/* API"
}

output "fe_bucket_url" {
  value       = module.cdn.fe_bucket_url
  description = "gs:// URL of the frontend static files bucket"
}

output "artifact_repo_url" {
  value       = module.runtime.artifact_repo_url
  description = "Fully-qualified Artifact Registry repo path for the backend image"
}

output "backend_cloud_run_uri" {
  value       = module.runtime.service_uri
  description = "Cloud Run service URI (reachable only via a load balancer)"
}

output "cloud_run_sa_email" {
  value       = module.runtime.service_account_email
  description = "Email of the Cloud Run service account"
}

output "vpc_connector_id" {
  value       = module.network.connector_id
  description = "VPC Connector id for Cloud Run egress"
}

output "sql_private_ip" {
  value       = module.data.sql_private_ip
  description = "Private IP of the Cloud SQL instance"
}

output "sql_database_name" {
  value       = module.data.sql_database_name
  description = "Database name"
}

output "sql_user_name" {
  value       = module.data.sql_user_name
  description = "Database user name"
}

output "redis_host" {
  value       = module.data.redis_host
  description = "Private IP of the Memorystore Redis instance"
}

output "redis_port" {
  value       = module.data.redis_port
  description = "Port of the Memorystore Redis instance"
}

output "db_password_secret_id" {
  value       = module.data.db_password_secret_name
  description = "Secret Manager secret id for the DB password"
}

output "jwt_secret_id" {
  value       = module.data.jwt_secret_name
  description = "Secret Manager secret id for the JWT signing key"
}
