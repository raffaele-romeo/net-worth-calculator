terraform {
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 7.0.0"
    }
  }
}

resource "google_compute_network" "nwc_vpc_network" {
  name                    = "nwc-vpc"
  auto_create_subnetworks = false
}

locals {
  subnets = {
    public  = { cidr = "10.10.0.0/24", private_google_access = false }
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
    name                    = google_compute_subnetwork.subnets["private"].id
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
  network                 = google_compute_network.nwc_vpc_network.id
  service                 = "servicenetworking.googleapis.com"
  reserved_peering_ranges = [google_compute_global_address.private_ip_alloc.name]
}
