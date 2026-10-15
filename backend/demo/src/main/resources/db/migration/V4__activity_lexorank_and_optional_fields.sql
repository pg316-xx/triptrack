-- Activities are ordered by a LexoRank string instead of an integer, so a
-- move only rewrites the moved row. COLLATE "C" makes the database sort the
-- same way Java's String.compareTo does.
-- Existing integers become zero-padded ranks ending in '1' (ranks may not
-- end in '0'), which keeps their current order.
ALTER TABLE activities
    ALTER COLUMN order_index TYPE varchar(255) COLLATE "C"
    USING lpad(greatest(order_index, 0)::text, 10, '0') || '1';

CREATE INDEX idx_activities_day_order
    ON activities (itinerary_day_id, order_index);

-- Only the title is required; location/coordinates may come later
-- (e.g. from a maps lookup) and times/cost are optional while planning.
ALTER TABLE activities ALTER COLUMN location DROP NOT NULL;
ALTER TABLE activities ALTER COLUMN latitude DROP NOT NULL;
ALTER TABLE activities ALTER COLUMN longitude DROP NOT NULL;
ALTER TABLE activities ALTER COLUMN start_time DROP NOT NULL;
ALTER TABLE activities ALTER COLUMN end_time DROP NOT NULL;
ALTER TABLE activities ALTER COLUMN estimated_cost DROP NOT NULL;
