package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.DishService;
import com.sky.service.ShoppingCartService;
import com.sky.vo.DishVO;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private DishService dishService;

    @Autowired
    private SetmealMapper setmealMapper;
    
    @Autowired
    private RedisTemplate redisTemplate;

    private static final String SHOPPING_CART_KEY_PREFIX = "shopping_cart::";
    private static final int CART_TTL = 10;

    /**
     * 添加购物车
     * @param shoppingCartDTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        Long userId = BaseContext.getUserId();
        String cartKey = SHOPPING_CART_KEY_PREFIX + userId;
        
        // 生成商品Field
        String field;
        if (shoppingCartDTO.getDishId() != null) {
            field = "dish::" + shoppingCartDTO.getDishId();
        } else {
            field = "setmeal::" + shoppingCartDTO.getSetmealId();
        }
        
        // 从Redis中获取购物车商品
        String cartItemJson = (String) redisTemplate.opsForHash().get(cartKey, field);
        
        if (cartItemJson != null) {
            // 商品已存在，更新数量
            ShoppingCart shoppingCart = JSON.parseObject(cartItemJson, ShoppingCart.class);
            shoppingCart.setNumber(shoppingCart.getNumber() + 1);
            redisTemplate.opsForHash().put(cartKey, field, JSON.toJSONString(shoppingCart));
        } else {
            // 商品不存在，添加新商品
            ShoppingCart shoppingCart = new ShoppingCart();
            BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
            shoppingCart.setUserId(userId);
            
            Long dishId = shoppingCartDTO.getDishId();
            if (dishId != null) {
//                DishVO dishVO = dishService.getByIdWithFlavor(dishId);
                DishVO dishVO = (DishVO) redisTemplate.opsForValue().get("dishDetailCache::" + dishId);
//                if (dishVO != null) {}
                shoppingCart.setName(dishVO.getName());
                shoppingCart.setImage(dishVO.getImage());
                shoppingCart.setAmount(dishVO.getPrice());
            } else {
                // 添加到购物车的是套餐
                // 从数据库查询套餐信息（套餐详情缓存未实现）
                Setmeal setmeal = setmealMapper.getById(shoppingCartDTO.getSetmealId());
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setImage(setmeal.getImage());
                shoppingCart.setAmount(setmeal.getPrice());
            }
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());
            
            // 存入Redis
            redisTemplate.opsForHash().put(cartKey, field, JSON.toJSONString(shoppingCart));
            //设置过期时间
            redisTemplate.expire(cartKey, CART_TTL, TimeUnit.MINUTES);
            
            // 同时存入数据库作为持久化备份
            shoppingCartMapper.insert(shoppingCart);
        }
    }

    /**
     * 查看购物车
     * @return
     */
    @Override
    public List<ShoppingCart> showShoppingCart() {
        Long userId = BaseContext.getUserId();
        String cartKey = SHOPPING_CART_KEY_PREFIX + userId;
        
        // 从Redis中获取购物车所有商品
        Map<Object, Object> cartItems = redisTemplate.opsForHash().entries(cartKey);
        List<ShoppingCart> shoppingCarts = new ArrayList<>();
        
        if (cartItems != null && !cartItems.isEmpty()) {
            for (Object value : cartItems.values()) {
                String cartItemJson = (String) value;
                ShoppingCart shoppingCart = JSON.parseObject(cartItemJson, ShoppingCart.class);
                shoppingCarts.add(shoppingCart);
            }
        } else {
            // Redis中没有数据，从数据库查询
            shoppingCarts = shoppingCartMapper.list(ShoppingCart.builder().userId(userId).build());
            
            // 将数据库中的数据同步到Redis
            for (ShoppingCart cart : shoppingCarts) {
                String field;
                if (cart.getDishId() != null) {
                    field = "dish:" + cart.getDishId();
                } else {
                    field = "setmeal:" + cart.getSetmealId();
                }
                redisTemplate.opsForHash().put(cartKey, field, JSON.toJSONString(cart));
            }
        }

        redisTemplate.expire(cartKey, CART_TTL, TimeUnit.MINUTES);
        
        return shoppingCarts;
    }

    /**
     * 清空购物车商品
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cleanShoppingCart() {
        Long userId = BaseContext.getUserId();
        String cartKey = SHOPPING_CART_KEY_PREFIX + userId;
        
        // 清空Redis中的购物车
        redisTemplate.delete(cartKey);
        
        // 清空数据库中的购物车
        shoppingCartMapper.deleteByUserId(userId);
    }

    /**
     * 删除购物车中一个商品
     * @param shoppingCartDTO
     */
    @Override
    public void subShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        Long userId = BaseContext.getUserId();
        String cartKey = SHOPPING_CART_KEY_PREFIX + userId;
        
        // 生成商品Field
        String field;
        if (shoppingCartDTO.getDishId() != null) {
            field = "dish:" + shoppingCartDTO.getDishId();
        } else {
            field = "setmeal:" + shoppingCartDTO.getSetmealId();
        }
        
        // 从Redis中获取购物车商品
        String cartItemJson = (String) redisTemplate.opsForHash().get(cartKey, field);
        
        if (cartItemJson != null) {
            ShoppingCart shoppingCart = JSON.parseObject(cartItemJson, ShoppingCart.class);
            Integer number = shoppingCart.getNumber();
            
            if (number == 1) {
                // 当前商品在购物车中份数为1，直接删除
                redisTemplate.opsForHash().delete(cartKey, field);
                // 同时从数据库删除
                shoppingCartMapper.deleteById(shoppingCart.getId());
            } else {
                // 当前商品在购物车中的份数不为1，修改份数
                shoppingCart.setNumber(number - 1);
                redisTemplate.opsForHash().put(cartKey, field, JSON.toJSONString(shoppingCart));
                // 同时更新数据库
                shoppingCartMapper.updateNumberById(shoppingCart);
            }
        }
    }

}
