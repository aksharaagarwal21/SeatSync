package com.seatsync.mapper;

import com.seatsync.dto.auth.UserResponse;
import com.seatsync.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}
