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

module "network" {
  source = "./modules/network"
  region = var.region

  depends_on = [google_project_service.apis]
}

module "data" {
  source     = "./modules/data"
  region     = var.region
  network_id = module.network.network_id

  depends_on = [google_project_service.apis, module.network]
}

module "runtime" {
  source        = "./modules/runtime"
  project_id    = var.project_id
  region        = var.region
  backend_image = var.backend_image
  connector_id  = module.network.connector_id

  db_host    = module.data.sql_private_ip
  db_user    = module.data.sql_user_name
  db_name    = module.data.sql_database_name
  redis_host = module.data.redis_host
  redis_port = module.data.redis_port

  db_password_secret_name = module.data.db_password_secret_name
  db_password_secret_id   = module.data.db_password_secret_id
  jwt_secret_id           = module.data.jwt_secret_id

  depends_on = [google_project_service.apis, module.data]
}

module "cdn" {
  source                 = "./modules/cdn"
  region                 = var.region
  fe_bucket_name         = var.fe_bucket_name
  cloud_run_service_name = module.runtime.service_name

  depends_on = [google_project_service.apis]
}
