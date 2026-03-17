package com.fintech.system.auth.Repository;

import com.fintech.system.auth.Model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;


import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);
    Optional<User> findByIdAndSoftDeleteIsFalse(String id);
//    Optional<User> findByUsername(String username);
    boolean existsByEmail(String email);


    Page<User> findBySoftDeleteFalse(Pageable pageable);


}
