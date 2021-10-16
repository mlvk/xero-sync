-- :name get-items :? :*
-- :doc gets all items
SELECT
    *
FROM
    items;

-- :name get-unsynced-items :? :*
-- :doc gets all unsynced items
SELECT
    *
FROM
    items i
WHERE
    i.sync_state = 0;

-- :name get-synced-items :? :*
-- :doc gets all synced items
SELECT
    *
FROM
    items i
WHERE
    i.sync_state = 1;

-- :name mark-unsynced! :! :n
-- :doc set the specified item to unsynced state
UPDATE
    items
SET
    sync_state = 0
WHERE
    id = :id;

-- :name mark-synced! :! :n
-- :doc set the specified item to unsynced state
UPDATE
    items
SET
    sync_state = 1
WHERE
    id = :id;

