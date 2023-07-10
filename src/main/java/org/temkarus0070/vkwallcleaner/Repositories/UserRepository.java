package org.temkarus0070.vkwallcleaner.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.temkarus0070.vkwallcleaner.entities.User;

@Repository
public interface UserRepository extends JpaRepository<User,Integer> {}
