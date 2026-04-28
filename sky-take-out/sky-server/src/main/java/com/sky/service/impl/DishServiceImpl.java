package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.Setmeal;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;

import com.sky.service.RedisCacheService;
import com.sky.vo.DishVO;
import org.redisson.api.RBloomFilter;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

@Service
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    @Autowired
    private SetmealMapper setmealMapper;

    @Resource
    private RBloomFilter<Long> dishBloomFilter;

    @Resource
    private RedisCacheService redisCacheServiceImpl;

    /**
     * 新增菜品
     * @param dishDTO
     * @return 新创建的菜品ID
     */
    @Override
    @Transactional
    public Long saveWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);

//        向菜品表插入1条数据
        dishMapper.insert(dish);

//        获取insert语句生成的主键值
        Long dishId = dish.getId();

        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() > 0) {
            flavors.forEach(dishFlavor -> dishFlavor.setDishId(dishId));
//            向口味表插入n条数据
            dishFlavorMapper.insertBatch(flavors);
        }

        // todo 优化：调用线程池异步清理
        redisCacheServiceImpl.clearCacheAsync("dishListCache*");
        redisCacheServiceImpl.clearCacheAsync("dishDetailCache*");
        redisCacheServiceImpl.clearCacheAsync("setmealCache*");
        dishBloomFilter.add(dishId);
        return dishId;
    }

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO> page=dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 菜品批量删除
     * @param ids
     */
    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
//        判断当前菜品是否能够删除---是否存在起售中的菜品？？
        ids.forEach(id->{
            Dish dish = dishMapper.getById(id);
            if (dish.getStatus() == StatusConstant.ENABLE) {
//                当前菜品处于起售中，不能删除
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        });

//        判断当前菜品是否能够删除---是否被套餐关联了？？
        List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);
        if (setmealIds != null && setmealIds.size() > 0) {
//            当前菜品被套餐关联了，不能删除
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

//        删除菜品表中的菜品数据
        ids.forEach(id->{
            dishMapper.deleteById(id);

//            删除菜单关联的口味数据
            dishFlavorMapper.deleteByDishId(id);
        });

        // todo 优化：调用线程池异步清理，一般只在非主业务营业时间内执行，数据时效性要求不高
        redisCacheServiceImpl.clearCacheAsync("dishListCache*");
        redisCacheServiceImpl.clearCacheAsync("dishDetailCache*");
        redisCacheServiceImpl.clearCacheAsync("setmealCache*");
    }

    /**
     * 根据id查询菜品
     * @param id
     * @return
     */
    @Override
    @Cacheable(cacheNames = "dishDetailCache", key = "#id")
    public DishVO getByIdWithFlavor(Long id) {
//        根据id查询菜品数据
        Dish dish = dishMapper.getById(id);

        // 检查菜品是否存在
        if (dish == null) {
            throw new com.sky.exception.DishException(com.sky.constant.MessageConstant.DISH_NOT_FOUND);
        }

//        根据菜品id查询口味数据
        List<DishFlavor> dishFlavorList = dishFlavorMapper.getByDishId(id);

//        将查询到的数据封装到vo
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(dishFlavorList);

        return dishVO;
    }

    /**
     * 修改菜品
     * @param dishDTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);

//        修改菜品基本信息
        dishMapper.update(dish);

//        删除原有的口味信息
        dishFlavorMapper.deleteByDishId(dishDTO.getId());

//        重新插入口味数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() > 0) {
            flavors.forEach(dishFlavor -> dishFlavor.setDishId(dishDTO.getId()));

//            向口味表插入n条数据
            dishFlavorMapper.insertBatch(flavors);
        }

        // todo 技术简化：自定义一个线程池专门处理异步清理
        redisCacheServiceImpl.clearCacheAsync("dishListCache*");
        redisCacheServiceImpl.clearCacheAsync("dishDetailCache*");
        redisCacheServiceImpl.clearCacheAsync("setmealCache*");
    }

    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    // todo 大键优化
    @Override
    @Cacheable(cacheNames = "dishListCache", key = "#categoryId")
    public List<Dish> list(Long categoryId) {
        Dish dish = Dish.builder()
                .categoryId(categoryId)
                .status(StatusConstant.ENABLE)
                .build();
        return dishMapper.list(dish);
    }

    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    @Override
    @Cacheable(
            cacheNames = "dishListCache",
            key = "#dish.categoryId + ':' + (T(java.util.Objects).toString(#dish.status))")
    public List<DishVO> listWithFlavor(Dish dish) {
        List<Dish> dishList = dishMapper.list(dish);

        ArrayList<DishVO> dishVOArrayList = new ArrayList<>();

        dishList.forEach(d->{
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d, dishVO);

//            根据菜品id查询对应的口味
            List<DishFlavor> flavors = dishFlavorMapper.getByDishId(d.getId());

            dishVO.setFlavors(flavors);
            dishVOArrayList.add(dishVO);
        });

        return dishVOArrayList;
    }


    // todo 超买超卖问题
    /**
     * 菜品起售停售
     * @param status
     * @param id
     */
    @Override
    @Transactional
    public void startOrStop(Integer status, Long id) {
        Dish dish = Dish.builder()
                .id(id)
                .status(status)
                .build();
        dishMapper.update(dish);

        if (status == StatusConstant.DISABLE) {
            // 如果是停售操作，还需要将包含当前菜品的套餐也停售
            List<Long> dishIds = new ArrayList<>();
            dishIds.add(id);
            // select setmeal_id from setmeal_dish where dish_id in (?,?,?)
            List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(dishIds);
            if (setmealIds != null && setmealIds.size() > 0) {
                for (Long setmealId : setmealIds) {
                    Setmeal setmeal = Setmeal.builder()
                            .id(setmealId)
                            .status(StatusConstant.DISABLE)
                            .build();
                    setmealMapper.update(setmeal);
                }
            }
        }

        // todo 改成同步清理，数据的时效性要求较高，需要将菜品最新状态及时反映到缓存中，防止生成“无效订单”
        redisCacheServiceImpl.clearCacheSync("dishListCache*");
        redisCacheServiceImpl.clearCacheSync("dishDetailCache*");
        redisCacheServiceImpl.clearCacheSync("setmealCache*");
    }

}
