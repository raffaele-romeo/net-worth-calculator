terraform {
  backend "gcs" {
    bucket = "project-1a8f85f3-160a-4515-bb4_tf-state"
    prefix = "nwc/state"
  }
}