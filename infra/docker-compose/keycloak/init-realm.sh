#!/bin/bash
# Initialize Keycloak FleetIQ realm
set -e

echo "Waiting for Keycloak to be ready..."
until curl -s http://localhost:8080/health > /dev/null 2>&1; do
    sleep 3
done

echo "Keycloak is ready. Configuring FleetIQ realm..."

# Authenticate
/opt/keycloak/bin/kcadm.sh config credentials \
    --server http://localhost:8080 \
    --realm master \
    --user admin \
    --password admin

# Create realm if it doesn't exist
if ! /opt/keycloak/bin/kcadm.sh get realms/fleetiq > /dev/null 2>&1; then
    /opt/keycloak/bin/kcadm.sh create realms \
        -s realm=fleetiq \
        -s enabled=true \
        -s sslRequired=none
    echo "FleetIQ realm created."
else
    echo "FleetIQ realm already exists."
fi

# Create service client
if ! /opt/keycloak/bin/kcadm.sh get clients -r fleetiq -q clientId=fleetiq-services 2>/dev/null | grep -q fleetiq-services; then
    /opt/keycloak/bin/kcadm.sh create clients -r fleetiq \
        -s clientId=fleetiq-services \
        -s enabled=true \
        -s protocol=openid-connect \
        -s publicClient=false \
        -s serviceAccountsEnabled=true \
        -s authorizationServicesEnabled=true \
        -s clientAuthenticatorType=client-secret \
        -s secret=fleetiq-service-secret-change-in-production
    echo "Service client created."
fi

# Create device client
if ! /opt/keycloak/bin/kcadm.sh get clients -r fleetiq -q clientId=fleetiq-devices 2>/dev/null | grep -q fleetiq-devices; then
    /opt/keycloak/bin/kcadm.sh create clients -r fleetiq \
        -s clientId=fleetiq-devices \
        -s enabled=true \
        -s protocol=openid-connect \
        -s publicClient=false \
        -s serviceAccountsEnabled=true \
        -s clientAuthenticatorType=client-secret \
        -s secret=fleetiq-device-secret-change-in-production
    echo "Device client created."
fi

# Create roles
for role in device operator service; do
    if ! /opt/keycloak/bin/kcadm.sh get roles -r fleetiq -q search=$role 2>/dev/null | grep -q "\"name\" : \"$role\""; then
        /opt/keycloak/bin/kcadm.sh create roles -r fleetiq -s name=$role
        echo "Role '$role' created."
    fi
done

echo "FleetIQ realm configuration complete."
