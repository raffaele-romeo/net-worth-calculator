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

resource "google_sql_database_instance" "nwc_sql_db" {
  name             = "main-instance"
  database_version = "POSTGRES_15"
  region           = var.region

  deletion_protection = false

  settings {
    tier = "db-f1-micro"
    ip_configuration {
      ipv4_enabled    = false
      private_network = var.network_id
    }
  }
}

resource "google_sql_database" "database" {
  name     = "nwc-db"
  instance = google_sql_database_instance.nwc_sql_db.name
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

  authorized_network = var.network_id
  connect_mode       = "PRIVATE_SERVICE_ACCESS"
  redis_version      = "REDIS_7_0"
}

resource "google_secret_manager_secret" "db_password" {
  secret_id = "nwc-db-password"

  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_version" "db_password" {
  secret      = google_secret_manager_secret.db_password.id
  secret_data = random_password.db_password.result
}

resource "google_secret_manager_secret" "jwt_signing_key" {
  secret_id = "nwc-jwt-signing-key"

  replication {
    auto {}
  }
}

resource "random_password" "jwt_signing_key" {
  length  = 64
  special = false
}

resource "google_secret_manager_secret_version" "jwt_signing_key" {
  secret      = google_secret_manager_secret.jwt_signing_key.id
  secret_data = random_password.jwt_signing_key.result
}
