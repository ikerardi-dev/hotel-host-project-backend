-- Datos reales: 3 edificios, 19 apartamentos y sus camas.
-- Pendiente de confirmar con el hotel: Casa Baratura 1B (en la lista original venía "1C" repetido).

INSERT INTO buildings (name) VALUES
    ('Legazpi'),
    ('Casa Baratura'),
    ('La Refierta');

INSERT INTO apartments (building_id, code, capacity)
SELECT b.id, a.code, a.capacity
FROM buildings b
JOIN (
    SELECT 'Legazpi' AS building, 'A' AS code, 2 AS capacity
    UNION ALL SELECT 'Legazpi', 'B', 4
    UNION ALL SELECT 'Legazpi', 'C', 4
    UNION ALL SELECT 'Legazpi', 'D', 2
    UNION ALL SELECT 'Casa Baratura', '1A', 2
    UNION ALL SELECT 'Casa Baratura', '1B', 4
    UNION ALL SELECT 'Casa Baratura', '1C', 4
    UNION ALL SELECT 'Casa Baratura', '1D', 2
    UNION ALL SELECT 'Casa Baratura', '2A', 4
    UNION ALL SELECT 'Casa Baratura', '2B', 6
    UNION ALL SELECT 'Casa Baratura', '2D', 4
    UNION ALL SELECT 'Casa Baratura', '3A', 4
    UNION ALL SELECT 'Casa Baratura', '3C', 6
    UNION ALL SELECT 'Casa Baratura', '3D', 4
    UNION ALL SELECT 'Casa Baratura', '4A', 4
    UNION ALL SELECT 'Casa Baratura', '4B', 6
    UNION ALL SELECT 'Casa Baratura', '4C', 6
    UNION ALL SELECT 'La Refierta', 'A', 5
    UNION ALL SELECT 'La Refierta', 'B', 2
) a ON a.building = b.name;

INSERT INTO beds (apartment_id, type, quantity)
SELECT ap.id, d.type, d.quantity
FROM apartments ap
JOIN buildings b ON b.id = ap.building_id
JOIN (
    SELECT 'Legazpi' AS building, 'A' AS code, 'DOUBLE_BED' AS type, 1 AS quantity
    UNION ALL SELECT 'Legazpi', 'D', 'DOUBLE_BED', 1
    UNION ALL SELECT 'Legazpi', 'B', 'DOUBLE_BED', 1
    UNION ALL SELECT 'Legazpi', 'B', 'DOUBLE_SOFA_BED', 1
    UNION ALL SELECT 'Legazpi', 'C', 'DOUBLE_BED', 1
    UNION ALL SELECT 'Legazpi', 'C', 'DOUBLE_SOFA_BED', 1
    UNION ALL SELECT 'Casa Baratura', '1A', 'DOUBLE_BED', 1
    UNION ALL SELECT 'Casa Baratura', '1D', 'DOUBLE_BED', 1
    UNION ALL SELECT 'Casa Baratura', '1B', 'DOUBLE_SOFA_BED', 1
    UNION ALL SELECT 'Casa Baratura', '1B', 'SINGLE_BED', 2
    UNION ALL SELECT 'Casa Baratura', '1C', 'DOUBLE_SOFA_BED', 1
    UNION ALL SELECT 'Casa Baratura', '1C', 'SINGLE_BED', 2
    UNION ALL SELECT 'Casa Baratura', '2A', 'DOUBLE_BED', 1
    UNION ALL SELECT 'Casa Baratura', '2A', 'DOUBLE_SOFA_BED', 1
    UNION ALL SELECT 'Casa Baratura', '2D', 'DOUBLE_BED', 1
    UNION ALL SELECT 'Casa Baratura', '2D', 'DOUBLE_SOFA_BED', 1
    UNION ALL SELECT 'Casa Baratura', '3A', 'DOUBLE_BED', 1
    UNION ALL SELECT 'Casa Baratura', '3A', 'DOUBLE_SOFA_BED', 1
    UNION ALL SELECT 'Casa Baratura', '3D', 'DOUBLE_BED', 1
    UNION ALL SELECT 'Casa Baratura', '3D', 'DOUBLE_SOFA_BED', 1
    UNION ALL SELECT 'Casa Baratura', '4A', 'DOUBLE_BED', 1
    UNION ALL SELECT 'Casa Baratura', '4A', 'DOUBLE_SOFA_BED', 1
    UNION ALL SELECT 'Casa Baratura', '2B', 'DOUBLE_BED', 1
    UNION ALL SELECT 'Casa Baratura', '2B', 'DOUBLE_SOFA_BED', 1
    UNION ALL SELECT 'Casa Baratura', '2B', 'SINGLE_BED', 2
    UNION ALL SELECT 'Casa Baratura', '3C', 'DOUBLE_BED', 1
    UNION ALL SELECT 'Casa Baratura', '3C', 'DOUBLE_SOFA_BED', 1
    UNION ALL SELECT 'Casa Baratura', '3C', 'SINGLE_BED', 2
    UNION ALL SELECT 'Casa Baratura', '4B', 'DOUBLE_BED', 1
    UNION ALL SELECT 'Casa Baratura', '4B', 'DOUBLE_SOFA_BED', 1
    UNION ALL SELECT 'Casa Baratura', '4B', 'SINGLE_BED', 2
    UNION ALL SELECT 'Casa Baratura', '4C', 'DOUBLE_BED', 2
    UNION ALL SELECT 'Casa Baratura', '4C', 'SINGLE_BED', 2
    UNION ALL SELECT 'La Refierta', 'A', 'DOUBLE_BED', 1
    UNION ALL SELECT 'La Refierta', 'A', 'SINGLE_BED', 1
    UNION ALL SELECT 'La Refierta', 'A', 'DOUBLE_SOFA_BED', 1
    UNION ALL SELECT 'La Refierta', 'B', 'DOUBLE_BED', 1
) d ON d.building = b.name AND d.code = ap.code;
