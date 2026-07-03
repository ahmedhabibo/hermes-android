#!/bin/bash
# Generate a debug-grade release keystore for hermes-android
# Run once: ./generate-keystore.sh
# Output: keystore/hermes-release.jks

set -e

KEYSTORE_DIR="$(dirname "$0")"
KEYSTORE_FILE="$KEYSTORE_DIR/hermes-release.jks"
STORE_PASS="hermes-android-release"
KEY_PASS="hermes-android-release"
ALIAS="hermes-release"
DNAME="CN=Hermes Android, OU=Mobile, O=HermesAgent, L=Cairo, ST=Cairo, C=EG"

if [ -f "$KEYSTORE_FILE" ]; then
    echo "Keystore already exists: $KEYSTORE_FILE"
    echo "Delete it first if you want to regenerate."
    exit 1
fi

keytool -genkeypair \
    -alias "$ALIAS" \
    -keypass "$KEY_PASS" \
    -keystore "$KEYSTORE_FILE" \
    -storepass "$STORE_PASS" \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -dname "$DNAME"

echo ""
echo "Keystore generated: $KEYSTORE_FILE"
echo "Store password: $STORE_PASS"
echo "Key password:   $KEY_PASS"
echo "Alias:          $ALIAS"
echo ""
echo "Next: Create keystore/secrets.properties from keystore/keystore.properties.template"
