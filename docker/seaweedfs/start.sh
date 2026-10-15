#!/bin/sh
# Single-node SeaweedFS (master + volume + filer + S3 gateway) with one
# identity taken from S3_ACCESS_KEY / S3_SECRET_KEY. Anonymous access is denied.
set -e
cat > /tmp/s3.json <<JSON
{
  "identities": [
    {
      "name": "tripnest",
      "credentials": [{ "accessKey": "${S3_ACCESS_KEY}", "secretKey": "${S3_SECRET_KEY}" }],
      "actions": ["Admin", "Read", "Write", "List", "Tagging"]
    }
  ]
}
JSON
exec weed server -dir=/data -ip.bind=0.0.0.0 -volume.max=0 -s3 -s3.port=8333 -s3.config=/tmp/s3.json
