CREATE TABLE event (
  id UUID NOT NULL PRIMARY KEY,
  title TEXT NOT NULL,
  description TEXT NOT NULL,
  host TEXT NOT NULL, -- todo
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);