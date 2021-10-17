-- :name get-unsynced-orders :? :*
-- :doc get all unsynced order
SELECT
    *
FROM
    orders o
WHERE
    o.sync_state = 0;

