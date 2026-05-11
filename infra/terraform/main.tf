terraform {
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 7.0.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "3.6.3"
    }
  }
}

provider "google" {
  project = var.project_id
  region  = var.region
}


resource "google_storage_bucket" "tf_state" {
  name                        = var.state_bucket_name
  location                    = "EU"
  uniform_bucket_level_access = true
  public_access_prevention    = "enforced"
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
  name                        = var.fe_bucket_name
  location                    = "EU"
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


resource "google_compute_network" "nwc_vpc_network" {
  name                    = "nwc-vpc"
  auto_create_subnetworks = false
}

locals {
  subnets = {
    public = { cidr = "10.10.0.0/24", private_google_access = false }
    private = { cidr = "10.10.1.0/24", private_google_access = true }
  }
}

resource "google_compute_subnetwork" "subnets" {
  for_each                 = local.subnets
  name                     = "nwc-${each.key}"
  ip_cidr_range            = each.value.cidr
  region                   = var.region
  network                  = google_compute_network.nwc_vpc_network.id
  private_ip_google_access = each.value.private_google_access
}

resource "google_compute_router" "nwc_router" {
  name    = "nwc-router"
  network = google_compute_network.nwc_vpc_network.id
  region  = var.region
}

resource "google_compute_router_nat" "nwc_nat" {
  name                               = "nwc-nat"
  router                             = google_compute_router.nwc_router.name
  source_subnetwork_ip_ranges_to_nat = "LIST_OF_SUBNETWORKS"
  region                             = var.region
  nat_ip_allocate_option             = "AUTO_ONLY"

  subnetwork {
    name = google_compute_subnetwork.subnets["private"].id
    source_ip_ranges_to_nat = ["ALL_IP_RANGES"]
  }
}

resource "google_vpc_access_connector" "nwc_connector" {
  name          = "nwc-connector"
  ip_cidr_range = "10.10.2.0/28"
  network       = google_compute_network.nwc_vpc_network.name
  region        = var.region
  machine_type  = "e2-micro"
  min_instances = 2
  max_instances = 3
}

resource "google_compute_global_address" "private_ip_alloc" {
  name          = "private-ip-alloc"
  purpose       = "VPC_PEERING"
  address_type  = "INTERNAL"
  prefix_length = 16
  network       = google_compute_network.nwc_vpc_network.id
}

resource "google_service_networking_connection" "private_vpc_peering" {
  network = google_compute_network.nwc_vpc_network.id
  service = "servicenetworking.googleapis.com"
  reserved_peering_ranges = [google_compute_global_address.private_ip_alloc.name]
}

resource "google_sql_database" "database" {
  name     = "nwc-db"
  instance = google_sql_database_instance.nwc_sql_db.name
}

resource "google_sql_database_instance" "nwc_sql_db" {
  name             = "main-instance"
  database_version = "POSTGRES_15"
  region           = var.region

  deletion_protection = false

  settings {
    tier = "db-f1-micro"
    ip_configuration {
      ipv4_enabled    = false
      private_network = google_compute_network.nwc_vpc_network.id
    }
  }
  depends_on = [google_service_networking_connection.private_vpc_peering]
}

resource "random_password" "db_password" {
  length  = 32
  special = false
}

resource "google_sql_user" "users" {
  name     = "postgres"
  instance = google_sql_database_instance.nwc_sql_db.name
  password = random_password.db_password.result
}

resource "google_redis_instance" "nwc_redis" {
  name           = "nwc-redis"
  tier           = "BASIC"
  memory_size_gb = 1
  region         = var.region

  authorized_network = google_compute_network.nwc_vpc_network.id
  connect_mode       = "PRIVATE_SERVICE_ACCESS"
  redis_version      = "REDIS_7_0"

  depends_on = [google_service_networking_connection.private_vpc_peering]
}

resource "google_secret_manager_secret" "nwc_db_pass_secret_manager" {
  secret_id = "nwc-db-password"

  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_version" "nwc_db_pass_secret_manager_version" {
  secret = google_secret_manager_secret.nwc_db_pass_secret_manager.id

  secret_data = random_password.db_password.result
}

resource "google_secret_manager_secret" "nwc_jwt_signing_secret_manager" {
  secret_id = "nwc-jwt-signing-key"

  replication {
    auto {}
  }
}

resource "random_password" "jwt_signing_key" {
  length  = 64
  special = false
}

resource "google_secret_manager_secret_version" "nwc_jwt_signing_secret_manager_version" {
  secret = google_secret_manager_secret.nwc_jwt_signing_secret_manager.id

  secret_data = random_password.jwt_signing_key.result
}

resource "google_service_account" "nwc_service_account" {
  account_id   = "nwc-backend-sa"
  display_name = "Net Worth Calculator backend (Cloud Run)"
}

resource "google_secret_manager_secret_iam_member" "nwc_db_password_secret_manager_iam_member" {
  secret_id = google_secret_manager_secret.nwc_db_pass_secret_manager.id
  role      = "roles/secretmanager.secretAccessor"
  # Grant the new deployed service account access to this secret.
  member     = "serviceAccount:${google_service_account.nwc_service_account.email}"
}

resource "google_secret_manager_secret_iam_member" "nwc_jwt_signing_secret_manager_iam_member" {
  secret_id = google_secret_manager_secret.nwc_jwt_signing_secret_manager.id
  role      = "roles/secretmanager.secretAccessor"
  # Grant the new deployed service account access to this secret.
  member     = "serviceAccount:${google_service_account.nwc_service_account.email}"
}
