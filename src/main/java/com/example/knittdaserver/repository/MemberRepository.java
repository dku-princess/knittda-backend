package com.example.knittdaserver.repository;

import com.example.knittdaserver.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Member save(Member member);
}
