package com.sky.controller.user;

import com.sky.constant.StatusConstant;
import com.sky.entity.Dish;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@RestController("userDishController")
@RequestMapping("/user/dish")
@Slf4j
@Api(tags = "C端-菜品浏览接口")
public class DishController {
    @Autowired
    private DishService dishService;

    @Resource
    private RBloomFilter<Long> categoryBloomFilter;

    @Resource
    private RBloomFilter<Long> dishBloomFilter;



    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<DishVO>> list(Long categoryId) {
        if (categoryId != null && !categoryBloomFilter.contains(categoryId)) {
            log.info("分类不存在: {}", categoryId);
            return Result.success(new ArrayList<>());
        }

        Dish dish = new Dish();
        dish.setCategoryId(categoryId);
        dish.setStatus(StatusConstant.ENABLE);

        List<DishVO> list = dishService.listWithFlavor(dish);
        return Result.success(list != null ? list : new ArrayList<>());
    }

    @GetMapping("/{id}")
    @ApiOperation("根据菜品ID查询菜品详情")
    public Result<DishVO> getById(@PathVariable Long id) {
        if (!dishBloomFilter.contains(id)) {
            return Result.error("菜品不存在");
        }

        DishVO dishVO = dishService.getByIdWithFlavor(id);
        return Result.success(dishVO);
    }

}
