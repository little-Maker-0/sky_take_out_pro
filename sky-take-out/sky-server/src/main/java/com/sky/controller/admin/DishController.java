package com.sky.controller.admin;


import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
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
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;

/**
 * 菜品管理
 */
@RestController
@RequestMapping("/admin/dish")
@Api(tags = "菜品相关接口")
@Slf4j
public class DishController {

    @Autowired
    private DishService dishService;

    @Autowired
    private RedisTemplate redisTemplate;
    
    @Autowired
    private RedisBloomFilter redisBloomFilter;
    
    // 异步处理线程池
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    /**
     * 新增菜品
     * @param dishDTO
     * @return
     */
    @PostMapping()
    @ApiOperation("新增菜品")
    public Result save(@RequestBody DishDTO dishDTO) {
        log.info("新增菜品：{}", dishDTO);
        Long dishId = dishService.saveWithFlavor(dishDTO);

//        清理缓存数据
        String key = "dish_" + dishDTO.getCategoryId();
        asyncClearCache(key);
        
//        将新菜品ID添加到布隆过滤器
        redisBloomFilter.add("dish", dishId.toString());

        return Result.success();
    }

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @ApiOperation("菜品分页查询")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO) {
        log.info("菜品分页查询:{}", dishPageQueryDTO);
        PageResult pageResult = dishService.pageQuery(dishPageQueryDTO);//后绪步骤定义
        return Result.success(pageResult);
    }

    /**
     * 菜品批量删除
     *
     * @param ids
     * @return
     */
    @DeleteMapping
    @ApiOperation("菜品批量删除")
    public Result delete(@RequestParam List<Long> ids) {
        log.info("菜品批量删除：{}", ids);
        dishService.deleteBatch(ids);

//        将所有的菜品缓存数据清理掉，所有以dish_开头的key
        clearCache("dish_*");
        return Result.success();
    }

    /**
     * 根据id查询菜品
     *
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation("根据id查询菜品")
    public Result<DishVO> getById(@PathVariable Long id) {
        log.info("根据id查询菜品：{}", id);
        DishVO dishVO = dishService.getByIdWithFlavor(id);
        return Result.success(dishVO);
    }

    /**
     * 修改菜品
     *
     * @param dishDTO
     * @return
     */
    @PutMapping
    @ApiOperation("修改菜品")
    public Result update(@RequestBody DishDTO dishDTO) {
        log.info("修改菜品：{}", dishDTO);
        dishService.updateWithFlavor(dishDTO);

        //将所有的菜品缓存数据清理掉，所有以dish_开头的key
        asyncClearCache("dish_*");

        return Result.success();
    }

    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<Dish>> list(Long categoryId) {
        List<Dish> list = dishService.list(categoryId);
        return Result.success(list);
    }

    /**
     * 菜品起售停售
     *
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @ApiOperation("菜品起售停售")
    public Result<String> startOrStop(@PathVariable Integer status, Long id) {
        dishService.startOrStop(status, id);

        //将所有的菜品缓存数据清理掉，所有以dish_开头的key
        asyncClearCache("dish_*");

        return Result.success();
    }

    /**
     * 清理缓存数据
     * @param pattern
     */
    private void clearCache(String pattern) {
        try {
            // 使用SCAN命令替代KEYS命令，避免阻塞Redis
            Set<String> keys = new HashSet<>();
            Cursor<String> cursor = redisTemplate.scan(
                ScanOptions.scanOptions()
                    .match(pattern)
                    .count(100)
                    .build()
            );
            
            while (cursor.hasNext()) {
                keys.add(cursor.next());
                // 每收集1000个键就删除一次
                if (keys.size() >= 1000) {
                    redisTemplate.delete(keys);
                    keys.clear();
                }
            }
            
            // 删除剩余的键
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
            log.info("缓存清理完成，模式：{}", pattern);
        } catch (Exception e) {
            log.error("清理缓存失败: {}", pattern, e);
        }
    }
    
    /**
     * 异步清理缓存数据
     * @param pattern
     */
    private void asyncClearCache(String pattern) {
        executorService.execute(() -> {
            try {
                // 延迟100ms执行，确保数据库事务已提交
                Thread.sleep(100);
                clearCache(pattern);
            } catch (Exception e) {
                log.error("异步清理缓存失败: {}", pattern, e);
            }
        });
    }

}
