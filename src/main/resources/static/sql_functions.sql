CREATE OR REPLACE FUNCTION get_user_reservations(
    p_user_id BIGINT,
    p_start_date DATE DEFAULT NULL,
    p_end_date DATE DEFAULT NULL
)
RETURNS TABLE (
    reservation_id BIGINT,
    tool_id BIGINT,
    tool_name VARCHAR,
    reservation_date DATE,
    review_point INTEGER,
    has_review BOOLEAN,
    owner_name VARCHAR,
    owner_email VARCHAR
) AS $$
DECLARE
    res_record RECORD;
    res_cursor CURSOR FOR
        SELECT 
            r.id,
            r.tool_id,
            t.tool_name,
            r.reservation_date,
            rev.review_point,
            u.fullname as owner_name,
            u.email as owner_email
        FROM reservation r
        JOIN tools t ON r.tool_id = t.id
        LEFT JOIN users u ON t.user_id = u.id
        LEFT JOIN review rev ON r.review_id = rev.id
        WHERE r.user_id = p_user_id
          AND (p_start_date IS NULL OR r.reservation_date >= p_start_date)
          AND (p_end_date IS NULL OR r.reservation_date <= p_end_date)
        ORDER BY r.reservation_date DESC;
BEGIN
    FOR res_record IN res_cursor LOOP
        reservation_id := res_record.id;
        tool_id := res_record.tool_id;
        tool_name := res_record.tool_name;
        reservation_date := res_record.reservation_date;
        review_point := res_record.review_point;
        has_review := (res_record.review_point IS NOT NULL);
        owner_name := res_record.owner_name;
        owner_email := res_record.owner_email;
        RETURN NEXT;
    END LOOP;
    RETURN;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION get_tool_reservation_stats(
    p_tool_id BIGINT
)
RETURNS TABLE (
    tool_id BIGINT,
    tool_name VARCHAR,
    total_reservations BIGINT,
    future_reservations BIGINT,
    past_reservations BIGINT,
    avg_review_point NUMERIC,
    total_reviews BIGINT,
    owner_id BIGINT,
    owner_name VARCHAR,
    owner_email VARCHAR,
    first_reservation_date DATE,
    last_reservation_date DATE
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        t.id as tool_id,
        t.tool_name,
        COUNT(r.id)::BIGINT as total_reservations,
        COUNT(CASE WHEN r.reservation_date >= CURRENT_DATE THEN 1 END)::BIGINT as future_reservations,
        COUNT(CASE WHEN r.reservation_date < CURRENT_DATE THEN 1 END)::BIGINT as past_reservations,
        COALESCE(AVG(rev.review_point), 0)::NUMERIC(5,2) as avg_review_point,
        COUNT(rev.id)::BIGINT as total_reviews,
        u.id as owner_id,
        u.fullname as owner_name,
        u.email as owner_email,
        MIN(r.reservation_date) as first_reservation_date,
        MAX(r.reservation_date) as last_reservation_date
    FROM tools t
    LEFT JOIN reservation r ON r.tool_id = t.id
    LEFT JOIN review rev ON rev.reservation_id = r.id
    LEFT JOIN users u ON t.user_id = u.id
    WHERE t.id = p_tool_id
    GROUP BY t.id, t.tool_name, u.id, u.fullname, u.email;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION get_pending_review_reservations(
    p_user_id BIGINT,
    p_days_past INTEGER DEFAULT 0
)
RETURNS TABLE (
    reservation_id BIGINT,
    tool_id BIGINT,
    tool_name VARCHAR,
    reservation_date DATE,
    days_since_reservation INTEGER,
    owner_name VARCHAR,
    owner_email VARCHAR,
    owner_phone VARCHAR
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        r.id as reservation_id,
        r.tool_id,
        t.tool_name,
        r.reservation_date,
        (CURRENT_DATE - r.reservation_date)::INTEGER as days_since_reservation,
        u.fullname as owner_name,
        u.email as owner_email,
        u.phone_number as owner_phone
    FROM reservation r
    JOIN tools t ON r.tool_id = t.id
    JOIN users u ON t.user_id = u.id
    WHERE r.user_id = p_user_id
      AND r.reservation_date < CURRENT_DATE
      AND r.review_id IS NULL
      AND (p_days_past = 0 OR r.reservation_date >= CURRENT_DATE - p_days_past)
    ORDER BY r.reservation_date DESC;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION update_tool_avg_review()
RETURNS TRIGGER AS $$
DECLARE
    v_tool_id BIGINT;
    v_avg_review NUMERIC;
    v_review_id BIGINT;
BEGIN
    v_review_id := COALESCE(NEW.id, OLD.id);
    
    SELECT r.tool_id INTO v_tool_id
    FROM review rev
    JOIN reservation r ON rev.reservation_id = r.id
    WHERE rev.id = v_review_id;
    
    IF v_tool_id IS NULL THEN
        RETURN COALESCE(NEW, OLD);
    END IF;
    
    SELECT COALESCE(AVG(rev.review_point), 0) INTO v_avg_review
    FROM reservation r
    JOIN review rev ON rev.reservation_id = r.id
    WHERE r.tool_id = v_tool_id
      AND rev.review_point IS NOT NULL;
    
    UPDATE tools
    SET avg_review = v_avg_review
    WHERE id = v_tool_id;
    
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_tool_avg_review
AFTER INSERT OR UPDATE OR DELETE ON review
FOR EACH ROW
EXECUTE FUNCTION update_tool_avg_review();

CREATE OR REPLACE FUNCTION validate_reservation_date()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.reservation_date < CURRENT_DATE THEN
        RAISE EXCEPTION 'Geçmiş tarihli rezervasyon oluşturulamaz veya güncellenemez! Rezervasyon tarihi: %, Bugünün tarihi: %', 
            NEW.reservation_date, CURRENT_DATE;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_validate_reservation_date
BEFORE INSERT OR UPDATE ON reservation
FOR EACH ROW
EXECUTE FUNCTION validate_reservation_date();

