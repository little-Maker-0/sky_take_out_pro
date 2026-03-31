-- 扩充用户数据
INSERT INTO user (openid, name, phone, sex, id_number, avatar, create_time) VALUES
('o4QSz7fCvmYvIDzVVdigaUqul0n5', '用户5', '13800138005', '1', '110101199001010005', 'https://example.com/avatar5.jpg', NOW());

-- 扩充订单数据
INSERT INTO orders (number, status, user_id, address_book_id, order_time, pay_method, pay_status, amount, remark, phone, address, user_name) VALUES
('202603190006', 1, 1, 1, NOW(), 1, 0, 50.5, '测试订单6', '13800138001', '北京市朝阳区', '用户1');

-- 扩充菜品数据
INSERT INTO dish (name, category_id, price, image, description, status, create_time, update_time, create_user, update_user) VALUES
('宫保鸡丁', 1, 28.0, 'https://example.com/dish1.jpg', '经典川菜', 1, NOW(), NOW(), 1, 1);

-- 初始化购物车数据
INSERT INTO shopping_cart (name, image, user_id, dish_id, setmeal_id, dish_flavor, number, amount, create_time) VALUES
('宫保鸡丁', 'https://example.com/dish1.jpg', 1, 46, NULL, '微辣', 1, 25.5, NOW());
