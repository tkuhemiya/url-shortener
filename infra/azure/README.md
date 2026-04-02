# Azure deployment notes

## Option A: Azure Container Apps

1. Build and push image to Azure Container Registry (ACR).
2. Update placeholders in `containerapp.yaml`.
3. Deploy:

```bash
az containerapp create --resource-group <RESOURCE_GROUP> --yaml infra/azure/containerapp.yaml
```

## Option B: Azure App Service (custom container)

1. Build and push Docker image to ACR.
2. Create a Web App for Containers and point it to your image.
3. Apply env vars from `appservice.env.example` in App Service configuration.

## Required managed services

- Azure Database for PostgreSQL
- Azure Cache for Redis
