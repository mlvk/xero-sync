-- :name get-items :? :*
-- :doc gets all items
SELECT
    *
FROM
    items;

-- :name get-sell-items :? :*
-- :doc gets all items that are for sale
SELECT
    *
FROM
    items i
WHERE
    i.is_sold IS TRUE;

-- :name get-purchase-items :? :*
-- :doc gets all items that are purschased
SELECT
    *
FROM
    items i
WHERE
    i.is_purchased IS TRUE;

-- :name get-item-by-id :? :1
-- :doc get an item by its id
SELECT
    *
FROM
    items i
WHERE
    i.id = :id;

-- :name get-item-by-xero-id :? :1
-- :doc get an item by its xero id
SELECT
    *
FROM
    items i
WHERE
    i.xero_id = :xero_id;

-- :name get-item-by-code :? :1
-- :doc get an item by its code
SELECT
    *
FROM
    items i
WHERE
    i.code = :code;

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

-- :name mark-item-unsynced! :! :n
-- :doc set the specified item to unsynced state
UPDATE
    items
SET
    sync_state = 0
WHERE
    id = :id;

-- :name mark-item-synced! :! :n
-- :doc set the specified item to unsynced state
UPDATE
    items
SET
    sync_state = 1
WHERE
    id = :id;

