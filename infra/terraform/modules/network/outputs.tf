output "network_id" {
  value       = google_compute_network.nwc_vpc_network.id
  description = "VPC network id (used as private_network / authorized_network)"
}

output "connector_id" {
  value       = google_vpc_access_connector.nwc_connector.id
  description = "VPC Access Connector id for Cloud Run egress"
}
