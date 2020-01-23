CREATE TABLE event (
  id UUID NOT NULL PRIMARY KEY,
  title TEXT NOT NULL,
  description TEXT NOT NULL,
  host TEXT NOT NULL, -- todo
  date_time TIMESTAMP WITH TIME ZONE NOT NULL,
  time_zone TEXT NOT NULL,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL
);