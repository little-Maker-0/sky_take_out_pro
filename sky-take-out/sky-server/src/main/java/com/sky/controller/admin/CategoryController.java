package com.sky.controller.admin;

import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.CategoryService;
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
import com.sky.utils.RedisBloomFilter;

/**
 * 分类管理
 */
@RestController
@RequestMapping("/admin/category")
@Api(tags = "分类相关接口")
@Slf4j
public class CategoryController {

    @Autowired
    private CategoryService categoryService;
    
    @Autowired
    private RedisBloomFilter redisBloomFilter;
    
    @Autowired
    private RedisTemplate redisTemplate;
    
    // 异步处理线程池
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    /**
     * 新增分类
     * @param categoryDTO
     * @return
     */
    @PostMapping
    @ApiOperation("新增分类")
    public Result<String> save(@RequestBody CategoryDTO categoryDTO){
        log.info("新增分类：{}", categoryDTO);
        Long categoryId = categoryService.save(categoryDTO);
        
        // 将新分类ID添加到布隆过滤器
        redisBloomFilter.add("category", categoryId.toString());
        log.info("分类ID {} 已添加到布隆过滤器", categoryId);
        
        return Result.success();
    }

    /**
     * 分类分页查询
     * @param categoryPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @ApiOperation("分类分页查询")
    public Result<PageResult> page(CategoryPageQueryDTO categoryPageQueryDTO){
        log.info("分页查询：{}", categoryPageQueryDTO);
        PageResult pageResult = categoryService.pageQuery(categoryPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 删除分类
     * @param id
     * @return
     */
    @DeleteMapping
    @ApiOperation("删除分类")
    public Result<String> deleteById(Long id){
        log.info("删除分类：{}", id);
        categoryService.deleteById(id);
        
        // 同步清理缓存，确保缓存立即被删除
        clearCache("dish_*" + id);
        clearCache("dish_*" + id + "*");
        
        return Result.success();
    }

    /**
     * 修改分类
     * @param categoryDTO
     * @return
     */
    @PutMapping
    @ApiOperation("修改分类")
    public Result<String> update(@RequestBody CategoryDTO categoryDTO){
        categoryService.update(categoryDTO);
        
        // 异步清理缓存，提高响应速度
        asyncClearCache("dish_*" + categoryDTO.getId());
        asyncClearCache("dish_*" + categoryDTO.getId() + "*");
        
        return Result.success();
    }

    /**
     * 启用、禁用分类
     * @param status
     * @param id
     * @return
     */
    @PostMapping("/status/{status}")
    @ApiOperation("启用禁用分类")
    public Result<String> startOrStop(@PathVariable("status") Integer status, Long id){
        categoryService.startOrStop(status,id);
        
        // 异步清理缓存，提高响应速度
        asyncClearCache("dish_*" + id);
        asyncClearCache("dish_*" + id + "*");
        
        return Result.success();
    }

    /**
     * 根据类型查询分类
     * @param type
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据类型查询分类")
    public Result<List<Category>> list(Integer type){
        List<Category> list = categoryService.list(type);
        return Result.success(list);
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
