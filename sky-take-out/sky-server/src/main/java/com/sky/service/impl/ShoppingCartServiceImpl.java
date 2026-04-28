package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import com.sky.utils.RedissonUtil;
import com.sky.vo.DishVO;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private SetmealMapper setmealMapper;
    
    @Autowired
    private RedisTemplate redisTemplate;

    @Resource
    private TransactionAwareCacheService transactionAwareCacheService;

    @Value("${sky.cache.shopping-cart-ttl-hours}")
    private Long CART_TTL; // 购物车数据在Redis中的过期时间

    private static final String SHOPPING_CART_KEY_PREFIX = "shoppingcart:";
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
            field = "dish:" + shoppingCartDTO.getDishId();
        } else {
            field = "setmeal:" + shoppingCartDTO.getSetmealId();
        }
        
        // 从Redis中获取购物车商品
        String cartItemJson = (String) redisTemplate.opsForHash().get(cartKey, field);
        
        if (cartItemJson != null) {
            // 商品已存在，更新数量
            ShoppingCart shoppingCart = JSON.parseObject(cartItemJson, ShoppingCart.class);
            shoppingCart.setNumber(shoppingCart.getNumber() + 1);
            // DB 先写，Redis 在事务提交后更新
            shoppingCartMapper.updateNumberById(shoppingCart);
            transactionAwareCacheService.executeAfterCommit(() -> {
                redisTemplate.opsForHash().put(cartKey, field, shoppingCart);
                redisTemplate.expire(cartKey, CART_TTL, TimeUnit.HOURS);
            });
        } else {
            // 商品不存在，添加新商品
            ShoppingCart shoppingCart = new ShoppingCart();
            BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
            shoppingCart.setUserId(userId);

            Long dishId = shoppingCartDTO.getDishId();
            if (dishId != null) {
//                DishVO dishVO = dishService.getByIdWithFlavor(dishId);
                DishVO dishVO = (DishVO) redisTemplate.opsForValue().get("dishDetailCache:" + dishId);
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

            // DB 先写，Redis 在事务提交后更新
            shoppingCartMapper.insert(shoppingCart);
            transactionAwareCacheService.executeAfterCommit(() -> {
                redisTemplate.opsForHash().put(cartKey, field, shoppingCart);
                redisTemplate.expire(cartKey, CART_TTL, TimeUnit.HOURS);
            });
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
                redisTemplate.opsForHash().put(cartKey, field, cart);
            }
        }

        redisTemplate.expire(cartKey, CART_TTL, TimeUnit.HOURS);
        
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
        // 清空数据库中的购物车
        shoppingCartMapper.deleteByUserId(userId);
        // Redis 在事务提交后清空
        transactionAwareCacheService.executeAfterCommit(() ->
            redisTemplate.delete(cartKey));
    }

    /**
     * 删除购物车中一个商品
     * @param shoppingCartDTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
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
                // DB 先删，Redis 在事务提交后删除
                shoppingCartMapper.deleteById(shoppingCart.getId());
                transactionAwareCacheService.executeAfterCommit(() ->
                    redisTemplate.opsForHash().delete(cartKey, field));
            } else {
                // DB 先更新，Redis 在事务提交后更新
                shoppingCart.setNumber(number - 1);
                shoppingCartMapper.updateNumberById(shoppingCart);
                transactionAwareCacheService.executeAfterCommit(() ->
                    redisTemplate.opsForHash().put(cartKey, field, shoppingCart));
            }
        }
    }
}
