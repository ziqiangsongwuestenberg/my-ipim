INSERT INTO delivery_target (target_key, client_id, type, enabled, config_json, creation_time, update_time, creation_user, update_user)
VALUES (
        'client-1001-webhook-main',
           12,
           'WEBHOOK',
           true,
           '{"url":"http://localhost:8081/mock/webhook1","secret":"dev-secret","timeoutMs":3000}',
           now(), now() ,'system', 'system'
       ),
       (
           'client-1002-webhook-main',
           12,
           'WEBHOOK',
           true,
           '{"url":"http://localhost:8081/mock/webhook2","secret":"dev-secret","timeoutMs":3000}',
           now(), now() ,'system', 'system'
       ),
       (
           'client-1003-webhook-main',
           12,
           'WEBHOOK',
           true,
           '{"url":"http://localhost:8081/mock/webhook3","secret":"dev-secret","timeoutMs":3000}',
           now(), now() ,'system', 'system'
       ),
       (
           'client-1004-webhook-main',
           12,
           'WEBHOOK',
           true,
           '{"url":"http://localhost:8081/mock/webhook4","secret":"dev-secret","timeoutMs":3000}',
           now(), now() ,'system', 'system'
       ),
       (
           'client-1005-webhook-main',
           12,
           'WEBHOOK',
           true,
           '{"url":"http://localhost:8081/mock/webhook5","secret":"dev-secret","timeoutMs":3000}',
           now(), now() ,'system', 'system'
       )
ON CONFLICT (target_key) DO NOTHING;
