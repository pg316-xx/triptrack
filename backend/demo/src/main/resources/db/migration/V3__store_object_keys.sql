-- Files now live in private S3/MinIO buckets; store the object key and
-- generate presigned URLs on read instead of persisting public URLs.
ALTER TABLE documents RENAME COLUMN file_url TO storage_key;
ALTER TABLE trip_media RENAME COLUMN file_url TO storage_key;
