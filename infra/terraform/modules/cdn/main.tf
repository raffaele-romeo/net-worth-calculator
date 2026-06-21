terraform {
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 7.0.0"
    }
  }
}

resource "google_storage_bucket" "fe_static_files" {
  name                        = var.fe_bucket_name
  location                    = "EU"
  uniform_bucket_level_access = true

  website {
    main_page_suffix = "index.html"
    not_found_page   = "index.html"
  }

  lifecycle {
    prevent_destroy = true
  }
  labels = {
    owned-by = "raf"
  }
}

resource "google_storage_bucket_iam_member" "fe_public_read" {
  bucket = google_storage_bucket.fe_static_files.name
  role   = "roles/storage.objectViewer"
  member = "allUsers"
}

resource "google_compute_backend_bucket" "fe_backend" {
  name        = "nwc-fe-backend"
  bucket_name = google_storage_bucket.fe_static_files.name
  enable_cdn  = true
}

resource "google_compute_global_address" "fe_public_ip" {
  name = "nwc-fe-public-ip"
}

resource "google_compute_region_network_endpoint_group" "nwc_backend_neg" {
  name                  = "nwc-backend-neg"
  region                = var.region
  network_endpoint_type = "SERVERLESS"

  cloud_run {
    service = var.cloud_run_service_name
  }
}

resource "google_compute_backend_service" "api_backend" {
  name                  = "nwc-api-backend"
  load_balancing_scheme = "EXTERNAL_MANAGED"
  protocol              = "HTTPS"

  backend {
    group = google_compute_region_network_endpoint_group.nwc_backend_neg.id
  }
}

resource "google_compute_url_map" "fe_urlmap" {
  name            = "nwc-fe-urlmap"
  default_service = google_compute_backend_bucket.fe_backend.id

  host_rule {
    hosts        = ["*"]
    path_matcher = "main"
  }

  path_matcher {
    name            = "main"
    default_service = google_compute_backend_bucket.fe_backend.id

    path_rule {
      paths   = ["/v1", "/v1/*"]
      service = google_compute_backend_service.api_backend.id
    }
  }
}

resource "google_compute_target_http_proxy" "fe_proxy" {
  name    = "nwc-fe-proxy"
  url_map = google_compute_url_map.fe_urlmap.id
}

resource "google_compute_global_forwarding_rule" "fe_fr" {
  name                  = "nwc-fe-fr"
  load_balancing_scheme = "EXTERNAL_MANAGED"
  target                = google_compute_target_http_proxy.fe_proxy.id
  ip_address            = google_compute_global_address.fe_public_ip.id
  port_range            = "80"
}
