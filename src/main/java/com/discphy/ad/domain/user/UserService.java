package com.discphy.ad.domain.user;

import com.discphy.ad.exception.CoreException;
import com.discphy.ad.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User get(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "사용자가 존재하지 않습니다."));
    }
}
