moved {
  from = google_compute_network.nwc_vpc_network
  to   = module.network.google_compute_network.nwc_vpc_network
}

moved {
  from = google_compute_subnetwork.subnets["public"]
  to   = module.network.google_compute_subnetwork.subnets["public"]
}

moved {
  from = google_compute_subnetwork.subnets["private"]
  to   = module.network.google_compute_subnetwork.subnets["private"]
}

moved {
  from = google_compute_router.nwc_router
  to   = module.network.google_compute_router.nwc_router
}

moved {
  from = google_compute_router_nat.nwc_nat
  to   = module.network.google_compute_router_nat.nwc_nat
}

moved {
  from = google_vpc_access_connector.nwc_connector
  to   = module.network.google_vpc_access_connector.nwc_connector
}

moved {
  from = google_compute_global_address.private_ip_alloc
  to   = module.network.google_compute_global_address.private_ip_alloc
}

moved {
  from = google_service_networking_connection.private_vpc_peering
  to   = module.network.google_service_networking_connection.private_vpc_peering
}

moved {
  from = google_sql_database_instance.nwc_sql_db
  to   = module.data.google_sql_database_instance.nwc_sql_db
}

moved {
  from = google_sql_database.database
  to   = module.data.google_sql_database.database
}

moved {
  from = google_sql_user.users
  to   = module.data.google_sql_user.users
}

moved {
  from = random_password.db_password
  to   = module.data.random_password.db_password
}

moved {
  from = google_redis_instance.nwc_redis
  to   = module.data.google_redis_instance.nwc_redis
}

moved {
  from = google_secret_manager_secret.nwc_db_pass_secret_manager
  to   = module.data.google_secret_manager_secret.db_password
}

moved {
  from = google_secret_manager_secret_version.nwc_db_pass_secret_manager_version
  to   = module.data.google_secret_manager_secret_version.db_password
}

moved {
  from = google_secret_manager_secret.nwc_jwt_signing_secret_manager
  to   = module.data.google_secret_manager_secret.jwt_signing_key
}

moved {
  from = random_password.jwt_signing_key
  to   = module.data.random_password.jwt_signing_key
}

moved {
  from = google_secret_manager_secret_version.nwc_jwt_signing_secret_manager_version
  to   = module.data.google_secret_manager_secret_version.jwt_signing_key
}

moved {
  from = google_service_account.nwc_service_account
  to   = module.runtime.google_service_account.nwc_service_account
}

moved {
  from = google_artifact_registry_repository.nwc_backend_repo
  to   = module.runtime.google_artifact_registry_repository.nwc_backend_repo
}

moved {
  from = google_secret_manager_secret_iam_member.nwc_db_password_secret_manager_iam_member
  to   = module.runtime.google_secret_manager_secret_iam_member.db_password
}

moved {
  from = google_secret_manager_secret_iam_member.nwc_jwt_signing_secret_manager_iam_member
  to   = module.runtime.google_secret_manager_secret_iam_member.jwt_signing_key
}

moved {
  from = google_cloud_run_v2_service.nwc_cloud_run
  to   = module.runtime.google_cloud_run_v2_service.nwc_backend
}

moved {
  from = google_storage_bucket.fe_static_files
  to   = module.cdn.google_storage_bucket.fe_static_files
}
