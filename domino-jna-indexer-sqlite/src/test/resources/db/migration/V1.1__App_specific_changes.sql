/* Sample for an app specific SQL file that runs on DB migration after the Domino JNA SQL file
 * which sets up the basic database structures.
 * 
 * Creates an index on the "lastname" JSON item so that SELECT's that contain
 * JSON_EXTRACT(__json, '$.lastname') will use it and get faster */
CREATE INDEX dominodocs_lastname ON dominodocs (JSON_EXTRACT(__json, '$.lastname') ASC);
