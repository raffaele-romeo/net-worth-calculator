
terraform {
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 7.0.0"
    }
  }
}

provider "google" {
  project     = var.project_id
  region      = var.region
}


resource "google_storage_bucket" "tf_state" {
  name          = var.state_bucket_name
  location      = "EU"
  uniform_bucket_level_access = true
  public_access_prevention = "enforced"
  versioning {
    enabled = true
  }
  lifecycle {
    prevent_destroy = true
  }
  labels = {
    owned-by = "raf"
  }
}

resource "google_storage_bucket" "fe_static_files" {
  name          = var.fe_bucket_name
  location      = "EU"
  uniform_bucket_level_access = true
  lifecycle {
    prevent_destroy = true
  }
  labels = {
    owned-by = "raf"
  }
}

resource "google_project_service" "apis" {
  for_each = toset([
    "run.googleapis.com",
    "sqladmin.googleapis.com",
    "redis.googleapis.com",
    "artifactregistry.googleapis.com",
    "compute.googleapis.com",
    "vpcaccess.googleapis.com",
    "servicenetworking.googleapis.com",
    "secretmanager.googleapis.com",
    "iam.googleapis.com",
    "iamcredentials.googleapis.com",
    "cloudresourcemanager.googleapis.com",
    "pubsub.googleapis.com",
  ])
  service            = each.value
  disable_on_destroy = false
}

resource "google_artifact_registry_repository" "nwc_backend_repo" {
  location      = var.region
  repository_id = "nwc-backend"
  description   = "My GCP artifact repo"
  format        = "DOCKER"
  depends_on = [google_project_service.apis]
}