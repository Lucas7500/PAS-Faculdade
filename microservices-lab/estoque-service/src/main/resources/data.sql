-- Dados iniciais. ON CONFLICT garante que reinicializações não sobrescrevem o estoque atual.
INSERT INTO produto (id, nome, quantidade) VALUES (1, 'Notebook', 10) ON CONFLICT (id) DO NOTHING;
INSERT INTO produto (id, nome, quantidade) VALUES (2, 'Mouse', 50)    ON CONFLICT (id) DO NOTHING;
INSERT INTO produto (id, nome, quantidade) VALUES (3, 'Teclado', 20)  ON CONFLICT (id) DO NOTHING;
