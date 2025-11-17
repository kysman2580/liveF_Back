package com.livef.livef_memberservice.member.model.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.livef.livef_memberservice.member.model.entity.MemberEntity;

public interface MemberRepository extends JpaRepository<MemberEntity, Long>{
	boolean existsByMemberId(String memberId);

	Optional<MemberEntity> findByMemberId(String memberId);
}
