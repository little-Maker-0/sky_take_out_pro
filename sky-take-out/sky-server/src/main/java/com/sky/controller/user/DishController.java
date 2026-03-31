package com.sky.controller.user;

import com.sky.constant.StatusConstant;
import com.sky.entity.Dish;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.utils.RedisBloomFilter;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.ArrayList;

@RestController("userDishController")
@RequestMapping("/user/dish")
@Slf4j
@Api(tags = "C端-菜品浏览接口")
public class DishController {
    @Autowired
    private DishService dishService;

    @Autowired
    private RedisTemplate redisTemplate;
    
    @Autowired
    private RedisBloomFilter redisBloomFilter;

    /**
     * 根据分类id查询菜品
     *
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<DishVO>> list(Long categoryId) {
        // 先通过布隆过滤器判断分类是否存在
        if (categoryId != null && !redisBloomFilter.contains("category", categoryId.toString())) {
            log.info("分类不存在: {}", categoryId);
            return Result.success(new ArrayList<>());
        }
        
//        构造redis中的key，规则：dish_分类Id
        String key = "dish_" + categoryId;

//        查询redis中是否存在菜品数据
        List<DishVO> list = (List<DishVO>) redisTemplate.opsForValue().get(key);
        if (list != null && list.size() > 0) {
//            如果存在，直接返回，无需查询数据库
            return Result.success(list);
        }


//        如果不存在，查询数据库，将查询到的数据放入redis中
        Dish dish = new Dish();
        dish.setCategoryId(categoryId);
        dish.setStatus(StatusConstant.ENABLE);//查询起售中的菜品

        list = dishService.listWithFlavor(dish);

//        放入redis
        redisTemplate.opsForValue().set(key, list);
        return Result.success(list);
    }

    /**
     * 根据菜品ID查询菜品详情
     *
     * @param id 菜品ID
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("根据菜品ID查询菜品详情")
    public Result<DishVO> getById(@PathVariable Long id) {
//        先通过布隆过滤器判断菜品是否存在
        if (!redisBloomFilter.contains("dish", id.toString())) {
            return Result.error("菜品不存在");
        }
        
//        构造redis中的key，规则：dish_detail_菜品ID
        String key = "dish_detail_" + id;
        
//        查询redis中是否存在菜品详情数据
        DishVO dishVO = (DishVO) redisTemplate.opsForValue().get(key);
        if (dishVO != null) {
//            如果存在，直接返回，无需查询数据库
            return Result.success(dishVO);
        }
        
//        如果不存在，查询数据库，将查询到的数据放入redis中
        dishVO = dishService.getByIdWithFlavor(id);
        if (dishVO != null) {
//            放入redis
            redisTemplate.opsForValue().set(key, dishVO);
        }
        
        return Result.success(dishVO);
    }

}
