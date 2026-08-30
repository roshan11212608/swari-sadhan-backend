-- Update selling_price from the specifications JSON where a sellPrice exists
-- This ensures the dedicated selling_price column has the correct selling amount

UPDATE vehicles
SET selling_price = CAST(
    REPLACE(
      JSON_UNQUOTE(JSON_EXTRACT(specifications, '$.sellPrice')),
      ',',
      ''
    ) AS DECIMAL(12,2)
  )
WHERE specifications IS NOT NULL
  AND JSON_UNQUOTE(JSON_EXTRACT(specifications, '$.sellPrice')) IS NOT NULL
  AND JSON_UNQUOTE(JSON_EXTRACT(specifications, '$.sellPrice')) != ''
  AND CAST(
    REPLACE(
      JSON_UNQUOTE(JSON_EXTRACT(specifications, '$.sellPrice')),
      ',',
      ''
    ) AS DECIMAL(12,2)
  ) > 0;
