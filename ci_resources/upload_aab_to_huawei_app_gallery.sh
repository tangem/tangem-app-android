#!/bin/bash

set -eo pipefail

# ----------------------------------------
# 1. Obtain OAuth access token
# ----------------------------------------
# Sending a POST request to retrieve access token using client credentials
TOKEN=$(curl -s -v -X POST "https://connect-api.cloud.huawei.com/api/oauth2/v1/token" \
  -H "Content-Type: application/json" \
  -d "{
        \"grant_type\": \"client_credentials\",
        \"client_id\": \"$CLIENT_ID\",
        \"client_secret\": \"$CLIENT_SECRET\"
      }" | jq -r .access_token)

echo "------------------------------------------------"
echo "[INFO] Token = $TOKEN"
echo "------------------------------------------------"

# ----------------------------------------
# 2. Get upload information (temporary URL + required headers)
# ----------------------------------------
# Extract file name and size for the upload request
echo "[INFO] File path = $FILE_PATH"
FILE_NAME=$(basename "$FILE_PATH")
echo "[INFO] File name = $FILE_NAME"
CONTENT_LENGTH=$(stat -c %s "$FILE_PATH")
echo "[INFO] Content length = $CONTENT_LENGTH"

# Request upload URL and headers for OBS (Huawei Object Storage Service)
UPLOAD_INFO=$(curl -s -v -X GET \
  "https://connect-api.cloud.huawei.com/api/publish/v2/upload-url/for-obs?appId=$APP_ID&fileName=$FILE_NAME&contentLength=$CONTENT_LENGTH" \
  -H "Authorization: Bearer $TOKEN" \
  -H "client_id: $CLIENT_ID" \
  -H "Content-Type: application/json")

echo "------------------------------------------------"
echo "[INFO] Upload: $UPLOAD_INFO"
echo "------------------------------------------------"

# ----------------------------------------
# 3. Upload the AAB file to Huawei OBS using signed URL and headers
# ----------------------------------------
# Parse the response to get upload URL and required headers
UPLOAD_URL=$(echo "$UPLOAD_INFO" | jq -r '.urlInfo.url')
HEADERS=$(echo "$UPLOAD_INFO" | jq -r '.urlInfo.headers')

echo "------------------------------------------------"
echo "[INFO] Parsed upload URL: $UPLOAD_URL"
echo "[INFO] Extracting headers..."
echo "------------------------------------------------"

# Extract individual headers for the PUT request
AUTH_HEADER=$(echo "$HEADERS" | jq -r '."Authorization"')
SHA256_HEADER=$(echo "$HEADERS" | jq -r '."x-amz-content-sha256"')
DATE_HEADER=$(echo "$HEADERS" | jq -r '."x-amz-date"')
HOST_HEADER=$(echo "$HEADERS" | jq -r '."Host"')
UA_HEADER=$(echo "$HEADERS" | jq -r '."user-agent"')
CT_HEADER=$(echo "$HEADERS" | jq -r '."Content-Type"')

# Log headers (for debug purposes)
echo "[INFO] Authorization: $AUTH_HEADER"
echo "[INFO] x-amz-content-sha256: $SHA256_HEADER"
echo "[INFO] x-amz-date: $DATE_HEADER"
echo "[INFO] Host: $HOST_HEADER"
echo "[INFO] user-agent: $UA_HEADER"
echo "[INFO] Content-Type: $CT_HEADER"
echo "------------------------------------------------"

# Perform the actual file upload using the signed PUT URL
echo "[INFO] Uploading file '$FILE_PATH' to Huawei OBS..."
RESPONSE=$(curl -v -X PUT "$UPLOAD_URL" \
  -H "Authorization: $AUTH_HEADER" \
  -H "x-amz-content-sha256: $SHA256_HEADER" \
  -H "x-amz-date: $DATE_HEADER" \
  -H "Host: $HOST_HEADER" \
  -H "user-agent: $UA_HEADER" \
  -H "Content-Type: $CT_HEADER" \
  --data-binary @"$FILE_PATH" 2>&1)

echo "$RESPONSE"
echo "------------------------------------------------"

# Check if upload succeeded
if echo "$RESPONSE" | grep -q "HTTP/1.1 200"; then
  echo "[SUCCESS] File upload completed successfully."
else
  echo "[ERROR] File upload failed:"
  echo "$RESPONSE"
  exit 1
fi

# ----------------------------------------
# 4. Commit the uploaded file to AppGallery (register the uploaded file)
# ----------------------------------------
# Huawei requires an additional API call to register the uploaded file to the app
echo "[INFO] Committing uploaded AAB file to AppGallery..."

# Extract path (excluding hostname) from the upload URL
FILE_DEST_URL=$(echo "$UPLOAD_URL" | sed -E 's|https://[^/]+/||')

# Prepare commit payload
COMMIT_PAYLOAD=$(cat <<EOF
{
  "fileType": 5,
  "files": [{
    "fileName": "$FILE_NAME",
    "fileDestUrl": "$FILE_DEST_URL"
  }]
}
EOF
)

# Send PUT request to finalize (commit) the uploaded file
COMMIT_RESPONSE=$(curl -s -X PUT \
  "https://connect-api.cloud.huawei.com/api/publish/v2/app-file-info?appId=$APP_ID" \
  -H "Authorization: Bearer $TOKEN" \
  -H "client_id: $CLIENT_ID" \
  -H "Content-Type: application/json" \
  -d "$COMMIT_PAYLOAD")

# Log commit response and validate success
echo "------------------------------------------------"
echo "[INFO] Commit response:"
echo "$COMMIT_RESPONSE"

if echo "$COMMIT_RESPONSE" | jq -e '.ret.code == 0' > /dev/null; then
  echo "[SUCCESS] File committed successfully and is now visible in AppGallery Console."
else
  echo "[ERROR] Failed to commit file:"
  echo "$COMMIT_RESPONSE"
  exit 1
fi
