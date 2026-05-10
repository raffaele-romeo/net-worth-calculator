variable "project_id" {
  type        = string
  description = "The GCP project id"
}

variable "region" {
  type        = string
  description = "The GCP region"
  default = "europe-west2"
}

variable "state_bucket_name" {
  type        = string
  description = "Name of the bucket holding remote Terraform state"
}

variable "fe_bucket_name" {
  type        = string
  description = "Name of the bucket serving the frontend static files"
}