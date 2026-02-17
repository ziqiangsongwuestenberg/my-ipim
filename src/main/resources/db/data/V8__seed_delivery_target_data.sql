INSERT INTO delivery_target (client_id, type, enabled, config_json, creation_time, update_time, creation_user, update_user)
VALUES (
           12,
           'WEBHOOK',
           true,
           '{"url":"http://localhost:8081/mock/webhook","secret":"dev-secret","timeoutMs":3000}',
           now(), now() ,'system', 'system'
       );
