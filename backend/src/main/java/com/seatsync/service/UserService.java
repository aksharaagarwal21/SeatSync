package com.seatsync.service;

import com.seatsync.dto.PageResponse;
import com.seatsync.dto.admin.AdminUserResponse;
import com.seatsync.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> listUsers(int page, int size) {
        return PageResponse.of(userRepository.findUserSummaries(PageRequest.of(page, size)));
    }
}
