-- Allow image-only posts: content becomes optional.
-- The "content OR at least one image" rule is enforced in the service layer.
ALTER TABLE posts ALTER COLUMN content DROP NOT NULL;
