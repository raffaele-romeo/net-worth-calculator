terraform {
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 7.0.0"
    }
  }
}

resource "google_artifact_registry_repository" "nwc_backend_repo" {
  location      = var.region
  repository_id = "nwc-backend"
  description   = "My GCP artifact repo"
  format        = "DOCKER"
}

resource "google_service_account" "nwc_service_account" {
  account_id   = "nwc-backend-sa"
  display_name = "Net Worth Calculator backend (Cloud Run)"
}

resource "google_secret_manager_secret_iam_member" "db_password" {
  secret_id = var.db_password_secret_id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.nwc_service_account.email}"
}

resource "google_secret_manager_secret_iam_member" "jwt_signing_key" {
  secret_id = var.jwt_secret_id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.nwc_service_account.email}"
}

resource "google_project_iam_member" "cloudsql_client" {
  project = var.project_id
  role    = "roles/cloudsql.client"
  member  = "serviceAccount:${google_service_account.nwc_service_account.email}"
}

resource "google_cloud_run_v2_service" "nwc_backend" {
  name     = "nwc-backend"
  location = var.region

  ingress = "INGRESS_TRAFFIC_INTERNAL_LOAD_BALANCER"

  template {
    service_account = google_service_account.nwc_service_account.email

    vpc_access {
      connector = var.connector_id
      egress    = "PRIVATE_RANGES_ONLY"
    }

    containers {
      image = var.backend_image

      env {
        name  = "DB_HOST"
        value = var.db_host
      }
      env {
        name  = "DB_USER"
        value = var.db_user
      }
      env {
        name  = "DB_NAME"
        value = var.db_name
      }
      env {
        name  = "REDIS_URI"
        value = "redis://${var.redis_host}:${var.redis_port}"
      }

      env {
        name = "DB_PASSWORD"
        value_source {
          secret_key_ref {
            secret  = var.db_password_secret_name
            version = "latest"
          }
        }
      }
    }
  }

  depends_on = [
    google_secret_manager_secret_iam_member.db_password,
  ]
}

resource "google_cloud_run_v2_service_iam_member" "api_public_invoker" {
  name     = google_cloud_run_v2_service.nwc_backend.name
  location = var.region
  role     = "roles/run.invoker"
  member   = "allUsers"
}
