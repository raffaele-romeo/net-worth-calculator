variable "region" {
  type        = string
  description = "The GCP region for the serverless NEG"
}

variable "fe_bucket_name" {
  type        = string
  description = "Name of the bucket serving the frontend static files"
}

variable "cloud_run_service_name" {
  type        = string
  description = "Cloud Run service name the API path matcher routes to"
}
