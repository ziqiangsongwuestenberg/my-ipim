insert into job
(name, job_type, enabled, client_id, cron, params_json, next_run_at,
 creation_time, update_time, creation_user, update_user)
values
('daily export', 'EXPORT_ARTICLES_XML_TO_S3', true, 12, '0 */5 * * * *', '{}'::jsonb, now(),
 now(), now(), 'system', 'system');