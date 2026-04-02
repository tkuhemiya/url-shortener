resource "azurerm_resource_group" "rg" {
  name     = var.resource_group_name
  location = var.location
}

resource "azurerm_container_registry" "acr" {
  name                = var.acr_name
  resource_group_name = azurerm_resource_group.rg.name
  location            = azurerm_resource_group.rg.location
  sku                 = "Basic"
  admin_enabled       = false
}

resource "azurerm_app_service_plan" "plan" {
  name                = var.app_service_plan_name
  location            = azurerm_resource_group.rg.location
  resource_group_name = azurerm_resource_group.rg.name

  kind = "Linux"

  sku {
    tier = "Basic"
    size = var.app_service_sku
  }

  reserved = true
}

resource "azurerm_app_service" "app" {
  name                = var.webapp_name
  location            = azurerm_resource_group.rg.location
  resource_group_name = azurerm_resource_group.rg.name
  app_service_plan_id = azurerm_app_service_plan.plan.id

  identity {
    type = "SystemAssigned"
  }

  site_config {
    # linux_fx_version is in the form DOCKER|<registry>/<repo>:<tag>
    linux_fx_version = "DOCKER|${azurerm_container_registry.acr.login_server}/${var.acr_repo}:${var.image_tag}"
    always_on        = true
  }

  app_settings = {
    "WEBSITES_ENABLE_APP_SERVICE_STORAGE" = "false"
    "WEBSITES_PORT"                      = "8080"
    "DOCKER_REGISTRY_SERVER_URL"         = "https://${azurerm_container_registry.acr.login_server}"

    # Spring Boot environment variables (inferred from application.yml)
    "DB_URL"                             = var.db_url
    "DB_USER"                            = var.db_user
    "DB_PASSWORD"                        = var.db_password

    "REDIS_HOST"                         = var.redis_host
    "REDIS_PORT"                         = tostring(var.redis_port)
    "SPRING_REDIS_PASSWORD"              = var.redis_password
    "SPRING_REDIS_SSL"                   = tostring(var.redis_ssl)
    "SPRING_DATA_REDIS_HOST"             = var.redis_host
    "SPRING_DATA_REDIS_PORT"             = tostring(var.redis_port)
    "SPRING_DATA_REDIS_PASSWORD"         = var.redis_password

    "SHORT_BASE_URL"                     = var.short_base_url
  }

  depends_on = [azurerm_container_registry.acr]
}

# Grant the Web App's system identity AcrPull on the registry
resource "azurerm_role_assignment" "acr_pull" {
  scope                = azurerm_container_registry.acr.id
  role_definition_name = "AcrPull"
  principal_id         = azurerm_app_service.app.identity.principal_id
}

# Optional: local build + push using `az acr build` executed where you run terraform
resource "null_resource" "acr_build_and_push" {
  count = var.do_local_acr_build ? 1 : 0

  triggers = {
    image_tag = var.image_tag
    repo      = var.acr_repo
  }

  provisioner "local-exec" {
    command = <<EOT
set -e
# login to ACR (requires az cli and that you're logged in)
az acr login --name ${var.acr_name}
# build & push using ACR's build service
az acr build --registry ${var.acr_name} --image ${var.acr_repo}:${var.image_tag} .
EOT
    interpreter = ["/bin/bash", "-c"]
  }

  depends_on = [azurerm_container_registry.acr]
}
