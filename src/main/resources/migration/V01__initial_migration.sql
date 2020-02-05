CREATE TABLE "user" (
  id UUID PRIMARY KEY,
  name TEXT NOT NULL,
  email TEXT NOT NULL,
  password_hash TEXT NOT NULL
);


CREATE TABLE event (
  id UUID PRIMARY KEY,
  title TEXT NOT NULL,
  description TEXT NOT NULL,
  host_id UUID NOT NULL REFERENCES "user",
  date_time TIMESTAMP WITH TIME ZONE NOT NULL,
  time_zone TEXT NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE OR REPLACE FUNCTION update__updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update__event__updated_at
  AFTER UPDATE ON event
  FOR EACH ROW
  EXECUTE PROCEDURE update__updated_at();

