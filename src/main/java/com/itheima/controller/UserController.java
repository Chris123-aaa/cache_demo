package com.itheima.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.entity.User;
import com.itheima.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {
    //统一不同的缓存数据
    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private UserService userService;

    /**新增（数据库新增数据，要放入缓存）
     *
     * @CachePut：将方法返回值放入缓存
     *      value：缓存的名称，表示“一类缓存”的意思，每个缓存名称下面可以有多个key
     *      key：缓存的key（用户id作为唯一标识）【#result表示方法的返回值（SpEL语言）】
     */
    @CachePut(value = "userCache",key = "#user.id")
    @PostMapping
    public User save(User user){
        userService.save(user);
        return user;
    }

    /**删除（删除数据库数据，并清除缓存）
     * @CacheEvict：清理指定缓存
     *      value：缓存的名称，表示“一类缓存”的意思，每个缓存名称下面可以有多个key
     *      key：缓存的key（用户id作为唯一标识）【#result表示方法的返回值（SpEL语言）】
     *
     * 四种取key方式效果一样：①#p0    ②#root.args[0]  ③#id    ④#result.id获取返回值
     */
    @CacheEvict(value = "userCache",key = "#p0")  //#p0：取方法第一个参数
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id){
        userService.removeById(id);
    }

    /**更新（更新完数据库数据要清除缓存，避免脏数据）
     *   四种取key方式效果一样：①#p0.id    ②#root.args[0]  ③#id    ④#result.id获取返回值
     */
    @CacheEvict(value = "userCache",key = "#result.id")
    @PutMapping
    public User update(User user){
        userService.updateById(user);
        return user;
    }

    /**根据id查询
     * @Cacheable：在方法执行前spring先查看缓存中是否有数据，如果有数据，则直接返回缓存数据；
     *                                                 若没有数据，调用方法并将方法返回值放到缓存中
     * 注意：condition不允许使用#result获取返回值，unless可以
     *
     *      value：缓存的名称，表示“一类缓存”的意思，每个缓存名称下面可以有多个key
     *      key：缓存的key（用户id作为唯一标识）#result表示方法的返回值（SpEL语言）
     *      condition：条件，满足条件时才缓存数据
     *      unless：满足条件则不缓存（#result == null表示当返回结果为null时不缓存，避免存无用数据，影响整体效果）
     */
    @Cacheable(value = "userCache",key = "#id",unless = "#result == null")
    @GetMapping("/{id}")
    public User getById(@PathVariable Long id){
        User user = userService.getById(id);
        return user;
    }

    @Cacheable(value = "userCache",key = "#user.id + '_' + #user.name")//保证不同的查询数据对应不同的缓存条件
    @GetMapping("/list")
    public List<User> list(User user){
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(user.getId() != null,User::getId,user.getId());
        queryWrapper.eq(user.getName() != null,User::getName,user.getName());
        List<User> list = userService.list(queryWrapper);
        return list;
    }
}
