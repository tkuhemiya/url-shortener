Overview

This Terraform configuration provisions:
- Resource Group
- Azure Container Registry (ACR)
- App Service Plan (Linux)
- App Service (Web App) with SystemAssigned identity
- Role assignment so the Web App can pull images from the ACR (AcrPull)

It will also (optionally) run `az acr build` locally to build and push your Docker image into ACR.

Notes about existing managed services
- You already have PostgreSQL and Redis elsewhere. This Terraform config will NOT create those resources.
- Provide their connection values as Terraform variables (sensitive).

Security
- Secrets (DB/Redis passwords) are accepted as sensitive variables. They will be stored in the Terraform state in accordance with Terraform behavior. For production, consider using Azure Key Vault and referencing secrets via Managed Identity.

Files
- providers.tf  : provider configuration
- variables.tf  : input variables (sensitive flags used)
- main.tf       : primary resources
- outputs.tf    : useful outputs

Quick start
1) Install: terraform, azure cli, docker (if you use do_local_acr_build=true)
2) az login
3) Create a terraform.tfvars with your values (example below) or set environment variables
4) terraform init
5) terraform apply

Example terraform.tfvars (DO NOT CHECK THIS INTO SOURCE CONTROL)

resource_group_name = "rg-shortener"
location = "eastus"
acr_name = "myacrshortener"
acr_repo = "shortener"
image_tag = "v1"
app_service_plan_name = "shortener-plan"
webapp_name = "shortener-app-12345"
app_service_sku = "B1"

# existing services
db_url = "<REDACTED_DB_URL>"
db_user = "<REDACTED_DB_USER>"
db_password = "<REDACTED_DB_PASSWORD>"
redis_host = "<REDACTED_REDIS_HOST>"
redis_port = 6380
redis_password = "<REDACTED_REDIS_PASSWORD>"
redis_ssl = true

short_base_url = "https://links.example.com"

# If you want Terraform to run az acr build locally, keep this true.
do_local_acr_build = true

Firewall allowlisting (manual step)
- After apply, run `terraform output web_app_outbound_ips` and add each IP to your PostgreSQL and Redis firewall rules.
- Example Azure CLI commands (replace placeholders):
  # Postgres Single Server
  az postgres server firewall-rule create --resource-group <pg_rg> --server-name <pg_name> --name allow_webapp_1 --start-ip-address <IP> --end-ip-address <IP>

  # Postgres Flexible Server
  az postgres flexible-server firewall-rule create --resource-group <pg_rg> --name <pg_name> --rule-name allow_webapp_1 --start-ip-address <IP> --end-ip-address <IP>

  # Azure Cache for Redis (public firewall support varies by SKU)
  # If supported, add IPs in the portal or using az cli for redis (if available for your sku).

How to run
1) az login
2) terraform init
3) terraform apply -var-file="terraform.tfvars"

After apply
- Get web app URL:
  terraform output web_app_default_hostname

- Get outbound IPs to add to DB/Redis firewall:
  terraform output web_app_outbound_ips

- Tail logs:
  az webapp log tail --name <webapp_name> --resource-group <rg>

Optional improvements
- Use Azure Key Vault to hold DB/Redis secrets and reference them from App Service via Managed Identity.
- Use Private Endpoints and VNet Integration to avoid public firewall rules.

If you want, I can:
- Add Key Vault integration to this Terraform configuration
- Add a GitHub Actions workflow that runs `az acr build` and then `terraform apply` remotely

