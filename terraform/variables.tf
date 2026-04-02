variable "location" {
  type    = string
  default = "eastus"
}

variable "resource_group_name" {
  type    = string
  default = "rg-shortener"
}

variable "acr_name" {
  type        = string
  description = "ACR name (must be globally unique)"
}

variable "acr_repo" {
  type        = string
  description = "Repository/image name inside ACR (e.g. shortener)"
  default     = "shortener"
}

variable "image_tag" {
  type    = string
  default = "latest"
}

variable "app_service_plan_name" {
  type    = string
  default = "shortener-plan"
}

variable "webapp_name" {
  type        = string
  description = "Name of the App Service (must be unique)"
}

variable "app_service_sku" {
  type    = string
  default = "B1"
}

# Existing DB/Redis secrets (sensitive)
variable "db_url" {
  type      = string
  sensitive = true
}
variable "db_user" {
  type      = string
  sensitive = true
}
variable "db_password" {
  type      = string
  sensitive = true
}

variable "redis_host" {
  type = string
}
variable "redis_port" {
  type = number
  default = 6380
}
variable "redis_password" {
  type      = string
  sensitive = true
}
variable "redis_ssl" {
  type    = bool
  default = true
}

variable "short_base_url" {
  type    = string
  default = "https://links.example.com"
}

# Whether Terraform should run `az acr build` locally to build+push the image
variable "do_local_acr_build" {
  type    = bool
  default = true
}
