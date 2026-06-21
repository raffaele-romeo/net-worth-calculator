variable "region" {
  type        = string
  description = "The GCP region for the data layer"
}

variable "network_id" {
  type        = string
  description = "VPC network id for private SQL/Redis connectivity"
}
