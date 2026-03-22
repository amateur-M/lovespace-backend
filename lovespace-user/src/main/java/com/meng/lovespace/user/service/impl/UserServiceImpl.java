package com.meng.lovespace.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.meng.lovespace.user.entity.User;
import com.meng.lovespace.user.mapper.UserMapper;
import com.meng.lovespace.user.service.UserService;
import org.springframework.stereotype.Service;

/**
 * {@link UserService} 默认实现，委托 MyBatis-Plus {@link ServiceImpl} 完成持久化。
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {}

