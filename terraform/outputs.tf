output "web_app_default_hostname" {
  description = "Default web app hostname (use https://<value>)"
  value       = azurerm_app_service.app.default_site_hostname
}

output "web_app_outbound_ips" {
  description = "Space-separated list of outbound IP addresses for the Web App. Add these to your DB/Redis firewall rules."
  value       = azurerm_app_service.app.outbound_ip_addresses
}

output "web_app_possible_outbound_ips" {
  description = "Possible outbound IP addresses (scaling)."
  value       = azurerm_app_service.app.possible_outbound_ip_addresses
}

output "acr_login_server" {
  description = "ACR login server (registry URL)"
  value       = azurerm_container_registry.acr.login_server
}
