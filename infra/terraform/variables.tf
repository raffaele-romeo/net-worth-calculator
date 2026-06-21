variable "project_id" {
  type        = string
  description = "The GCP project id"
}

variable "region" {
  type        = string
  description = "The GCP region"
  default     = "europe-west2"
}

variable "state_bucket_name" {
  type        = string
  description = "Name of the bucket holding remote Terraform state"
}

variable "fe_bucket_name" {
  type        = string
  description = "Name of the bucket serving the frontend static files"
}

variable "backend_image" {
  type        = string
  description = "Fully-qualified backend container image tag deployed to Cloud Run"
  default     = "europe-west2-docker.pkg.dev/project-1a8f85f3-160a-4515-bb4/nwc-backend/backend:v1"
}