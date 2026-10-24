INSERT INTO sellers (name, city, status) VALUES
  ('Shree Traders', 'Pune', 'APPROVED'),
  ('Gupta Building Supplies', 'Mumbai', 'APPROVED'),
  ('Mahalaxmi Hardware', 'Nashik', 'APPROVED'),
  ('New Star Cement Depot', 'Thane', 'PENDING'),
  ('Quick Deals Co', 'Pune', 'REJECTED')
ON CONFLICT (name) DO NOTHING;

INSERT INTO products (name, brand, category, unit) VALUES
  ('UltraTech PPC Cement 50 kg', 'UltraTech', 'Cement', 'bag'),
  ('ACC Gold Water Shield Cement 50 kg', 'ACC', 'Cement', 'bag'),
  ('Ambuja Plus Cement 50 kg', 'Ambuja', 'Cement', 'bag'),
  ('Tata Tiscon TMT Bar 12 mm', 'Tata Tiscon', 'Steel', 'tonne'),
  ('JSW Neosteel TMT Bar 16 mm', 'JSW', 'Steel', 'tonne'),
  ('River Sand (Medium)', 'Local', 'Aggregates', 'tonne'),
  ('20 mm Crushed Aggregate', 'Local', 'Aggregates', 'tonne'),
  ('Red Clay Bricks (Class A)', 'Local', 'Bricks', '1000 pcs'),
  ('Fly Ash Bricks', 'Local', 'Bricks', '1000 pcs'),
  ('Asian Paints Apex Ultima 20 L', 'Asian Paints', 'Paints', 'bucket'),
  ('Birla White Wall Putty 40 kg', 'Birla White', 'Paints', 'bag'),
  ('Supreme CPVC Pipe 1 inch', 'Supreme', 'Plumbing', 'piece')
ON CONFLICT (name) DO NOTHING;

-- Same product, several sellers, different price/stock/MOQ. Unique key keeps re-runs harmless.
INSERT INTO seller_listings (product_id, seller_id, price, stock, min_order_qty)
SELECT p.id, s.id, v.price, v.stock, v.moq
FROM (VALUES
  ('UltraTech PPC Cement 50 kg', 'Shree Traders', 390, 500, 10),
  ('UltraTech PPC Cement 50 kg', 'Gupta Building Supplies', 385, 120, 20),
  ('UltraTech PPC Cement 50 kg', 'Mahalaxmi Hardware', 405, 2000, 1),
  ('UltraTech PPC Cement 50 kg', 'New Star Cement Depot', 350, 900, 5),
  ('UltraTech PPC Cement 50 kg', 'Quick Deals Co', 300, 100, 1),
  ('ACC Gold Water Shield Cement 50 kg', 'Shree Traders', 410, 300, 10),
  ('ACC Gold Water Shield Cement 50 kg', 'Gupta Building Supplies', 415, 0, 10),
  ('Ambuja Plus Cement 50 kg', 'Mahalaxmi Hardware', 395, 450, 5),
  ('Tata Tiscon TMT Bar 12 mm', 'Gupta Building Supplies', 61500, 40, 1),
  ('Tata Tiscon TMT Bar 12 mm', 'Shree Traders', 60800, 25, 2),
  ('JSW Neosteel TMT Bar 16 mm', 'Gupta Building Supplies', 59900, 15, 1),
  ('River Sand (Medium)', 'Mahalaxmi Hardware', 1800, 200, 5),
  ('River Sand (Medium)', 'Shree Traders', 1750, 60, 10),
  ('20 mm Crushed Aggregate', 'Mahalaxmi Hardware', 1100, 300, 5),
  ('Red Clay Bricks (Class A)', 'Shree Traders', 8500, 50, 2),
  ('Red Clay Bricks (Class A)', 'Mahalaxmi Hardware', 8200, 20, 5),
  ('Fly Ash Bricks', 'Gupta Building Supplies', 6500, 80, 3),
  ('Asian Paints Apex Ultima 20 L', 'Gupta Building Supplies', 7900, 35, 1),
  ('Birla White Wall Putty 40 kg', 'Shree Traders', 1350, 100, 5),
  ('Supreme CPVC Pipe 1 inch', 'New Star Cement Depot', 240, 500, 10)
) AS v(product, seller, price, stock, moq)
JOIN products p ON p.name = v.product
JOIN sellers s ON s.name = v.seller
ON CONFLICT (product_id, seller_id) DO NOTHING;
