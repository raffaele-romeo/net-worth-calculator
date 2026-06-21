variable "project_id" {
  type        = string
  description = "The GCP project id"
}

variable "region" {
  type        = string
  description = "The GCP region"
}

variable "backend_image" {
  type        = string
  description = "Fully-qualified backend container image tag"
}

variable "connector_id" {
  type        = string
  description = "VPC Access Connector id for Cloud Run egress"
}

variable "db_host" {
  type        = string
  description = "Cloud SQL private IP"
}

variable "db_user" {
  type        = string
  description = "Database user name"
}

variable "db_name" {
  type        = string
  description = "Database name"
}

variable "redis_host" {
  type        = string
  description = "Redis private IP"
}

variable "redis_port" {
  type        = number
  description = "Redis port"
}

variable "db_password_secret_name" {
  type        = string
  description = "Short secret id for the DB password (Cloud Run env secret_key_ref)"
}

variable "db_password_secret_id" {
  type        = string
  description = "Full resource id for the DB password secret (IAM binding)"
}

variable "jwt_secret_id" {
  type        = string
  description = "Full resource id for the JWT signing key secret (IAM binding)"
}
