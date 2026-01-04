CREATE OR REPLACE VIEW tool_performance_view AS
SELECT 
    t.id AS tool_id,
    t.tool_name,
    t.created AS tool_created_date,
    t.avg_review AS tool_avg_review,
    u.id AS owner_id,
    u.fullname AS owner_name,
    u.email AS owner_email,
    u.phone_number AS owner_phone,
    COUNT(DISTINCT r.id) AS total_reservations,
    COUNT(DISTINCT rev.id) AS total_reviews,
    COALESCE(AVG(rev.review_point), 0) AS calculated_avg_review,
    MAX(r.reservation_date) AS last_reservation_date,
    MIN(r.reservation_date) AS first_reservation_date
FROM 
    tools t
    LEFT JOIN users u ON t.user_id = u.id
    LEFT JOIN reservation r ON r.tool_id = t.id
    LEFT JOIN review rev ON rev.reservation_id = r.id
GROUP BY 
    t.id, t.tool_name, t.created, t.avg_review, 
    u.id, u.fullname, u.email, u.phone_number;

